package com.escuelaing.matching.infrastructure.adapter.out.cache;

import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Representación serializable (JSON) de una {@link Sugerencia} para
 * almacenar en Redis. El dominio no se serializa directamente para no
 * acoplar el modelo a Jackson.
 */
public record SugerenciaCacheDto(
        UUID usuarioId,
        UUID candidatoId,
        double intereses,
        double academico,
        double disponibilidad,
        Instant calculadoEn
) {

    @JsonCreator
    public SugerenciaCacheDto(
            @JsonProperty("usuarioId") UUID usuarioId,
            @JsonProperty("candidatoId") UUID candidatoId,
            @JsonProperty("intereses") double intereses,
            @JsonProperty("academico") double academico,
            @JsonProperty("disponibilidad") double disponibilidad,
            @JsonProperty("calculadoEn") Instant calculadoEn
    ) {
        this.usuarioId = usuarioId;
        this.candidatoId = candidatoId;
        this.intereses = intereses;
        this.academico = academico;
        this.disponibilidad = disponibilidad;
        this.calculadoEn = calculadoEn;
    }

    public static SugerenciaCacheDto desde(Sugerencia sugerencia) {
        return new SugerenciaCacheDto(
                sugerencia.usuarioId(),
                sugerencia.candidatoId(),
                sugerencia.score().intereses(),
                sugerencia.score().academico(),
                sugerencia.score().disponibilidad(),
                sugerencia.calculadoEn()
        );
    }

    public Sugerencia aDominio() {
        return new Sugerencia(
                usuarioId,
                candidatoId,
                ScoreCompatibilidad.de(intereses, academico, disponibilidad),
                calculadoEn
        );
    }
}
