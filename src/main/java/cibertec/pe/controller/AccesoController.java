package cibertec.pe.controller;

import cibertec.pe.service.IUsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccesoController {
    private final IUsuarioService usuarios;

    public AccesoController(IUsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/admin/ingresar")
    public String mostrarIngreso(@RequestParam(required = false) String volver, Model modelo) {
        modelo.addAttribute("volver", "/".equals(volver) ? "/" : "");
        return "ingreso";
    }

    @GetMapping("/admin/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/admin/registro")
    public String registrarCuenta(@RequestParam String nombre,
                                  @RequestParam String clave,
                                  @RequestParam String confirmarClave,
                                  Model modelo,
                                  RedirectAttributes redireccion) {
        if (!clave.equals(confirmarClave)) {
            modelo.addAttribute("error", "Las contraseñas no coinciden.");
            modelo.addAttribute("nombre", nombre);
            return "registro";
        }
        try {
            usuarios.registrarPublico(nombre, clave);
            redireccion.addFlashAttribute("registrado", true);
            return "redirect:/admin/ingresar";
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("error", e.getMessage());
            modelo.addAttribute("nombre", nombre);
            return "registro";
        }
    }
}