package com.tienda.util;

import com.tienda.modelos.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class Sesion {

    public static final String USUARIO = "usuario";
    public static final String ROL_ADMINISTRADOR = "administrador";
    public static final String ROL_EMPLEADO = "empleado";

    private Sesion() {
    }

    public static void iniciar(HttpServletRequest request, Usuario usuario) {
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        request.getSession(true).setAttribute(USUARIO, usuario);
    }

    public static void cerrar(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
    }

    public static Usuario usuarioActual(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return sesion == null ? null : (Usuario) sesion.getAttribute(USUARIO);
    }

    public static Usuario requerirUsuario(HttpServletRequest request) {
        Usuario usuario = usuarioActual(request);
        if (usuario == null) {
            throw ApiExcepcion.noAutenticado();
        }
        return usuario;
    }

    public static boolean esPersonal(Usuario usuario) {
        return usuario != null
                && (ROL_ADMINISTRADOR.equals(usuario.getRol()) || ROL_EMPLEADO.equals(usuario.getRol()));
    }

    public static boolean esAdministrador(Usuario usuario) {
        return usuario != null && ROL_ADMINISTRADOR.equals(usuario.getRol());
    }
}
