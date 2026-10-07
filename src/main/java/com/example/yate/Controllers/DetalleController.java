package com.example.yate.Controllers;

import com.example.yate.Modelos.Service.DetalleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/detalles")
public class DetalleController {
    private final DetalleService detalles;
    public DetalleController(DetalleService detalles) { this.detalles = detalles; }
    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        model.addAttribute("detalles", detalles.listarDetalles());
        return "detalles/listar";
    }
}
