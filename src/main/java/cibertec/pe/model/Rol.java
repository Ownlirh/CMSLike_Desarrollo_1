package cibertec.pe.model;

public enum Rol {
    MANAGER("Manager"),
    CONTENT_CREATOR_MANAGER("Content Creator Manager"),
    CONTENT_CREATOR("Content Creator");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean puedeRevisar() {
        return this != CONTENT_CREATOR;
    }
}
