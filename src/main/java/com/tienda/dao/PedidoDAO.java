package com.tienda.dao;

import com.tienda.conexion.ConexionDB;
import com.tienda.modelos.ArticuloPedido;
import com.tienda.modelos.CambioEstadoPedido;
import com.tienda.modelos.DetallePedido;
import com.tienda.modelos.Direccion;
import com.tienda.modelos.NuevoPedido;
import com.tienda.modelos.Pedido;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Cifrado;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PedidoDAO {

    public static final BigDecimal COSTO_ENVIO = new BigDecimal("99.00");
    public static final List<String> ESTADOS = List.of("pendiente", "pagado", "enviado", "entregado", "cancelado");
    public static final Map<String, List<String>> TRANSICIONES = Map.of(
            "pendiente", List.of("cancelado"),
            "pagado", List.of("enviado", "cancelado"),
            "enviado", List.of("entregado"),
            "entregado", List.of(),
            "cancelado", List.of());

    private static final String SELECT_BASE = """
            SELECT p.*, d.id_usuario, u.nombre || ' ' || u.apellidos AS cliente, u.email,
                   mp.nombre AS metodo_pago,
                   (SELECT COALESCE(SUM(dp.cantidad), 0) FROM detalle_pedido dp WHERE dp.id_pedido = p.id_pedido) AS total_articulos
              FROM pedidos p
              JOIN direcciones d ON d.id_direccion = p.id_direccion
              JOIN usuarios u ON u.id_usuario = d.id_usuario
              LEFT JOIN pagos pg ON pg.id_pedido = p.id_pedido
              LEFT JOIN metodos_pago mp ON mp.id_metodo = pg.id_metodo
            """;

    public int crear(int idUsuario, NuevoPedido nuevo) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                validarDireccion(conn, idUsuario, nuevo.getIdDireccion());
                List<DetallePedido> detalles = new ArrayList<>();
                boolean requiereEnvio = false;
                for (ArticuloPedido articulo : nuevo.getArticulos()) {
                    Renglon renglon = prepararRenglon(conn, articulo);
                    requiereEnvio |= renglon.requiereEnvio();
                    detalles.add(renglon.detalle());
                }
                int idPedido;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO pedidos (id_direccion, costo_envio) VALUES (?, ?) RETURNING id_pedido")) {
                    ps.setInt(1, nuevo.getIdDireccion());
                    ps.setBigDecimal(2, requiereEnvio ? COSTO_ENVIO : BigDecimal.ZERO);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        idPedido = rs.getInt(1);
                    }
                }
                String sqlDetalle = """
                        INSERT INTO detalle_pedido (id_pedido, sku, id_variante, cantidad, precio_unitario)
                        VALUES (?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    for (DetallePedido detalle : detalles) {
                        ps.setInt(1, idPedido);
                        ps.setString(2, detalle.getSku());
                        ps.setObject(3, detalle.getIdVariante());
                        ps.setInt(4, detalle.getCantidad());
                        ps.setBigDecimal(5, detalle.getPrecioUnitario());
                        ps.executeUpdate();
                    }
                }
                String sqlTotales = """
                        UPDATE pedidos
                           SET subtotal = t.suma, total = t.suma + costo_envio
                          FROM (SELECT SUM(importe) AS suma FROM detalle_pedido WHERE id_pedido = ?) t
                         WHERE id_pedido = ?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sqlTotales)) {
                    ps.setInt(1, idPedido);
                    ps.setInt(2, idPedido);
                    ps.executeUpdate();
                }
                conn.commit();
                return idPedido;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Pedido> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = SELECT_BASE + " WHERE d.id_usuario = ? ORDER BY p.fecha_pedido DESC, p.id_pedido DESC";
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        pedidos.add(mapear(rs));
                    }
                }
            }
            for (Pedido pedido : pedidos) {
                pedido.setDetalles(listarDetalles(conn, pedido.getIdPedido()));
            }
        }
        return pedidos;
    }

    public List<Pedido> listarTodos(String estado) throws SQLException {
        String sql = SELECT_BASE + (estado == null ? "" : " WHERE p.estado = ?::estado_pedido")
                + " ORDER BY p.fecha_pedido DESC, p.id_pedido DESC";
        List<Pedido> pedidos = new ArrayList<>();
        try (Connection conn = ConexionDB.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (estado != null) {
                ps.setString(1, estado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(mapear(rs));
                }
            }
        }
        return pedidos;
    }

    public Pedido buscar(int idPedido) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            Pedido pedido;
            try (PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " WHERE p.id_pedido = ?")) {
                ps.setInt(1, idPedido);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    pedido = mapear(rs);
                }
            }
            pedido.setDireccion(buscarDireccion(conn, pedido.getIdDireccion()));
            pedido.setDetalles(listarDetalles(conn, idPedido));
            pedido.setPago(new PagoDAO().buscarPorPedido(conn, idPedido));
            return pedido;
        }
    }

    public void cambiarEstado(int idPedido, CambioEstadoPedido cambio) throws SQLException {
        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            try {
                String actual = bloquearEstado(conn, idPedido);
                if (!TRANSICIONES.get(actual).contains(cambio.getEstado())) {
                    throw ApiExcepcion.conflicto("Un pedido " + actual + " no puede pasar a " + cambio.getEstado());
                }
                if ("cancelado".equals(cambio.getEstado())) {
                    regresarStock(conn, idPedido);
                }
                String sql = """
                        UPDATE pedidos
                           SET estado = ?::estado_pedido,
                               paqueteria = COALESCE(?, paqueteria),
                               numero_guia = COALESCE(?, numero_guia)
                         WHERE id_pedido = ?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, cambio.getEstado());
                    ps.setString(2, cambio.getPaqueteria());
                    ps.setString(3, cambio.getNumeroGuia());
                    ps.setInt(4, idPedido);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void cancelarPorCliente(int idUsuario, int idPedido) throws SQLException {
        Pedido pedido = buscar(idPedido);
        if (pedido == null || pedido.getIdUsuario() != idUsuario) {
            throw ApiExcepcion.noEncontrado("El pedido no existe");
        }
        if (!"pendiente".equals(pedido.getEstado())) {
            throw ApiExcepcion.conflicto("Solo se pueden cancelar pedidos pendientes de pago");
        }
        CambioEstadoPedido cambio = new CambioEstadoPedido();
        cambio.setEstado("cancelado");
        cambiarEstado(idPedido, cambio);
    }

    private Direccion buscarDireccion(Connection conn, int idDireccion) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM direcciones WHERE id_direccion = ?")) {
            ps.setInt(1, idDireccion);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Direccion direccion = new Direccion();
                direccion.setIdDireccion(rs.getInt("id_direccion"));
                direccion.setIdUsuario(rs.getInt("id_usuario"));
                direccion.setCalle(Cifrado.descifrar(rs.getString("calle")));
                direccion.setNumero(Cifrado.descifrar(rs.getString("numero")));
                direccion.setColonia(rs.getString("colonia"));
                direccion.setCiudad(rs.getString("ciudad"));
                direccion.setEstado(rs.getString("estado"));
                direccion.setCodigoPostal(rs.getString("codigo_postal"));
                direccion.setReferencias(Cifrado.descifrar(rs.getString("referencias")));
                direccion.setEsPrincipal(rs.getBoolean("es_principal"));
                return direccion;
            }
        }
    }

    private String bloquearEstado(Connection conn, int idPedido) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT estado FROM pedidos WHERE id_pedido = ? FOR UPDATE")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw ApiExcepcion.noEncontrado("El pedido no existe");
                }
                return rs.getString(1);
            }
        }
    }

    private void regresarStock(Connection conn, int idPedido) throws SQLException {
        String sqlVariantes = """
                UPDATE variantes_producto v
                   SET stock = v.stock + d.cantidad
                  FROM (SELECT id_variante, SUM(cantidad) AS cantidad FROM detalle_pedido
                         WHERE id_pedido = ? AND id_variante IS NOT NULL GROUP BY id_variante) d
                 WHERE v.id_variante = d.id_variante
                """;
        String sqlProductos = """
                UPDATE productos p
                   SET stock = p.stock + d.cantidad
                  FROM (SELECT sku, SUM(cantidad) AS cantidad FROM detalle_pedido
                         WHERE id_pedido = ? AND id_variante IS NULL GROUP BY sku) d
                 WHERE p.sku = d.sku
                """;
        for (String sql : List.of(sqlVariantes, sqlProductos)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPedido);
                ps.executeUpdate();
            }
        }
    }

    private void validarDireccion(Connection conn, int idUsuario, int idDireccion) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM direcciones WHERE id_direccion = ? AND id_usuario = ?")) {
            ps.setInt(1, idDireccion);
            ps.setInt(2, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw ApiExcepcion.solicitudInvalida("La dirección no pertenece a tu cuenta");
                }
            }
        }
    }

    private Renglon prepararRenglon(Connection conn, ArticuloPedido articulo) throws SQLException {
        String sqlProducto = """
                SELECT nombre, activo, tipo_opciones, COALESCE(precio_oferta, precio) AS precio, stock, requiere_envio
                  FROM productos WHERE sku = ? FOR UPDATE
                """;
        DetallePedido detalle = new DetallePedido();
        detalle.setSku(articulo.getSku());
        detalle.setCantidad(articulo.getCantidad());
        boolean requiereEnvio;
        try (PreparedStatement ps = conn.prepareStatement(sqlProducto)) {
            ps.setString(1, articulo.getSku());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !rs.getBoolean("activo")) {
                    throw ApiExcepcion.solicitudInvalida("El producto " + articulo.getSku() + " no está disponible");
                }
                detalle.setNombre(rs.getString("nombre"));
                detalle.setPrecioUnitario(rs.getBigDecimal("precio"));
                requiereEnvio = rs.getBoolean("requiere_envio");
                if ("ninguna".equals(rs.getString("tipo_opciones"))) {
                    if (articulo.getIdVariante() != null) {
                        throw ApiExcepcion.solicitudInvalida("El producto " + detalle.getNombre() + " no tiene variantes");
                    }
                    validarStock(rs.getInt("stock"), articulo.getCantidad(), detalle.getNombre());
                    return new Renglon(detalle, requiereEnvio);
                }
            }
        }
        if (articulo.getIdVariante() == null) {
            throw ApiExcepcion.solicitudInvalida("Elige una opción para " + detalle.getNombre());
        }
        String sqlVariante = """
                SELECT precio_extra, stock FROM variantes_producto
                 WHERE id_variante = ? AND sku = ? AND activo FOR UPDATE
                """;
        try (PreparedStatement ps = conn.prepareStatement(sqlVariante)) {
            ps.setInt(1, articulo.getIdVariante());
            ps.setString(2, articulo.getSku());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw ApiExcepcion.solicitudInvalida("La opción elegida de " + detalle.getNombre() + " no existe");
                }
                validarStock(rs.getInt("stock"), articulo.getCantidad(), detalle.getNombre());
                detalle.setIdVariante(articulo.getIdVariante());
                detalle.setPrecioUnitario(detalle.getPrecioUnitario().add(rs.getBigDecimal("precio_extra")));
            }
        }
        return new Renglon(detalle, requiereEnvio);
    }

    private void validarStock(int disponible, int cantidad, String nombre) {
        if (disponible < cantidad) {
            throw ApiExcepcion.conflicto("No hay suficiente stock de " + nombre + " (disponibles: " + disponible + ")");
        }
    }

    private List<DetallePedido> listarDetalles(Connection conn, int idPedido) throws SQLException {
        String sql = """
                SELECT d.*, p.nombre, p.imagen_url,
                       NULLIF(CONCAT_WS(' · ', v.talla, v.color, v.capacidad, v.presentacion), '') AS variante
                  FROM detalle_pedido d
                  JOIN productos p ON p.sku = d.sku
                  LEFT JOIN variantes_producto v ON v.id_variante = d.id_variante
                 WHERE d.id_pedido = ?
                 ORDER BY d.id_detalle
                """;
        List<DetallePedido> detalles = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePedido detalle = new DetallePedido();
                    detalle.setIdDetalle(rs.getInt("id_detalle"));
                    detalle.setIdPedido(rs.getInt("id_pedido"));
                    detalle.setSku(rs.getString("sku"));
                    detalle.setNombre(rs.getString("nombre"));
                    detalle.setImagenUrl(rs.getString("imagen_url"));
                    detalle.setIdVariante(rs.getObject("id_variante", Integer.class));
                    detalle.setVariante(rs.getString("variante"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    detalle.setImporte(rs.getBigDecimal("importe"));
                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(rs.getInt("id_pedido"));
        pedido.setIdDireccion(rs.getInt("id_direccion"));
        pedido.setIdUsuario(rs.getInt("id_usuario"));
        pedido.setCliente(rs.getString("cliente"));
        pedido.setEmail(rs.getString("email"));
        pedido.setFechaPedido(rs.getObject("fecha_pedido", LocalDateTime.class));
        pedido.setEstado(rs.getString("estado"));
        pedido.setSubtotal(rs.getBigDecimal("subtotal"));
        pedido.setCostoEnvio(rs.getBigDecimal("costo_envio"));
        pedido.setTotal(rs.getBigDecimal("total"));
        pedido.setPaqueteria(rs.getString("paqueteria"));
        pedido.setNumeroGuia(rs.getString("numero_guia"));
        pedido.setMetodoPago(rs.getString("metodo_pago"));
        pedido.setTotalArticulos(rs.getInt("total_articulos"));
        return pedido;
    }

    private record Renglon(DetallePedido detalle, boolean requiereEnvio) {
    }
}
