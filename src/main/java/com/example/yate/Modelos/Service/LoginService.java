package com.example.yate.Modelos.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import com.example.yate.Modelos.Entity.Login;
import com.example.yate.Modelos.Entity.Rol;
import com.example.yate.Modelos.Form.RegistroForm;
import com.example.yate.Modelos.DAO.LoginDAO;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginService implements UserDetailsService {
    private final LoginDAO loginDAO;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaccion;

    public LoginService(LoginDAO loginDAO, PasswordEncoder passwordEncoder, PlatformTransactionManager gestor) {
        this.loginDAO = loginDAO;
        this.passwordEncoder = passwordEncoder;
        this.transaccion = new TransactionTemplate(gestor);
    }

    public boolean necesitaAdministrador() {
        return loginDAO.count() == 0;
    }

    public List<Login> listarPendientes() {
        return loginDAO.findByActivoFalseOrderByIdAsc();
    }

    public Login buscarLogin(String email) {
        return loginDAO.findByEmail(normalizarCorreo(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    @Transactional
    public void registrar(RegistroForm datos) {
        if (necesitaAdministrador()) {
            throw new IllegalArgumentException("Primero debe crear el administrador inicial");
        }
        crearCuenta(datos, false, Rol.CLIENTE);
    }

    // Disponible únicamente mientras no exista ninguna cuenta.
    public synchronized void crearAdministradorInicial(RegistroForm datos) {
        transaccion.executeWithoutResult(estado -> {
            if (!necesitaAdministrador()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El administrador inicial ya fue creado");
            }
            crearCuenta(datos, true, Rol.SUPER_ADMIN);
        });
    }

    private void crearCuenta(RegistroForm datos, boolean activo, Rol rol) {
        String email = normalizarCorreo(datos.getEmail());
        if (loginDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }
        if (datos.getContrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña es demasiado larga; use menos caracteres");
        }
        Login login = new Login();
        login.setEmail(email);
        login.setContrasena(passwordEncoder.encode(datos.getContrasena()));
        login.setActivo(activo);
        login.setRol(rol);
        loginDAO.saveAndFlush(login);
    }

    @Transactional
    public void activarCuenta(Long id, Rol rol, String correoAdministrador) {
        Login responsable = buscarLogin(correoAdministrador);
        if (!responsable.isActivo() || (responsable.getRol() != Rol.SUPER_ADMIN
                && (responsable.getRol() != Rol.ADMIN || rol != Rol.CLIENTE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un superadministrador puede asignar roles de administración");
        }
        activarCuenta(id, rol);
    }

    @Transactional
    public void activarCuenta(Long id, Rol rol) {
        Login login = loginDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        if (login.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está activa");
        }
        login.setRol(rol);
        login.setActivo(true);
        loginDAO.save(login);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Login login = loginDAO.findByEmail(normalizarCorreo(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
        return User.withUsername(login.getEmail()).password(login.getContrasena())
                .roles(login.getRol().name()).disabled(!login.isActivo()).build();
    }

    private String normalizarCorreo(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
