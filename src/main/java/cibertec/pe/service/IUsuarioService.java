package cibertec.pe.service;

import cibertec.pe.model.Rol;
import cibertec.pe.model.Usuario;

import java.util.List;

public interface IUsuarioService {
    Usuario buscarUsuario(String nombre);
    void exigirManager(Usuario actor);
    List<Usuario> listarUsuarios(Usuario actor);
    List<Usuario> listarCreadores(Usuario actor);
    Usuario registrarUsuario(Usuario actor, String nombre, String clave, Rol rol);
    Usuario registrarPublico(String nombre, String clave);
    void actualizarUsuario(Usuario actor, Long id, Rol rol, boolean activo, String clave);
    void iniciarManager(String nombre, String clave);
}