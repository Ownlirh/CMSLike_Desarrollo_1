package cibertec.pe.service;

import cibertec.pe.model.*;
import cibertec.pe.repository.IPaginaRepository;
import cibertec.pe.repository.IRevisionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class PaginaImplement {
    private final IPaginaRepository paginas;
    private final IRevisionRepository revisiones;
    private final PermisoImplement permisos;
    private final ContenidoImplement contenido;
    private final AuditoriaImplement auditoria;

    public PaginaImplement(IPaginaRepository paginas, IRevisionRepository revisiones, PermisoImplement permisos,
                           ContenidoImplement contenido, AuditoriaImplement auditoria) {
        this.paginas = paginas;
        this.revisiones = revisiones;
        this.permisos = permisos;
        this.contenido = contenido;
        this.auditoria = auditoria;
    }

    public List<Pagina> listarPaginas(Usuario actor) {
        List<Pagina> lista = paginas.findAllByOrderByRutaAsc();
        if (actor.getRol().puedeRevisar()) {
            return lista;
        }
        var asignadas = permisos.paginasAsignadas(actor);
        return lista.stream().filter(pagina -> asignadas.contains(pagina.getId())).toList();
    }

    public Pagina buscarPagina(Long id) {
        return paginas.findById(id).orElseThrow(() -> noEncontrado());
    }

    public Pagina editarPagina(Usuario actor, Long id) {
        Pagina pagina = buscarPagina(id);
        permisos.exigirEdicion(actor, pagina);
        return pagina;
    }

    public List<Pagina> listarPublicadas() {
        return paginas.findByPublicadaIdIsNotNullOrderByRutaAsc();
    }

    public Revision buscarPublicada(String ruta) {
        return paginas.findByRuta(ruta).map(Pagina::getPublicadaId)
                .flatMap(revisiones::findById).orElseThrow(() -> noEncontrado());
    }

    public List<Revision> listarHistorial(Usuario actor, Long id) {
        editarPagina(actor, id);
        return revisiones.findByPaginaIdOrderByIdDesc(id);
    }

    public List<Revision> listarPendientes(Usuario actor) {
        permisos.exigirRevisor(actor);
        return revisiones.findByEstadoOrderByIdAsc(EstadoRevision.EN_REVISION);
    }

    public Revision previsualizar(Usuario actor, Long id) {
        Revision revision = buscarRevision(id);
        editarPagina(actor, revision.getPaginaId());
        return revision;
    }

    public Revision buscarBaseEditor(Usuario actor, Long paginaId, Long revisionId) {
        Pagina pagina = editarPagina(actor, paginaId);
        List<Revision> historial = revisiones.findByPaginaIdOrderByIdDesc(paginaId);
        if (revisionId != null) {
            return historial.stream().filter(revision -> revision.getId().equals(revisionId))
                    .findFirst().orElseThrow(() -> noEncontrado());
        }
        return historial.stream().filter(revision -> revision.getAutorId().equals(actor.getId())
                        && revision.getEstado() != EstadoRevision.APROBADA
                        && Objects.equals(revision.getBaseId(), pagina.getPublicadaId()))
                .findFirst().orElseGet(() -> pagina.getPublicadaId() == null ? null : buscarRevision(pagina.getPublicadaId()));
    }

    @Transactional
    public Revision crearPagina(Usuario actor, String ruta, String titulo, String plantilla, String bloques) {
        RutasCms.validarRuta(ruta);
        permisos.exigirCreacion(actor, ruta);
        if (paginas.findByRuta(ruta).isPresent()) {
            throw conflicto("Ya existe una página con esa ruta.");
        }

        Pagina pagina = new Pagina();
        pagina.setRuta(ruta);
        pagina.setAutorId(actor.getId());
        try {
            paginas.saveAndFlush(pagina);
        } catch (DataIntegrityViolationException e) {
            throw conflicto("Ya existe una página con esa ruta.");
        }
        auditoria.registrar(actor, pagina.getId(), null, "CREAR_PAGINA", ruta);
        permisos.asignarPaginaNueva(actor, pagina);
        return guardarRevision(actor, pagina, null, titulo, plantilla, bloques);
    }

    @Transactional
    public Revision guardarBorrador(Usuario actor, Long paginaId, Long origenId,
                                    String titulo, String plantilla, String bloques) {
        Pagina pagina = bloquearPagina(paginaId);
        permisos.exigirEdicion(actor, pagina);
        if (origenId == null) {
            if (pagina.getPublicadaId() != null) {
                throw conflicto("Abre la versión publicada antes de guardar un cambio.");
            }
        } else {
            Revision origen = buscarRevision(origenId);
            if (!origen.getPaginaId().equals(paginaId)) {
                throw noEncontrado();
            }
            Long base = origen.getEstado() == EstadoRevision.APROBADA ? origen.getId() : origen.getBaseId();
            if (!Objects.equals(base, pagina.getPublicadaId())) {
                throw conflicto("Ya existe una publicación más reciente. Abre esa versión para editar.");
            }
        }
        return guardarRevision(actor, pagina, origenId, titulo, plantilla, bloques);
    }

    private Revision guardarRevision(Usuario actor, Pagina pagina, Long origenId,
                                     String titulo, String plantilla, String bloques) {
        if (titulo == null || titulo.isBlank() || titulo.strip().length() > 160) {
            throw new IllegalArgumentException("Ingresa un título de hasta 160 caracteres.");
        }
        Revision revision = new Revision();
        revision.setPaginaId(pagina.getId());
        revision.setAutorId(actor.getId());
        revision.setBaseId(pagina.getPublicadaId());
        revision.setTitulo(titulo.strip());
        revision.setPlantilla(contenido.validarPlantilla(plantilla));
        revision.setBloques(contenido.guardarBloques(bloques));
        revision.setEstado(EstadoRevision.BORRADOR);
        revision.setCreadoEn(Instant.now());
        revisiones.save(revision);
        auditoria.registrar(actor, pagina.getId(), revision.getId(), "GUARDAR_BORRADOR",
                origenId == null ? null : "Revisión de origen " + origenId);
        return revision;
    }

    @Transactional
    public Revision enviarRevision(Usuario actor, Long id) {
        Pagina pagina = bloquearPagina(revisiones.buscarPaginaId(id).orElseThrow(() -> noEncontrado()));
        permisos.exigirEdicion(actor, pagina);
        Revision revision = actualizarEstado(id);
        if (!actor.getRol().puedeRevisar() && !actor.getId().equals(revision.getAutorId())) {
            throw new AccessDeniedException("Solo puedes enviar tus propios borradores");
        }
        exigirEstado(revision, EstadoRevision.BORRADOR);
        exigirBaseActual(pagina, revision);
        if (revisiones.existsByPaginaIdAndEstado(pagina.getId(), EstadoRevision.EN_REVISION)) {
            throw conflicto("Ya existe una revisión pendiente para esta página.");
        }
        revision.setEstado(EstadoRevision.EN_REVISION);
        auditoria.registrar(actor, pagina.getId(), id, "ENVIAR_REVISION", null);
        return revision;
    }

    @Transactional
    public Revision aprobarRevision(Usuario actor, Long id) {
        permisos.exigirRevisor(actor);
        Pagina pagina = bloquearPagina(revisiones.buscarPaginaId(id).orElseThrow(() -> noEncontrado()));
        Revision revision = actualizarEstado(id);
        exigirEstado(revision, EstadoRevision.EN_REVISION);
        exigirBaseActual(pagina, revision);
        revision.setEstado(EstadoRevision.APROBADA);
        pagina.setPublicadaId(id);
        auditoria.registrar(actor, pagina.getId(), id, "APROBAR_REVISION", null);
        auditoria.registrar(actor, pagina.getId(), id, "PUBLICAR_REVISION", null);
        return revision;
    }

    @Transactional
    public Revision devolverRevision(Usuario actor, Long id, String observacion) {
        permisos.exigirRevisor(actor);
        if (observacion == null || observacion.isBlank() || observacion.strip().length() > 500) {
            throw new IllegalArgumentException("Escribe una observación de hasta 500 caracteres.");
        }
        Pagina pagina = bloquearPagina(revisiones.buscarPaginaId(id).orElseThrow(() -> noEncontrado()));
        Revision revision = actualizarEstado(id);
        exigirEstado(revision, EstadoRevision.EN_REVISION);
        revision.setEstado(EstadoRevision.DEVUELTA);
        revision.setObservacion(observacion.strip());
        auditoria.registrar(actor, pagina.getId(), id, "DEVOLVER_REVISION", revision.getObservacion());
        return revision;
    }

    private Revision buscarRevision(Long id) {
        return revisiones.findById(id).orElseThrow(() -> noEncontrado());
    }

    private Revision actualizarEstado(Long id) {
        return revisiones.bloquearRevision(id).orElseThrow(() -> noEncontrado());
    }

    private Pagina bloquearPagina(Long id) {
        return paginas.bloquearPagina(id).orElseThrow(() -> noEncontrado());
    }

    private void exigirEstado(Revision revision, EstadoRevision estado) {
        if (revision.getEstado() != estado) {
            throw conflicto("La revisión cambió de estado. Vuelve a abrirla.");
        }
    }

    private void exigirBaseActual(Pagina pagina, Revision revision) {
        if (!Objects.equals(revision.getBaseId(), pagina.getPublicadaId())) {
            throw conflicto("La revisión se basa en una publicación anterior.");
        }
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
