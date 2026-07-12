package com.escuelaing.matching.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_matches_par_usuarios",
                columnNames = {"usuario_menor_id", "usuario_mayor_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_menor_id", nullable = false)
    private UUID usuarioMenorId;

    @Column(name = "usuario_mayor_id", nullable = false)
    private UUID usuarioMayorId;

    @Column(name = "score_intereses", nullable = false)
    private double scoreIntereses;

    @Column(name = "score_academico", nullable = false)
    private double scoreAcademico;

    @Column(name = "score_disponibilidad", nullable = false)
    private double scoreDisponibilidad;

    @Column(name = "confirmado_en", nullable = false)
    private Instant confirmadoEn;
}
