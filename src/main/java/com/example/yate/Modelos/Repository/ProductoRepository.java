package com.example.yate.Modelos.Repository;

import com.example.yate.Modelos.Entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
