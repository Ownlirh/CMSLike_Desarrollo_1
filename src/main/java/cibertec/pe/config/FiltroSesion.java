package cibertec.pe.config;

import cibertec.pe.repository.IUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

final class FiltroSesion extends OncePerRequestFilter {
    private final IUsuarioRepository usuarios;

    FiltroSesion(IUsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado principal
                && usuarios.findByNombre(principal.getUsername()).filter(principal::sesionVigente).isEmpty()) {
            new SecurityContextLogoutHandler().logout(solicitud, respuesta, autenticacion);
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
