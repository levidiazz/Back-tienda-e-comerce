package com.tienda.servlets;

import com.tienda.dao.ProductoDAO;
import com.tienda.modelos.Producto;
import com.tienda.util.ApiExcepcion;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/productos/*")
public class ProductoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    public static final List<String> ORDENES = List.of("precio_asc", "precio_desc", "recientes", "calificacion", "nombre");

    private final transient ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, productoDAO.listar(
                    parametroId(request, "categoria"),
                    parametroId(request, "subcategoria"),
                    parametroTexto(request, "q", 60),
                    parametroBooleano(request, "ofertas"),
                    false,
                    parametroOpcion(request, "orden", ORDENES)));
        } else if (ruta.size() == 1) {
            Producto producto = productoDAO.buscar(sku(ruta.get(0)), false);
            if (producto == null) {
                throw ApiExcepcion.noEncontrado("El producto no existe");
            }
            ok(response, producto);
        } else {
            throw noEncontrada();
        }
    }
}
