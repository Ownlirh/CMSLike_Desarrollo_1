package cibertec.pe.config;

import cibertec.pe.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

final class UsuarioAutenticado extends User {
    private final long versionSesion;

    UsuarioAutenticado(Usuario usuario) {
        super(usuario.getNombre(), usuario.getClave(), usuario.isActivo(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
        this.versionSesion = usuario.getVersionSesion();
    }

    boolean sesionVigente(Usuario usuario) {
        return usuario.isActivo() && usuario.getVersionSesion() == versionSesion;
    }
}
