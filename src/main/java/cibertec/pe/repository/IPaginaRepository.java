package cibertec.pe.repository;

import cibertec.pe.model.Pagina;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IPaginaRepository extends JpaRepository<Pagina, Long> {
    Optional<Pagina> findByRuta(String ruta);
    List<Pagina> findAllByOrderByRutaAsc();
    List<Pagina> findByPublicadaIdIsNotNullOrderByRutaAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pagina p where p.id = :id")
    Optional<Pagina> bloquearPagina(@Param("id") Long id);
}
