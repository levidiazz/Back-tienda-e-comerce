package com.tienda.servlets;

import com.tienda.dao.MetodoPagoDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/metodos-pago")
public class MetodoPagoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ok(response, metodoPagoDAO.listarActivos());
    }
}
