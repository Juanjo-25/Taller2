package com.example.yate;

import com.example.yate.Modelos.Entity.*;
import com.example.yate.Modelos.Form.RegistroForm;
import com.example.yate.Modelos.Repository.*;
import com.example.yate.Modelos.Service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:login;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class LoginTests {
    @Autowired MockMvc mvc;
    @Autowired LoginRepository cuentas;
    @Autowired ClienteRepository clientes;
    @Autowired LoginService loginService;
    @Autowired PasswordEncoder codificador;

    @BeforeEach
    void preparar() {
        clientes.deleteAll();
        cuentas.deleteAll();
        RegistroForm registro = new RegistroForm();
        registro.setEmail("admin@example.com");
        registro.setContrasena("Admin123!");
        loginService.crearAdministradorInicial(registro);
    }

    private MockHttpSession ingresar(String email, String contrasena) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login/ingresar").with(csrf())
                .param("email", email).param("contrasena", contrasena))
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);
    }

    private Login registrar(String email) throws Exception {
        mvc.perform(post("/login/registro").with(csrf()).param("email", email)
                .param("contrasena", "Cliente123!"))
                .andExpect(redirectedUrl("/login/ingresar"));
        return cuentas.findByEmail(email).orElseThrow();
    }

    @Test
    void administradorInicialSoloUnaVez() throws Exception {
        cuentas.deleteAll();
        mvc.perform(get("/login/ingresar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Crear administrador inicial")));
        mvc.perform(get("/login/inicializar")).andExpect(status().isOk());
        mvc.perform(post("/login/inicializar").with(csrf()).param("email", "admin@example.com")
                .param("contrasena", "Admin123!"))
                .andExpect(redirectedUrl("/login/ingresar"));
        Login admin = cuentas.findByEmail("admin@example.com").orElseThrow();
        assertThat(admin.isActivo()).isTrue();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(codificador.matches("Admin123!", admin.getContrasena())).isTrue();
        mvc.perform(post("/login/inicializar").with(csrf()).param("email", "otro@example.com")
                .param("contrasena", "Admin123!"))
                .andExpect(status().isForbidden());
        assertThat(cuentas.count()).isEqualTo(1);
    }

    @Test
    void registroPendienteAprobacionLoginYSesion() throws Exception {
        Login cliente = registrar("cliente@example.com");
        assertThat(cliente.isActivo()).isFalse();
        assertThat(cliente.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(cliente.getContrasena()).isNotEqualTo("Cliente123!");
        mvc.perform(post("/login/ingresar").with(csrf()).param("email", cliente.getEmail())
                .param("contrasena", "Cliente123!"))
                .andExpect(redirectedUrl("/login/ingresar?error=inactiva"));
        MockHttpSession admin = ingresar("admin@example.com", "Admin123!");
        mvc.perform(get("/login/pendientes").session(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString(cliente.getEmail())));
        mvc.perform(post("/login/activar/" + cliente.getId()).session(admin).with(csrf())
                .param("rol", "CLIENTE")).andExpect(redirectedUrl("/login/pendientes"));
        mvc.perform(post("/login/ingresar").with(csrf()).param("email", cliente.getEmail())
                .param("contrasena", "incorrecta"))
                .andExpect(redirectedUrl("/login/ingresar?error=credenciales"));
        MockHttpSession sesion = ingresar(" CLIENTE@example.com ", "Cliente123!");
        mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/clientes/perfil"));
        mvc.perform(get("/clientes/perfil").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/clientes/listar").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(get("/login/pendientes").session(sesion)).andExpect(status().isForbidden());
        mvc.perform(post("/login/salir").session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/login/ingresar?salida"));
        assertThat(sesion.isInvalid()).isTrue();
        mvc.perform(get("/clientes/perfil")).andExpect(redirectedUrl("/login/ingresar"));
    }

    @Test
    void validacionesDuplicadosYProteccionCsrf() throws Exception {
        mvc.perform(get("/login/registro")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/login/registro").with(csrf()).param("email", "invalido")
                .param("contrasena", "123"))
                .andExpect(model().attributeHasFieldErrors("registro", "email", "contrasena"));
        assertThat(cuentas.count()).isEqualTo(1);
        mvc.perform(post("/login/registro").with(csrf()).param("email", " ADMIN@example.com ")
                .param("contrasena", "Cliente123!"))
                .andExpect(status().isOk());
        assertThat(cuentas.count()).isEqualTo(1);
        mvc.perform(post("/login/registro").param("email", "nuevo@example.com")
                .param("contrasena", "Cliente123!"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/clientes/listar")).andExpect(redirectedUrl("/login/ingresar"));
    }

    @Test
    void perfilSiemprePerteneceAlUsuarioAutenticado() throws Exception {
        Login cuenta = registrar("cliente@example.com");
        loginService.activarCuenta(cuenta.getId(), Rol.CLIENTE);
        Cliente ajeno = new Cliente("Otro", "Cliente", "otro@example.com", null, new java.util.Date());
        ajeno = clientes.saveAndFlush(ajeno);
        MockHttpSession sesion = ingresar(cuenta.getEmail(), "Cliente123!");
        mvc.perform(post("/clientes/perfil").session(sesion).with(csrf())
                .param("id", ajeno.getId().toString()).param("nombre", "Ana")
                .param("apellido", "Perez").param("email", "contacto@example.com")
                .param("login.id", cuentas.findByEmail("admin@example.com").orElseThrow().getId().toString()))
                .andExpect(redirectedUrl("/clientes/perfil"));
        assertThat(clientes.findById(ajeno.getId()).orElseThrow().getNombre()).isEqualTo("Otro");
        Cliente perfil = clientes.findByLoginEmail(cuenta.getEmail()).orElseThrow();
        assertThat(perfil.getNombre()).isEqualTo("Ana");
        assertThat(perfil.getEmail()).isEqualTo("contacto@example.com");
        mvc.perform(get("/").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Mi perfil")));
        mvc.perform(get("/clientes/perfil").session(sesion)).andExpect(status().isOk())
                .andExpect(content().string(containsString("contacto@example.com")));
        mvc.perform(post("/clientes/guardar").session(sesion).with(csrf())
                .param("id", ajeno.getId().toString()).param("nombre", "Alterado"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorAccedeHomeYAsignaRolAdmin() throws Exception {
        Login cuenta = registrar("nuevo@example.com");
        MockHttpSession admin = ingresar("admin@example.com", "Admin123!");
        mvc.perform(get("/").session(admin)).andExpect(status().isOk());
        mvc.perform(get("/clientes/listar").session(admin)).andExpect(status().isOk());
        mvc.perform(post("/login/activar/" + cuenta.getId()).session(admin).with(csrf())
                .param("rol", "ADMIN")).andExpect(redirectedUrl("/login/pendientes"));
        MockHttpSession nuevo = ingresar(cuenta.getEmail(), "Cliente123!");
        mvc.perform(get("/login/pendientes").session(nuevo)).andExpect(status().isOk());
        mvc.perform(post("/login/activar/" + cuenta.getId()).session(admin).with(csrf())
                .param("rol", "ADMIN")).andExpect(status().isConflict());
    }
}
