package com.tienda.conexion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Configuracion {

    private static final Properties PROPIEDADES = cargar();

    private Configuracion() {
    }

    private static Properties cargar() {
        Properties propiedades = new Properties();
        try (InputStream entrada = Configuracion.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró db.properties en src/main/resources");
            }
            propiedades.load(entrada);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer db.properties", e);
        }
        return propiedades;
    }

    public static String obtener(String clave) {
        return PROPIEDADES.getProperty(clave);
    }

    public static String obtener(String clave, String valorPorDefecto) {
        return PROPIEDADES.getProperty(clave, valorPorDefecto);
    }
}
