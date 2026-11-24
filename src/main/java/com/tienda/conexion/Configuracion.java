package com.tienda.conexion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class Configuracion {

    private static final Properties PROPIEDADES = cargar();

    private Configuracion() {
    }

    private static Properties cargar() {
        Properties propiedades = new Properties();
        try (InputStream entrada = Configuracion.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (entrada != null) {
                propiedades.load(entrada);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer db.properties", e);
        }
        aplicarDatabaseUrl(propiedades);
        return propiedades;
    }

    public static String obtener(String clave) {
        String variable = System.getenv(clave.toUpperCase().replace('.', '_'));
        if (variable != null && !variable.isBlank()) {
            return variable.trim();
        }
        return PROPIEDADES.getProperty(clave);
    }

    public static String obtener(String clave, String valorPorDefecto) {
        String valor = obtener(clave);
        return valor == null || valor.isBlank() ? valorPorDefecto : valor;
    }

    public static String requerido(String clave) {
        String valor = obtener(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta " + clave + " en db.properties o la variable de entorno "
                    + clave.toUpperCase().replace('.', '_'));
        }
        return valor;
    }

    private static void aplicarDatabaseUrl(Properties propiedades) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || System.getenv("DB_URL") != null) {
            return;
        }
        URI uri = URI.create(databaseUrl.trim());
        if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
            throw new IllegalStateException("DATABASE_URL debe empezar con postgresql://");
        }
        int puerto = uri.getPort() == -1 ? 5432 : uri.getPort();
        String consulta = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        propiedades.setProperty("db.url", "jdbc:postgresql://" + uri.getHost() + ":" + puerto + uri.getRawPath() + consulta);
        String usuario = uri.getRawUserInfo();
        if (usuario != null) {
            int separador = usuario.indexOf(':');
            propiedades.setProperty("db.usuario", decodificar(separador < 0 ? usuario : usuario.substring(0, separador)));
            if (separador >= 0) {
                propiedades.setProperty("db.contrasena", decodificar(usuario.substring(separador + 1)));
            }
        }
    }

    private static String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }
}
