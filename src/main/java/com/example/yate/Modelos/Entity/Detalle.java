package com.example.yate.Modelos.Entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
public class Detalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "encabezado_id", nullable = false)
    private Encabezado encabezado;
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false, precision = 24, scale = 2)
    private BigDecimal valor;
    public Detalle() { }
    public Long getId() { return id; }
    public Encabezado getEncabezado() { return encabezado; }
    public void setEncabezado(Encabezado encabezado) { this.encabezado = encabezado; }
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    @jakarta.persistence.Transient
    public java.math.BigDecimal getPrecioUnitarioCompra() {
        return valor.divide(java.math.BigDecimal.valueOf(cantidad.longValue()), 2, java.math.RoundingMode.HALF_UP);
    }
}
