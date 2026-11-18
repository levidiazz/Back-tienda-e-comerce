package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Proveedor;
import com.tienda.util.ApiExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    private static final String SELECT_BASE = """
            SELECT pv.*, (SELECT COUNT(*) FROM productos p WHERE p.id_proveedor = pv.id_proveedor) AS total_productos
              FROM proveedores pv
            """;

    public List<Proveedor> listar() throws SQLException {
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " ORDER BY pv.razon_social");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                proveedores.add(mapear(rs));
            }
        }
        return proveedores;
    }

    public Proveedor buscar(int idProveedor) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " WHERE pv.id_proveedor = ?")) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public int insertar(Proveedor proveedor) throws SQLException {
        String sql = "INSERT INTO proveedores (razon_social, rfc, contacto, telefono, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenar(ps, proveedor);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(int idProveedor, Proveedor proveedor) throws SQLException {
        String sql = """
                UPDATE proveedores
                   SET razon_social = ?, rfc = ?, contacto = ?, telefono = ?, email = ?
                 WHERE id_proveedor = ?
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            llenar(ps, proveedor);
            ps.setInt(6, idProveedor);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("El proveedor no existe");
            }
        }
    }

    public void eliminar(int idProveedor) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM proveedores WHERE id_proveedor = ?")) {
            ps.setInt(1, idProveedor);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("El proveedor no existe");
            }
        }
    }

    private void llenar(PreparedStatement ps, Proveedor proveedor) throws SQLException {
        ps.setString(1, proveedor.getRazonSocial());
        ps.setString(2, proveedor.getRfc());
        ps.setString(3, proveedor.getContacto());
        ps.setString(4, proveedor.getTelefono());
        ps.setString(5, proveedor.getEmail());
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor proveedor = new Proveedor();
        proveedor.setIdProveedor(rs.getInt("id_proveedor"));
        proveedor.setRazonSocial(rs.getString("razon_social"));
        proveedor.setRfc(rs.getString("rfc"));
        proveedor.setContacto(rs.getString("contacto"));
        proveedor.setTelefono(rs.getString("telefono"));
        proveedor.setEmail(rs.getString("email"));
        proveedor.setTotalProductos(rs.getInt("total_productos"));
        return proveedor;
    }
}
