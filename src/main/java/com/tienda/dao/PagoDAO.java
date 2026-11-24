package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Pago;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Cifrado;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {

    private static final String SELECT_BASE = """
            SELECT pg.*, mp.nombre AS metodo, u.nombre || ' ' || u.apellidos AS cliente
              FROM pagos pg
              JOIN metodos_pago mp ON mp.id_metodo = pg.id_metodo
              JOIN pedidos p ON p.id_pedido = pg.id_pedido
              JOIN direcciones d ON d.id_direccion = p.id_direccion
              JOIN usuarios u ON u.id_usuario = d.id_usuario
            """;

    public String nombreMetodoActivo(int idMetodo) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT nombre FROM metodos_pago WHERE id_metodo = ? AND activo")) {
            ps.setInt(1, idMetodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public Pago registrar(int idUsuario, int idPedido, int idMetodo, String referencia) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal total;
                String sqlPedido = """
                        SELECT p.estado, p.total, d.id_usuario
                          FROM pedidos p
                          JOIN direcciones d ON d.id_direccion = p.id_direccion
                         WHERE p.id_pedido = ?
                           FOR UPDATE OF p
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sqlPedido)) {
                    ps.setInt(1, idPedido);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt("id_usuario") != idUsuario) {
                            throw ApiExcepcion.noEncontrado("El pedido no existe");
                        }
                        if (!"pendiente".equals(rs.getString("estado"))) {
                            throw ApiExcepcion.conflicto("El pedido ya no está pendiente de pago");
                        }
                        total = rs.getBigDecimal("total");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO pagos (id_pedido, id_metodo, monto, referencia) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, idPedido);
                    ps.setInt(2, idMetodo);
                    ps.setBigDecimal(3, total);
                    ps.setString(4, Cifrado.cifrar(referencia));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE pedidos SET estado = 'pagado' WHERE id_pedido = ?")) {
                    ps.setInt(1, idPedido);
                    ps.executeUpdate();
                }
                Pago pago = buscarPorPedido(conn, idPedido);
                conn.commit();
                return pago;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Pago> listar() throws SQLException {
        List<Pago> pagos = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " ORDER BY pg.fecha_pago DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pagos.add(mapear(rs));
            }
        }
        return pagos;
    }

    Pago buscarPorPedido(Connection conn, int idPedido) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " WHERE pg.id_pedido = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private Pago mapear(ResultSet rs) throws SQLException {
        Pago pago = new Pago();
        pago.setIdPago(rs.getInt("id_pago"));
        pago.setIdPedido(rs.getInt("id_pedido"));
        pago.setIdMetodo(rs.getInt("id_metodo"));
        pago.setMetodo(rs.getString("metodo"));
        pago.setCliente(rs.getString("cliente"));
        pago.setMonto(rs.getBigDecimal("monto"));
        pago.setFechaPago(rs.getObject("fecha_pago", LocalDateTime.class));
        pago.setReferencia(Cifrado.descifrar(rs.getString("referencia")));
        pago.setEstado(rs.getString("estado"));
        return pago;
    }
}
