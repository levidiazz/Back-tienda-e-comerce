package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Resena;
import com.tienda.util.ApiExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ResenaDAO {

    private static final String SELECT_BASE = """
            SELECT r.*, u.nombre || ' ' || LEFT(u.apellidos, 1) || '.' AS autor, p.nombre AS producto
              FROM resenas r
              JOIN usuarios u ON u.id_usuario = r.id_usuario
              JOIN productos p ON p.sku = r.sku
            """;

    public List<Resena> listarPorProducto(String sku) throws SQLException {
        return consultar(SELECT_BASE + " WHERE r.sku = ? ORDER BY r.fecha DESC", sku);
    }

    public List<Resena> listarPorUsuario(int idUsuario) throws SQLException {
        return consultar(SELECT_BASE + " WHERE r.id_usuario = ? ORDER BY r.fecha DESC", idUsuario);
    }

    public List<Resena> listarTodas() throws SQLException {
        return consultar(SELECT_BASE + " ORDER BY r.fecha DESC");
    }

    public boolean compraEntregada(int idUsuario, String sku) throws SQLException {
        String sql = """
                SELECT 1
                  FROM detalle_pedido dp
                  JOIN pedidos p ON p.id_pedido = dp.id_pedido
                  JOIN direcciones d ON d.id_direccion = p.id_direccion
                 WHERE d.id_usuario = ? AND dp.sku = ? AND p.estado = 'entregado'
                 LIMIT 1
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(int idUsuario, Resena resena) throws SQLException {
        String sql = "INSERT INTO resenas (id_usuario, sku, calificacion, comentario) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, resena.getSku());
            ps.setInt(3, resena.getCalificacion());
            ps.setString(4, resena.getComentario());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(int idUsuario, int idResena, Resena resena) throws SQLException {
        String sql = """
                UPDATE resenas SET calificacion = ?, comentario = ?, fecha = CURRENT_TIMESTAMP
                 WHERE id_resena = ? AND id_usuario = ?
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resena.getCalificacion());
            ps.setString(2, resena.getComentario());
            ps.setInt(3, idResena);
            ps.setInt(4, idUsuario);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La reseña no existe");
            }
        }
    }

    public void eliminar(int idResena, Integer idUsuario) throws SQLException {
        String sql = "DELETE FROM resenas WHERE id_resena = ?" + (idUsuario == null ? "" : " AND id_usuario = ?");
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idResena);
            if (idUsuario != null) {
                ps.setInt(2, idUsuario);
            }
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La reseña no existe");
            }
        }
    }

    private List<Resena> consultar(String sql, Object... parametros) throws SQLException {
        List<Resena> resenas = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Resena resena = new Resena();
                    resena.setIdResena(rs.getInt("id_resena"));
                    resena.setIdUsuario(rs.getInt("id_usuario"));
                    resena.setAutor(rs.getString("autor"));
                    resena.setSku(rs.getString("sku"));
                    resena.setProducto(rs.getString("producto"));
                    resena.setCalificacion(rs.getInt("calificacion"));
                    resena.setComentario(rs.getString("comentario"));
                    resena.setFecha(rs.getObject("fecha", LocalDateTime.class));
                    resenas.add(resena);
                }
            }
        }
        return resenas;
    }
}
