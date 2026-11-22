package cibertec.pe.service;

import cibertec.pe.model.Configuracion;
import cibertec.pe.model.Usuario;
import cibertec.pe.repository.IConfiguracionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SitioImplement {
    private final IConfiguracionRepository configuraciones;
    private final UsuarioImplement usuarios;
    private final ContenidoImplement contenido;
    private final AuditoriaImplement auditoria;

    public SitioImplement(IConfiguracionRepository configuraciones, UsuarioImplement usuarios,
                          ContenidoImplement contenido, AuditoriaImplement auditoria) {
        this.configuraciones = configuraciones;
        this.usuarios = usuarios;
        this.contenido = contenido;
        this.auditoria = auditoria;
    }

    public Configuracion leerConfiguracion() {
        return configuraciones.findById(1L).orElseThrow();
    }

    @Transactional
    public void guardarConfiguracion(Usuario actor, String nombre, String logo, String color) {
        usuarios.exigirManager(actor);
        if (nombre.isBlank() || nombre.strip().length() > 100 || !color.matches("#[a-fA-F0-9]{6}")
                || !logo.isBlank() && !contenido.enlaceValido(logo)) {
            throw new IllegalArgumentException("Revisa el nombre, el enlace del logo y el color.");
        }
        Configuracion configuracion = leerConfiguracion();
        configuracion.setNombre(nombre.strip());
        configuracion.setLogo(logo.isBlank() ? null : logo);
        configuracion.setColor(color);
        auditoria.registrar(actor, null, null, "CONFIGURAR_SITIO", nombre.strip());
    }
}
