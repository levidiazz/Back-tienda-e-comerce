package com.tienda.modelos;

public class CambioEstadoPedido {

    private String estado;
    private String paqueteria;
    private String numeroGuia;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getPaqueteria() {
        return paqueteria;
    }

    public void setPaqueteria(String paqueteria) {
        this.paqueteria = paqueteria;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }
}
