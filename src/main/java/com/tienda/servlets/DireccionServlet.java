package com.tienda.servlets;

import com.tienda.dao.DireccionDAO;
import com.tienda.modelos.Direccion;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/direcciones/*")
public class DireccionServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final int MAXIMO_DIRECCIONES = 10;

    private final transient DireccionDAO direccionDAO = new DireccionDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, direccionDAO.listarPorUsuario(usuario.getIdUsuario()));
        } else if (ruta.size() == 1) {
            ok(response, propia(usuario, id(ruta.get(0))));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        Direccion direccion = validar(leer(request, Direccion.class));
        if (direccionDAO.contarPorUsuario(usuario.getIdUsuario()) >= MAXIMO_DIRECCIONES) {
            throw ApiExcepcion.conflicto("Puedes guardar máximo " + MAXIMO_DIRECCIONES + " direcciones");
        }
        int id = direccionDAO.insertar(usuario.getIdUsuario(), direccion);
        creado(response, direccionDAO.buscar(usuario.getIdUsuario(), id));
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        int id = id(ruta.get(0));
        direccionDAO.actualizar(usuario.getIdUsuario(), id, validar(leer(request, Direccion.class)));
        ok(response, direccionDAO.buscar(usuario.getIdUsuario(), id));
    }

    @Override
    protected void patch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 2 || !ruta.get(1).equals("principal")) {
            throw noEncontrada();
        }
        direccionDAO.marcarPrincipal(usuario.getIdUsuario(), id(ruta.get(0)));
        ok(response, direccionDAO.listarPorUsuario(usuario.getIdUsuario()));
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        direccionDAO.eliminar(usuario.getIdUsuario(), id(ruta.get(0)));
        sinContenido(response);
    }

    private Direccion propia(Usuario usuario, int idDireccion) throws Exception {
        Direccion direccion = direccionDAO.buscar(usuario.getIdUsuario(), idDireccion);
        if (direccion == null) {
            throw ApiExcepcion.noEncontrado("La dirección no existe");
        }
        return direccion;
    }

    private Direccion validar(Direccion d) {
        d.setCalle(Validar.texto(d.getCalle(), "calle", 3, 100));
        d.setNumero(Validar.patron(d.getNumero(), "numero", 1, 10, Validar.NUMERO_EXTERIOR, "letras, números, espacios, / o -"));
        d.setColonia(Validar.texto(d.getColonia(), "colonia", 3, 80));
        d.setCiudad(Validar.nombre(d.getCiudad(), "ciudad", 80));
        d.setEstado(Validar.opcion(d.getEstado(), "estado", Validar.ESTADOS_MEXICO));
        d.setCodigoPostal(Validar.patron(d.getCodigoPostal(), "codigoPostal", 5, 5, Validar.CODIGO_POSTAL, "5 dígitos"));
        d.setReferencias(Validar.textoOpcional(d.getReferencias(), "referencias", 200));
        d.setEsPrincipal(Validar.booleano(d.getEsPrincipal(), false));
        return d;
    }
}
