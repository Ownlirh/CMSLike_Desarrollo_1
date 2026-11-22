package cibertec.pe;

import cibertec.pe.service.ContenidoImplement;
import cibertec.pe.service.RutasCms;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class ContenidoTest {
    private final ContenidoImplement contenido = new ContenidoImplement(JsonMapper.builder().build());

    @Test
    void aceptaUnaFilaDeDosImagenes() {
        String bloques = """
                [{"tipo":"FILA_IMAGENES","imagenes":[
                    {"url":"https://ejemplo.test/uno.png","alternativo":"Uno"},
                    {"url":"/img/dos.png","alternativo":"Dos","enlace":"/noticias"}
                ]}]
                """;
        assertEquals(2, contenido.leerBloques(contenido.guardarBloques(bloques)).getFirst().imagenes().size());
    }

    @Test
    void rechazaBloquesYEnlacesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> contenido.leerBloques("null"));
        assertThrows(IllegalArgumentException.class, () -> contenido.leerBloques("[null]"));
        assertThrows(IllegalArgumentException.class, () -> contenido.leerBloques("[{\"tipo\":\"FILA_IMAGENES\"}]"));
        assertFalse(contenido.enlaceValido("javascript:alert(1)"));
        assertFalse(contenido.enlaceValido("//ejemplo.test"));
        assertFalse(contenido.enlaceValido("/\\ejemplo.test"));
        assertFalse(contenido.enlaceValido("https://usuario@ejemplo.test"));
        assertTrue(contenido.enlaceValido("/noticias"));
    }

    @Test
    void validaRutasYLimitesDelPrefijo() {
        assertEquals("/noticias/fin-de-semana", RutasCms.validarRuta("/noticias/fin-de-semana"));
        assertThrows(IllegalArgumentException.class, () -> RutasCms.validarRuta("/admin/test"));
        assertThrows(IllegalArgumentException.class, () -> RutasCms.validarRuta("/noticias/../home"));
        assertThrows(IllegalArgumentException.class, () -> RutasCms.validarRuta("/noticias//uno"));
        assertTrue(RutasCms.perteneceAPrefijo("/noticias/uno", "/noticias"));
        assertFalse(RutasCms.perteneceAPrefijo("/noticias-privadas/uno", "/noticias"));
        assertFalse(RutasCms.perteneceAPrefijo("/noticias", "/noticias"));
    }
}
