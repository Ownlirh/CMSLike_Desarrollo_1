package cibertec.pe.service;

import cibertec.pe.model.*;
import cibertec.pe.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermisoImplement implements IPermisoService {
    private final IPermisoPaginaRepository accesoPaginas;
    private final IPermisoCreacionRepository accesoRutas;
    private final IUsuarioRepository usuarios;
    private final IPaginaRepository paginas;
    private final IAuditoriaService auditoria;

    public PermisoImplement(IPermisoPaginaRepository accesoPaginas, IPermisoCreacionRepository accesoRutas,
                            IUsuarioRepository usuarios, IPaginaRepository paginas, IAuditoriaService auditoria) {
        this.accesoPaginas = accesoPaginas;
        this.accesoRutas = accesoRutas;
        this.usuarios = usuarios;
        this.paginas = paginas;
        this.auditoria = auditoria;
    }

    @Override
    public void exigirRevisor(Usuario actor) {
        if (!actor.getRol().puedeRevisar()) {
            throw new AccessDeniedException("Se requiere un rol de revisión");
        }
    }

    @Override
    public Set<Long> paginasAsignadas(Usuario actor) {
        return accesoPaginas.findByUsuarioId(actor.getId()).stream()
                .map(PermisoPagina::getPaginaId).collect(Collectors.toSet());
    }

    @Override
    public void exigirEdicion(Usuario actor, Pagina pagina) {
        if (!actor.getRol().puedeRevisar()
                && accesoPaginas.findByUsuarioIdAndPaginaId(actor.getId(), pagina.getId()).isEmpty()) {
            throw new AccessDeniedException("La página no está asignada");
        }
    }

    @Override
    public boolean puedeCrear(Usuario actor) {
        return actor.getRol().puedeRevisar() || !accesoRutas.findByUsuarioId(actor.getId()).isEmpty();
    }

    @Override
    public void exigirCreacion(Usuario actor, String ruta) {
        if (actor.getRol().puedeRevisar()) {
            return;
        }
        bloquearCreador(actor.getId());
        verificarCreacion(actor, ruta);
    }

    @Override
    public void verificarCreacion(Usuario actor, String ruta) {
        if (!actor.getRol().puedeRevisar() && accesoRutas.findByUsuarioId(actor.getId()).stream()
                .noneMatch(permiso -> RutasCms.perteneceAPrefijo(ruta, permiso.getPrefijo()))) {
            throw new AccessDeniedException("No tienes permiso para crear esa ruta");
        }
    }

    @Override
    @Transactional
    public void cambiarEdicion(Usuario actor, Long usuarioId, Long paginaId, boolean conceder) {
        exigirRevisor(actor);
        Pagina pagina = paginas.bloquearPagina(paginaId)
                .orElseThrow(() -> new IllegalArgumentException("La página no existe."));
        if (conceder && pagina.isArchivada()) {
            throw new IllegalArgumentException("Desarchiva la página antes de conceder edición.");
        }
        Usuario creador = buscarCreador(usuarioId);
        cambiarAccesoPagina(actor, creador, paginaId, conceder);
    }

    @Override
    public void asignarPaginaNueva(Usuario actor, Pagina pagina) {
        if (actor.getRol() == Rol.CONTENT_CREATOR) {
            cambiarAccesoPagina(actor, actor, pagina.getId(), true);
        }
    }

    private void cambiarAccesoPagina(Usuario actor, Usuario creador, Long paginaId, boolean conceder) {
        var actual = accesoPaginas.findByUsuarioIdAndPaginaId(creador.getId(), paginaId);
        if (conceder && actual.isEmpty()) {
            PermisoPagina permiso = new PermisoPagina();
            permiso.setUsuarioId(creador.getId());
            permiso.setPaginaId(paginaId);
            accesoPaginas.save(permiso);
        } else if (!conceder && actual.isPresent()) {
            accesoPaginas.delete(actual.get());
        } else {
            return;
        }
        auditoria.registrar(actor, paginaId, null, conceder ? "CONCEDER_EDICION" : "REVOCAR_EDICION", creador.getNombre());
    }

    @Override
    @Transactional
    public void cambiarCreacion(Usuario actor, Long usuarioId, String prefijo, boolean conceder) {
        exigirRevisor(actor);
        Usuario creador = bloquearCreador(usuarioId);
        RutasCms.validarRuta(prefijo);
        var actual = accesoRutas.findByUsuarioIdAndPrefijo(usuarioId, prefijo);
        if (conceder && actual.isEmpty()) {
            PermisoCreacion permiso = new PermisoCreacion();
            permiso.setUsuarioId(usuarioId);
            permiso.setPrefijo(prefijo);
            accesoRutas.save(permiso);
        } else if (!conceder && actual.isPresent()) {
            accesoRutas.delete(actual.get());
        } else {
            return;
        }
        auditoria.registrar(actor, null, null, conceder ? "CONCEDER_CREACION" : "REVOCAR_CREACION",
                creador.getNombre() + " " + prefijo);
    }

    @Override
    public List<String> listarPrefijos(Usuario creador) {
        return accesoRutas.findByUsuarioId(creador.getId()).stream().map(PermisoCreacion::getPrefijo).sorted().toList();
    }

    private Usuario bloquearCreador(Long id) {
        return usuarios.bloquearUsuario(id).filter(usuario -> usuario.getRol() == Rol.CONTENT_CREATOR)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un Content Creator."));
    }

    @Override
    public Usuario buscarCreador(Long id) {
        return usuarios.findById(id).filter(usuario -> usuario.getRol() == Rol.CONTENT_CREATOR)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un Content Creator."));
    }
}