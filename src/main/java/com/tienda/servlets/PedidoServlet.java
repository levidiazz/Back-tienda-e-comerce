package com.tienda.servlets;

import com.tienda.dao.PagoDAO;
import com.tienda.dao.PedidoDAO;
import com.tienda.modelos.ArticuloPedido;
import com.tienda.modelos.NuevoPedido;
import com.tienda.modelos.Pedido;
import com.tienda.modelos.SolicitudPago;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@WebServlet("/api/pedidos/*")
public class PedidoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final int MAXIMO_ARTICULOS = 50;
    private static final int MAXIMO_POR_ARTICULO = 20;
    private static final Pattern TARJETA = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern VENCIMIENTO = Pattern.compile("^(0[1-9]|1[0-2])/[0-9]{2}$");
    private static final Pattern CVV = Pattern.compile("^[0-9]{3,4}$");
    private static final Pattern REFERENCIA_PAYPAL = Pattern.compile("^[A-Z0-9-]{8,40}$");

    private final transient PedidoDAO pedidoDAO = new PedidoDAO();
    private final transient PagoDAO pagoDAO = new PagoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, pedidoDAO.listarPorUsuario(usuario.getIdUsuario()));
        } else if (ruta.size() == 1) {
            ok(response, pedidoPropio(usuario, id(ruta.get(0))));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            NuevoPedido nuevo = leer(request, NuevoPedido.class);
            validar(nuevo);
            int idPedido = pedidoDAO.crear(usuario.getIdUsuario(), nuevo);
            creado(response, pedidoDAO.buscar(idPedido));
        } else if (ruta.size() == 2 && ruta.get(1).equals("pago")) {
            int idPedido = id(ruta.get(0));
            SolicitudPago solicitud = leer(request, SolicitudPago.class);
            int idMetodo = Validar.id(solicitud.getIdMetodo(), "idMetodo");
            String metodo = pagoDAO.nombreMetodoActivo(idMetodo);
            if (metodo == null) {
                throw ApiExcepcion.solicitudInvalida("El método de pago no está disponible");
            }
            String referencia = metodo.toLowerCase().contains("tarjeta") ? validarTarjeta(solicitud) : validarPaypal(solicitud);
            creado(response, pagoDAO.registrar(usuario.getIdUsuario(), idPedido, idMetodo, referencia));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void patch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 2 || !ruta.get(1).equals("cancelar")) {
            throw noEncontrada();
        }
        int idPedido = id(ruta.get(0));
        pedidoDAO.cancelarPorCliente(usuario.getIdUsuario(), idPedido);
        ok(response, pedidoDAO.buscar(idPedido));
    }

    private Pedido pedidoPropio(Usuario usuario, int idPedido) throws Exception {
        Pedido pedido = pedidoDAO.buscar(idPedido);
        if (pedido == null || !pedido.getIdUsuario().equals(usuario.getIdUsuario())) {
            throw ApiExcepcion.noEncontrado("El pedido no existe");
        }
        return pedido;
    }

    private void validar(NuevoPedido nuevo) {
        Validar.id(nuevo.getIdDireccion(), "idDireccion");
        List<ArticuloPedido> articulos = Validar.lista(nuevo.getArticulos(), "articulos", 1, MAXIMO_ARTICULOS);
        Set<String> repetidos = new HashSet<>();
        for (int i = 0; i < articulos.size(); i++) {
            ArticuloPedido articulo = articulos.get(i);
            String campo = "articulos[" + i + "]";
            articulo.setSku(Validar.sku(articulo.getSku()));
            articulo.setIdVariante(Validar.idOpcional(articulo.getIdVariante(), campo + ".idVariante"));
            articulo.setCantidad(Validar.entero(articulo.getCantidad(), campo + ".cantidad", 1, MAXIMO_POR_ARTICULO));
            if (!repetidos.add(articulo.getSku() + "#" + articulo.getIdVariante())) {
                throw ApiExcepcion.solicitudInvalida("El producto " + articulo.getSku() + " está repetido; usa la cantidad");
            }
        }
    }

    private String validarTarjeta(SolicitudPago solicitud) {
        String numero = Validar.requerido(solicitud.getNumeroTarjeta(), "numeroTarjeta").replaceAll("[\\s-]", "");
        if (!TARJETA.matcher(numero).matches() || !luhn(numero)) {
            throw ApiExcepcion.solicitudInvalida("El número de tarjeta no es válido");
        }
        Validar.nombre(solicitud.getTitular(), "titular", 80);
        String vencimiento = Validar.patron(solicitud.getVencimiento(), "vencimiento", 5, 5, VENCIMIENTO, "formato MM/AA");
        YearMonth fecha = YearMonth.of(2000 + Integer.parseInt(vencimiento.substring(3)), Integer.parseInt(vencimiento.substring(0, 2)));
        if (fecha.isBefore(YearMonth.now())) {
            throw ApiExcepcion.solicitudInvalida("La tarjeta está vencida");
        }
        Validar.patron(solicitud.getCvv(), "cvv", 3, 4, CVV, "3 o 4 dígitos");
        return "**** " + numero.substring(numero.length() - 4);
    }

    private String validarPaypal(SolicitudPago solicitud) {
        return Validar.patron(solicitud.getReferenciaPaypal(), "referenciaPaypal", 8, 40, REFERENCIA_PAYPAL,
                "folio de PayPal en mayúsculas y números");
    }

    private boolean luhn(String numero) {
        int suma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }
}
