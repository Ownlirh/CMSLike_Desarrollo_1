package cibertec.pe.config;

import cibertec.pe.repository.IUsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfig {
    public static final String PANEL = "/admin/DashBoardAdminCMSPanel";

    @Bean
    SecurityFilterChain configurarSeguridad(HttpSecurity http, IUsuarioRepository usuarios) {
        http.headers(cabeceras -> cabeceras
                .contentSecurityPolicy(politica -> politica.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                                + "img-src 'self' https:; object-src 'none'; base-uri 'self'; "
                                + "form-action 'self'; frame-ancestors 'none'")))
            .authorizeHttpRequests(reglas -> reglas
                .requestMatchers("/admin/ingresar").permitAll()
                .requestMatchers("/admin/**", "/admin").authenticated()
                .anyRequest().permitAll())
            .formLogin(formulario -> formulario.loginPage("/admin/ingresar")
                .defaultSuccessUrl(PANEL, true))
            .logout(salida -> salida.logoutUrl("/admin/salir").logoutSuccessUrl("/"));
        http.addFilterBefore(new FiltroSesion(usuarios), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    UserDetailsService cargarUsuarios(IUsuarioRepository usuarios) {
        return nombre -> usuarios.findByNombre(nombre)
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario desconocido"));
    }

    @Bean
    PasswordEncoder codificarClaves() {
        return new BCryptPasswordEncoder();
    }
}
