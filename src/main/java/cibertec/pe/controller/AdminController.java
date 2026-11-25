package cibertec.pe.controller;

import cibertec.pe.config.SeguridadConfig;
import cibertec.pe.model.Revision;
import cibertec.pe.model.Usuario;
import cibertec.pe.service.IPaginaService;
import cibertec.pe.service.IPermisoService;
import cibertec.pe.service.IUsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping(SeguridadConfig.PANEL)
public class AdminController {
    private final IUsuarioService usuarios;
    private final IPaginaService paginas;
    private final IPermisoService permisos;

    public AdminController(IUsuarioService usuarios, IPaginaService paginas, IPermisoService permisos) {
        this.usuarios = usuarios;
        this.paginas = paginas;
        this.permisos = permisos;
    }

    @ModelAttribute("actor")
    public Usuario identificarUsuario(Principal principal) {
        return usuarios.buscarUsuario(principal.getName());
    }

    @GetMapping
    public String listarPaginas(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                @RequestParam(required = false) String buscar,
                                Model modelo) {
        modelo.addAttribute("paginas", paginas.listarPaginas(actor, buscar));
        modelo.addAttribute("buscar", buscar == null ? "" : buscar);
        modelo.addAttribute("puedeCrear", permisos.puedeCrear(actor));
        modelo.addAttribute("puedeArchivar", actor.getRol().puedeRevisar());
        return "panel";
    }

    @PostMapping("/paginas/{id}/archivar")
    public String archivarPagina(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                 @PathVariable Long id,
                                 RedirectAttributes redireccion) {
        paginas.archivarPagina(actor, id);
        redireccion.addFlashAttribute("mensaje", "Página archivada.");
        return "redirect:" + SeguridadConfig.PANEL;
    }

    @PostMapping("/paginas/{id}/desarchivar")
    public String desarchivarPagina(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                                    @PathVariable Long id,
                                    RedirectAttributes redireccion) {
        paginas.desarchivarPagina(actor, id);
        redireccion.addFlashAttribute("mensaje", "Página desarchivada.");
        return "redirect:" + SeguridadConfig.PANEL;
    }

    @GetMapping("/paginas/nueva")
    public String formNuevaPagina(@ModelAttribute(value = "actor", binding = false) Usuario actor, Model modelo) {
        if (!permisos.puedeCrear(actor)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para crear páginas");
        }
        cargarEditor(modelo, null, "", "normal", "[]");
        return "editor";
    }

    @PostMapping("/paginas")
    public String crearPagina(@ModelAttribute(value = "actor", binding = false) Usuario actor, @RequestParam String ruta,
                              @RequestParam String titulo, @RequestParam String plantilla, @RequestParam String bloques,
                              Model modelo, RedirectAttributes redireccion) {
        try {
            return confirmarGuardado(paginas.crearPagina(actor, ruta, titulo, plantilla, bloques), redireccion);
        } catch (IllegalArgumentException e) {
            cargarEditor(modelo, null, titulo, plantilla, bloques);
            modelo.addAttribute("ruta", ruta);
            modelo.addAttribute("error", e.getMessage());
            return "editor";
        }
    }

    @GetMapping("/paginas/{id}")
    public String verHistorial(@ModelAttribute(value = "actor", binding = false) Usuario actor,
                               @PathVariable Long id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model modelo) {
        Page<Revision> historialPage = paginas.listarHistorialPaginado(actor, id, page, size);
        modelo.addAttribute("pagina", paginas.editarPagina(actor, id));
        modelo.addAttribute("historialPage", historialPage);
        modelo.addAttribute("historial", historialPage.getContent());
        return "historial";
    }

    @GetMapping("/paginas/{id}/editar")
    public String formEditarPagina(@ModelAttribute(value = "actor", binding = false) Usuario actor, @PathVariable Long id,
                                   @RequestParam(required = false) Long revision, Model modelo) {
        modelo.addAttribute("pagina", paginas.editarPagina(actor, id));
        Revision origen = paginas.buscarBaseEditor(actor, id, revision);
        cargarEditor(modelo, origen == null ? null : origen.getId(), origen == null ? "" : origen.getTitulo(),
                origen == null ? "normal" : origen.getPlantilla(), origen == null ? "[]" : origen.getBloques());
        return "editor";
    }

    @PostMapping("/paginas/{id}/borradores")
    public String guardarBorrador(@ModelAttribute(value = "actor", binding = false) Usuario actor, @PathVariable Long id,
                                  @RequestParam(required = false) Long origen, @RequestParam String titulo,
                                  @RequestParam String plantilla, @RequestParam String bloques,
                                  Model modelo, RedirectAttributes redireccion) {
        try {
            return confirmarGuardado(paginas.guardarBorrador(actor, id, origen, titulo, plantilla, bloques), redireccion);
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("pagina", paginas.editarPagina(actor, id));
            cargarEditor(modelo, origen, titulo, plantilla, bloques);
            modelo.addAttribute("error", e.getMessage());
            return "editor";
        }
    }

    @GetMapping("/revisiones")
    public String listarPendientes(@ModelAttribute(value = "actor", binding = false) Usuario actor, Model modelo) {
        modelo.addAttribute("revisiones", paginas.listarPendientes(actor));
        return "revisiones";
    }

    @PostMapping("/revisiones/{id}/enviar")
    public String enviarRevision(@ModelAttribute(value = "actor", binding = false) Usuario actor, @PathVariable Long id,
                                 RedirectAttributes redireccion) {
        Revision revision = paginas.enviarRevision(actor, id);
        redireccion.addFlashAttribute("mensaje", "Revisión enviada.");
        return volverAlHistorial(revision);
    }

    @PostMapping("/revisiones/{id}/aprobar")
    public String aprobarRevision(@ModelAttribute(value = "actor", binding = false) Usuario actor, @PathVariable Long id,
                                  RedirectAttributes redireccion) {
        Revision revision = paginas.aprobarRevision(actor, id);
        redireccion.addFlashAttribute("mensaje", "La revisión ya está publicada.");
        return volverAlHistorial(revision);
    }

    @PostMapping("/revisiones/{id}/devolver")
    public String devolverRevision(@ModelAttribute(value = "actor", binding = false) Usuario actor, @PathVariable Long id,
                                   @RequestParam String observacion, RedirectAttributes redireccion) {
        Revision revision = paginas.devolverRevision(actor, id, observacion);
        redireccion.addFlashAttribute("mensaje", "Revisión devuelta.");
        return volverAlHistorial(revision);
    }

    private String confirmarGuardado(Revision revision, RedirectAttributes redireccion) {
        redireccion.addFlashAttribute("mensaje", "Borrador guardado.");
        return volverAlHistorial(revision);
    }

    private String volverAlHistorial(Revision revision) {
        return "redirect:" + SeguridadConfig.PANEL + "/paginas/" + revision.getPaginaId();
    }

    private void cargarEditor(Model modelo, Long origen, String titulo, String plantilla, String bloques) {
        modelo.addAttribute("origen", origen);
        modelo.addAttribute("titulo", titulo);
        modelo.addAttribute("plantilla", plantilla);
        modelo.addAttribute("bloques", bloques);
    }
}