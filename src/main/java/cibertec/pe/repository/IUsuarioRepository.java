package cibertec.pe.repository;

import cibertec.pe.model.Rol;
import cibertec.pe.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IUsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNombre(String nombre);
    List<Usuario> findByRolOrderByNombreAsc(Rol rol);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearUsuario(@Param("id") Long id);
}
