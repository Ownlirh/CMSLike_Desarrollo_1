package cibertec.pe.model;

import java.util.List;

public record Bloque(Tipo tipo, String titulo, String texto, String url, String estilo, List<Imagen> imagenes) {
    public enum Tipo { ENCABEZADO, DESTACADA, FILA_IMAGENES, TEXTO, BOTON }

    public record Imagen(String url, String alternativo, String enlace) {
    }
}
