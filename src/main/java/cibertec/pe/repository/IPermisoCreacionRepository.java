package cibertec.pe.repository;

import cibertec.pe.model.PermisoCreacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IPermisoCreacionRepository extends JpaRepository<PermisoCreacion, Long> {
    Optional<PermisoCreacion> findByUsuarioIdAndPrefijo(Long usuarioId, String prefijo);
    List<PermisoCreacion> findByUsuarioId(Long usuarioId);
}
