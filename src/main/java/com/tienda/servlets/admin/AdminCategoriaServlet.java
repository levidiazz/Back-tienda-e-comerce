package com.tienda.servlets.admin;

import com.tienda.dao.CategoriaAdminDAO;
import com.tienda.dao.CategoriaDAO;
import com.tienda.modelos.Categoria;
import com.tienda.servlets.ApiServlet;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/admin/categorias/*")
public class AdminCategoriaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final transient CategoriaAdminDAO categoriaAdminDAO = new CategoriaAdminDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        ok(response, categoriaDAO.listar());
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        categoriaAdminDAO.insertar(validar(leer(request, Categoria.class)));
        creado(response, categoriaDAO.listar());
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        categoriaAdminDAO.actualizar(id(ruta.get(0)), validar(leer(request, Categoria.class)));
        ok(response, categoriaDAO.listar());
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        categoriaAdminDAO.eliminar(id(ruta.get(0)));
        sinContenido(response);
    }

    private Categoria validar(Categoria categoria) {
        categoria.setNombre(Validar.nombre(categoria.getNombre(), "nombre", 60));
        categoria.setDescripcion(Validar.textoOpcional(categoria.getDescripcion(), "descripcion", 200));
        return categoria;
    }
}
