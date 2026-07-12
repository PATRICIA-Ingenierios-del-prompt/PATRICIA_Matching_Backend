package com.escuelaing.matching.domain.port.out;

import com.escuelaing.matching.domain.model.Match;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: persistencia de matches confirmados en PostgreSQL
 * (fuente de verdad de relaciones de match, a diferencia de la cola
 * efímera de sugerencias en Redis).
 */
public interface MatchRepositoryPort {

    Match guardar(Match match);

    Optional<Match> buscarEntre(UUID usuarioA, UUID usuarioB);

    List<Match> buscarPorUsuario(UUID usuarioId);

    boolean existeEntre(UUID usuarioA, UUID usuarioB);
}
