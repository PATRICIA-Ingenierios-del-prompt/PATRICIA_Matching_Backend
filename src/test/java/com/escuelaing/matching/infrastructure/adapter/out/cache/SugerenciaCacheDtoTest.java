package com.escuelaing.matching.infrastructure.adapter.out.cache;

import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SugerenciaCacheDtoTest {

    @Test
    void desdeYADominioHacenRoundTripSinPerderInformacion() {
        Sugerencia original = new Sugerencia(
                UUID.randomUUID(), UUID.randomUUID(),
                ScoreCompatibilidad.de(0.8, 0.6, 1.0),
                Instant.parse("2026-01-01T00:00:00Z"));

        SugerenciaCacheDto dto = SugerenciaCacheDto.desde(original);
        Sugerencia reconstruida = dto.aDominio();

        assertEquals(original.usuarioId(), reconstruida.usuarioId());
        assertEquals(original.candidatoId(), reconstruida.candidatoId());
        assertEquals(original.score().total(), reconstruida.score().total(), 0.0001);
        assertEquals(original.calculadoEn(), reconstruida.calculadoEn());
    }
}
