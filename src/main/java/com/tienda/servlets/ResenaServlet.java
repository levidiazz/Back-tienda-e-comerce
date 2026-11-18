package com.tienda.servlets;

import com.tienda.dao.ProductoDAO;
import com.tienda.dao.ResenaDAO;
import com.tienda.modelos.Resena;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/resenas/*")
public class ResenaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient ResenaDAO resenaDAO = new ResenaDAO();
    private final transient ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            String sku = Validar.sku(request.getParameter("sku"));
            ok(response, resenaDAO.listarPorProducto(sku));
        } else if (ruta.equals(List.of("mias"))) {
            Usuario usuario = Sesion.requerirUsuario(request);
            ok(response, resenaDAO.listarPorUsuario(usuario.getIdUsuario()));
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
        Resena resena = validar(leer(request, Resena.class));
        resena.setSku(Validar.sku(resena.getSku()));
        if (productoDAO.buscar(resena.getSku(), true) == null) {
            throw ApiExcepcion.noEncontrado("El producto no existe");
        }
        if (!resenaDAO.compraEntregada(usuario.getIdUsuario(), resena.getSku())) {
            throw ApiExcepcion.prohibido("Solo puedes opinar sobre productos que ya te entregamos");
        }
        int id = resenaDAO.insertar(usuario.getIdUsuario(), resena);
        resena.setIdResena(id);
        resena.setIdUsuario(usuario.getIdUsuario());
        creado(response, resena);
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        resenaDAO.actualizar(usuario.getIdUsuario(), id(ruta.get(0)), validar(leer(request, Resena.class)));
        ok(response, mensaje("Reseña actualizada"));
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        resenaDAO.eliminar(id(ruta.get(0)), usuario.getIdUsuario());
        sinContenido(response);
    }

    private Resena validar(Resena resena) {
        resena.setCalificacion(Validar.entero(resena.getCalificacion(), "calificacion", 1, 5));
        resena.setComentario(Validar.textoLargoOpcional(resena.getComentario(), "comentario", 500));
        return resena;
    }
}
