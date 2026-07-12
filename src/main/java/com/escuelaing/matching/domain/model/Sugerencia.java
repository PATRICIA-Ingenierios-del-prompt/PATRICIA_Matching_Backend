package com.escuelaing.matching.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Sugerencia de compatibilidad entre dos usuarios, calculada por el
 * algoritmo de Matching y persistida temporalmente en la cola de Redis.
 * <p>
 * No es un match confirmado: representa un candidato propuesto a
 * {@code usuarioId} para que decida si lo acepta (like) o lo descarta.
 */
public record Sugerencia(
        UUID usuarioId,
        UUID candidatoId,
        ScoreCompatibilidad score,
        Instant calculadoEn
) {

    public Sugerencia {
        if (usuarioId == null || candidatoId == null) {
            throw new IllegalArgumentException("usuarioId y candidatoId son obligatorios");
        }
        if (usuarioId.equals(candidatoId)) {
            throw new IllegalArgumentException("Un usuario no puede ser su propio candidato");
        }
        if (score == null) {
            throw new IllegalArgumentException("score es obligatorio");
        }
        if (calculadoEn == null) {
            calculadoEn = Instant.now();
        }
    }

    public boolean superaUmbralAutomatico(double umbral) {
        return score.total() >= umbral;
    }
}
