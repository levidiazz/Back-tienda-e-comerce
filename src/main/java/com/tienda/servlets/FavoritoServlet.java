package com.tienda.servlets;

import com.tienda.dao.FavoritoDAO;
import com.tienda.dao.ProductoDAO;
import com.tienda.modelos.Favorito;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/favoritos/*")
public class FavoritoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient FavoritoDAO favoritoDAO = new FavoritoDAO();
    private final transient ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        ok(response, favoritoDAO.listar(usuario.getIdUsuario()));
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        String sku = Validar.sku(leer(request, Favorito.class).getSku());
        if (productoDAO.buscar(sku, false) == null) {
            throw ApiExcepcion.noEncontrado("El producto no existe");
        }
        favoritoDAO.agregar(usuario.getIdUsuario(), sku);
        creado(response, favoritoDAO.listar(usuario.getIdUsuario()));
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario usuario = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        favoritoDAO.quitar(usuario.getIdUsuario(), sku(ruta.get(0)));
        sinContenido(response);
    }
}
