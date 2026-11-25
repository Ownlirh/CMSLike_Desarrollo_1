package cibertec.pe.service;

import cibertec.pe.model.Evento;
import cibertec.pe.model.Usuario;
import cibertec.pe.repository.IEventoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditoriaImplement implements IAuditoriaService {
    private final IEventoRepository eventos;

    public AuditoriaImplement(IEventoRepository eventos) {
        this.eventos = eventos;
    }

    @Override
    public void registrar(Usuario actor, Long pagina, Long revision, String accion, String detalle) {
        Evento evento = new Evento();
        evento.setActorId(actor.getId());
        evento.setPaginaId(pagina);
        evento.setRevisionId(revision);
        evento.setAccion(accion);
        evento.setDetalle(detalle);
        evento.setFecha(Instant.now());
        eventos.save(evento);
    }

    @Override
    public List<Evento> listarEventos(Usuario actor) {
        return listarEventos(actor, null, null, null);
    }

    @Override
    public List<Evento> listarEventos(Usuario actor, String accion, Instant desde, Instant hasta) {
        List<Evento> base = switch (actor.getRol()) {
            case MANAGER -> eventos.findTop100ByOrderByIdDesc();
            case CONTENT_CREATOR_MANAGER -> eventos.findTop100ByAccionNotInOrderByIdDesc(
                    List.of("REGISTRAR_USUARIO", "ACTUALIZAR_USUARIO", "CONFIGURAR_SITIO"));
            case CONTENT_CREATOR -> throw new AccessDeniedException("Se requiere un rol de revisión");
        };

        return base.stream()
                .filter(e -> accion == null || accion.isBlank() || accion.equalsIgnoreCase(e.getAccion()))
                .filter(e -> desde == null || !e.getFecha().isBefore(desde))
                .filter(e -> hasta == null || !e.getFecha().isAfter(hasta))
                .toList();
    }
}