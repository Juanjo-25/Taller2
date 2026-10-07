package com.example.yate.Controllers;

import com.example.yate.Modelos.Service.ClienteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final ClienteService clienteService;

    public HomeController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping({"/", "/home"})
    public String index(Authentication usuario) {
        boolean cliente = usuario.getAuthorities().stream()
                .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_CLIENTE"));
        if (cliente && !clienteService.tienePerfil(usuario.getName())) {
            return "redirect:/clientes/perfil";
        }
        return "home";
    }
}
