package com.example.yate.Modelos.DAO;

import com.example.yate.Modelos.Entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteDAO extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByLoginEmail(String email);
    boolean existsByLoginEmail(String email);
}
