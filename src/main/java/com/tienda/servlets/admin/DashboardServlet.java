package com.tienda.servlets.admin;

import com.tienda.dao.DashboardDAO;
import com.tienda.servlets.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/admin/dashboard")
public class DashboardServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient DashboardDAO dashboardDAO = new DashboardDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ok(response, dashboardDAO.resumen());
    }
}
