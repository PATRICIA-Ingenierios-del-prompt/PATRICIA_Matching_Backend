package com.escuelaing.matching.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Match confirmado entre dos usuarios: ambos emitieron LIKE mutuamente
 * sobre la sugerencia calculada por el algoritmo. Es la entidad raíz
 * persistida en PostgreSQL (fuente de verdad de matches confirmados).
 * <p>
 * Los IDs de usuario se normalizan (usuarioMenorId &lt; usuarioMayorId)
 * para que el par sea único independientemente del orden en que se
 * emitieron los likes, evitando duplicados A-B / B-A.
 */
public class Match {

    private final UUID id;
    private final UUID usuarioMenorId;
    private final UUID usuarioMayorId;
    private final ScoreCompatibilidad score;
    private final Instant confirmadoEn;

    private Match(UUID id, UUID usuarioMenorId, UUID usuarioMayorId,
                  ScoreCompatibilidad score, Instant confirmadoEn) {
        this.id = id;
        this.usuarioMenorId = usuarioMenorId;
        this.usuarioMayorId = usuarioMayorId;
        this.score = score;
        this.confirmadoEn = confirmadoEn;
    }

    /**
     * Crea un nuevo match confirmado a partir de un par de usuarios (orden libre)
     * y el score de compatibilidad calculado.
     */
    public static Match confirmar(UUID usuarioA, UUID usuarioB, ScoreCompatibilidad score) {
        if (usuarioA == null || usuarioB == null) {
            throw new IllegalArgumentException("Ambos usuarios son obligatorios para confirmar un match");
        }
        if (usuarioA.equals(usuarioB)) {
            throw new IllegalArgumentException("Un usuario no puede hacer match consigo mismo");
        }
        if (score == null) {
            throw new IllegalArgumentException("score es obligatorio");
        }

        UUID menor = usuarioA.compareTo(usuarioB) <= 0 ? usuarioA : usuarioB;
        UUID mayor = usuarioA.compareTo(usuarioB) <= 0 ? usuarioB : usuarioA;

        return new Match(UUID.randomUUID(), menor, mayor, score, Instant.now());
    }

    /** Reconstruye un match existente desde el adaptador de persistencia. */
    public static Match reconstruir(UUID id, UUID usuarioMenorId, UUID usuarioMayorId,
                                     ScoreCompatibilidad score, Instant confirmadoEn) {
        return new Match(id, usuarioMenorId, usuarioMayorId, score, confirmadoEn);
    }

    public boolean involucraA(UUID usuarioId) {
        return usuarioMenorId.equals(usuarioId) || usuarioMayorId.equals(usuarioId);
    }

    public UUID otroUsuario(UUID usuarioId) {
        if (usuarioMenorId.equals(usuarioId)) {
            return usuarioMayorId;
        }
        if (usuarioMayorId.equals(usuarioId)) {
            return usuarioMenorId;
        }
        throw new IllegalArgumentException("El usuario " + usuarioId + " no participa en este match");
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioMenorId() {
        return usuarioMenorId;
    }

    public UUID usuarioMayorId() {
        return usuarioMayorId;
    }

    public ScoreCompatibilidad score() {
        return score;
    }

    public Instant confirmadoEn() {
        return confirmadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Match other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
