package com.example.yate.Controllers;

import com.example.yate.Modelos.Form.CompraForm;
import com.example.yate.Modelos.Service.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/encabezados")
public class EncabezadoController {
    private final EncabezadoService encabezados;
    private final DetalleService detalles;
    private final ProductoService productos;
    private final ClienteService clientes;
    public EncabezadoController(EncabezadoService encabezados, DetalleService detalles,
                                 ProductoService productos, ClienteService clientes) {
        this.encabezados = encabezados; this.detalles = detalles;
        this.productos = productos; this.clientes = clientes;
    }
    @InitBinder("compra")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("lineas[*].productoId", "lineas[*].cantidad");
        binder.setAutoGrowCollectionLimit(200);
    }
    private boolean esAdmin(Authentication usuario) {
        return usuario.getAuthorities().stream().anyMatch(a -> (a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN")));
    }
    @GetMapping({"", "/listar"})
    public String listar(Authentication usuario, Model model) {
        model.addAttribute("compras", encabezados.listarCompras(usuario.getName(), esAdmin(usuario)));
        return "encabezados/listar";
    }
    @GetMapping("/compra")
    public String compra(Authentication usuario, Model model) {
        if (!clientes.tienePerfil(usuario.getName())) { return "redirect:/clientes/perfil"; }
        CompraForm compra = new CompraForm();
        for (var producto : productos.listarProductos().stream().limit(200).toList()) {
            CompraForm.Linea linea = new CompraForm.Linea();
            linea.setProductoId(producto.getId()); compra.getLineas().add(linea);
        }
        model.addAttribute("compra", compra);
        prepararCompra(model);
        return "encabezados/compra";
    }
    private void prepararCompra(Model model) {
        model.addAttribute("productos", productos.listarProductos().stream().limit(200).toList());
        java.util.Map<Long, Integer> cantidades = new java.util.HashMap<>();
        CompraForm compra = (CompraForm) model.getAttribute("compra");
        if (compra != null && compra.getLineas() != null) {
            for (var linea : compra.getLineas()) {
                if (linea != null && linea.getProductoId() != null && linea.getCantidad() != null) {
                    cantidades.put(linea.getProductoId(), linea.getCantidad());
                }
            }
        }
        model.addAttribute("cantidades", cantidades);
    }
    @PostMapping("/guardar")
    public String guardar(Authentication usuario, @Valid @ModelAttribute("compra") CompraForm compra,
                           BindingResult resultado, Model model, RedirectAttributes mensajes) {
        if (!clientes.tienePerfil(usuario.getName())) { return "redirect:/clientes/perfil"; }
        if (!resultado.hasErrors()) {
            try {
                var encabezado = encabezados.crearCompra(usuario.getName(), compra);
                mensajes.addFlashAttribute("mensaje", "Compra realizada correctamente");
                mensajes.addFlashAttribute("imprimirFactura", true);
                return "redirect:/encabezados/ver/" + encabezado.getId();
            } catch (IllegalArgumentException error) {
                resultado.reject("compra", error.getMessage());
            } catch (DataAccessException error) {
                resultado.reject("compra", "No se pudo confirmar la compra. Revise el stock e intente nuevamente.");
            }
        }
        prepararCompra(model);
        return "encabezados/compra";
    }
    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Authentication usuario, Model model) {
        model.addAttribute("encabezado", encabezados.buscarCompra(id, usuario.getName(), esAdmin(usuario)));
        model.addAttribute("detalles", detalles.listarDetallesCompra(id));
        return "encabezados/ver";
    }
}
