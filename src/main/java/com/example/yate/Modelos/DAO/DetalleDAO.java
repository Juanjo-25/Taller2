package com.example.yate.Modelos.DAO;

import java.util.List;
import com.example.yate.Modelos.Entity.Detalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleDAO extends JpaRepository<Detalle, Long> {
    List<Detalle> findByEncabezadoIdOrderByIdAsc(Long id);
    List<Detalle> findAllByOrderByIdDesc();
    boolean existsByProductoId(Long id);
}
