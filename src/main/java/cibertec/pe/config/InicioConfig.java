package cibertec.pe.config;

import cibertec.pe.service.UsuarioImplement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InicioConfig {
    @Bean
    ApplicationRunner iniciarCms(UsuarioImplement usuarios, @Value("${cms.manager.usuario}") String nombre,
                                  @Value("${cms.manager.clave}") String clave) {
        return args -> usuarios.iniciarManager(nombre, clave);
    }
}
