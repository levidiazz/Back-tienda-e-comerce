package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.Producto;
import com.tienda.modelos.Variante;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProductoDAO {

    private static final String SELECT_BASE = """
            SELECT p.*, pv.razon_social AS proveedor, s.stock_total, c.promedio, c.num_resenas,
                   ARRAY(SELECT ps.id_subcategoria FROM producto_subcategoria ps
                          WHERE ps.sku = p.sku ORDER BY ps.id_subcategoria) AS ids_subcategorias,
                   ARRAY(SELECT DISTINCT ca.nombre FROM producto_subcategoria ps
                           JOIN subcategorias sb ON sb.id_subcategoria = ps.id_subcategoria
                           JOIN categorias ca ON ca.id_categoria = sb.id_categoria
                          WHERE ps.sku = p.sku) AS nombres_categorias
              FROM productos p
              JOIN proveedores pv ON pv.id_proveedor = p.id_proveedor
              JOIN vw_stock_productos s ON s.sku = p.sku
              JOIN vw_calificacion_productos c ON c.sku = p.sku
            """;

    public List<Producto> listar(Integer idCategoria, Integer idSubcategoria, String busqueda,
                                 boolean soloOfertas, boolean incluirInactivos, String orden) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (!incluirInactivos) {
            sql.append(" AND p.activo");
        }
        if (idCategoria != null) {
            sql.append("""
                     AND EXISTS (SELECT 1 FROM producto_subcategoria ps
                                   JOIN subcategorias sb ON sb.id_subcategoria = ps.id_subcategoria
                                  WHERE ps.sku = p.sku AND sb.id_categoria = ?)
                    """);
            parametros.add(idCategoria);
        }
        if (idSubcategoria != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM producto_subcategoria ps WHERE ps.sku = p.sku AND ps.id_subcategoria = ?)");
            parametros.add(idSubcategoria);
        }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (p.nombre ILIKE ? ESCAPE '\\' OR p.descripcion ILIKE ? ESCAPE '\\' OR p.sku ILIKE ? ESCAPE '\\')");
            String patron = "%" + escaparComodines(busqueda.trim()) + "%";
            parametros.addAll(List.of(patron, patron, patron));
        }
        if (soloOfertas) {
            sql.append(" AND p.precio_oferta IS NOT NULL");
        }
        sql.append(switch (orden == null ? "" : orden) {
            case "precio_asc" -> " ORDER BY COALESCE(p.precio_oferta, p.precio)";
            case "precio_desc" -> " ORDER BY COALESCE(p.precio_oferta, p.precio) DESC";
            case "recientes" -> " ORDER BY p.fecha_alta DESC";
            case "calificacion" -> " ORDER BY c.promedio DESC NULLS LAST";
            default -> " ORDER BY p.nombre";
        });
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapear(rs));
                }
            }
        }
        return productos;
    }

    public Producto buscar(String sku, boolean incluirInactivos) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.sku = ?" + (incluirInactivos ? "" : " AND p.activo");
        try (Connection conn = ConexionDB.getConexion()) {
            Producto producto;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sku);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    producto = mapear(rs);
                }
            }
            producto.setVariantes(listarVariantes(conn, sku, incluirInactivos));
            return producto;
        }
    }

    private List<Variante> listarVariantes(Connection conn, String sku, boolean incluirInactivas) throws SQLException {
        String sql = "SELECT * FROM variantes_producto WHERE sku = ?" + (incluirInactivas ? "" : " AND activo")
                + " ORDER BY id_variante";
        List<Variante> variantes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Variante variante = new Variante();
                    variante.setIdVariante(rs.getInt("id_variante"));
                    variante.setSku(rs.getString("sku"));
                    variante.setTalla(rs.getString("talla"));
                    variante.setColor(rs.getString("color"));
                    variante.setCapacidad(rs.getString("capacidad"));
                    variante.setPresentacion(rs.getString("presentacion"));
                    variante.setPrecioExtra(rs.getBigDecimal("precio_extra"));
                    variante.setStock(rs.getInt("stock"));
                    variante.setActivo(rs.getBoolean("activo"));
                    variantes.add(variante);
                }
            }
        }
        return variantes;
    }

    private String escaparComodines(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setSku(rs.getString("sku"));
        producto.setIdProveedor(rs.getInt("id_proveedor"));
        producto.setProveedor(rs.getString("proveedor"));
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setPrecioOferta(rs.getBigDecimal("precio_oferta"));
        producto.setTipoOpciones(rs.getString("tipo_opciones"));
        producto.setStock(rs.getInt("stock"));
        producto.setStockTotal(rs.getInt("stock_total"));
        producto.setRequiereEnvio(rs.getBoolean("requiere_envio"));
        producto.setSoloMayores(rs.getBoolean("solo_mayores"));
        producto.setImagenUrl(rs.getString("imagen_url"));
        producto.setActivo(rs.getBoolean("activo"));
        producto.setFechaAlta(rs.getObject("fecha_alta", LocalDateTime.class));
        producto.setCalificacion(rs.getBigDecimal("promedio"));
        producto.setNumResenas(rs.getInt("num_resenas"));
        producto.setSubcategorias(enteros(rs.getArray("ids_subcategorias")));
        producto.setCategorias(textos(rs.getArray("nombres_categorias")));
        return producto;
    }

    private List<Integer> enteros(Array arreglo) throws SQLException {
        return arreglo == null ? List.of() : Arrays.asList((Integer[]) arreglo.getArray());
    }

    private List<String> textos(Array arreglo) throws SQLException {
        return arreglo == null ? List.of() : Arrays.asList((String[]) arreglo.getArray());
    }
}
