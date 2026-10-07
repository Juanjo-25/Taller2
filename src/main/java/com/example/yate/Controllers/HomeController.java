package com.example.yate.Controllers;

import com.example.yate.Modelos.Service.ClienteService;
import com.example.yate.Modelos.Service.ProductoService;
import com.example.yate.Modelos.Service.EncabezadoService;
import org.springframework.ui.Model;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final EncabezadoService encabezadoService;

    public HomeController(ClienteService clienteService, ProductoService productoService, EncabezadoService encabezadoService) {
        this.clienteService = clienteService;
        this.productoService = productoService;
        this.encabezadoService = encabezadoService;
    }

    @GetMapping({"/", "/home"})
    public String index(Authentication usuario, Model model) {
        boolean cliente = usuario.getAuthorities().stream()
                .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_CLIENTE"));
        if (cliente && !clienteService.tienePerfil(usuario.getName())) {
            return "redirect:/clientes/perfil";
        }
        var compras = encabezadoService.listarCompras(usuario.getName(), !cliente);
        model.addAttribute("compras", compras.stream().limit(5).toList());
        model.addAttribute("numeroVentas", compras.size());
        model.addAttribute("totalVentas", compras.stream().map(c -> c.getTotal()).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (!cliente) {
            var productos = productoService.listarProductos();
            model.addAttribute("numeroProductos", productos.size());
            model.addAttribute("unidades", productos.stream().mapToLong(p -> p.getStock()).sum());
            model.addAttribute("alertas", productos.stream().filter(p -> p.getStock() <= 5).toList());
        }
        return "home";
    }
}
