package com.tienda.modelos;

public class SolicitudPago {

    private Integer idMetodo;
    private String numeroTarjeta;
    private String titular;
    private String vencimiento;
    private String cvv;
    private String referenciaPaypal;

    public Integer getIdMetodo() {
        return idMetodo;
    }

    public void setIdMetodo(Integer idMetodo) {
        this.idMetodo = idMetodo;
    }

    public String getNumeroTarjeta() {
        return numeroTarjeta;
    }

    public void setNumeroTarjeta(String numeroTarjeta) {
        this.numeroTarjeta = numeroTarjeta;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getVencimiento() {
        return vencimiento;
    }

    public void setVencimiento(String vencimiento) {
        this.vencimiento = vencimiento;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getReferenciaPaypal() {
        return referenciaPaypal;
    }

    public void setReferenciaPaypal(String referenciaPaypal) {
        this.referenciaPaypal = referenciaPaypal;
    }
}
