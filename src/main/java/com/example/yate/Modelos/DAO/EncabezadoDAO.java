package com.example.yate.Modelos.DAO;

import java.util.List;
import com.example.yate.Modelos.Entity.Encabezado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncabezadoDAO extends JpaRepository<Encabezado, Long> {
    List<Encabezado> findAllByOrderByIdDesc();
    List<Encabezado> findByClienteLoginEmailOrderByIdDesc(String email);
    boolean existsByClienteId(Long id);
}
