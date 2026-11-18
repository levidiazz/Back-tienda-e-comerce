package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardDAO {

    public Map<String, Object> resumen() throws SQLException {
        Map<String, Object> resumen = new LinkedHashMap<>();
        try (Connection conn = ConexionDB.getConexion()) {
            resumen.put("indicadores", fila(conn, """
                    SELECT (SELECT COALESCE(SUM(monto), 0) FROM pagos WHERE estado = 'aprobado') AS ventas,
                           (SELECT COUNT(*) FROM pedidos WHERE estado <> 'cancelado') AS pedidos,
                           (SELECT COUNT(*) FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol
                             WHERE r.nombre = 'cliente') AS clientes,
                           (SELECT COALESCE(ROUND(AVG(total), 2), 0) FROM pedidos WHERE estado <> 'cancelado') AS ticket_promedio
                    """));
            resumen.put("ventasPorDia", filas(conn, """
                    SELECT TO_CHAR(dia, 'YYYY-MM-DD') AS fecha, COALESCE(SUM(pg.monto), 0) AS monto
                      FROM generate_series(CURRENT_DATE - 6, CURRENT_DATE, INTERVAL '1 day') AS dia
                      LEFT JOIN pagos pg ON pg.fecha_pago::date = dia::date AND pg.estado = 'aprobado'
                     GROUP BY dia
                     ORDER BY dia
                    """));
            resumen.put("masVendidos", filas(conn, """
                    SELECT v.sku, v.nombre, p.imagen_url, v.unidades_vendidas, v.total_vendido
                      FROM vw_ventas_por_producto v
                      JOIN productos p ON p.sku = v.sku
                     ORDER BY v.unidades_vendidas DESC
                     LIMIT 5
                    """));
            resumen.put("stockBajo", filas(conn, """
                    SELECT s.sku, s.nombre, s.stock_total
                      FROM vw_stock_productos s
                      JOIN productos p ON p.sku = s.sku AND p.activo
                     WHERE s.stock_total <= 5
                     ORDER BY s.stock_total, s.nombre
                    """));
            resumen.put("pedidosRecientes", filas(conn, """
                    SELECT id_pedido, cliente, fecha_pedido, estado, total
                      FROM vw_pedidos_clientes
                     ORDER BY fecha_pedido DESC, id_pedido DESC
                     LIMIT 5
                    """));
        }
        return resumen;
    }

    private Map<String, Object> fila(Connection conn, String sql) throws SQLException {
        List<Map<String, Object>> resultado = filas(conn, sql);
        return resultado.isEmpty() ? Map.of() : resultado.get(0);
    }

    private List<Map<String, Object>> filas(Connection conn, String sql) throws SQLException {
        List<Map<String, Object>> resultado = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> fila = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    fila.put(camelCase(meta.getColumnLabel(i)), valor(rs, i, meta.getColumnTypeName(i)));
                }
                resultado.add(fila);
            }
        }
        return resultado;
    }

    private Object valor(ResultSet rs, int columna, String tipo) throws SQLException {
        if (tipo.startsWith("timestamp")) {
            return rs.getObject(columna, LocalDateTime.class);
        }
        if (tipo.equals("estado_pedido")) {
            return rs.getString(columna);
        }
        return rs.getObject(columna);
    }

    private String camelCase(String columna) {
        StringBuilder resultado = new StringBuilder();
        boolean mayuscula = false;
        for (char c : columna.toCharArray()) {
            if (c == '_') {
                mayuscula = true;
            } else {
                resultado.append(mayuscula ? Character.toUpperCase(c) : c);
                mayuscula = false;
            }
        }
        return resultado.toString();
    }
}
