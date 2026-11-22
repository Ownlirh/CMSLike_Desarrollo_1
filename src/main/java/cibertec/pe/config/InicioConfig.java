package cibertec.pe.config;

import cibertec.pe.service.DemoImplement;
import cibertec.pe.service.UsuarioImplement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InicioConfig {
    @Bean
    ApplicationRunner iniciarCms(UsuarioImplement usuarios, DemoImplement demo,
                                  @Value("${cms.manager.usuario}") String nombre,
                                  @Value("${cms.manager.clave}") String clave,
                                  @Value("${cms.demo.activa}") boolean demoActiva,
                                  @Value("${cms.demo.revisor-clave}") String revisor,
                                  @Value("${cms.demo.creador-clave}") String creador) {
        return args -> {
            usuarios.iniciarManager(nombre, clave);
            if (demoActiva) {
                demo.sembrarDemo(nombre, revisor, creador);
            }
        };
    }
}
