package com.example.yate.Modelos.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import com.example.yate.Modelos.Entity.*;
import com.example.yate.Modelos.Form.CompraForm;
import com.example.yate.Modelos.DAO.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EncabezadoService {
    private final EncabezadoDAO encabezados;
    private final DetalleDAO detalles;
    private final ProductoDAO productos;
    private final ClienteDAO clientes;

    public EncabezadoService(EncabezadoDAO encabezados, DetalleDAO detalles,
                             ProductoDAO productos, ClienteDAO clientes) {
        this.encabezados = encabezados;
        this.detalles = detalles;
        this.productos = productos;
        this.clientes = clientes;
    }

    public List<Encabezado> listarCompras(String email, boolean admin) {
        return admin ? encabezados.findAllByOrderByIdDesc()
                : encabezados.findByClienteLoginEmailOrderByIdDesc(email);
    }

    public Encabezado buscarCompra(Long id, String email, boolean admin) {
        Encabezado compra = encabezados.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!admin && (compra.getCliente().getLogin() == null
                || !compra.getCliente().getLogin().getEmail().equals(email))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return compra;
    }

    @Transactional
    public Encabezado crearCompra(String email, CompraForm datos) {
        Cliente cliente = clientes.findByLoginEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Complete primero sus datos personales"));
        if (datos.getLineas() == null || datos.getLineas().size() > 200) {
            throw new IllegalArgumentException("La selección de productos no es válida");
        }
        Map<Long, Integer> seleccion = new TreeMap<>();
        for (CompraForm.Linea linea : datos.getLineas()) {
            if (linea == null || linea.getProductoId() == null || linea.getCantidad() == null
                    || linea.getCantidad() < 0 || linea.getProductoId() <= 0) {
                throw new IllegalArgumentException("La selección de productos no es válida");
            }
            if (linea.getCantidad() > 0 && seleccion.putIfAbsent(linea.getProductoId(), linea.getCantidad()) != null) {
                throw new IllegalArgumentException("No repita el mismo producto en la compra");
            }
        }
        if (seleccion.isEmpty()) {
            throw new IllegalArgumentException("Seleccione al menos un producto con cantidad mayor que cero");
        }
        // Bloquear en orden de ID evita vender las mismas unidades en compras concurrentes.
        List<Detalle> nuevasLineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var entrada : seleccion.entrySet()) {
            Producto producto = productos.buscarParaCompra(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no está disponible"));
            int cantidad = entrada.getValue();
            if (producto.getStock() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                        + ". Disponible: " + producto.getStock());
            }
            Detalle detalle = new Detalle();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setValor(producto.getValorUnitario().multiply(BigDecimal.valueOf(cantidad)));
            nuevasLineas.add(detalle);
            total = total.add(detalle.getValor());
            producto.setStock(producto.getStock() - cantidad);
        }
        Encabezado compra = new Encabezado();
        compra.setCliente(cliente);
        compra.setFecha(LocalDateTime.now());
        compra.setTotal(total);
        encabezados.save(compra);
        for (Detalle detalle : nuevasLineas) {
            detalle.setEncabezado(compra);
        }
        detalles.saveAll(nuevasLineas);
        detalles.flush();
        return compra;
    }
}
