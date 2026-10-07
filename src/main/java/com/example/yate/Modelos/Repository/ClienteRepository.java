package com.example.yate.Modelos.Repository;

import com.example.yate.Modelos.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
