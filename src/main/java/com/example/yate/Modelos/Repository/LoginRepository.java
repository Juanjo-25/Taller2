package com.example.yate.Modelos.Repository;

import java.util.List;
import java.util.Optional;
import com.example.yate.Modelos.Entity.Login;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRepository extends JpaRepository<Login, Long> {
    Optional<Login> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Login> findByActivoFalseOrderByIdAsc();
}
