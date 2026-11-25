package cibertec.pe.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class ErroresController {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String mostrarDatosInvalidos(IllegalArgumentException error, Model modelo) {
        modelo.addAttribute("tituloError", "Revisa los datos ingresados");
        modelo.addAttribute("mensaje", error.getMessage() != null ? error.getMessage() : "Los datos enviados no son válidos.");
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String mostrarAccesoDenegado(AccessDeniedException error, Model modelo) {
        modelo.addAttribute("tituloError", "Acceso no permitido");
        modelo.addAttribute("mensaje", error.getMessage() != null ? error.getMessage() : "No tienes permisos para realizar esta acción.");
        return "error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String mostrarConflictoDatos(DataIntegrityViolationException error, Model modelo) {
        modelo.addAttribute("tituloError", "Conflicto con los datos existentes");
        modelo.addAttribute("mensaje", "Ya existe un registro con ese identificador o ruta.");
        return "error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String mostrarEstadoHttp(ResponseStatusException error,
                                    HttpServletResponse respuesta,
                                    Model modelo) {
        int codigo = error.getStatusCode().value();
        respuesta.setStatus(codigo);
        HttpStatus estado = HttpStatus.resolve(codigo);
        String detalle = error.getReason();
        if (detalle == null || detalle.isBlank()) {
            detalle = estado == HttpStatus.NOT_FOUND
                    ? "El recurso solicitado no existe o ya no está disponible."
                    : "No se pudo completar la operación solicitada.";
        }
        modelo.addAttribute("tituloError", estado != null ? estado.getReasonPhrase() : "Aviso del sistema");
        modelo.addAttribute("mensaje", detalle);
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String mostrarErrorGeneral(Exception error, Model modelo) {
        modelo.addAttribute("tituloError", "Ocurrió un inconveniente");
        modelo.addAttribute("mensaje", "No se pudo procesar tu solicitud en este momento.");
        return "error";
    }
}