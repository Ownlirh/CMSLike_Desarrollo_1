package cibertec.pe.service;

import cibertec.pe.model.Bloque;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DeserializationFeature;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Service
public class ContenidoImplement {
    private final ObjectMapper json;
    private static final TypeReference<List<Bloque>> TIPO_BLOQUES = new TypeReference<>() {};
    private static final Set<String> PLANTILLAS = Set.of("normal", "amplia");

    public ContenidoImplement(ObjectMapper json) {
        this.json = json;
    }

    public List<Bloque> leerBloques(String contenido) {
        if (contenido == null || contenido.length() > 64000) {
            throw new IllegalArgumentException("El contenido supera el límite permitido.");
        }
        List<Bloque> bloques;
        try {
            bloques = json.readerFor(TIPO_BLOQUES).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(contenido);
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalArgumentException("Revisa el formato de los bloques.");
        }
        if (bloques == null || bloques.size() > 40) {
            throw new IllegalArgumentException("Agrega como máximo 40 bloques.");
        }
        bloques.forEach(this::validarBloque);
        return bloques;
    }

    public String guardarBloques(String contenido) {
        String normalizado = json.writeValueAsString(leerBloques(contenido));
        if (normalizado.length() > 64000) {
            throw new IllegalArgumentException("El contenido supera el límite permitido.");
        }
        return normalizado;
    }

    public String validarPlantilla(String plantilla) {
        if (plantilla == null || !PLANTILLAS.contains(plantilla)) {
            throw new IllegalArgumentException("Selecciona una plantilla disponible.");
        }
        return plantilla;
    }

    public boolean enlaceValido(String valor) {
        if (valor == null || valor.isBlank() || valor.length() > 500
                || valor.indexOf('\\') >= 0 || valor.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        try {
            URI url = URI.create(valor);
            return "https".equalsIgnoreCase(url.getScheme()) && url.getHost() != null && url.getUserInfo() == null
                    || valor.startsWith("/") && !valor.startsWith("//") && url.getRawAuthority() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void validarBloque(Bloque bloque) {
        if (bloque == null || bloque.tipo() == null || largo(bloque.titulo()) > 160
                || largo(bloque.texto()) > 4000 || largo(bloque.estilo()) > 20
                || bloque.url() != null && !bloque.url().isBlank() && !enlaceValido(bloque.url())) {
            throw new IllegalArgumentException("Revisa los datos de los bloques.");
        }
        List<Bloque.Imagen> imagenes = bloque.imagenes() == null ? List.of() : bloque.imagenes();
        int cantidad = imagenes.size();
        boolean valido = switch (bloque.tipo()) {
            case ENCABEZADO -> presente(bloque.titulo()) && cantidad == 0
                    && Set.of("izquierda", "centro", "derecha").contains(estilo(bloque));
            case TEXTO -> presente(bloque.texto()) && cantidad == 0
                    && Set.of("", "parrafos", "lista").contains(estilo(bloque));
            case BOTON -> presente(bloque.titulo()) && enlaceValido(bloque.url()) && cantidad == 0
                    && Set.of("principal", "secundario").contains(estilo(bloque));
            case DESTACADA -> cantidad == 1 && Set.of("baja", "media", "alta").contains(estilo(bloque));
            case FILA_IMAGENES -> cantidad >= 1 && cantidad <= 4;
        };
        if (!valido || imagenes.stream().anyMatch(imagen -> imagen == null || !enlaceValido(imagen.url())
                || !presente(imagen.alternativo()) || largo(imagen.alternativo()) > 160
                || presente(imagen.enlace()) && !enlaceValido(imagen.enlace()))) {
            throw new IllegalArgumentException("Completa los campos requeridos y usa enlaces HTTPS o rutas internas.");
        }
    }

    private String estilo(Bloque bloque) {
        return bloque.estilo() == null ? "" : bloque.estilo();
    }

    private int largo(String valor) {
        return valor == null ? 0 : valor.length();
    }

    private boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }
}
