package com.example.yate.Controllers;

import com.example.yate.Modelos.Entity.Rol;
import com.example.yate.Modelos.Form.RegistroForm;
import com.example.yate.Modelos.Service.LoginService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/login")
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/ingresar")
    public String ingresar(Model model) {
        model.addAttribute("necesitaAdministrador", loginService.necesitaAdministrador());
        return "login/ingresar";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        if (loginService.necesitaAdministrador()) {
            return "redirect:/login/inicializar";
        }
        prepararFormulario(model, false);
        return "login/registro";
    }

    @GetMapping("/inicializar")
    public String inicializar(Model model) {
        if (!loginService.necesitaAdministrador()) {
            return "redirect:/login/ingresar";
        }
        prepararFormulario(model, true);
        return "login/registro";
    }

    private void prepararFormulario(Model model, boolean inicial) {
        model.addAttribute("registro", new RegistroForm());
        model.addAttribute("inicial", inicial);
    }

    @PostMapping({"/registro", "/inicializar"})
    public String guardar(@Valid @ModelAttribute("registro") RegistroForm registro,
                          BindingResult resultado, Model model,
                          jakarta.servlet.http.HttpServletRequest solicitud, RedirectAttributes mensajes) {
        boolean inicial = solicitud.getRequestURI().equals(solicitud.getContextPath() + "/login/inicializar");
        if (inicial && !loginService.necesitaAdministrador()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("inicial", inicial);
        if (resultado.hasErrors()) {
            return "login/registro";
        }
        try {
            if (inicial) {
                loginService.crearAdministradorInicial(registro);
            } else {
                loginService.registrar(registro);
            }
        } catch (IllegalArgumentException error) {
            resultado.reject("registro", error.getMessage());
            return "login/registro";
        } catch (DataIntegrityViolationException error) {
            resultado.reject("registro", "Ya existe una cuenta con ese correo");
            return "login/registro";
        }
        mensajes.addFlashAttribute("mensaje", inicial
                ? "Superadministrador creado. Ya puede iniciar sesión."
                : "Registro completado. Un administrador debe aprobar su cuenta antes de iniciar sesión.");
        return "redirect:/login/ingresar";
    }

    @GetMapping("/pendientes")
    public String pendientes(Model model) {
        model.addAttribute("cuentas", loginService.listarPendientes());
        return "login/pendientes";
    }

    @PostMapping("/activar/{id}")
    public String activar(@PathVariable Long id, @RequestParam Rol rol, RedirectAttributes mensajes,
                          org.springframework.security.core.Authentication usuario) {
        loginService.activarCuenta(id, rol, usuario.getName());
        mensajes.addFlashAttribute("mensaje", "Cuenta activada correctamente");
        return "redirect:/login/pendientes";
    }
}
