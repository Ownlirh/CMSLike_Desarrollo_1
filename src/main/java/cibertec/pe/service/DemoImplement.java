package cibertec.pe.service;

import cibertec.pe.model.Bloque;
import cibertec.pe.model.Rol;
import cibertec.pe.model.Usuario;
import cibertec.pe.repository.IConfiguracionRepository;
import cibertec.pe.repository.IPaginaRepository;
import cibertec.pe.repository.IUsuarioRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Service
public class DemoImplement {
    private final IConfiguracionRepository configuraciones;
    private final IUsuarioRepository cuentas;
    private final IPaginaRepository rutas;
    private final UsuarioImplement usuarios;
    private final PaginaImplement paginas;
    private final PermisoImplement permisos;
    private final SitioImplement sitio;
    private final ObjectMapper json;

    public DemoImplement(IConfiguracionRepository configuraciones, IUsuarioRepository cuentas,
                          IPaginaRepository rutas, UsuarioImplement usuarios, PaginaImplement paginas,
                          PermisoImplement permisos, SitioImplement sitio, ObjectMapper json) {
        this.configuraciones = configuraciones;
        this.cuentas = cuentas;
        this.rutas = rutas;
        this.usuarios = usuarios;
        this.paginas = paginas;
        this.permisos = permisos;
        this.sitio = sitio;
        this.json = json;
    }

    @Transactional(rollbackFor = IOException.class)
    public void sembrarDemo(String nombreManager, String claveRevisor, String claveCreador) throws IOException {
        var configuracion = configuraciones.bloquearConfiguracion();
        Usuario manager = usuarios.buscarUsuario(nombreManager);
        usuarios.exigirManager(manager);
        if (rutas.count() == 0 && configuracion.getNombre().equals("CMSLike")
                && configuracion.getLogo() == null && configuracion.getColor().equals("#234c47")) {
            sitio.guardarConfiguracion(manager, "Rutas del Perú", "", "#234c47");
        }
        prepararCuenta(manager, "revisor-demo", claveRevisor, Rol.CONTENT_CREATOR_MANAGER);
        Usuario creador = prepararCuenta(manager, "creador-demo", claveCreador, Rol.CONTENT_CREATOR);
        for (Destino destino : leerDestinos()) {
            if (rutas.findByRuta(destino.ruta()).isPresent()) {
                continue;
            }
            var revision = paginas.crearPagina(manager, destino.ruta(), destino.titulo(), "normal",
                    json.writeValueAsString(destino.bloques()));
            paginas.enviarRevision(manager, revision.getId());
            paginas.aprobarRevision(manager, revision.getId());
            if (creador.getRol() == Rol.CONTENT_CREATOR && creador.isActivo() && !destino.ruta().equals("/home")) {
                permisos.cambiarEdicion(manager, creador.getId(), revision.getPaginaId(), true);
            }
        }
    }

    private Usuario prepararCuenta(Usuario manager, String nombre, String clave, Rol rol) {
        return cuentas.findByNombre(nombre).orElseGet(() -> usuarios.registrarUsuario(manager, nombre, clave, rol));
    }

    private List<Destino> leerDestinos() throws IOException {
        try (var archivo = new ClassPathResource("demo/destinos.json").getInputStream()) {
            return json.readValue(archivo, new TypeReference<List<Destino>>() {});
        }
    }

    private record Destino(String ruta, String titulo, List<Bloque> bloques) {
    }
}
