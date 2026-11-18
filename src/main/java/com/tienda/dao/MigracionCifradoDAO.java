package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.util.Cifrado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MigracionCifradoDAO {

    private record Tabla(String nombre, String llave, List<String> columnas) {
    }

    private static final List<Tabla> TABLAS = List.of(
            new Tabla("usuarios", "id_usuario", List.of("telefono")),
            new Tabla("direcciones", "id_direccion", List.of("calle", "numero", "referencias")),
            new Tabla("pagos", "id_pago", List.of("referencia")));

    public int cifrarPendientes() throws SQLException {
        int total = 0;
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                for (Tabla tabla : TABLAS) {
                    for (String columna : tabla.columnas()) {
                        total += cifrarColumna(conn, tabla, columna);
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
        return total;
    }

    private int cifrarColumna(Connection conn, Tabla tabla, String columna) throws SQLException {
        String consulta = "SELECT " + tabla.llave() + ", " + columna + " FROM " + tabla.nombre()
                + " WHERE " + columna + " IS NOT NULL AND " + columna + " NOT LIKE 'v1:%'";
        String actualizacion = "UPDATE " + tabla.nombre() + " SET " + columna + " = ? WHERE " + tabla.llave() + " = ?";
        int cifrados = 0;
        try (PreparedStatement select = conn.prepareStatement(consulta);
             PreparedStatement update = conn.prepareStatement(actualizacion);
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                update.setString(1, Cifrado.cifrar(rs.getString(2)));
                update.setInt(2, rs.getInt(1));
                update.addBatch();
                cifrados++;
            }
            update.executeBatch();
        }
        return cifrados;
    }
}
