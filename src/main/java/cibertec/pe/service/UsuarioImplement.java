package cibertec.pe.service;

import cibertec.pe.model.Rol;
import cibertec.pe.model.Usuario;
import cibertec.pe.repository.IUsuarioRepository;
import cibertec.pe.repository.IConfiguracionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UsuarioImplement {
    private final IUsuarioRepository usuarios;
    private final PasswordEncoder claves;
    private final AuditoriaImplement auditoria;
    private final IConfiguracionRepository configuraciones;

    public UsuarioImplement(IUsuarioRepository usuarios, PasswordEncoder claves, AuditoriaImplement auditoria,
                            IConfiguracionRepository configuraciones) {
        this.usuarios = usuarios;
        this.claves = claves;
        this.auditoria = auditoria;
        this.configuraciones = configuraciones;
    }

    public Usuario buscarUsuario(String nombre) {
        return usuarios.findByNombre(nombre).filter(Usuario::isActivo)
                .orElseThrow(() -> new AccessDeniedException("Usuario sin acceso"));
    }

    public void exigirManager(Usuario actor) {
        if (actor.getRol() != Rol.MANAGER) {
            throw new AccessDeniedException("Se requiere el rol Manager");
        }
    }

    public List<Usuario> listarUsuarios(Usuario actor) {
        exigirManager(actor);
        return usuarios.findAll();
    }

    public List<Usuario> listarCreadores(Usuario actor) {
        if (!actor.getRol().puedeRevisar()) {
            throw new AccessDeniedException("Se requiere un rol de revisión");
        }
        return usuarios.findByRolOrderByNombreAsc(Rol.CONTENT_CREATOR);
    }

    @Transactional
    public Usuario registrarUsuario(Usuario actor, String nombre, String clave, Rol rol) {
        exigirManager(actor);
        Usuario nuevo = guardarUsuario(nombre, clave, rol);
        auditoria.registrar(actor, null, null, "REGISTRAR_USUARIO", nuevo.getNombre());
        return nuevo;
    }

    @Transactional
    public void actualizarUsuario(Usuario actor, Long id, Rol rol, boolean activo, String clave) {
        configuraciones.bloquearConfiguracion();
        exigirManager(buscarUsuario(actor.getNombre()));
        Usuario usuario = usuarios.bloquearUsuario(id)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un usuario."));
        if (id.equals(actor.getId()) && (rol != Rol.MANAGER || !activo)) {
            throw new IllegalArgumentException("Mantén tu cuenta activa con el rol Manager.");
        }
        if (!clave.isBlank() || usuario.isActivo() != activo || usuario.getRol() != rol) {
            usuario.setVersionSesion(usuario.getVersionSesion() + 1);
        }
        if (!clave.isBlank()) {
            validarClave(clave);
            usuario.setClave(claves.encode(clave));
        }
        usuario.setRol(rol);
        usuario.setActivo(activo);
        auditoria.registrar(actor, null, null, "ACTUALIZAR_USUARIO", usuario.getNombre());
    }

    @Transactional
    public void iniciarManager(String nombre, String clave) {
        if (!nombre.isBlank() && !clave.isBlank() && usuarios.count() == 0) {
            guardarUsuario(nombre, clave, Rol.MANAGER);
        }
    }

    private Usuario guardarUsuario(String nombre, String clave, Rol rol) {
        if (!nombre.matches("[a-zA-Z0-9._-]{3,100}") || rol == null) {
            throw new IllegalArgumentException("Revisa el usuario, el rol y la longitud de la contraseña.");
        }
        validarClave(clave);
        if (usuarios.findByNombre(nombre).isPresent()) {
            throw new IllegalArgumentException("El usuario ya existe.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setClave(claves.encode(clave));
        usuario.setRol(rol);
        try {
            return usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("El usuario ya existe.");
        }
    }
    private void validarClave(String clave) {
        if (clave.length() < 12 || clave.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Usa al menos 12 caracteres y hasta 72 bytes en la contraseña.");
        }
    }
}
