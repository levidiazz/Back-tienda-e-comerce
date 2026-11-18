package com.tienda.filtros;

import com.tienda.conexion.Configuracion;
import com.tienda.modelos.Usuario;
import com.tienda.util.Sesion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/api/*")
public class ApiFiltro extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Cache-Control", "no-store");
        String origen = request.getHeader("Origin");
        String permitido = Configuracion.obtener("cors.origen", "http://localhost:4200");
        if (origen != null && origen.equals(permitido)) {
            response.setHeader("Access-Control-Allow-Origin", origen);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Vary", "Origin");
        }
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(204);
            return;
        }
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (ruta.startsWith("/api/admin")) {
            Usuario usuario = Sesion.usuarioActual(request);
            if (usuario == null) {
                rechazar(response, 401, "Debes iniciar sesión");
                return;
            }
            if (!Sesion.esPersonal(usuario)) {
                rechazar(response, 403, "No tienes permiso para entrar al panel");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + mensaje + "\"}");
    }
}
