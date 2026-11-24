package com.tienda.servlets.admin;

import com.tienda.dao.PedidoDAO;
import com.tienda.modelos.CambioEstadoPedido;
import com.tienda.modelos.Pedido;
import com.tienda.servlets.ApiServlet;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/admin/pedidos/*")
public class AdminPedidoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, pedidoDAO.listarTodos(parametroOpcion(request, "estado", PedidoDAO.ESTADOS)));
        } else if (ruta.size() == 1) {
            Pedido pedido = pedidoDAO.buscar(id(ruta.get(0)));
            if (pedido == null) {
                throw ApiExcepcion.noEncontrado("El pedido no existe");
            }
            ok(response, pedido);
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void patch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        int idPedido = id(ruta.get(0));
        CambioEstadoPedido cambio = leer(request, CambioEstadoPedido.class);
        cambio.setEstado(Validar.opcion(cambio.getEstado(), "estado", PedidoDAO.ESTADOS));
        if ("enviado".equals(cambio.getEstado())) {
            cambio.setPaqueteria(Validar.texto(cambio.getPaqueteria(), "paqueteria", 2, 40));
            cambio.setNumeroGuia(Validar.patron(cambio.getNumeroGuia(), "numeroGuia", 5, 40, Validar.GUIA,
                    "letras, números y guiones"));
        } else {
            cambio.setPaqueteria(Validar.textoOpcional(cambio.getPaqueteria(), "paqueteria", 40));
            cambio.setNumeroGuia(Validar.patronOpcional(cambio.getNumeroGuia(), "numeroGuia", 40, Validar.GUIA,
                    "letras, números y guiones"));
        }
        pedidoDAO.cambiarEstado(idPedido, cambio);
        ok(response, pedidoDAO.buscar(idPedido));
    }
}
