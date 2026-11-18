package com.tienda.servlets.admin;

import com.tienda.dao.ProveedorDAO;
import com.tienda.modelos.Proveedor;
import com.tienda.servlets.ApiServlet;
import com.tienda.util.ApiExcepcion;
import com.tienda.util.Validar;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@WebServlet("/api/admin/proveedores/*")
public class AdminProveedorServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    private final transient ProveedorDAO proveedorDAO = new ProveedorDAO();

    @Override
    protected void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.isEmpty()) {
            ok(response, proveedorDAO.listar());
        } else if (ruta.size() == 1) {
            ok(response, existente(id(ruta.get(0))));
        } else {
            throw noEncontrada();
        }
    }

    @Override
    protected void post(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!segmentos(request).isEmpty()) {
            throw noEncontrada();
        }
        int id = proveedorDAO.insertar(validar(leer(request, Proveedor.class)));
        creado(response, proveedorDAO.buscar(id));
    }

    @Override
    protected void put(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        int id = existente(id(ruta.get(0))).getIdProveedor();
        proveedorDAO.actualizar(id, validar(leer(request, Proveedor.class)));
        ok(response, proveedorDAO.buscar(id));
    }

    @Override
    protected void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> ruta = segmentos(request);
        if (ruta.size() != 1) {
            throw noEncontrada();
        }
        Proveedor proveedor = existente(id(ruta.get(0)));
        if (proveedor.getTotalProductos() > 0) {
            throw ApiExcepcion.conflicto("No se puede eliminar: el proveedor tiene productos");
        }
        proveedorDAO.eliminar(proveedor.getIdProveedor());
        sinContenido(response);
    }

    private Proveedor existente(int id) throws Exception {
        Proveedor proveedor = proveedorDAO.buscar(id);
        if (proveedor == null) {
            throw ApiExcepcion.noEncontrado("El proveedor no existe");
        }
        return proveedor;
    }

    private Proveedor validar(Proveedor p) {
        p.setRazonSocial(Validar.texto(p.getRazonSocial(), "razonSocial", 3, 120));
        p.setRfc(Validar.patron(p.getRfc() == null ? null : p.getRfc().toUpperCase(), "rfc", 12, 13, Validar.RFC,
                "12 caracteres para persona moral o 13 para persona física, ej. TOA010101AB1"));
        p.setContacto(Validar.nombreOpcional(p.getContacto(), "contacto", 80));
        p.setTelefono(Validar.telefonoOpcional(p.getTelefono()));
        p.setEmail(Validar.emailOpcional(p.getEmail()));
        return p;
    }
}
