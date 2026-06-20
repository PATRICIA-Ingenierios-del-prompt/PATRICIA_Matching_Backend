package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.exception.SugerenciaNoEncontradaException;
import com.escuelaing.matching.domain.model.DecisionMatching;
import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.in.DecidirSobreSugerenciaUseCase;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.escuelaing.matching.domain.port.out.DecisionPendientePort;
import com.escuelaing.matching.domain.port.out.EventoMatchingPort;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecidirSobreSugerenciaService implements DecidirSobreSugerenciaUseCase {

    private final ColaSugerenciasPort colaSugerenciasPort;
    private final DecisionPendientePort decisionPendientePort;
    private final MatchRepositoryPort matchRepositoryPort;
    private final EventoMatchingPort eventoMatchingPort;

    // @Transactional aquí solo protege la escritura en MatchRepositoryPort (JPA/PostgreSQL).
    // Las operaciones sobre Redis (cola de sugerencias, likes pendientes) no participan de
    // esta transacción; su consistencia se resuelve por diseño idempotente (eliminar
    // sugerencia/like es seguro de reintentar).
    @Override
    @Transactional
    public Optional<Match> decidir(UUID usuarioId, UUID candidatoId, DecisionMatching decision) {
        Sugerencia sugerencia = colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)
                .orElseThrow(() -> new SugerenciaNoEncontradaException(
                        "No hay una sugerencia vigente de " + candidatoId + " para el usuario " + usuarioId));

        // La sugerencia se consume al decidir, sea LIKE o DESCARTE.
        colaSugerenciasPort.eliminarSugerencia(usuarioId, candidatoId);

        if (decision == DecisionMatching.DESCARTE) {
            log.debug("Usuario {} descartó al candidato {}", usuarioId, candidatoId);
            return Optional.empty();
        }

        if (matchRepositoryPort.existeEntre(usuarioId, candidatoId)) {
            log.debug("Match ya existente entre {} y {}", usuarioId, candidatoId);
            return matchRepositoryPort.buscarEntre(usuarioId, candidatoId);
        }

        if (decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)) {
            return Optional.of(confirmarMatch(usuarioId, candidatoId, sugerencia.score()));
        }

        decisionPendientePort.registrarLike(usuarioId, candidatoId);
        log.debug("Usuario {} dio LIKE a {}, pendiente de reciprocidad", usuarioId, candidatoId);
        return Optional.empty();
    }

    private Match confirmarMatch(UUID usuarioId, UUID candidatoId, ScoreCompatibilidad score) {
        Match match = Match.confirmar(usuarioId, candidatoId, score);
        matchRepositoryPort.guardar(match);

        decisionPendientePort.eliminarLike(candidatoId, usuarioId);
        decisionPendientePort.eliminarLike(usuarioId, candidatoId);

        eventoMatchingPort.publicarMatchConfirmado(match);
        log.info("Match confirmado entre {} y {} (score={})", usuarioId, candidatoId, score.total());
        return match;
    }
}
