package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Categoria;
import com.tienda.modelos.Subcategoria;
import com.tienda.util.ApiExcepcion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CategoriaAdminDAO {

    public boolean existeCategoria(int idCategoria) throws SQLException {
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM categorias WHERE id_categoria = ?")) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categorias (nombre, descripcion) VALUES (?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getDescripcion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizar(int idCategoria, Categoria categoria) throws SQLException {
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ? WHERE id_categoria = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoria.getNombre());
            ps.setString(2, categoria.getDescripcion());
            ps.setInt(3, idCategoria);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La categoría no existe");
            }
        }
    }

    public void eliminar(int idCategoria) throws SQLException {
        String sqlProductos = """
                SELECT COUNT(*) FROM producto_subcategoria ps
                  JOIN subcategorias s ON s.id_subcategoria = ps.id_subcategoria
                 WHERE s.id_categoria = ?
                """;
        try (Connection conn = ConexionDB.getConexion()) {
            if (contar(conn, sqlProductos, idCategoria) > 0) {
                throw ApiExcepcion.conflicto("No se puede eliminar: la categoría tiene productos");
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM categorias WHERE id_categoria = ?")) {
                ps.setInt(1, idCategoria);
                if (ps.executeUpdate() == 0) {
                    throw ApiExcepcion.noEncontrado("La categoría no existe");
                }
            }
        }
    }

    public int insertarSubcategoria(Subcategoria subcategoria) throws SQLException {
        String sql = "INSERT INTO subcategorias (id_categoria, nombre, descripcion) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, subcategoria.getIdCategoria());
            ps.setString(2, subcategoria.getNombre());
            ps.setString(3, subcategoria.getDescripcion());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void actualizarSubcategoria(int idSubcategoria, Subcategoria subcategoria) throws SQLException {
        String sql = "UPDATE subcategorias SET id_categoria = ?, nombre = ?, descripcion = ? WHERE id_subcategoria = ?";
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, subcategoria.getIdCategoria());
            ps.setString(2, subcategoria.getNombre());
            ps.setString(3, subcategoria.getDescripcion());
            ps.setInt(4, idSubcategoria);
            if (ps.executeUpdate() == 0) {
                throw ApiExcepcion.noEncontrado("La subcategoría no existe");
            }
        }
    }

    public void eliminarSubcategoria(int idSubcategoria) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            if (contar(conn, "SELECT COUNT(*) FROM producto_subcategoria WHERE id_subcategoria = ?", idSubcategoria) > 0) {
                throw ApiExcepcion.conflicto("No se puede eliminar: la subcategoría tiene productos");
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM subcategorias WHERE id_subcategoria = ?")) {
                ps.setInt(1, idSubcategoria);
                if (ps.executeUpdate() == 0) {
                    throw ApiExcepcion.noEncontrado("La subcategoría no existe");
                }
            }
        }
    }

    private int contar(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
