package com.example.yate.Modelos.Service;

import java.util.List;
import com.example.yate.Modelos.Entity.Producto;
import com.example.yate.Modelos.Repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductoService {
    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    public Producto buscarProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Transactional
    public Producto guardarProducto(Producto datos) {
        Producto producto = datos.getId() == null ? new Producto() : buscarProducto(datos.getId());
        producto.setNombre(datos.getNombre().trim());
        producto.setDescripcion(datos.getDescripcion().trim());
        producto.setValorUnitario(datos.getValorUnitario());
        producto.setStock(datos.getStock());
        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        productoRepository.delete(buscarProducto(id));
    }
}
