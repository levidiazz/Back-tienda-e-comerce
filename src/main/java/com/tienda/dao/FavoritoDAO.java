package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Producto;
import com.tienda.util.ApiExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FavoritoDAO {

    public List<Producto> listar(int idUsuario) throws SQLException {
        String sql = """
                SELECT p.sku, p.nombre, p.precio, p.precio_oferta, p.tipo_opciones, p.imagen_url, p.activo,
                       s.stock_total, c.promedio, c.num_resenas
                  FROM favoritos f
                  JOIN productos p ON p.sku = f.sku
                  JOIN vw_stock_productos s ON s.sku = p.sku
                  JOIN vw_calificacion_productos c ON c.sku = p.sku
                 WHERE f.id_usuario = ?
                 ORDER BY f.fecha_agregado DESC
                """;
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto producto = new Producto();
                    producto.setSku(rs.getString("sku"));
                    producto.setNombre(rs.getString("nombre"));
                    producto.setPrecio(rs.getBigDecimal("precio"));
                    producto.setPrecioOferta(rs.getBigDecimal("precio_oferta"));
                    producto.setTipoOpciones(rs.getString("tipo_opciones"));
                    producto.setImagenUrl(rs.getString("imagen_url"));
                    producto.setActivo(rs.getBoolean("activo"));
                    producto.setStockTotal(rs.getInt("stock_total"));
                    producto.setCalificacion(rs.getBigDecimal("promedio"));
                    producto.setNumResenas(rs.getInt("num_resenas"));
                    productos.add(producto);
                }
            }
        }
        return productos;
    }

    public void agregar(int idUsuario, String sku) throws SQLException {
        String sql = "INSERT INTO favoritos (id_usuario, sku) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, sku);
            ps.executeUpdate();
        }
    }

    public void quitar(int idUsuario, String sku) throws SQLException {
        String sql = "DELETE FROM favoritos WHERE id_usuario = ? AND sku = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, sku);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("El producto no está en tus favoritos");
            }
        }
    }
}
