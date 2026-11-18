package com.tienda.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IntentosLogin {

    private static final int MAXIMO_FALLOS = 5;
    private static final long BLOQUEO_MS = 15 * 60 * 1000L;
    private static final Map<String, Registro> REGISTROS = new ConcurrentHashMap<>();

    private IntentosLogin() {
    }

    public static void verificar(String email) {
        Registro registro = REGISTROS.get(email);
        if (registro != null && registro.bloqueadoHasta > System.currentTimeMillis()) {
            long minutos = Math.max(1, (registro.bloqueadoHasta - System.currentTimeMillis()) / 60000);
            throw new ApiExcepcion(429, "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " minutos");
        }
    }

    public static void registrarFallo(String email) {
        REGISTROS.compute(email, (clave, actual) -> {
            Registro registro = actual == null || actual.bloqueadoHasta != 0 && actual.bloqueadoHasta < System.currentTimeMillis()
                    ? new Registro() : actual;
            registro.fallos++;
            if (registro.fallos >= MAXIMO_FALLOS) {
                registro.bloqueadoHasta = System.currentTimeMillis() + BLOQUEO_MS;
            }
            return registro;
        });
    }

    public static void limpiar(String email) {
        REGISTROS.remove(email);
    }

    private static final class Registro {
        private int fallos;
        private long bloqueadoHasta;
    }
}
