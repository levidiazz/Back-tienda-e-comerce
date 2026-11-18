package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Producto;
import com.tienda.modelos.Variante;
import com.tienda.util.ApiExcepcion;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ProductoAdminDAO {

    public boolean existeProveedor(int idProveedor) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM proveedores WHERE id_proveedor = ?")) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int contarSubcategorias(List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM subcategorias WHERE id_subcategoria = ANY (?)")) {
            ps.setArray(1, conn.createArrayOf("integer", ids.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void insertar(Producto producto) throws SQLException {
        String sql = """
                INSERT INTO productos (sku, id_proveedor, nombre, descripcion, precio, precio_oferta, tipo_opciones,
                                       stock, requiere_envio, solo_mayores, imagen_url, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?::tipo_opciones, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, producto.getSku());
                    llenar(ps, producto, 2);
                    ps.executeUpdate();
                }
                guardarSubcategorias(conn, producto.getSku(), producto.getSubcategorias());
                if (producto.getVariantes() != null) {
                    for (Variante variante : producto.getVariantes()) {
                        insertarVariante(conn, producto.getSku(), variante);
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void actualizar(String sku, Producto producto) throws SQLException {
        String sql = """
                UPDATE productos
                   SET id_proveedor = ?, nombre = ?, descripcion = ?, precio = ?, precio_oferta = ?,
                       tipo_opciones = ?::tipo_opciones, stock = ?, requiere_envio = ?, solo_mayores = ?,
                       imagen_url = ?, activo = ?
                 WHERE sku = ?
                """;
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    llenar(ps, producto, 1);
                    ps.setString(12, sku);
                    if (ps.executeUpdate() == 0) {
                        throw ApiExcepcion.noEncontrado("El producto no existe");
                    }
                }
                if (producto.getSubcategorias() != null) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM producto_subcategoria WHERE sku = ?")) {
                        ps.setString(1, sku);
                        ps.executeUpdate();
                    }
                    guardarSubcategorias(conn, sku, producto.getSubcategorias());
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void desactivar(String sku) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement("UPDATE productos SET activo = FALSE WHERE sku = ?")) {
            ps.setString(1, sku);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("El producto no existe");
            }
        }
    }

    public int insertarVariante(String sku, Variante variante) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            return insertarVariante(conn, sku, variante);
        }
    }

    public void actualizarVariante(String sku, int idVariante, Variante variante) throws SQLException {
        String sql = """
                UPDATE variantes_producto
                   SET talla = ?, color = ?, capacidad = ?, presentacion = ?, precio_extra = ?, stock = ?, activo = ?
                 WHERE id_variante = ? AND sku = ?
                """;
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            llenarVariante(ps, variante, 1);
            ps.setBoolean(7, variante.getActivo() == null || variante.getActivo());
            ps.setInt(8, idVariante);
            ps.setString(9, sku);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La variante no existe");
            }
        }
    }

    public void desactivarVariante(String sku, int idVariante) throws SQLException {
        String sql = "UPDATE variantes_producto SET activo = FALSE WHERE id_variante = ? AND sku = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVariante);
            ps.setString(2, sku);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La variante no existe");
            }
        }
    }

    private int insertarVariante(Connection conn, String sku, Variante variante) throws SQLException {
        String sql = """
                INSERT INTO variantes_producto (sku, talla, color, capacidad, presentacion, precio_extra, stock)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sku);
            llenarVariante(ps, variante, 2);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void guardarSubcategorias(Connection conn, String sku, List<Integer> subcategorias) throws SQLException {
        if (subcategorias == null || subcategorias.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO producto_subcategoria (sku, id_subcategoria) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Integer idSubcategoria : subcategorias) {
                ps.setString(1, sku);
                ps.setInt(2, idSubcategoria);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void llenar(PreparedStatement ps, Producto p, int inicio) throws SQLException {
        ps.setInt(inicio, p.getIdProveedor());
        ps.setString(inicio + 1, p.getNombre());
        ps.setString(inicio + 2, p.getDescripcion());
        ps.setBigDecimal(inicio + 3, p.getPrecio());
        ps.setBigDecimal(inicio + 4, p.getPrecioOferta());
        ps.setString(inicio + 5, p.getTipoOpciones() == null ? "ninguna" : p.getTipoOpciones());
        ps.setInt(inicio + 6, p.getStock() == null ? 0 : p.getStock());
        ps.setBoolean(inicio + 7, p.getRequiereEnvio() == null || p.getRequiereEnvio());
        ps.setBoolean(inicio + 8, Boolean.TRUE.equals(p.getSoloMayores()));
        ps.setString(inicio + 9, p.getImagenUrl());
        ps.setBoolean(inicio + 10, p.getActivo() == null || p.getActivo());
    }

    private void llenarVariante(PreparedStatement ps, Variante v, int inicio) throws SQLException {
        ps.setString(inicio, v.getTalla());
        ps.setString(inicio + 1, v.getColor());
        ps.setString(inicio + 2, v.getCapacidad());
        ps.setString(inicio + 3, v.getPresentacion());
        ps.setBigDecimal(inicio + 4, v.getPrecioExtra() == null ? BigDecimal.ZERO : v.getPrecioExtra());
        ps.setInt(inicio + 5, v.getStock() == null ? 0 : v.getStock());
    }
}
