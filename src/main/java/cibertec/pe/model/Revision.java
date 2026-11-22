package cibertec.pe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "public", name = "revision")
@Getter
@Setter
public class Revision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pagina_id", nullable = false)
    private Long paginaId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "base_id")
    private Long baseId;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 20)
    private String plantilla;

    @Column(nullable = false, length = 64000)
    private String bloques;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRevision estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(length = 500)
    private String observacion;
}
