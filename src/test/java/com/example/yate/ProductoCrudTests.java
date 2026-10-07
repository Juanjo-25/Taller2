package com.example.yate;

import com.example.yate.Modelos.Repository.ProductoRepository;
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
    "spring.datasource.url=jdbc:h2:mem:productos;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class ProductoCrudTests {
    @Autowired MockMvc mvc;
    @Autowired ProductoRepository repositorio;

    @Test
    void crearListarEditarEliminar() throws Exception {
        mvc.perform(get("/productos")).andExpect(redirectedUrl("/productos/listar"));
        mvc.perform(get("/productos/form")).andExpect(status().isOk());
        mvc.perform(post("/productos/guardar").with(csrf()).param("nombre", "Mesa")
                .param("descripcion", "Mesa de madera").param("valorUnitario", "150.50").param("stock", "5"))
                .andExpect(redirectedUrl("/productos/listar"));
        var producto = repositorio.findAll().getFirst();
        assertThat(producto.getValorUnitario()).isEqualByComparingTo("150.50");
        mvc.perform(get("/productos/listar")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mesa de madera")));
        mvc.perform(get("/productos/form/" + producto.getId())).andExpect(status().isOk());
        mvc.perform(post("/productos/guardar").with(csrf()).param("id", producto.getId().toString())
                .param("nombre", "Mesa grande").param("descripcion", "Mesa de madera")
                .param("valorUnitario", "200.00").param("stock", "0"))
                .andExpect(redirectedUrl("/productos/listar"));
        assertThat(repositorio.findById(producto.getId()).orElseThrow().getNombre()).isEqualTo("Mesa grande");
        assertThat(repositorio.findById(producto.getId()).orElseThrow().getStock()).isZero();
        mvc.perform(post("/productos/eliminar/" + producto.getId()).with(csrf()))
                .andExpect(redirectedUrl("/productos/listar"));
        assertThat(repositorio.count()).isZero();
    }

    @Test
    void rechazarDatosInvalidosYProductosInexistentes() throws Exception {
        mvc.perform(post("/productos/guardar").with(csrf()).param("nombre", " ")
                .param("descripcion", "").param("valorUnitario", "0").param("stock", "-1"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("producto", "nombre", "descripcion", "valorUnitario", "stock"));
        assertThat(repositorio.count()).isZero();
        mvc.perform(get("/productos/form/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/productos/eliminar/999999").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/productos/eliminar/999999")).andExpect(status().isMethodNotAllowed());
    }
    @Test
    void rechazarDecimalesEnStockYTextoEnPrecio() throws Exception {
        mvc.perform(post("/productos/guardar").with(csrf()).param("nombre", "Mesa")
                .param("descripcion", "Madera").param("valorUnitario", "texto").param("stock", "1.5"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("producto", "valorUnitario", "stock"));
        assertThat(repositorio.count()).isZero();
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNoPuedeGestionarProductos() throws Exception {
        mvc.perform(get("/productos/listar")).andExpect(status().isForbidden());
        mvc.perform(post("/productos/guardar").with(csrf()).param("nombre", "Mesa"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/productos/eliminar/1").with(csrf())).andExpect(status().isForbidden());
    }
}
