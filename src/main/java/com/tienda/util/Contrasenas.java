package com.tienda.util;

import org.mindrot.jbcrypt.BCrypt;

public final class Contrasenas {

    private static final int COSTO = 12;

    private Contrasenas() {
    }

    public static String cifrar(String contrasena) {
        return BCrypt.hashpw(contrasena, BCrypt.gensalt(COSTO));
    }

    public static boolean coincide(String contrasena, String hash) {
        if (contrasena == null || hash == null || !hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) {
            return false;
        }
        return BCrypt.checkpw(contrasena, hash.replaceFirst("^\\$2[by]\\$", "\\$2a\\$"));
    }
}
