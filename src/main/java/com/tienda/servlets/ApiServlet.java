package com.tienda.servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Json;
import com.tienda.util.Validar;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

public abstract class ApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final long TAMANO_MAXIMO = 64 * 1024;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            switch (request.getMethod()) {
                case "GET" -> get(request, response);
                case "POST" -> post(request, response);
                case "PUT" -> put(request, response);
                case "PATCH" -> patch(request, response);
                case "DELETE" -> delete(request, response);
                default -> throw metodoNoPermitido();
            }
        } catch (ApiExcepcion e) {
            error(response, e.getEstado(), e.getMessage());
        } catch (JsonMappingException e) {
            error(response, 400, mensajeJson(e));
        } catch (JsonProcessingException e) {
            error(response, 400, "El JSON enviado no es válido");
        } catch (SQLException e) {
            manejarErrorSql(response, e);
        } catch (Exception e) {
            log("Error no controlado", e);
            error(response, 500, "Error interno del servidor");
        }
    }

    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw metodoNoPermitido();
    }

    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw metodoNoPermitido();
    }

    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw metodoNoPermitido();
    }

    protected void patch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw metodoNoPermitido();
    }

    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw metodoNoPermitido();
    }

    protected <T> T leer(HttpServletRequest request, Class<T> tipo) throws IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw new ApiExcepcion(415, "El cuerpo debe enviarse como application/json");
        }
        if (request.getContentLengthLong() > TAMANO_MAXIMO) {
            throw new ApiExcepcion(413, "El cuerpo de la petición es demasiado grande");
        }
        T cuerpo = Json.MAPPER.readValue(request.getReader(), tipo);
        if (cuerpo == null) {
            throw ApiExcepcion.solicitudInvalida("Falta el cuerpo de la petición");
        }
        return cuerpo;
    }

    protected void ok(HttpServletResponse response, Object cuerpo) throws IOException {
        responder(response, 200, cuerpo);
    }

    protected void creado(HttpServletResponse response, Object cuerpo) throws IOException {
        responder(response, 201, cuerpo);
    }

    protected void sinContenido(HttpServletResponse response) {
        response.setStatus(204);
    }

    protected void responder(HttpServletResponse response, int estado, Object cuerpo) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Json.MAPPER.writeValue(response.getWriter(), cuerpo);
    }

    protected List<String> segmentos(HttpServletRequest request) {
        String ruta = request.getPathInfo();
        if (ruta == null || ruta.equals("/")) {
            return List.of();
        }
        List<String> partes = Arrays.stream(ruta.split("/")).filter(s -> !s.isEmpty()).toList();
        if (partes.size() > 4) {
            throw noEncontrada();
        }
        return partes;
    }

    protected int id(String valor) {
        if (valor == null || !valor.matches("^[1-9][0-9]{0,8}$")) {
            throw ApiExcepcion.noEncontrado("Recurso no encontrado");
        }
        return Integer.parseInt(valor);
    }

    protected String sku(String valor) {
        if (valor == null || valor.length() > 30 || !Validar.SKU.matcher(valor.toUpperCase()).matches()) {
            throw ApiExcepcion.noEncontrado("El producto no existe");
        }
        return valor.toUpperCase();
    }

    protected Integer parametroId(HttpServletRequest request, String nombre) {
        String valor = request.getParameter(nombre);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (!valor.matches("^[1-9][0-9]{0,8}$")) {
            throw ApiExcepcion.solicitudInvalida("El parámetro " + nombre + " debe ser un número mayor a 0");
        }
        return Integer.valueOf(valor);
    }

    protected String parametroTexto(HttpServletRequest request, String nombre, int maximo) {
        return Validar.textoOpcional(request.getParameter(nombre), nombre, maximo);
    }

    protected String parametroOpcion(HttpServletRequest request, String nombre, List<String> opciones) {
        String valor = request.getParameter(nombre);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return Validar.opcion(valor, nombre, opciones);
    }

    protected boolean parametroBooleano(HttpServletRequest request, String nombre) {
        String valor = request.getParameter(nombre);
        if (valor == null || valor.isBlank()) {
            return false;
        }
        return Boolean.parseBoolean(Validar.opcion(valor, nombre, List.of("true", "false")));
    }

    protected Map<String, Object> mensaje(String texto) {
        return Map.of("mensaje", texto);
    }

    protected ApiExcepcion noEncontrada() {
        return ApiExcepcion.noEncontrado("Ruta no encontrada");
    }

    private ApiExcepcion metodoNoPermitido() {
        return new ApiExcepcion(405, "Método no permitido");
    }

    private void error(HttpServletResponse response, int estado, String mensaje) throws IOException {
        responder(response, estado, Map.of("error", mensaje));
    }

    private String mensajeJson(JsonMappingException e) {
        String campo = e.getPath().stream()
                .map(r -> r.getFieldName() != null ? r.getFieldName() : "[" + r.getIndex() + "]")
                .collect(Collectors.joining("."));
        return campo.isEmpty() ? "El JSON enviado no es válido" : "El campo " + campo + " tiene un tipo de dato incorrecto";
    }

    private void manejarErrorSql(HttpServletResponse response, SQLException e) throws IOException {
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String detalle = detalle(e);
        switch (estado) {
            case "23505" -> error(response, 409, "Ya existe un registro con esos datos");
            case "23503" -> {
                if (detalle.contains("is not present")) {
                    error(response, 400, "Uno de los registros relacionados no existe");
                } else {
                    error(response, 409, "No se puede completar porque el registro tiene datos relacionados");
                }
            }
            case "23502", "23514", "22P02", "22001", "22003", "22007", "22008" ->
                    error(response, 400, "Los datos no cumplen las reglas de la base de datos");
            case "P0001" -> error(response, 400, mensajeServidor(e));
            default -> {
                log("Error de base de datos", e);
                error(response, 500, "Error en la base de datos");
            }
        }
    }

    private String detalle(SQLException e) {
        if (e instanceof PSQLException psql) {
            ServerErrorMessage mensaje = psql.getServerErrorMessage();
            if (mensaje != null && mensaje.getDetail() != null) {
                return mensaje.getDetail();
            }
        }
        return "";
    }

    private String mensajeServidor(SQLException e) {
        if (e instanceof PSQLException psql && psql.getServerErrorMessage() != null) {
            return psql.getServerErrorMessage().getMessage();
        }
        return "No se pudo completar la operación";
    }
}
