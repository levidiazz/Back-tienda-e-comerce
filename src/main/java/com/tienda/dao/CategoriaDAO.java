package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Categoria;
import com.tienda.modelos.Subcategoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriaDAO {

    public List<Categoria> listar() throws SQLException {
        String sqlCategorias = """
                SELECT c.id_categoria, c.nombre, c.descripcion,
                       (SELECT COUNT(DISTINCT ps.sku)
                          FROM producto_subcategoria ps
                          JOIN subcategorias s ON s.id_subcategoria = ps.id_subcategoria
                          JOIN productos p ON p.sku = ps.sku AND p.activo
                         WHERE s.id_categoria = c.id_categoria) AS total_productos
                  FROM categorias c
                 ORDER BY c.id_categoria
                """;
        String sqlSubcategorias = "SELECT * FROM subcategorias ORDER BY id_categoria, nombre";
        Map<Integer, Categoria> categorias = new LinkedHashMap<>();
        try (Connection conn = ConexionDB.getConexion()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlCategorias);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Categoria categoria = new Categoria();
                    categoria.setIdCategoria(rs.getInt("id_categoria"));
                    categoria.setNombre(rs.getString("nombre"));
                    categoria.setDescripcion(rs.getString("descripcion"));
                    categoria.setTotalProductos(rs.getInt("total_productos"));
                    categoria.setSubcategorias(new ArrayList<>());
                    categorias.put(categoria.getIdCategoria(), categoria);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlSubcategorias);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Subcategoria subcategoria = mapearSubcategoria(rs);
                    Categoria categoria = categorias.get(subcategoria.getIdCategoria());
                    if (categoria != null) {
                        categoria.getSubcategorias().add(subcategoria);
                    }
                }
            }
        }
        return new ArrayList<>(categorias.values());
    }

    private Subcategoria mapearSubcategoria(ResultSet rs) throws SQLException {
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setIdSubcategoria(rs.getInt("id_subcategoria"));
        subcategoria.setIdCategoria(rs.getInt("id_categoria"));
        subcategoria.setNombre(rs.getString("nombre"));
        subcategoria.setDescripcion(rs.getString("descripcion"));
        return subcategoria;
    }
}
