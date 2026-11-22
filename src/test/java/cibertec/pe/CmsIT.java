package cibertec.pe;

import cibertec.pe.config.SeguridadConfig;
import cibertec.pe.model.Revision;
import cibertec.pe.model.Rol;
import cibertec.pe.model.Usuario;
import cibertec.pe.repository.IEventoRepository;
import cibertec.pe.service.PaginaImplement;
import cibertec.pe.service.PermisoImplement;
import cibertec.pe.service.UsuarioImplement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "cms.manager.usuario=manager",
        "cms.manager.clave=clave-de-prueba-2026"
})
@ActiveProfiles("test")
class CmsIT {
    private static final String PANEL = SeguridadConfig.PANEL;
    private static final String CLAVE = "clave-de-prueba-2026";
    private static final String BLOQUES = """
            [{"tipo":"ENCABEZADO","titulo":"Título del bloque","estilo":"centro"},
            {"tipo":"TEXTO","estilo":"clasico","texto":"<script>Contenido escapado</script>"},
            {"tipo":"TEXTO","titulo":"Lista de temas","estilo":"lista","texto":"Uno\\nDos","url":"/home"},
            {"tipo":"FILA_IMAGENES","imagenes":[
                {"url":"https://ejemplo.test/a.png","alternativo":"Imagen A"},
                {"url":"https://ejemplo.test/b.png","alternativo":"Imagen B","enlace":"/home"}
            ]}]
            """;

    @Autowired WebApplicationContext contexto;
    @Autowired UsuarioImplement usuarios;
    @Autowired PaginaImplement paginas;
    @Autowired PermisoImplement permisos;
    @Autowired IEventoRepository eventos;
    @Autowired JdbcTemplate sql;

    private MockMvc mvc;
    private Usuario manager;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        manager = usuarios.buscarUsuario("manager");
    }

    @Test
    void protegePanelYAutenticaAlManager() throws Exception {
        mvc.perform(get("/css/cms.css")).andExpect(status().isOk());
        mvc.perform(get("/js/editor.js")).andExpect(status().isOk());
        mvc.perform(get(PANEL)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ingresar"));
        mvc.perform(get("/admin/ingresar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresar al CMS")));
        mvc.perform(formLogin("/admin/ingresar").user("manager").password(CLAVE))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(PANEL));
        for (String ruta : List.of("", "/usuarios", "/permisos", "/auditoria", "/configuracion", "/paginas/nueva")) {
            mvc.perform(get(PANEL + ruta).with(user("manager"))).andExpect(status().isOk());
        }
        mvc.perform(post(PANEL + "/configuracion").with(user("manager"))
                .param("nombre", "Portal").param("logo", "").param("color", "#123456"))
                .andExpect(status().isForbidden());
    }

    @Test
    void creaRevisaYPublicaConPermisosYAuditoria() throws Exception {
        Usuario creador = usuarios.registrarUsuario(manager, "creador-editorial", CLAVE, Rol.CONTENT_CREATOR);
        Usuario revisor = usuarios.registrarUsuario(manager, "revisor-editorial", CLAVE, Rol.CONTENT_CREATOR_MANAGER);
        Usuario externo = usuarios.registrarUsuario(manager, "creador-externo", CLAVE, Rol.CONTENT_CREATOR);
        permisos.cambiarCreacion(revisor, creador.getId(), "/noticias", true);

        mvc.perform(post(PANEL + "/paginas").with(user(creador.getNombre())).with(csrf())
                .param("ruta", "/noticias-privadas/evento").param("titulo", "Ruta ajena")
                .param("plantilla", "normal").param("bloques", BLOQUES)).andExpect(status().isForbidden());

        String historial = mvc.perform(post(PANEL + "/paginas").with(user(creador.getNombre())).with(csrf())
                .param("ruta", "/noticias/evento").param("titulo", "Evento en borrador")
                .param("plantilla", "amplia").param("bloques", BLOQUES))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        Long paginaId = Long.valueOf(historial.substring(historial.lastIndexOf('/') + 1));
        Revision revision = paginas.listarHistorial(creador, paginaId).getFirst();
        String revisar = PANEL + "/revisiones/" + revision.getId();

        mvc.perform(get("/noticias/evento")).andExpect(status().isNotFound());
        mvc.perform(post(PANEL + "/editor/preview").with(user(creador.getNombre())).with(csrf())
                .param("paginaId", paginaId.toString()).param("titulo", "Vista antes de guardar")
                .param("plantilla", "amplia").param("bloques", BLOQUES))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Vista antes de guardar")));
        mvc.perform(post(PANEL + "/editor/preview").with(user(externo.getNombre())).with(csrf())
                .param("paginaId", paginaId.toString()).param("titulo", "Vista ajena")
                .param("plantilla", "normal").param("bloques", BLOQUES)).andExpect(status().isForbidden());
        mvc.perform(get(revisar + "/preview")).andExpect(status().is3xxRedirection());
        mvc.perform(get(revisar + "/preview").with(user(externo.getNombre()))).andExpect(status().isForbidden());
        mvc.perform(get(revisar + "/preview").with(user(revisor.getNombre())))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("columnas-2")))
                .andExpect(content().string(containsString("&lt;script&gt;Contenido escapado")));
        mvc.perform(get(historial).with(user(creador.getNombre()))).andExpect(status().isOk());
        mvc.perform(get(historial + "/editar").with(user(creador.getNombre()))).andExpect(status().isOk());

        mvc.perform(post(revisar + "/enviar").with(user(creador.getNombre())).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(revisar + "/preview").with(user(revisor.getNombre())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aprobar y publicar")))
                .andExpect(content().string(containsString("<li>Dos</li>")));
        mvc.perform(get(revisar + "/preview").with(user(creador.getNombre())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Aprobar y publicar"))));
        mvc.perform(get(PANEL + "/revisiones").with(user(revisor.getNombre()))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Evento en borrador")));
        mvc.perform(post(revisar + "/aprobar").with(user(creador.getNombre())).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(revisar + "/aprobar").with(user(revisor.getNombre())).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/noticias/evento")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Evento en borrador")))
                .andExpect(content().string(containsString("<li>Dos</li>")))
                .andExpect(content().string(containsString("href=\"/home\">Más información")))
                .andExpect(content().string(not(containsString("Aprobar y publicar"))))
                .andExpect(content().string(not(containsString("Simular móvil"))));
        assertEquals(1, sql.queryForObject(
                "select count(*) from audit.evento where revision_id = ? and accion = 'PUBLICAR_REVISION'",
                Integer.class, revision.getId()));

        Revision cambio = paginas.guardarBorrador(creador, paginaId, revision.getId(), "Segunda versión", "normal", BLOQUES);
        paginas.enviarRevision(creador, cambio.getId());
        paginas.devolverRevision(revisor, cambio.getId(), "Corrige el título");
        assertEquals("Evento en borrador", paginas.buscarPublicada("/noticias/evento").getTitulo());
        permisos.cambiarEdicion(revisor, creador.getId(), paginaId, false);
        mvc.perform(get(historial).with(user(creador.getNombre()))).andExpect(status().isForbidden());
    }

    @Test
    void cambiaRolesYRevocaAccesoDeUnaSesionExistente() throws Exception {
        Usuario cuenta = usuarios.registrarUsuario(manager, "cuenta-a-editar", CLAVE, Rol.CONTENT_CREATOR);
        String cambiar = PANEL + "/usuarios/" + cuenta.getId();
        mvc.perform(post(cambiar).with(user(cuenta.getNombre())).with(csrf())
                .param("rol", "MANAGER").param("activo", "true").param("clave", "")).andExpect(status().isForbidden());
        mvc.perform(post(cambiar).with(user("manager")).with(csrf())
                .param("rol", "CONTENT_CREATOR_MANAGER").param("activo", "true").param("clave", ""))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get(PANEL + "/revisiones").with(user(cuenta.getNombre()))).andExpect(status().isOk());
        usuarios.actualizarUsuario(manager, cuenta.getId(), Rol.CONTENT_CREATOR_MANAGER, false, "");
        mvc.perform(get(PANEL).with(user(cuenta.getNombre()))).andExpect(status().isForbidden());
        mvc.perform(formLogin("/admin/ingresar").user(cuenta.getNombre()).password(CLAVE))
                .andExpect(redirectedUrl("/admin/ingresar?error"));
    }

    @Test
    void revocaSesionesTrasRotarLaClaveODarDeBaja() throws Exception {
        Usuario cuenta = usuarios.registrarUsuario(manager, "cuenta-sesion", CLAVE, Rol.CONTENT_CREATOR);
        MockHttpSession primera = autenticar(cuenta.getNombre(), CLAVE);
        mvc.perform(get(PANEL).session(primera)).andExpect(status().isOk());

        String nuevaClave = "nueva-clave-de-prueba-2026";
        usuarios.actualizarUsuario(manager, cuenta.getId(), Rol.CONTENT_CREATOR, true, nuevaClave);
        mvc.perform(get(PANEL).session(primera)).andExpect(redirectedUrl("/admin/ingresar"));

        MockHttpSession segunda = autenticar(cuenta.getNombre(), nuevaClave);
        usuarios.actualizarUsuario(manager, cuenta.getId(), Rol.CONTENT_CREATOR, false, "");
        usuarios.actualizarUsuario(manager, cuenta.getId(), Rol.CONTENT_CREATOR, true, "");
        mvc.perform(get(PANEL).session(segunda)).andExpect(redirectedUrl("/admin/ingresar"));
        mvc.perform(get(PANEL).session(autenticar(cuenta.getNombre(), nuevaClave))).andExpect(status().isOk());
    }

    private MockHttpSession autenticar(String nombre, String clave) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/admin/ingresar").user(nombre).password(clave))
                .andExpect(redirectedUrl(PANEL)).andReturn().getRequest().getSession(false);
    }

    @Test
    void unFalloReviertePaginaYEventos() {
        long antes = eventos.count();
        assertThrows(IllegalArgumentException.class,
                () -> paginas.crearPagina(manager, "/ruta-revertida", "Prueba", "normal", "[] NO"));
        assertEquals(antes, eventos.count());
        assertTrue(paginas.listarPaginas(manager).stream().noneMatch(pagina -> pagina.getRuta().equals("/ruta-revertida")));
    }

    @Test
    void cuentaUnaPublicacionCuandoDosRevisoresApruebanALaVez() throws Exception {
        Usuario revisor = usuarios.registrarUsuario(manager, "revisor-concurrente", CLAVE, Rol.CONTENT_CREATOR_MANAGER);
        Revision borrador = paginas.crearPagina(manager, "/home", "Concurrente", "normal", "[]");
        paginas.enviarRevision(manager, borrador.getId());
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primero = ejecutor.submit(aprobarAlMismoTiempo(manager, borrador.getId(), salida));
            var segundo = ejecutor.submit(aprobarAlMismoTiempo(revisor, borrador.getId(), salida));
            salida.countDown();
            assertEquals(1, primero.get(15, TimeUnit.SECONDS) + segundo.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, sql.queryForObject(
                "select count(*) from audit.evento where revision_id = ? and accion = 'PUBLICAR_REVISION'",
                Integer.class, borrador.getId()));
        mvc.perform(get("/home")).andExpect(status().isOk()).andExpect(content().string(containsString("Concurrente")));
        mvc.perform(get("/")).andExpect(redirectedUrl("/home"));
    }

    private Callable<Integer> aprobarAlMismoTiempo(Usuario actor, Long revision, CountDownLatch salida) {
        return () -> {
            salida.await();
            try {
                paginas.aprobarRevision(actor, revision);
                return 1;
            } catch (ResponseStatusException error) {
                assertEquals(409, error.getStatusCode().value());
                return 0;
            }
        };
    }
}
