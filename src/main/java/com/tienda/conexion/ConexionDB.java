package com.tienda.conexion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class ConexionDB {

    private static final HikariDataSource POOL;

    static {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(Configuracion.obtener("db.url"));
        config.setUsername(Configuracion.obtener("db.usuario"));
        config.setPassword(Configuracion.obtener("db.contrasena", ""));
        config.setDriverClassName("org.postgresql.Driver");

        config.setPoolName("TiendaPool");
        config.setMaximumPoolSize(Integer.parseInt(Configuracion.obtener("db.pool.maximo", "10")));
        config.setMinimumIdle(Integer.parseInt(Configuracion.obtener("db.pool.minimo", "2")));
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);
        config.setInitializationFailTimeout(-1);

        config.addDataSourceProperty("prepareThreshold", "3");
        config.addDataSourceProperty("preparedStatementCacheQueries", "250");
        config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");

        POOL = new HikariDataSource(config);
    }

    private ConexionDB() {
    }

    public static Connection getConexion() throws SQLException {
        return POOL.getConnection();
    }

    public static void cerrar() {
        if (!POOL.isClosed()) {
            POOL.close();
        }
    }
}
