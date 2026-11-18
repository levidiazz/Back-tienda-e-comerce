package com.tienda.servlets.admin;

import com.tienda.dao.ProductoAdminDAO;
import com.tienda.dao.ProductoDAO;
import com.tienda.modelos.Producto;
import com.tienda.modelos.Variante;
import com.tienda.servlets.ApiServlet;
import com.tienda.servlets.ProductoServlet;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;

@WebServlet("/api/admin/productos/*")
public class AdminProductoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;
    private static final List<String> TIPOS = List.of("ninguna", "talla", "numero", "capacidad", "presentacion");
    private static final String PRECIO_MAXIMO = "99999999.99";
    private static final int STOCK_MAXIMO = 100000;

    private final transient ProductoDAO productoDAO = new ProductoDAO();
    private final transient ProductoAdminDAO productoAdminDAO = new ProductoAdminDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, productoDAO.listar(
                    parametroId(request, "categoria"),
                    parametroId(request, "subcategoria"),
                    parametroTexto(request, "q", 60),
                    parametroBooleano(request, "ofertas"),
                    true,
                    parametroOpcion(request, "orden", ProductoServlet.ORDENES)));
        } else if (ruta.size() == 1) {
            ok(response, existente(sku(ruta.get(0))));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            Producto producto = leer(request, Producto.class);
            producto.setSku(Validar.sku(producto.getSku()));
            validar(producto);
            List<Variante> variantes = Validar.lista(producto.getVariantes(), "variantes", 0, 50);
            for (int i = 0; i < variantes.size(); i++) {
                validar(variantes.get(i), producto.getTipoOpciones(), "variantes[" + i + "]");
            }
            if (!"ninguna".equals(producto.getTipoOpciones()) && variantes.isEmpty()) {
                throw ApiExcepcion.solicitudInvalida("Un producto de tipo " + producto.getTipoOpciones() + " necesita al menos una variante");
            }
            if (productoDAO.buscar(producto.getSku(), true) != null) {
                throw ApiExcepcion.conflicto("Ya existe un producto con el SKU " + producto.getSku());
            }
            productoAdminDAO.insertar(producto);
            creado(response, productoDAO.buscar(producto.getSku(), true));
        } else if (ruta.size() == 2 && ruta.get(1).equals("variantes")) {
            Producto producto = existente(sku(ruta.get(0)));
            Variante variante = validar(leer(request, Variante.class), producto.getTipoOpciones(), "variante");
            productoAdminDAO.insertarVariante(producto.getSku(), variante);
            creado(response, productoDAO.buscar(producto.getSku(), true));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() == 1) {
            Producto actual = existente(sku(ruta.get(0)));
            Producto producto = validar(leer(request, Producto.class));
            if (!producto.getTipoOpciones().equals(actual.getTipoOpciones()) && !actual.getVariantes().isEmpty()) {
                throw ApiExcepcion.conflicto("No se puede cambiar el tipo de opciones de un producto que ya tiene variantes");
            }
            productoAdminDAO.actualizar(actual.getSku(), producto);
            ok(response, productoDAO.buscar(actual.getSku(), true));
        } else if (ruta.size() == 3 && ruta.get(1).equals("variantes")) {
            Producto producto = existente(sku(ruta.get(0)));
            Variante variante = validar(leer(request, Variante.class), producto.getTipoOpciones(), "variante");
            productoAdminDAO.actualizarVariante(producto.getSku(), id(ruta.get(2)), variante);
            ok(response, productoDAO.buscar(producto.getSku(), true));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() == 1) {
            productoAdminDAO.desactivar(sku(ruta.get(0)));
        } else if (ruta.size() == 3 && ruta.get(1).equals("variantes")) {
            productoAdminDAO.desactivarVariante(sku(ruta.get(0)), id(ruta.get(2)));
        } else {
            throw noEncontrada();
        }
        sinContenido(response);
    }

    private Producto existente(String sku) throws Exception {
        Producto producto = productoDAO.buscar(sku, true);
        if (producto == null) {
            throw ApiExcepcion.noEncontrado("El producto no existe");
        }
        return producto;
    }

    private Producto validar(Producto p) throws Exception {
        int idProveedor = Validar.id(p.getIdProveedor(), "idProveedor");
        if (!productoAdminDAO.existeProveedor(idProveedor)) {
            throw ApiExcepcion.solicitudInvalida("El proveedor no existe");
        }
        p.setNombre(Validar.texto(p.getNombre(), "nombre", 3, 120));
        p.setDescripcion(Validar.textoLargoOpcional(p.getDescripcion(), "descripcion", 2000));
        p.setPrecio(Validar.dinero(p.getPrecio(), "precio", "0.01", PRECIO_MAXIMO));
        p.setPrecioOferta(Validar.dineroOpcional(p.getPrecioOferta(), "precioOferta", "0.01", PRECIO_MAXIMO));
        if (p.getPrecioOferta() != null && p.getPrecioOferta().compareTo(p.getPrecio()) >= 0) {
            throw ApiExcepcion.solicitudInvalida("El precio de oferta debe ser menor al precio normal");
        }
        p.setTipoOpciones(p.getTipoOpciones() == null ? "ninguna" : Validar.opcion(p.getTipoOpciones(), "tipoOpciones", TIPOS));
        p.setStock(Validar.enteroOpcional(p.getStock(), "stock", 0, STOCK_MAXIMO, 0));
        if (!"ninguna".equals(p.getTipoOpciones()) && p.getStock() > 0) {
            throw ApiExcepcion.solicitudInvalida("Un producto con variantes lleva el stock en cada variante, no en el producto");
        }
        p.setRequiereEnvio(Validar.booleano(p.getRequiereEnvio(), true));
        p.setSoloMayores(Validar.booleano(p.getSoloMayores(), false));
        p.setActivo(Validar.booleano(p.getActivo(), true));
        p.setImagenUrl(Validar.patronOpcional(p.getImagenUrl(), "imagenUrl", 255, Validar.IMAGEN,
                "ruta img/nombre.jpg o una dirección https"));
        List<Integer> subcategorias = Validar.lista(p.getSubcategorias(), "subcategorias", 1, 10);
        for (Integer id : subcategorias) {
            Validar.id(id, "subcategorias");
        }
        if (new HashSet<>(subcategorias).size() != subcategorias.size()) {
            throw ApiExcepcion.solicitudInvalida("El campo subcategorias tiene elementos repetidos");
        }
        if (productoAdminDAO.contarSubcategorias(subcategorias) != subcategorias.size()) {
            throw ApiExcepcion.solicitudInvalida("Una o más subcategorías no existen");
        }
        return p;
    }

    private Variante validar(Variante v, String tipo, String campo) {
        if ("ninguna".equals(tipo)) {
            throw ApiExcepcion.solicitudInvalida("Un producto sin opciones no lleva variantes");
        }
        v.setTalla(Validar.patronOpcional(v.getTalla(), campo + ".talla", 10, Validar.OPCION_VARIANTE, "letras y números"));
        v.setColor(Validar.nombreOpcional(v.getColor(), campo + ".color", 30));
        v.setCapacidad(Validar.patronOpcional(v.getCapacidad(), campo + ".capacidad", 20, Validar.OPCION_VARIANTE, "ej. 128 GB"));
        v.setPresentacion(Validar.patronOpcional(v.getPresentacion(), campo + ".presentacion", 20, Validar.OPCION_VARIANTE, "ej. 500 g"));
        String requerido = switch (tipo) {
            case "talla", "numero" -> v.getTalla();
            case "capacidad" -> v.getCapacidad();
            default -> v.getPresentacion();
        };
        if (requerido == null) {
            String nombreCampo = switch (tipo) {
                case "talla", "numero" -> "talla";
                case "capacidad" -> "capacidad";
                default -> "presentacion";
            };
            throw ApiExcepcion.solicitudInvalida("El campo " + campo + "." + nombreCampo + " es obligatorio para productos de tipo " + tipo);
        }
        v.setPrecioExtra(v.getPrecioExtra() == null ? null : Validar.dinero(v.getPrecioExtra(), campo + ".precioExtra", "0", "99999.99"));
        v.setStock(Validar.enteroOpcional(v.getStock(), campo + ".stock", 0, STOCK_MAXIMO, 0));
        v.setActivo(Validar.booleano(v.getActivo(), true));
        return v;
    }
}
