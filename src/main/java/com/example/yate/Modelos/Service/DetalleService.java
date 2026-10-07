package com.example.yate.Modelos.Service;

import java.util.List;
import com.example.yate.Modelos.Entity.Detalle;
import com.example.yate.Modelos.DAO.DetalleDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DetalleService {
    private final DetalleDAO detalles;
    public DetalleService(DetalleDAO detalles) { this.detalles = detalles; }
    public List<Detalle> listarDetalles() { return detalles.findAllByOrderByIdDesc(); }
    public List<Detalle> listarDetallesCompra(Long id) { return detalles.findByEncabezadoIdOrderByIdAsc(id); }
}
