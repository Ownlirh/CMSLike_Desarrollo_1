package cibertec.pe.repository;

import cibertec.pe.model.PermisoPagina;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IPermisoPaginaRepository extends JpaRepository<PermisoPagina, Long> {
    Optional<PermisoPagina> findByUsuarioIdAndPaginaId(Long usuarioId, Long paginaId);
    List<PermisoPagina> findByPaginaId(Long paginaId);
    List<PermisoPagina> findByUsuarioId(Long usuarioId);
}
