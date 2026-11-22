package cibertec.pe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(schema = "audit", name = "evento")
@Getter
@Setter
public class Evento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "pagina_id")
    private Long paginaId;

    @Column(name = "revision_id")
    private Long revisionId;

    @Column(nullable = false, length = 40)
    private String accion;

    @Column(length = 500)
    private String detalle;

    @Column(nullable = false)
    private Instant fecha;
}
