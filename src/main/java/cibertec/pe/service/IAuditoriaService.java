package cibertec.pe.service;

import cibertec.pe.model.Evento;
import cibertec.pe.model.Usuario;

import java.time.Instant;
import java.util.List;

public interface IAuditoriaService {
    void registrar(Usuario actor, Long pagina, Long revision, String accion, String detalle);
    List<Evento> listarEventos(Usuario actor);
    List<Evento> listarEventos(Usuario actor, String accion, Instant desde, Instant hasta);
}