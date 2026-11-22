package cibertec.pe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "public", name = "configuracion")
@Getter
@Setter
public class Configuracion {
    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String logo;

    @Column(nullable = false, length = 7)
    private String color;
}
