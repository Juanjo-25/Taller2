package com.example.yate;

import com.example.yate.Modelos.DAO.ClienteDAO;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:crud;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ClienteCrudTests {
    @Autowired MockMvc mvc;
    @Autowired ClienteDAO repositorio;

    @Test
    void crearListarEditarEliminar() throws Exception {
        mvc.perform(get("/clientes")).andExpect(redirectedUrl("/clientes/listar"));
        mvc.perform(get("/clientes/form")).andExpect(status().isOk());
        mvc.perform(post("/clientes/guardar").with(csrf()).param("nombre", "Ana")
                .param("apellido", "Perez").param("email", "ana@example.com"))
                .andExpect(redirectedUrl("/clientes/listar"));
        var cliente = repositorio.findAll().getFirst();
        var fecha = cliente.getCreateAt();
        assertThat(fecha).isNotNull();
        mvc.perform(get("/clientes/listar")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ana@example.com")));
        mvc.perform(get("/clientes/form/" + cliente.getId())).andExpect(status().isOk());
        mvc.perform(post("/clientes/guardar").with(csrf()).param("id", cliente.getId().toString())
                .param("nombre", "Ana Maria").param("apellido", "Perez")
                .param("email", "ana@example.com").param("createAt", "01/01/2000"))
                .andExpect(redirectedUrl("/clientes/listar"));
        assertThat(repositorio.findById(cliente.getId()).orElseThrow().getNombre()).isEqualTo("Ana Maria");
        assertThat(repositorio.findById(cliente.getId()).orElseThrow().getCreateAt()).isEqualTo(fecha);
        mvc.perform(post("/clientes/eliminar/" + cliente.getId()).with(csrf()))
                .andExpect(redirectedUrl("/clientes/listar"));
        assertThat(repositorio.count()).isZero();
    }

    @Test
    void rechazarDatosInvalidosYClientesInexistentes() throws Exception {
        mvc.perform(post("/clientes/guardar").with(csrf()).param("nombre", " ")
                .param("apellido", "").param("email", "invalido"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("cliente", "nombre", "apellido", "email"));
        assertThat(repositorio.count()).isZero();
        mvc.perform(get("/clientes/form/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/clientes/eliminar/999999").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/clientes/eliminar/999999")).andExpect(status().isMethodNotAllowed());
    }
}
