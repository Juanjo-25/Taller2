package com.example.yate.Modelos.Entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    @Column(nullable = false, length = 1000)
    private String descripcion;
    @NotNull(message = "El valor unitario es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor unitario debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "Use hasta 10 enteros y 2 decimales")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorUnitario;
    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long version;

    public Producto() { }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
