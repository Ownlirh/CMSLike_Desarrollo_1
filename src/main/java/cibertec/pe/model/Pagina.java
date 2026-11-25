package cibertec.pe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "public", name = "pagina")
@Getter
@Setter
public class Pagina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String ruta;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "publicada_id")
    private Long publicadaId;

    @Column(nullable = false)
    private boolean archivada = false;
}