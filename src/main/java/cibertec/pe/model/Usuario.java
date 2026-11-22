package cibertec.pe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "public", name = "usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String clave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;
}
