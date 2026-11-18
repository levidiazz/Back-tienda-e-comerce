package com.tienda.modelos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Producto {

    private String sku;
    private Integer idProveedor;
    private String proveedor;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal precioOferta;
    private String tipoOpciones;
    private Integer stock;
    private Integer stockTotal;
    private Boolean requiereEnvio;
    private Boolean soloMayores;
    private String imagenUrl;
    private Boolean activo;
    private LocalDateTime fechaAlta;
    private BigDecimal calificacion;
    private Integer numResenas;
    private List<Integer> subcategorias;
    private List<String> categorias;
    private List<Variante> variantes;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getPrecioOferta() {
        return precioOferta;
    }

    public void setPrecioOferta(BigDecimal precioOferta) {
        this.precioOferta = precioOferta;
    }

    public String getTipoOpciones() {
        return tipoOpciones;
    }

    public void setTipoOpciones(String tipoOpciones) {
        this.tipoOpciones = tipoOpciones;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Integer stockTotal) {
        this.stockTotal = stockTotal;
    }

    public Boolean getRequiereEnvio() {
        return requiereEnvio;
    }

    public void setRequiereEnvio(Boolean requiereEnvio) {
        this.requiereEnvio = requiereEnvio;
    }

    public Boolean getSoloMayores() {
        return soloMayores;
    }

    public void setSoloMayores(Boolean soloMayores) {
        this.soloMayores = soloMayores;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public BigDecimal getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(BigDecimal calificacion) {
        this.calificacion = calificacion;
    }

    public Integer getNumResenas() {
        return numResenas;
    }

    public void setNumResenas(Integer numResenas) {
        this.numResenas = numResenas;
    }

    public List<Integer> getSubcategorias() {
        return subcategorias;
    }

    public void setSubcategorias(List<Integer> subcategorias) {
        this.subcategorias = subcategorias;
    }

    public List<String> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<String> categorias) {
        this.categorias = categorias;
    }

    public List<Variante> getVariantes() {
        return variantes;
    }

    public void setVariantes(List<Variante> variantes) {
        this.variantes = variantes;
    }
}
