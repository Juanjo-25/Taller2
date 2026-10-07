package com.example.yate;

import java.math.BigDecimal;
import java.util.concurrent.*;
import com.example.yate.Modelos.Entity.*;
import com.example.yate.Modelos.Form.CompraForm;
import com.example.yate.Modelos.DAO.*;
import com.example.yate.Modelos.Service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:compras;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@WithMockUser(username = "cliente@example.com", roles = "CLIENTE")
class CompraTests {
    @Autowired MockMvc mvc;
    @Autowired EncabezadoService servicio;
    @Autowired ProductoService productoService;
    @Autowired ClienteService clienteService;
    @Autowired EncabezadoDAO encabezados;
    @Autowired DetalleDAO detalles;
    @Autowired ProductoDAO productos;
    @Autowired ClienteDAO clientes;
    @Autowired LoginDAO cuentas;
    Producto primero, segundo;
    Cliente cliente;

    @BeforeEach
    void preparar() {
        detalles.deleteAll(); encabezados.deleteAll(); clientes.deleteAll(); cuentas.deleteAll(); productos.deleteAll();
        Login login = new Login(); login.setEmail("cliente@example.com"); login.setContrasena("hash-de-prueba");
        login.setActivo(true); login.setRol(Rol.CLIENTE); cuentas.saveAndFlush(login);
        cliente = new Cliente("Ana", "Perez", login.getEmail(), null, new java.util.Date());
        cliente.setLogin(login); cliente = clientes.saveAndFlush(cliente);
        primero = producto("Mesa", "10.25", 5); segundo = producto("Silla", "3.50", 2);
    }
    private Producto producto(String nombre, String precio, int stock) {
        Producto p = new Producto(); p.setNombre(nombre); p.setDescripcion("Descripción");
        p.setValorUnitario(new BigDecimal(precio)); p.setStock(stock); return productos.saveAndFlush(p);
    }
    private CompraForm seleccion(Long id, int cantidad) {
        CompraForm f = new CompraForm(); CompraForm.Linea l = new CompraForm.Linea();
        l.setProductoId(id); l.setCantidad(cantidad); f.getLineas().add(l); return f;
    }

    @Test
    void comprarCalculaImportesGuardaDetallesYDescuentaStock() throws Exception {
        mvc.perform(get("/encabezados/compra")).andExpect(status().isOk());
        mvc.perform(post("/encabezados/guardar").with(csrf())
                .param("lineas[0].productoId", primero.getId().toString()).param("lineas[0].cantidad", "2")
                .param("lineas[1].productoId", segundo.getId().toString()).param("lineas[1].cantidad", "1")
                .param("total", "0").param("cliente.id", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("imprimirFactura", true));
        Encabezado compra = encabezados.findAll().getFirst();
        assertThat(compra.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(compra.getFecha()).isNotNull();
        assertThat(compra.getTotal()).isEqualByComparingTo("24.00");
        assertThat(detalles.findByEncabezadoIdOrderByIdAsc(compra.getId())).hasSize(2);
        assertThat(productos.findById(primero.getId()).orElseThrow().getStock()).isEqualTo(3);
        assertThat(productos.findById(segundo.getId()).orElseThrow().getStock()).isEqualTo(1);
        productos.findById(primero.getId()).ifPresent(p -> {
            p.setValorUnitario(new BigDecimal("99.00")); productos.saveAndFlush(p);
        });
        mvc.perform(get("/encabezados/ver/" + compra.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Factura #" + compra.getId())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10,25")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("24,00")));
        mvc.perform(get("/encabezados/listar")).andExpect(status().isOk());
        mvc.perform(get("/detalles/listar").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        assertThatThrownBy(() -> productoService.eliminarProducto(primero.getId())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> clienteService.eliminarCliente(cliente.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stockInsuficienteRevierteCambiosDeTodaLaCompra() {
        CompraForm compra = seleccion(primero.getId(), 1);
        compra.getLineas().add(seleccion(segundo.getId(), 3).getLineas().getFirst());
        assertThatThrownBy(() -> servicio.crearCompra("cliente@example.com", compra)).isInstanceOf(IllegalArgumentException.class);
        assertThat(encabezados.count()).isZero(); assertThat(detalles.count()).isZero();
        assertThat(productos.findById(primero.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(productos.findById(segundo.getId()).orElseThrow().getStock()).isEqualTo(2);
    }

    @Test
    void rechazarSinPerfilSinSeleccionDuplicadosYDatosInvalidos() throws Exception {
        assertThatThrownBy(() -> servicio.crearCompra("sinperfil@example.com", seleccion(primero.getId(), 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.crearCompra("cliente@example.com", seleccion(primero.getId(), 0)))
                .isInstanceOf(IllegalArgumentException.class);
        CompraForm duplicada = seleccion(primero.getId(), 1);
        duplicada.getLineas().add(seleccion(primero.getId(), 1).getLineas().getFirst());
        assertThatThrownBy(() -> servicio.crearCompra("cliente@example.com", duplicada)).isInstanceOf(IllegalArgumentException.class);
        mvc.perform(post("/encabezados/guardar").with(csrf()).param("lineas[0].productoId", primero.getId().toString())
                .param("lineas[0].cantidad", "-1")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("compra", "lineas[0].cantidad"));
        mvc.perform(get("/encabezados/compra").with(user("sinperfil@example.com").roles("CLIENTE")))
                .andExpect(redirectedUrl("/clientes/perfil"));
        assertThat(encabezados.count()).isZero();
    }

    @Test
    void clienteSoloVeSusComprasYAdminPuedeConsultarTodas() throws Exception {
        Encabezado compra = servicio.crearCompra("cliente@example.com", seleccion(primero.getId(), 1));
        mvc.perform(get("/encabezados/ver/" + compra.getId()).with(user("otro@example.com").roles("CLIENTE")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/encabezados/listar").with(user("otro@example.com").roles("CLIENTE")))
                .andExpect(model().attribute("compras", org.hamcrest.Matchers.empty()));
        mvc.perform(get("/encabezados/ver/" + compra.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/detalles/listar")).andExpect(status().isForbidden());
        mvc.perform(post("/encabezados/guardar")).andExpect(status().isForbidden());
        mvc.perform(get("/encabezados/compra").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void comprasConcurrentesNoVendenLaMismaUnidad() throws Exception {
        Producto ultima = producto("Última unidad", "2.00", 1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Boolean> comprar = () -> {
            inicio.await();
            try { servicio.crearCompra("cliente@example.com", seleccion(ultima.getId(), 1)); return true; }
            catch (IllegalArgumentException error) { return false; }
        };
        try {
            Future<Boolean> a = hilos.submit(comprar), b = hilos.submit(comprar); inicio.countDown();
            int exitos = (a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertThat(exitos).isEqualTo(1);
            assertThat(productos.findById(ultima.getId()).orElseThrow().getStock()).isZero();
            assertThat(encabezados.count()).isEqualTo(1); assertThat(detalles.count()).isEqualTo(1);
        } finally { hilos.shutdownNow(); }
    }
}
