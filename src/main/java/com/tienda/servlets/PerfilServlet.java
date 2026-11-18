package com.tienda.servlets;

import com.tienda.dao.UsuarioDAO;
import com.tienda.modelos.CambioContrasena;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Contrasenas;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/perfil/*")
public class PerfilServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario actual = Sesion.requerirUsuario(request);
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        ok(response, usuarioDAO.buscarPorId(actual.getIdUsuario()));
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario actual = Sesion.requerirUsuario(request);
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            Usuario datos = leer(request, Usuario.class);
            Validar.datosUsuario(datos);
            Usuario otro = usuarioDAO.buscarPorEmail(datos.getEmail());
            if (otro != null && !otro.getIdUsuario().equals(actual.getIdUsuario())) {
                throw ApiExcepcion.conflicto("Ya existe una cuenta con ese correo");
            }
            usuarioDAO.actualizarDatos(actual.getIdUsuario(), datos);
            Usuario actualizado = usuarioDAO.buscarPorId(actual.getIdUsuario());
            request.getSession().setAttribute(Sesion.USUARIO, actualizado);
            ok(response, actualizado);
        } else if (ruta.equals(List.of("contrasena"))) {
            CambioContrasena cambio = leer(request, CambioContrasena.class);
            String actualIngresada = Validar.contrasenaIngresada(cambio.getActual(), "actual");
            String nueva = Validar.contrasenaNueva(cambio.getNueva(), "nueva");
            if (actualIngresada.equals(nueva)) {
                throw ApiExcepcion.solicitudInvalida("La nueva contraseña debe ser diferente a la actual");
            }
            if (!Contrasenas.coincide(actualIngresada, usuarioDAO.obtenerHash(actual.getIdUsuario()))) {
                throw ApiExcepcion.solicitudInvalida("La contraseña actual no es correcta");
            }
            usuarioDAO.actualizarHash(actual.getIdUsuario(), Contrasenas.cifrar(nueva));
            ok(response, mensaje("Tu contraseña se actualizó"));
        } else {
            throw noEncontrada();
        }
    }
}
