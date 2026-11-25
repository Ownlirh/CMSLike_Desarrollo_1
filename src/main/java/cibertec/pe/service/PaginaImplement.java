package cibertec.pe.service;

import cibertec.pe.model.*;
import cibertec.pe.repository.IPaginaRepository;
import cibertec.pe.repository.IRevisionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PaginaImplement implements IPaginaService {
    public record EnlaceNavegacion(String ruta, String titulo) {
    }

    private final IPaginaRepository paginas;
    private final IRevisionRepository revisiones;
    private final IPermisoService permisos;
    private final ContenidoImplement contenido;
    private final IAuditoriaService auditoria;

    public PaginaImplement(IPaginaRepository paginas, IRevisionRepository revisiones, IPermisoService permisos,
                           ContenidoImplement contenido, IAuditoriaService auditoria) {
        this.paginas = paginas;
        this.revisiones = revisiones;
        this.permisos = permisos;
        this.contenido = contenido;
        this.auditoria = auditoria;
    }

    public static String validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank() || titulo.strip().length() > 160) {
            throw new IllegalArgumentException("Ingresa un título de hasta 160 caracteres.");
        }
        return titulo.strip();
    }

    @Override
    public List<Pagina> listarPaginas(Usuario actor, String filtro) {
        List<Pagina> lista = actor.getRol().puedeRevisar()
                ? paginas.findAllByOrderByRutaAsc()
                : paginas.findByArchivadaFalseOrderByRutaAsc();
        List<Pagina> visibles = actor.getRol().puedeRevisar()
                ? lista
                : lista.stream().filter(p -> permisos.paginasAsignadas(actor).contains(p.getId())).toList();

        if (filtro == null || filtro.isBlank()) {
            return visibles;
        }
        String termino = filtro.trim().toLowerCase();
        return visibles.stream().filter(p -> {
            boolean coincideRuta = p.getRuta().toLowerCase().contains(termino);
            String tituloActual = obtenerTituloReferencia(p).toLowerCase();
            return coincideRuta || tituloActual.contains(termino);
        }).toList();
    }

    @Override
    public String obtenerTituloReferencia(Pagina pagina) {
        if (pagina.getPublicadaId() != null) {
            return revisiones.findById(pagina.getPublicadaId())
                    .map(Revision::getTitulo).orElse(pagina.getRuta());
        }
        return revisiones.findByPaginaIdOrderByIdDesc(pagina.getId(), PageRequest.of(0, 1))
                .getContent().stream().findFirst().map(Revision::getTitulo).orElse(pagina.getRuta());
    }

    @Override
    public List<EnlaceNavegacion> listarNavegacionPublica() {
        return paginas.findByArchivadaFalseAndPublicadaIdIsNotNullOrderByRutaAsc().stream()
                .map(p -> new EnlaceNavegacion(
                        p.getRuta(),
                        revisiones.findById(p.getPublicadaId()).map(Revision::getTitulo).orElse(p.getRuta())
                ))
                .toList();
    }

    @Override
    public Set<String> rutasPublicadasActivas() {
        return paginas.findByArchivadaFalseAndPublicadaIdIsNotNullOrderByRutaAsc().stream()
                .map(Pagina::getRuta)
                .collect(Collectors.toSet());
    }

    @Override
    public Pagina buscarPagina(Long id) {
        return paginas.findById(id).filter(p -> !p.isArchivada()).orElseThrow(this::noEncontrado);
    }

    @Override
    public Pagina editarPagina(Usuario actor, Long id) {
        Pagina pagina = buscarPagina(id);
        permisos.exigirEdicion(actor, pagina);
        return pagina;
    }

    @Override
    public List<Pagina> listarPublicadas() {
        return paginas.findByArchivadaFalseAndPublicadaIdIsNotNullOrderByRutaAsc();
    }

    @Override
    public Revision buscarPublicada(String ruta) {
        return paginas.findByRutaAndArchivadaFalse(ruta).map(Pagina::getPublicadaId)
                .flatMap(revisiones::findById).orElseThrow(this::noEncontrado);
    }

    @Override
    public Page<Revision> listarHistorialPaginado(Usuario actor, Long id, int pagina, int tamano) {
        editarPagina(actor, id);
        int pageIndex = Math.max(0, pagina);
        int pageSize = Math.min(Math.max(1, tamano), 50);
        return revisiones.findByPaginaIdOrderByIdDesc(id, PageRequest.of(pageIndex, pageSize));
    }

    @Override
    public List<Revision> listarPendientes(Usuario actor) {
        permisos.exigirRevisor(actor);
        return revisiones.findByEstadoOrderByIdAsc(EstadoRevision.EN_REVISION);
    }

    @Override
    public Revision previsualizar(Usuario actor, Long id) {
        Revision revision = buscarRevision(id);
        editarPagina(actor, revision.getPaginaId());
        return revision;
    }

    @Override
    public Revision buscarBaseEditor(Usuario actor, Long paginaId, Long revisionId) {
        Pagina pagina = editarPagina(actor, paginaId);
        List<Revision> historial = revisiones.findByPaginaIdOrderByIdDesc(paginaId, PageRequest.of(0, 100)).getContent();
        if (revisionId != null) {
            return historial.stream().filter(revision -> revision.getId().equals(revisionId))
                    .findFirst().orElseThrow(this::noEncontrado);
        }
        return historial.stream().filter(revision -> revision.getAutorId().equals(actor.getId())
                        && revision.getEstado() != EstadoRevision.APROBADA
                        && Objects.equals(revision.getBaseId(), pagina.getPublicadaId()))
                .findFirst().orElseGet(() -> pagina.getPublicadaId() == null ? null : buscarRevision(pagina.getPublicadaId()));
    }

    @Override
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

    @Override
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
        String tituloLimpio = validarTitulo(titulo);
        Revision revision = new Revision();
        revision.setPaginaId(pagina.getId());
        revision.setAutorId(actor.getId());
        revision.setBaseId(pagina.getPublicadaId());
        revision.setTitulo(tituloLimpio);
        revision.setPlantilla(contenido.validarPlantilla(plantilla));
        revision.setBloques(contenido.guardarBloques(bloques));
        revision.setEstado(EstadoRevision.BORRADOR);
        revision.setCreadoEn(Instant.now());
        revisiones.save(revision);
        auditoria.registrar(actor, pagina.getId(), revision.getId(), "GUARDAR_BORRADOR",
                origenId == null ? null : "Revisión de origen " + origenId);
        return revision;
    }

    @Override
    @Transactional
    public Revision enviarRevision(Usuario actor, Long id) {
        Pagina pagina = bloquearPagina(revisiones.buscarPaginaId(id).orElseThrow(this::noEncontrado));
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

    @Override
    @Transactional
    public Revision aprobarRevision(Usuario actor, Long id) {
        permisos.exigirRevisor(actor);
        Pagina pagina = bloquearPagina(revisiones.buscarPaginaId(id).orElseThrow(this::noEncontrado));
        Revision revision = actualizarEstado(id);
        exigirEstado(revision, EstadoRevision.EN_REVISION);
        exigirBaseActual(pagina, revision);
        revision.setEstado(EstadoRevision.APROBADA);
        pagina.setPublicadaId(id);
        auditoria.registrar(actor, pagina.getId(), id, "APROBAR_REVISION", null);
        auditoria.registrar(actor, pagina.getId(), id, "PUBLICAR_REVISION", null);
        return revision;
    }

    @Override
    @Transactional
    public Revision devolverRevision(Usuario actor, Long id, String observacion) {
        permisos.exigirRevisor(actor);
        if (observacion == null || observacion.isBlank() || observacion.strip().length() > 500) {
            throw new IllegalArgumentException("Escribe una observación de hasta 500 caracteres.");
        }
        Pagina pagina = bloquearPagina(revisiones.buscarPaginaId(id).orElseThrow(this::noEncontrado));
        Revision revision = actualizarEstado(id);
        exigirEstado(revision, EstadoRevision.EN_REVISION);
        revision.setEstado(EstadoRevision.DEVUELTA);
        revision.setObservacion(observacion.strip());
        auditoria.registrar(actor, pagina.getId(), id, "DEVOLVER_REVISION", revision.getObservacion());
        return revision;
    }

    @Override
    @Transactional
    public void archivarPagina(Usuario actor, Long id) {
        permisos.exigirRevisor(actor);
        Pagina pagina = bloquearPagina(id);
        pagina.setArchivada(true);
        auditoria.registrar(actor, pagina.getId(), null, "ARCHIVAR_PAGINA", pagina.getRuta());
    }

    @Override
    @Transactional
    public void desarchivarPagina(Usuario actor, Long id) {
        permisos.exigirRevisor(actor);
        Pagina pagina = paginas.bloquearPagina(id).orElseThrow(this::noEncontrado);
        pagina.setArchivada(false);
        auditoria.registrar(actor, pagina.getId(), null, "DESARCHIVAR_PAGINA", pagina.getRuta());
    }

    private Revision buscarRevision(Long id) {
        return revisiones.findById(id).orElseThrow(this::noEncontrado);
    }

    private Revision actualizarEstado(Long id) {
        return revisiones.bloquearRevision(id).orElseThrow(this::noEncontrado);
    }

    private Pagina bloquearPagina(Long id) {
        return paginas.bloquearPagina(id).filter(p -> !p.isArchivada()).orElseThrow(this::noEncontrado);
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