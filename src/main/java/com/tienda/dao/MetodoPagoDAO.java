package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.MetodoPago;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MetodoPagoDAO {

    public List<MetodoPago> listarActivos() throws SQLException {
        String sql = "SELECT id_metodo, nombre, activo FROM metodos_pago WHERE activo ORDER BY id_metodo";
        List<MetodoPago> metodos = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MetodoPago metodo = new MetodoPago();
                metodo.setIdMetodo(rs.getInt("id_metodo"));
                metodo.setNombre(rs.getString("nombre"));
                metodo.setActivo(rs.getBoolean("activo"));
                metodos.add(metodo);
            }
        }
        return metodos;
    }
}
