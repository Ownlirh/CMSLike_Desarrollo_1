package cibertec.pe.service;

import cibertec.pe.model.Configuracion;
import cibertec.pe.model.Usuario;

public interface ISitioService {
    Configuracion leerConfiguracion();
    void guardarConfiguracion(Usuario actor, String nombre, String logo, String color);
}