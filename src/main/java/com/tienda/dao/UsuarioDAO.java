package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Usuario;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Cifrado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static final String SELECT_BASE = """
            SELECT u.id_usuario, u.id_rol, r.nombre AS rol, u.nombre, u.apellidos, u.email, u.telefono,
                   u.fecha_nacimiento, u.fecha_registro, u.activo
              FROM usuarios u
              JOIN roles r ON r.id_rol = u.id_rol
            """;

    public Usuario buscarPorId(int idUsuario) throws SQLException {
        String sql = SELECT_BASE + " WHERE u.id_usuario = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Usuario buscarPorEmail(String email) throws SQLException {
        String sql = SELECT_BASE + " WHERE u.email = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public String obtenerHash(int idUsuario) throws SQLException {
        String sql = "SELECT password_hash FROM usuarios WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public List<Usuario> listar() throws SQLException {
        String sql = """
                SELECT u.id_usuario, u.id_rol, r.nombre AS rol, u.nombre, u.apellidos, u.email, u.telefono,
                       u.fecha_nacimiento, u.fecha_registro, u.activo,
                       (SELECT COUNT(*) FROM pedidos p JOIN direcciones d ON d.id_direccion = p.id_direccion
                         WHERE d.id_usuario = u.id_usuario) AS total_pedidos
                  FROM usuarios u
                  JOIN roles r ON r.id_rol = u.id_rol
                 ORDER BY u.id_usuario
                """;
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario usuario = mapear(rs);
                usuario.setTotalPedidos(rs.getInt("total_pedidos"));
                usuarios.add(usuario);
            }
        }
        return usuarios;
    }

    public int registrar(Usuario usuario, String hash) throws SQLException {
        String sql = """
                INSERT INTO usuarios (id_rol, nombre, apellidos, email, password_hash, telefono, fecha_nacimiento)
                VALUES ((SELECT id_rol FROM roles WHERE nombre = 'cliente'), ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getApellidos());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, hash);
            ps.setString(5, Cifrado.cifrar(usuario.getTelefono()));
            ps.setObject(6, usuario.getFechaNacimiento());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizarDatos(int idUsuario, Usuario datos) throws SQLException {
        String sql = """
                UPDATE usuarios
                   SET nombre = ?, apellidos = ?, email = ?, telefono = ?, fecha_nacimiento = ?
                 WHERE id_usuario = ?
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, datos.getNombre());
            ps.setString(2, datos.getApellidos());
            ps.setString(3, datos.getEmail());
            ps.setString(4, Cifrado.cifrar(datos.getTelefono()));
            ps.setObject(5, datos.getFechaNacimiento());
            ps.setInt(6, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarHash(int idUsuario, String hash) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public boolean existeRol(int idRol) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM roles WHERE id_rol = ?")) {
            ps.setInt(1, idRol);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void actualizarRolYEstado(int idUsuario, Integer idRol, Boolean activo) throws SQLException {
        String sql = """
                UPDATE usuarios
                   SET id_rol = COALESCE(?, id_rol), activo = COALESCE(?, activo)
                 WHERE id_usuario = ?
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, idRol);
            ps.setObject(2, activo);
            ps.setInt(3, idUsuario);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("El usuario no existe");
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(rs.getInt("id_usuario"));
        usuario.setIdRol(rs.getInt("id_rol"));
        usuario.setRol(rs.getString("rol"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setEmail(rs.getString("email"));
        usuario.setTelefono(Cifrado.descifrar(rs.getString("telefono")));
        usuario.setFechaNacimiento(rs.getObject("fecha_nacimiento", LocalDate.class));
        usuario.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        usuario.setActivo(rs.getBoolean("activo"));
        return usuario;
    }
}
