package com.tienda.servlets.admin;

import com.tienda.dao.UsuarioDAO;
import com.tienda.modelos.CambioUsuario;
import com.tienda.modelos.Usuario;
import com.tienda.servlets.ApiServlet;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Sesion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/admin/usuarios/*")
public class AdminUsuarioServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, usuarioDAO.listar());
        } else if (ruta.size() == 1) {
            Usuario usuario = usuarioDAO.buscarPorId(id(ruta.get(0)));
            if (usuario == null) {
                throw ApiExcepcion.noEncontrado("El usuario no existe");
            }
            ok(response, usuario);
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void patch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        Usuario actual = Sesion.requerirUsuario(request);
        if (!Sesion.esAdministrador(actual)) {
            throw ApiExcepcion.prohibido("Solo un administrador puede cambiar usuarios");
        }
        int idUsuario = id(ruta.get(0));
        if (idUsuario == actual.getIdUsuario()) {
            throw ApiExcepcion.conflicto("No puedes cambiar tu propio rol o estado");
        }
        CambioUsuario cambio = leer(request, CambioUsuario.class);
        if (cambio.getIdRol() == null && cambio.getActivo() == null) {
            throw ApiExcepcion.solicitudInvalida("Envía idRol, activo o ambos");
        }
        Integer idRol = Validar.idOpcional(cambio.getIdRol(), "idRol");
        if (idRol != null && !usuarioDAO.existeRol(idRol)) {
            throw ApiExcepcion.solicitudInvalida("El rol no existe");
        }
        usuarioDAO.actualizarRolYEstado(idUsuario, idRol, cambio.getActivo());
        ok(response, usuarioDAO.buscarPorId(idUsuario));
    }
}
