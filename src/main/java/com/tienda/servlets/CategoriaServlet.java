package com.tienda.servlets;

import com.tienda.dao.CategoriaDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/categorias")
public class CategoriaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ok(response, categoriaDAO.listar());
    }
}
