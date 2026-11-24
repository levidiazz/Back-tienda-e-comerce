package com.tienda.servlets.admin;

import com.tienda.dao.PagoDAO;
import com.tienda.servlets.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/admin/pagos")
public class AdminPagoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient PagoDAO pagoDAO = new PagoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ok(response, pagoDAO.listar());
    }
}
