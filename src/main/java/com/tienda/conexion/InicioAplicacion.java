package com.tienda.conexion;

import com.tienda.dao.MigracionCifradoDAO;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class InicioAplicacion implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent evento) {
        try {
            int cifrados = new MigracionCifradoDAO().cifrarPendientes();
            if (cifrados > 0) {
                evento.getServletContext().log("Datos personales cifrados al iniciar: " + cifrados);
            }
        } catch (Exception e) {
            evento.getServletContext().log("No se pudieron cifrar los datos pendientes", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent evento) {
        ConexionDB.cerrar();
    }
}
