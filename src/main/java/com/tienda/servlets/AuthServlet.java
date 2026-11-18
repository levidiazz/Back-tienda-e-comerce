package com.tienda.servlets;

import com.tienda.dao.TokenDAO;
import com.tienda.dao.UsuarioDAO;
import com.tienda.modelos.Credenciales;
import com.tienda.modelos.Restablecimiento;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Contrasenas;
import com.tienda.util.IntentosLogin;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/auth/*")
public class AuthServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final transient TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!segmentos(request).equals(List.of("sesion"))) {
            throw noEncontrada();
        }
        ok(response, Sesion.requerirUsuario(request));
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        switch (ruta.get(0)) {
            case "registro" -> registrar(request, response);
            case "login" -> iniciarSesion(request, response);
            case "logout" -> {
                Sesion.cerrar(request);
                sinContenido(response);
            }
            case "recuperar" -> recuperar(request, response);
            case "restablecer" -> restablecer(request, response);
            default -> throw noEncontrada();
        }
    }

    private void registrar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Usuario nuevo = leer(request, Usuario.class);
        Validar.datosUsuario(nuevo);
        Validar.contrasenaNueva(nuevo.getContrasena(), "contrasena");
        if (usuarioDAO.buscarPorEmail(nuevo.getEmail()) != null) {
            throw ApiExcepcion.conflicto("Ya existe una cuenta con ese correo");
        }
        int id = usuarioDAO.registrar(nuevo, Contrasenas.cifrar(nuevo.getContrasena()));
        Usuario usuario = usuarioDAO.buscarPorId(id);
        Sesion.iniciar(request, usuario);
        creado(response, usuario);
    }

    private void iniciarSesion(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Credenciales credenciales = leer(request, Credenciales.class);
        String email = Validar.email(credenciales.getEmail());
        String contrasena = Validar.contrasenaIngresada(credenciales.getContrasena(), "contrasena");
        IntentosLogin.verificar(email);
        Usuario usuario = usuarioDAO.buscarPorEmail(email);
        if (usuario == null || !Contrasenas.coincide(contrasena, usuarioDAO.obtenerHash(usuario.getIdUsuario()))) {
            IntentosLogin.registrarFallo(email);
            throw new ApiExcepcion(401, "Correo o contraseña incorrectos");
        }
        if (!usuario.getActivo()) {
            throw ApiExcepcion.prohibido("Tu cuenta está desactivada");
        }
        IntentosLogin.limpiar(email);
        Sesion.iniciar(request, usuario);
        ok(response, usuario);
    }

    private void recuperar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Credenciales datos = leer(request, Credenciales.class);
        Usuario usuario = usuarioDAO.buscarPorEmail(Validar.email(datos.getEmail()));
        if (usuario != null && usuario.getActivo()) {
            String token = tokenDAO.crear(usuario.getIdUsuario());
            log("Enlace de recuperación para " + usuario.getEmail() + ": #/cuenta/restablecer?token=" + token);
        }
        ok(response, mensaje("Si el correo está registrado, recibirás un enlace para crear una nueva contraseña"));
    }

    private void restablecer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Restablecimiento datos = leer(request, Restablecimiento.class);
        String token = Validar.patron(datos.getToken(), "token", 64, 64, Validar.TOKEN, "enlace incompleto");
        String contrasena = Validar.contrasenaNueva(datos.getContrasena(), "contrasena");
        Integer idUsuario = tokenDAO.consumir(token);
        if (idUsuario == null) {
            throw ApiExcepcion.solicitudInvalida("El enlace no es válido o ya venció");
        }
        usuarioDAO.actualizarHash(idUsuario, Contrasenas.cifrar(contrasena));
        ok(response, mensaje("Tu contraseña se actualizó"));
    }
}
