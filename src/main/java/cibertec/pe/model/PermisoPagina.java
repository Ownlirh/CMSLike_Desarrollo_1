package cibertec.pe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(schema = "public", name = "permiso_pagina")
@Getter
@Setter
public class PermisoPagina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "pagina_id", nullable = false)
    private Long paginaId;
}
