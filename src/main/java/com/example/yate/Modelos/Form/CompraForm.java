package com.example.yate.Modelos.Form;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class CompraForm {
    @Valid @NotEmpty(message = "Seleccione al menos un producto")
    @Size(max = 200, message = "Máximo 200 productos por compra")
    private List<Linea> lineas = new ArrayList<>();
    public List<Linea> getLineas() { return lineas; }
    public void setLineas(List<Linea> lineas) { this.lineas = lineas; }

    public static class Linea {
        @NotNull @Positive
        private Long productoId;
        @NotNull(message = "Ingrese una cantidad")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        private Integer cantidad = 0;
        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}
