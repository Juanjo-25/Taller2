package com.example.yate.Controllers;

import com.example.yate.Modelos.Entity.Producto;
import com.example.yate.Modelos.Service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
public class ProductoController {
    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @InitBinder("producto")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("id", "nombre", "descripcion", "valorUnitario", "stock");
    }

    @GetMapping
    public String index() {
        return "redirect:/productos/listar";
    }

    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("titulo", "Listado de productos");
        model.addAttribute("productos", productoService.listarProductos());
        return "productos/listar";
    }

    @GetMapping("/form")
    public String crear(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Nuevo producto");
        return "productos/form";
    }

    @GetMapping("/form/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("producto", productoService.buscarProducto(id));
        model.addAttribute("titulo", "Editar producto");
        return "productos/form";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("producto") Producto producto,
                          BindingResult resultado, Model model, RedirectAttributes mensajes) {
        if (producto.getId() != null) {
            productoService.buscarProducto(producto.getId());
        }
        if (resultado.hasErrors()) {
            model.addAttribute("titulo", producto.getId() == null ? "Nuevo producto" : "Editar producto");
            return "productos/form";
        }
        productoService.guardarProducto(producto);
        mensajes.addFlashAttribute("mensaje", "Producto guardado correctamente");
        return "redirect:/productos/listar";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes mensajes) {
        productoService.eliminarProducto(id);
        mensajes.addFlashAttribute("mensaje", "Producto eliminado correctamente");
        return "redirect:/productos/listar";
    }
}
