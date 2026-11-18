package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.util.Cifrado;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

public class TokenDAO {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    public String crear(int idUsuario) throws SQLException {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        String invalidar = "UPDATE tokens_recuperacion SET usado = TRUE WHERE id_usuario = ? AND usado = FALSE";
        String insertar = """
                INSERT INTO tokens_recuperacion (id_usuario, token, expira)
                VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '30 minutes')
                """;
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(invalidar)) {
                    ps.setInt(1, idUsuario);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insertar)) {
                    ps.setInt(1, idUsuario);
                    ps.setString(2, Cifrado.sha256(token));
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
        return token;
    }

    public Integer consumir(String token) throws SQLException {
        String sql = """
                UPDATE tokens_recuperacion
                   SET usado = TRUE
                 WHERE token = ? AND usado = FALSE AND expira > CURRENT_TIMESTAMP
                RETURNING id_usuario
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Cifrado.sha256(token));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
}
