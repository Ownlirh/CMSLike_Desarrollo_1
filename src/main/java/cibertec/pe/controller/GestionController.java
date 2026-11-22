package cibertec.pe.controller;

import cibertec.pe.config.SeguridadConfig;
import cibertec.pe.model.Rol;
import cibertec.pe.model.Usuario;
import cibertec.pe.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping(SeguridadConfig.PANEL)
public class GestionController {
    private final UsuarioImplement usuarios;
    private final PaginaImplement paginas;
    private final PermisoImplement permisos;
    private final AuditoriaImplement auditoria;
    private final SitioImplement sitio;

    public GestionController(UsuarioImplement usuarios, PaginaImplement paginas, PermisoImplement permisos,
                             AuditoriaImplement auditoria, SitioImplement sitio) {
        this.usuarios = usuarios;
        this.paginas = paginas;
        this.permisos = permisos;
        this.auditoria = auditoria;
        this.sitio = sitio;
    }

    @ModelAttribute("actor")
    public Usuario identificarUsuario(Principal principal) {
        return usuarios.buscarUsuario(principal.getName());
    }

    @GetMapping("/usuarios")
    public String mostrarUsuarios(@ModelAttribute(value = "actor", binding = false) Usuario actor, Model modelo) {
        modelo.addAttribute("usuarios", usuarios.listarUsuarios(actor));
        modelo.addAttribute("roles", Rol.values());
        return "usuarios";
    }

    @PostMapping("/usuarios")
    public String registrarUsuario(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                  @RequestParam String nombre, @RequestParam String clave, @RequestParam Rol rol,
                                  RedirectAttributes redireccion) {
        usuarios.registrarUsuario(actor, nombre, clave, rol);
        redireccion.addFlashAttribute("mensaje", "Usuario registrado.");
        return volver("/usuarios");
    }

    @PostMapping("/usuarios/{id}")
    public String actualizarUsuario(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                    @PathVariable Long id, @RequestParam Rol rol,
                                    @RequestParam boolean activo, @RequestParam String clave,
                                    RedirectAttributes redireccion) {
        usuarios.actualizarUsuario(actor, id, rol, activo, clave);
        redireccion.addFlashAttribute("mensaje", "Usuario actualizado.");
        return volver("/usuarios");
    }

    @GetMapping("/permisos")
    public String mostrarPermisos(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                 @RequestParam(required = false) Long creador, Model modelo) {
        permisos.exigirRevisor(actor);
        modelo.addAttribute("paginas", paginas.listarPaginas(actor));
        modelo.addAttribute("creadores", usuarios.listarCreadores(actor));
        if (creador != null) {
            Usuario seleccionado = permisos.buscarCreador(creador);
            modelo.addAttribute("seleccionado", seleccionado);
            modelo.addAttribute("asignadas", permisos.paginasAsignadas(seleccionado));
            modelo.addAttribute("prefijos", permisos.listarPrefijos(seleccionado));
        }
        return "permisos";
    }

    @PostMapping("/permisos/edicion")
    public String cambiarEdicion(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                @RequestParam Long creador, @RequestParam Long pagina, @RequestParam boolean conceder,
                                RedirectAttributes redireccion) {
        permisos.exigirRevisor(actor);
        paginas.buscarPagina(pagina);
        permisos.cambiarEdicion(actor, creador, pagina, conceder);
        redireccion.addAttribute("creador", creador);
        return volver("/permisos");
    }

    @PostMapping("/permisos/creacion")
    public String cambiarCreacion(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                 @RequestParam Long creador, @RequestParam String prefijo, @RequestParam boolean conceder,
                                 RedirectAttributes redireccion) {
        permisos.cambiarCreacion(actor, creador, prefijo, conceder);
        redireccion.addAttribute("creador", creador);
        return volver("/permisos");
    }

    @GetMapping("/auditoria")
    public String mostrarAuditoria(@ModelAttribute(value = "actor", binding = false) Usuario actor, Model modelo) {
        modelo.addAttribute("eventos", auditoria.listarEventos(actor));
        return "auditoria";
    }

    @GetMapping("/configuracion")
    public String mostrarConfiguracion(@ModelAttribute(value = "actor", binding = false) Usuario actor, Model modelo) {
        usuarios.exigirManager(actor);
        modelo.addAttribute("sitio", sitio.leerConfiguracion());
        return "configuracion";
    }

    @PostMapping("/configuracion")
    public String guardarConfiguracion(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                      @RequestParam String nombre, @RequestParam String logo, @RequestParam String color,
                                      RedirectAttributes redireccion) {
        sitio.guardarConfiguracion(actor, nombre, logo, color);
        redireccion.addFlashAttribute("mensaje", "Configuración guardada.");
        return volver("/configuracion");
    }

    private String volver(String seccion) {
        return "redirect:" + SeguridadConfig.PANEL + seccion;
    }
}
