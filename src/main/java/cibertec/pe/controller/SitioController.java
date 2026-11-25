package cibertec.pe.controller;

import cibertec.pe.config.SeguridadConfig;
import cibertec.pe.model.Bloque;
import cibertec.pe.model.EstadoRevision;
import cibertec.pe.model.Revision;
import cibertec.pe.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Controller
public class SitioController {
    private final IPaginaService paginas;
    private final ISitioService sitio;
    private final ContenidoImplement contenido;
    private final IUsuarioService usuarios;
    private final IPermisoService permisos;

    public SitioController(IPaginaService paginas, ISitioService sitio, ContenidoImplement contenido,
                           IUsuarioService usuarios, IPermisoService permisos) {
        this.paginas = paginas;
        this.sitio = sitio;
        this.contenido = contenido;
        this.usuarios = usuarios;
        this.permisos = permisos;
    }

    @GetMapping("/")
    public String mostrarInicio(Model modelo, Authentication authentication) {
        modelo.addAttribute("autenticado", estaAutenticado(authentication));
        var publicadas = paginas.listarPublicadas();
        if (publicadas.stream().anyMatch(pagina -> pagina.getRuta().equals("/home"))) {
            return prepararPagina(paginas.buscarPublicada("/home"), modelo);
        }
        modelo.addAttribute("sitio", sitio.leerConfiguracion());
        modelo.addAttribute("navegacion", paginas.listarNavegacionPublica());
        return "publica";
    }

    @GetMapping("/{ruta:^(?!admin$|css$|js$|error$|actuator$)[a-z0-9-]+(?:\\/[a-z0-9-]+)*}")
    public String mostrarPublicada(@PathVariable String ruta, Model modelo, Authentication authentication) {
        modelo.addAttribute("autenticado", estaAutenticado(authentication));
        String rutaNormalizada = ruta.startsWith("/") ? ruta : "/" + ruta;
        return prepararPagina(paginas.buscarPublicada(rutaNormalizada), modelo);
    }

    @GetMapping(SeguridadConfig.PANEL + "/revisiones/{id}/preview")
    public String mostrarPreview(@PathVariable Long id, Principal principal, Authentication authentication,
                                 HttpServletResponse respuesta, Model modelo) {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Robots-Tag", "noindex");
        modelo.addAttribute("autenticado", estaAutenticado(authentication));
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
                                      @RequestParam String bloques, Principal principal, Authentication authentication,
                                      HttpServletResponse respuesta, Model modelo) {
        var actor = usuarios.buscarUsuario(principal.getName());
        if (paginaId == null) {
            RutasCms.validarRuta(ruta);
            permisos.verificarCreacion(actor, ruta);
        } else {
            paginas.editarPagina(actor, paginaId);
        }
        String tituloLimpio = PaginaImplement.validarTitulo(titulo);
        respuesta.setHeader("Cache-Control", "no-store");
        modelo.addAttribute("autenticado", estaAutenticado(authentication));
        Revision revision = new Revision();
        revision.setTitulo(tituloLimpio);
        revision.setPlantilla(contenido.validarPlantilla(plantilla));
        revision.setBloques(bloques);
        return prepararPagina(revision, modelo);
    }

    private String prepararPagina(Revision revision, Model modelo) {
        Set<String> rutasActivas = paginas.rutasPublicadasActivas();
        List<Bloque> bloquesFiltrados = contenido.leerBloques(revision.getBloques()).stream()
                .filter(bloque -> bloque.tipo() != Bloque.Tipo.BOTON
                        || bloque.url() == null
                        || !bloque.url().startsWith("/")
                        || rutasActivas.contains(bloque.url()))
                .toList();
        modelo.addAttribute("sitio", sitio.leerConfiguracion());
        modelo.addAttribute("navegacion", paginas.listarNavegacionPublica());
        modelo.addAttribute("revision", revision);
        modelo.addAttribute("bloques", bloquesFiltrados);
        return "publica";
    }

    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}