package cibertec.pe.repository;

import cibertec.pe.model.Revision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import cibertec.pe.model.EstadoRevision;

public interface IRevisionRepository extends JpaRepository<Revision, Long> {
    Page<Revision> findByPaginaIdOrderByIdDesc(Long paginaId, Pageable pageable);
    List<Revision> findByEstadoOrderByIdAsc(EstadoRevision estado);
    boolean existsByPaginaIdAndEstado(Long paginaId, EstadoRevision estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Revision r where r.id = :id")
    Optional<Revision> bloquearRevision(@Param("id") Long id);

    @Query("select r.paginaId from Revision r where r.id = :id")
    Optional<Long> buscarPaginaId(@Param("id") Long id);
}