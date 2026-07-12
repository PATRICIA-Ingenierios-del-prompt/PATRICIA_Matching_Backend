package com.escuelaing.matching.infrastructure.adapter.in.web.dto.response;

import com.escuelaing.matching.domain.model.Sugerencia;

import java.time.Instant;
import java.util.UUID;

public record SugerenciaResponse(
        UUID candidatoId,
        double scoreTotal,
        double scoreIntereses,
        double scoreAcademico,
        double scoreDisponibilidad,
        Instant calculadoEn
) {

    public static SugerenciaResponse desde(Sugerencia sugerencia) {
        return new SugerenciaResponse(
                sugerencia.candidatoId(),
                sugerencia.score().total(),
                sugerencia.score().intereses(),
                sugerencia.score().academico(),
                sugerencia.score().disponibilidad(),
                sugerencia.calculadoEn()
        );
    }
}
