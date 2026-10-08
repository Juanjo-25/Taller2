package com.example.yate.Controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MenuController {
    @ModelAttribute
    public void prepararMenu(Authentication autenticacion, Model model) {
        boolean sesionIniciada = autenticacion != null
                && autenticacion.isAuthenticated()
                && !(autenticacion instanceof AnonymousAuthenticationToken);
        model.addAttribute("sesionIniciada", sesionIniciada);
        model.addAttribute("esSuperAdmin", sesionIniciada && autenticacion.getAuthorities().stream()
                .anyMatch(permiso -> permiso.getAuthority().equals("ROLE_SUPER_ADMIN")));
        model.addAttribute("esAdmin", sesionIniciada && autenticacion.getAuthorities().stream()
                .anyMatch(permiso -> (permiso.getAuthority().equals("ROLE_ADMIN") || permiso.getAuthority().equals("ROLE_SUPER_ADMIN"))));
    }
}
