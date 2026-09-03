package com.example.yate.Controllers;

import java.util.Date;

import com.example.yate.Modelos.Entity.Cliente;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/clientes")
public class ClienteController {
    @GetMapping
    public String index() {
        return "/clientes/listar";
    }

    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("titulo","Listado clientes");

        Cliente cliente = new Cliente("Juan", "Martinez", "juan@example.com", 1L, new Date());
       model.addAttribute("datos", cliente.toString());
        return "/listar";
    }    
}
