package com.tienda.modelos;

import java.util.List;

public class NuevoPedido {

    private Integer idDireccion;
    private List<ArticuloPedido> articulos;

    public Integer getIdDireccion() {
        return idDireccion;
    }

    public void setIdDireccion(Integer idDireccion) {
        this.idDireccion = idDireccion;
    }

    public List<ArticuloPedido> getArticulos() {
        return articulos;
    }

    public void setArticulos(List<ArticuloPedido> articulos) {
        this.articulos = articulos;
    }
}
