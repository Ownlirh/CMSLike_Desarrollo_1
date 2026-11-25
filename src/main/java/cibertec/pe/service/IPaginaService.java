package cibertec.pe.service;

import cibertec.pe.model.Pagina;
import cibertec.pe.model.Revision;
import cibertec.pe.model.Usuario;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

public interface IPaginaService {
    List<Pagina> listarPaginas(Usuario actor, String filtro);
    String obtenerTituloReferencia(Pagina pagina);
    List<PaginaImplement.EnlaceNavegacion> listarNavegacionPublica();
    Set<String> rutasPublicadasActivas();
    Pagina buscarPagina(Long id);
    Pagina editarPagina(Usuario actor, Long id);
    List<Pagina> listarPublicadas();
    Revision buscarPublicada(String ruta);
    Page<Revision> listarHistorialPaginado(Usuario actor, Long id, int pagina, int tamano);
    List<Revision> listarPendientes(Usuario actor);
    Revision previsualizar(Usuario actor, Long id);
    Revision buscarBaseEditor(Usuario actor, Long paginaId, Long revisionId);
    Revision crearPagina(Usuario actor, String ruta, String titulo, String plantilla, String bloques);
    Revision guardarBorrador(Usuario actor, Long paginaId, Long origenId, String titulo, String plantilla, String bloques);
    Revision enviarRevision(Usuario actor, Long id);
    Revision aprobarRevision(Usuario actor, Long id);
    Revision devolverRevision(Usuario actor, Long id, String observacion);
    void archivarPagina(Usuario actor, Long id);
    void desarchivarPagina(Usuario actor, Long id);
}