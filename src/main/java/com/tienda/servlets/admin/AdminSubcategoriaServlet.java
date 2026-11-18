package com.tienda.servlets.admin;

import com.tienda.dao.CategoriaAdminDAO;
import com.tienda.dao.CategoriaDAO;
import com.tienda.modelos.Subcategoria;
import com.tienda.servlets.ApiServlet;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/admin/subcategorias/*")
public class AdminSubcategoriaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final transient CategoriaAdminDAO categoriaAdminDAO = new CategoriaAdminDAO();

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        categoriaAdminDAO.insertarSubcategoria(validar(leer(request, Subcategoria.class)));
        creado(response, categoriaDAO.listar());
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        categoriaAdminDAO.actualizarSubcategoria(id(ruta.get(0)), validar(leer(request, Subcategoria.class)));
        ok(response, categoriaDAO.listar());
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        categoriaAdminDAO.eliminarSubcategoria(id(ruta.get(0)));
        sinContenido(response);
    }

    private Subcategoria validar(Subcategoria subcategoria) throws Exception {
        int idCategoria = Validar.id(subcategoria.getIdCategoria(), "idCategoria");
        if (!categoriaAdminDAO.existeCategoria(idCategoria)) {
            throw ApiExcepcion.solicitudInvalida("La categoría no existe");
        }
        subcategoria.setNombre(Validar.nombre(subcategoria.getNombre(), "nombre", 60));
        subcategoria.setDescripcion(Validar.textoOpcional(subcategoria.getDescripcion(), "descripcion", 200));
        return subcategoria;
    }
}
