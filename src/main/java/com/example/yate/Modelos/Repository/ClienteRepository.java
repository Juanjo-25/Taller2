package com.example.yate.Modelos.Repository;

import com.example.yate.Modelos.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByLoginEmail(String email);
    boolean existsByLoginEmail(String email);
}
