package com.example.yate.Controllers;

import com.example.yate.Modelos.Entity.Cliente;
import com.example.yate.Modelos.Service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @InitBinder("cliente")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "apellido", "email");
    }

    @GetMapping
    public String index() {
        return "redirect:/clientes/listar";
    }

    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("titulo", "Listado de clientes");
        model.addAttribute("clientes", clienteService.listarClientes());
        return "clientes/listar";
    }

    @GetMapping("/form")
    public String crear(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("titulo", "Nuevo cliente");
        return "clientes/form";
    }

    @GetMapping("/form/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscarCliente(id));
        model.addAttribute("titulo", "Editar cliente");
        return "clientes/form";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("cliente") Cliente cliente,
                          BindingResult resultado, Model model, RedirectAttributes mensajes) {
        if (cliente.getId() != null) {
            clienteService.buscarCliente(cliente.getId());
        }
        if (resultado.hasErrors()) {
            model.addAttribute("titulo", cliente.getId() == null ? "Nuevo cliente" : "Editar cliente");
            return "clientes/form";
        }
        clienteService.guardarCliente(cliente);
        mensajes.addFlashAttribute("mensaje", "Cliente guardado correctamente");
        return "redirect:/clientes/listar";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes mensajes) {
        clienteService.eliminarCliente(id);
        mensajes.addFlashAttribute("mensaje", "Cliente eliminado correctamente");
        return "redirect:/clientes/listar";
    }
}
