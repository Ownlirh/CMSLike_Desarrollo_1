package cibertec.pe.repository;

import cibertec.pe.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface IConfiguracionRepository extends JpaRepository<Configuracion, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Configuracion c where c.id = 1")
    Configuracion bloquearConfiguracion();
}
