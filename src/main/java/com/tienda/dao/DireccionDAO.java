package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Direccion;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Cifrado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DireccionDAO {

    public List<Direccion> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM direcciones WHERE id_usuario = ? ORDER BY es_principal DESC, id_direccion";
        List<Direccion> direcciones = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    direcciones.add(mapear(rs));
                }
            }
        }
        return direcciones;
    }

    public Direccion buscar(int idUsuario, int idDireccion) throws SQLException {
        String sql = "SELECT * FROM direcciones WHERE id_direccion = ? AND id_usuario = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDireccion);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public int contarPorUsuario(int idUsuario) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            return contar(conn, idUsuario);
        }
    }

    public int insertar(int idUsuario, Direccion direccion) throws SQLException {
        String sql = """
                INSERT INTO direcciones (id_usuario, calle, numero, colonia, ciudad, estado, codigo_postal,
                                         referencias, es_principal)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                boolean principal = Boolean.TRUE.equals(direccion.getEsPrincipal()) || contar(conn, idUsuario) == 0;
                if (principal) {
                    quitarPrincipal(conn, idUsuario);
                }
                int id;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idUsuario);
                    llenar(ps, direccion, 2);
                    ps.setBoolean(9, principal);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(int idUsuario, int idDireccion, Direccion direccion) throws SQLException {
        String sql = """
                UPDATE direcciones
                   SET calle = ?, numero = ?, colonia = ?, ciudad = ?, estado = ?, codigo_postal = ?, referencias = ?
                 WHERE id_direccion = ? AND id_usuario = ?
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            llenar(ps, direccion, 1);
            ps.setInt(8, idDireccion);
            ps.setInt(9, idUsuario);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La dirección no existe");
            }
        }
    }

    public void marcarPrincipal(int idUsuario, int idDireccion) throws SQLException {
        String sql = "UPDATE direcciones SET es_principal = TRUE WHERE id_direccion = ? AND id_usuario = ?";
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                quitarPrincipal(conn, idUsuario);
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, idDireccion);
                    ps.setInt(2, idUsuario);
                    if (ps.executeUpdate() == 0) {
                        throw ApiExcepcion.noEncontrado("La dirección no existe");
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void eliminar(int idUsuario, int idDireccion) throws SQLException {
        String sql = "DELETE FROM direcciones WHERE id_direccion = ? AND id_usuario = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idDireccion);
            ps.setInt(2, idUsuario);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La dirección no existe");
            }
        }
    }

    private int contar(Connection conn, int idUsuario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM direcciones WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void quitarPrincipal(Connection conn, int idUsuario) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE direcciones SET es_principal = FALSE WHERE id_usuario = ?")) {
            ps.setInt(1, idUsuario);
            ps.executeUpdate();
        }
    }

    private void llenar(PreparedStatement ps, Direccion d, int inicio) throws SQLException {
        ps.setString(inicio, Cifrado.cifrar(d.getCalle()));
        ps.setString(inicio + 1, Cifrado.cifrar(d.getNumero()));
        ps.setString(inicio + 2, d.getColonia());
        ps.setString(inicio + 3, d.getCiudad());
        ps.setString(inicio + 4, d.getEstado());
        ps.setString(inicio + 5, d.getCodigoPostal());
        ps.setString(inicio + 6, Cifrado.cifrar(d.getReferencias()));
    }

    private Direccion mapear(ResultSet rs) throws SQLException {
        Direccion direccion = new Direccion();
        direccion.setIdDireccion(rs.getInt("id_direccion"));
        direccion.setIdUsuario(rs.getInt("id_usuario"));
        direccion.setCalle(Cifrado.descifrar(rs.getString("calle")));
        direccion.setNumero(Cifrado.descifrar(rs.getString("numero")));
        direccion.setColonia(rs.getString("colonia"));
        direccion.setCiudad(rs.getString("ciudad"));
        direccion.setEstado(rs.getString("estado"));
        direccion.setCodigoPostal(rs.getString("codigo_postal"));
        direccion.setReferencias(Cifrado.descifrar(rs.getString("referencias")));
        direccion.setEsPrincipal(rs.getBoolean("es_principal"));
        return direccion;
    }
}
