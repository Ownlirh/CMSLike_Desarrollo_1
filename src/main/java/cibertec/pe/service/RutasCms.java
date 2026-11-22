package cibertec.pe.service;

import java.util.Set;

public final class RutasCms {
    private static final Set<String> RESERVADAS = Set.of("admin", "css", "js", "error", "actuator");

    private RutasCms() {
    }

    public static String validarRuta(String ruta) {
        if (ruta == null || ruta.length() > 180 || !ruta.matches("(/[a-z0-9]+(-[a-z0-9]+)*)+")
                || RESERVADAS.contains(ruta.split("/")[1])) {
            throw new IllegalArgumentException("Usa una ruta disponible en minúsculas, como /noticias/evento.");
        }
        return ruta;
    }

    public static boolean perteneceAPrefijo(String ruta, String prefijo) {
        return ruta.startsWith(prefijo + "/");
    }
}
