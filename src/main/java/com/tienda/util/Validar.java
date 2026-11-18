package com.tienda.util;

import com.tienda.modelos.Usuario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class Validar {

    public static final Pattern TEXTO = Pattern.compile("^[^<>\\p{Cntrl}]*$");
    public static final Pattern TEXTO_LARGO = Pattern.compile("^[^<>\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]*$");
    public static final Pattern NOMBRE = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");
    public static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    public static final Pattern TELEFONO = Pattern.compile("^[0-9]{10}$");
    public static final Pattern CODIGO_POSTAL = Pattern.compile("^[0-9]{5}$");
    public static final Pattern SKU = Pattern.compile("^[A-Z0-9]+(-[A-Z0-9]+)*$");
    public static final Pattern RFC = Pattern.compile("^[A-ZÑ&]{3,4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{3}$");
    public static final Pattern NUMERO_EXTERIOR = Pattern.compile("^[A-Za-z0-9 /-]+$");
    public static final Pattern GUIA = Pattern.compile("^[A-Za-z0-9-]{5,40}$");
    public static final Pattern TOKEN = Pattern.compile("^[0-9a-f]{64}$");
    public static final Pattern IMAGEN = Pattern.compile("^(img/[a-z0-9][a-z0-9-]*\\.(jpg|jpeg|png|webp)|https://[^\\s<>\"']{1,240})$");
    public static final Pattern OPCION_VARIANTE = Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 .,/-]*$");

    public static final List<String> ESTADOS_MEXICO = List.of(
            "Aguascalientes", "Baja California", "Baja California Sur", "Campeche", "Chiapas", "Chihuahua",
            "Ciudad de México", "Coahuila", "Colima", "Durango", "Estado de México", "Guanajuato", "Guerrero",
            "Hidalgo", "Jalisco", "Michoacán", "Morelos", "Nayarit", "Nuevo León", "Oaxaca", "Puebla", "Querétaro",
            "Quintana Roo", "San Luis Potosí", "Sinaloa", "Sonora", "Tabasco", "Tamaulipas", "Tlaxcala", "Veracruz",
            "Yucatán", "Zacatecas");

    private Validar() {
    }

    public static String texto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        String limpio = valor.strip();
        if (limpio.length() < minimo || limpio.length() > maximo) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe tener entre " + minimo + " y " + maximo + " caracteres");
        }
        if (!TEXTO.matcher(limpio).matches()) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " contiene caracteres no permitidos");
        }
        return limpio;
    }

    public static String textoOpcional(String valor, String campo, int maximo) {
        return valor == null || valor.isBlank() ? null : texto(valor, campo, 1, maximo);
    }

    public static String textoLargoOpcional(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.strip();
        if (limpio.length() > maximo) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " admite máximo " + maximo + " caracteres");
        }
        if (!TEXTO_LARGO.matcher(limpio).matches()) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " contiene caracteres no permitidos");
        }
        return limpio;
    }

    public static String patron(String valor, String campo, int minimo, int maximo, Pattern patron, String formato) {
        String limpio = texto(valor, campo, minimo, maximo);
        if (!patron.matcher(limpio).matches()) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " no es válido: " + formato);
        }
        return limpio;
    }

    public static String patronOpcional(String valor, String campo, int maximo, Pattern patron, String formato) {
        return valor == null || valor.isBlank() ? null : patron(valor, campo, 1, maximo, patron, formato);
    }

    public static String nombre(String valor, String campo, int maximo) {
        return patron(valor, campo, 2, maximo, NOMBRE, "solo letras, espacios, puntos, apóstrofes y guiones");
    }

    public static String nombreOpcional(String valor, String campo, int maximo) {
        return valor == null || valor.isBlank() ? null : nombre(valor, campo, maximo);
    }

    public static String email(String valor) {
        return patron(valor, "email", 6, 120, EMAIL, "ejemplo: nombre@correo.com").toLowerCase();
    }

    public static String emailOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : email(valor);
    }

    public static String telefonoOpcional(String valor) {
        return patronOpcional(valor, "telefono", 10, TELEFONO, "10 dígitos sin espacios");
    }

    public static String sku(String valor) {
        return patron(valor == null ? null : valor.toUpperCase(), "sku", 3, 30, SKU,
                "letras mayúsculas y números separados por guiones, ej. ROPA-002");
    }

    public static String contrasenaNueva(String valor, String campo) {
        if (valor == null || valor.isEmpty()) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        int bytes = valor.getBytes(StandardCharsets.UTF_8).length;
        if (valor.length() < 8 || bytes > 72) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe tener entre 8 y 72 caracteres");
        }
        if (!valor.matches(".*[A-ZÁÉÍÓÚÑ].*") || !valor.matches(".*[a-záéíóúñ].*") || !valor.matches(".*[0-9].*")) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe tener al menos una mayúscula, una minúscula y un número");
        }
        if (valor.chars().anyMatch(Character::isWhitespace)) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " no puede tener espacios");
        }
        return valor;
    }

    public static String contrasenaIngresada(String valor, String campo) {
        if (valor == null || valor.isEmpty()) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es demasiado largo");
        }
        return valor;
    }

    public static int id(Integer valor, String campo) {
        if (valor == null) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        if (valor <= 0) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe ser un número mayor a 0");
        }
        return valor;
    }

    public static Integer idOpcional(Integer valor, String campo) {
        return valor == null ? null : id(valor, campo);
    }

    public static int entero(Integer valor, String campo, int minimo, int maximo) {
        if (valor == null) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        if (valor < minimo || valor > maximo) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe estar entre " + minimo + " y " + maximo);
        }
        return valor;
    }

    public static int enteroOpcional(Integer valor, String campo, int minimo, int maximo, int porDefecto) {
        return valor == null ? porDefecto : entero(valor, campo, minimo, maximo);
    }

    public static BigDecimal dinero(BigDecimal valor, String campo, String minimo, String maximo) {
        if (valor == null) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        if (valor.scale() > 2 && valor.stripTrailingZeros().scale() > 2) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " admite máximo 2 decimales");
        }
        if (valor.compareTo(new BigDecimal(minimo)) < 0 || valor.compareTo(new BigDecimal(maximo)) > 0) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe estar entre " + minimo + " y " + maximo);
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal dineroOpcional(BigDecimal valor, String campo, String minimo, String maximo) {
        return valor == null ? null : dinero(valor, campo, minimo, maximo);
    }

    public static boolean booleano(Boolean valor, boolean porDefecto) {
        return valor == null ? porDefecto : valor;
    }

    public static String opcion(String valor, String campo, Collection<String> opciones) {
        if (valor == null || !opciones.contains(valor)) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe ser uno de: " + String.join(", ", opciones));
        }
        return valor;
    }

    public static <T> List<T> lista(List<T> valor, String campo, int minimo, int maximo) {
        int tamano = valor == null ? 0 : valor.size();
        if (tamano < minimo || tamano > maximo) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " debe tener entre " + minimo + " y " + maximo + " elementos");
        }
        if (valor != null && valor.contains(null)) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " tiene elementos vacíos");
        }
        return valor == null ? List.of() : valor;
    }

    public static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + " es obligatorio");
        }
        return valor;
    }

    public static LocalDate fechaNacimientoOpcional(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        int edad = Period.between(fecha, LocalDate.now()).getYears();
        if (!fecha.isBefore(LocalDate.now()) || edad < 13 || edad > 120) {
            throw ApiExcepcion.solicitudInvalida("La fecha de nacimiento no es válida (edad entre 13 y 120 años)");
        }
        return fecha;
    }

    public static void datosUsuario(Usuario usuario) {
        usuario.setNombre(nombre(usuario.getNombre(), "nombre", 60));
        usuario.setApellidos(nombre(usuario.getApellidos(), "apellidos", 80));
        usuario.setEmail(email(usuario.getEmail()));
        usuario.setTelefono(telefonoOpcional(usuario.getTelefono()));
        usuario.setFechaNacimiento(fechaNacimientoOpcional(usuario.getFechaNacimiento()));
    }
}
