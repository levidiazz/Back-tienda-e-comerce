package com.tienda.servlets.admin;

import com.tienda.dao.ResenaDAO;
import com.tienda.servlets.ApiServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/admin/resenas/*")
public class AdminResenaServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient ResenaDAO resenaDAO = new ResenaDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        ok(response, resenaDAO.listarTodas());
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        resenaDAO.eliminar(id(ruta.get(0)), null);
        sinContenido(response);
    }
}
