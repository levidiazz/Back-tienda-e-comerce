package com.tienda.util;

public class ApiExcepcion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int estado;

    public ApiExcepcion(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int getEstado() {
        return estado;
    }

    public static ApiExcepcion solicitudInvalida(String mensaje) {
        return new ApiExcepcion(400, mensaje);
    }

    public static ApiExcepcion noAutenticado() {
        return new ApiExcepcion(401, "Debes iniciar sesión");
    }

    public static ApiExcepcion prohibido(String mensaje) {
        return new ApiExcepcion(403, mensaje);
    }

    public static ApiExcepcion noEncontrado(String mensaje) {
        return new ApiExcepcion(404, mensaje);
    }

    public static ApiExcepcion conflicto(String mensaje) {
        return new ApiExcepcion(409, mensaje);
    }
}
