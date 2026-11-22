package cibertec.pe.controller;

import cibertec.pe.config.SeguridadConfig;
import cibertec.pe.model.EstadoRevision;
import cibertec.pe.model.Revision;
import cibertec.pe.service.ContenidoImplement;
import cibertec.pe.service.PaginaImplement;
import cibertec.pe.service.SitioImplement;
import cibertec.pe.service.UsuarioImplement;
import cibertec.pe.service.PermisoImplement;
import cibertec.pe.service.RutasCms;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class SitioController {
    private final PaginaImplement paginas;
    private final SitioImplement sitio;
    private final ContenidoImplement contenido;
    private final UsuarioImplement usuarios;
    private final PermisoImplement permisos;

    public SitioController(PaginaImplement paginas, SitioImplement sitio, ContenidoImplement contenido,
                           UsuarioImplement usuarios, PermisoImplement permisos) {
        this.paginas = paginas;
        this.sitio = sitio;
        this.contenido = contenido;
        this.usuarios = usuarios;
        this.permisos = permisos;
    }

    @GetMapping("/")
    public String mostrarInicio(Model modelo) {
        var publicadas = paginas.listarPublicadas();
        if (publicadas.stream().anyMatch(pagina -> pagina.getRuta().equals("/home"))) {
            return "redirect:/home";
        }
        modelo.addAttribute("sitio", sitio.leerConfiguracion());
        modelo.addAttribute("navegacion", publicadas);
        return "publica";
    }

    @GetMapping("/{primero:(?!admin$|css$|js$|error$|actuator$)[a-z0-9-]+}/{*resto}")
    public String mostrarPublicada(@PathVariable String primero, @PathVariable String resto, Model modelo) {
        return prepararPagina(paginas.buscarPublicada("/" + primero + resto), modelo);
    }

    @GetMapping(SeguridadConfig.PANEL + "/revisiones/{id}/preview")
    public String mostrarPreview(@PathVariable Long id, Principal principal, HttpServletResponse respuesta, Model modelo) {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Robots-Tag", "noindex");
        var actor = usuarios.buscarUsuario(principal.getName());
        Revision revision = paginas.previsualizar(actor, id);
        modelo.addAttribute("preview", true);
        modelo.addAttribute("puedeDecidir", actor.getRol().puedeRevisar() && revision.getEstado() == EstadoRevision.EN_REVISION);
        return prepararPagina(revision, modelo);
    }

    @PostMapping(SeguridadConfig.PANEL + "/editor/preview")
    public String previsualizarEditor(@RequestParam(required = false) Long paginaId,
                                     @RequestParam(required = false) String ruta,
                                     @RequestParam String titulo, @RequestParam String plantilla,
                                     @RequestParam String bloques, Principal principal,
                                     HttpServletResponse respuesta, Model modelo) {
        var actor = usuarios.buscarUsuario(principal.getName());
        if (paginaId == null) {
            RutasCms.validarRuta(ruta);
            permisos.verificarCreacion(actor, ruta);
        } else {
            paginas.editarPagina(actor, paginaId);
        }
        if (titulo.isBlank() || titulo.strip().length() > 160) {
            throw new IllegalArgumentException("Ingresa un título de hasta 160 caracteres.");
        }
        respuesta.setHeader("Cache-Control", "no-store");
        Revision revision = new Revision();
        revision.setTitulo(titulo.strip());
        revision.setPlantilla(contenido.validarPlantilla(plantilla));
        revision.setBloques(bloques);
        return prepararPagina(revision, modelo);
    }

    private String prepararPagina(Revision revision, Model modelo) {
        modelo.addAttribute("sitio", sitio.leerConfiguracion());
        modelo.addAttribute("navegacion", paginas.listarPublicadas());
        modelo.addAttribute("revision", revision);
        modelo.addAttribute("bloques", contenido.leerBloques(revision.getBloques()));
        return "publica";
    }
}
