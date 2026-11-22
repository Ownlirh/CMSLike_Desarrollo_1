package cibertec.pe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccesoController {
    @GetMapping("/admin/ingresar")
    public String mostrarIngreso() {
        return "ingreso";
    }
}
