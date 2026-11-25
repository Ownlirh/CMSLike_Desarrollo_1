package cibertec.pe.service;

import cibertec.pe.model.Pagina;
import cibertec.pe.model.Usuario;

import java.util.List;
import java.util.Set;

public interface IPermisoService {
    void exigirRevisor(Usuario actor);
    Set<Long> paginasAsignadas(Usuario actor);
    void exigirEdicion(Usuario actor, Pagina pagina);
    boolean puedeCrear(Usuario actor);
    void exigirCreacion(Usuario actor, String ruta);
    void verificarCreacion(Usuario actor, String ruta);
    void cambiarEdicion(Usuario actor, Long usuarioId, Long paginaId, boolean conceder);
    void asignarPaginaNueva(Usuario actor, Pagina pagina);
    void cambiarCreacion(Usuario actor, Long usuarioId, String prefijo, boolean conceder);
    List<String> listarPrefijos(Usuario creador);
    Usuario buscarCreador(Long id);
}