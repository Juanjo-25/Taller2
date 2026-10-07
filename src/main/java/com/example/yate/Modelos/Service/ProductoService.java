package com.example.yate.Modelos.Service;

import java.util.List;
import com.example.yate.Modelos.Entity.Producto;
import com.example.yate.Modelos.DAO.ProductoDAO;
import com.example.yate.Modelos.DAO.DetalleDAO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductoService {
    private final ProductoDAO productoDAO;
    private final DetalleDAO referencias;

    public ProductoService(ProductoDAO productoDAO, DetalleDAO referencias) {
        this.productoDAO = productoDAO;
        this.referencias = referencias;
    }

    public List<Producto> listarProductos() {
        return productoDAO.findAll();
    }

    public Producto buscarProducto(Long id) {
        return productoDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Transactional
    public Producto guardarProducto(Producto datos) {
        Producto producto = datos.getId() == null ? new Producto() : buscarProducto(datos.getId());
        producto.setNombre(datos.getNombre().trim());
        producto.setDescripcion(datos.getDescripcion().trim());
        producto.setValorUnitario(datos.getValorUnitario());
        producto.setStock(datos.getStock());
        return productoDAO.save(producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        if (referencias.existsByProductoId(id)) {
            throw new IllegalArgumentException("No puede eliminar un producto incluido en una compra");
        }
        productoDAO.delete(buscarProducto(id));
    }
}
