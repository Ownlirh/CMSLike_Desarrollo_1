package cibertec.pe.repository;

import cibertec.pe.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface IEventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findTop100ByOrderByIdDesc();
    List<Evento> findTop100ByAccionNotInOrderByIdDesc(Collection<String> acciones);
}
