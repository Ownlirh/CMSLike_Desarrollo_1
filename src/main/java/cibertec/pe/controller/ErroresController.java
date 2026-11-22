package cibertec.pe.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ErroresController {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String mostrarDatosInvalidos(IllegalArgumentException error, Model modelo) {
        modelo.addAttribute("mensaje", error.getMessage());
        return "datosInvalidos";
    }
}
