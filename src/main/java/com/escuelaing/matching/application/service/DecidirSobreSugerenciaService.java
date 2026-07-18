package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.exception.SugerenciaNoEncontradaException;
import com.escuelaing.matching.domain.exception.UsuarioNoElegibleException;
import com.escuelaing.matching.domain.model.CalculadoraCompatibilidad;
import com.escuelaing.matching.domain.model.DecisionMatching;
import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.in.DecidirSobreSugerenciaUseCase;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.escuelaing.matching.domain.port.out.DecisionPendientePort;
import com.escuelaing.matching.domain.port.out.DecisionesTomadasPort;
import com.escuelaing.matching.domain.port.out.EventoMatchingPort;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import com.escuelaing.matching.domain.port.out.PerfilUsuarioPort;
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
    private final DecisionesTomadasPort decisionesTomadasPort;
    private final MatchRepositoryPort matchRepositoryPort;
    private final EventoMatchingPort eventoMatchingPort;
    private final PerfilUsuarioPort perfilUsuarioPort;

    // @Transactional aquí solo protege la escritura en MatchRepositoryPort (JPA/PostgreSQL).
    // Las operaciones sobre Redis (cola de sugerencias, likes pendientes) no participan de
    // esta transacción; su consistencia se resuelve por diseño idempotente (eliminar
    // sugerencia/like es seguro de reintentar).
    @Override
    @Transactional
    public Optional<Match> decidir(UUID usuarioId, UUID candidatoId, DecisionMatching decision) {
        Optional<Sugerencia> sugerencia = colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId);

        // Si no hay sugerencia vigente en la cola propia, solo es válido seguir
        // cuando se está respondiendo a una solicitud recibida (candidatoId ya
        // le dio LIKE a usuarioId). Sin esto, un candidato que ya quedó excluido
        // de futuros recálculos por decisionesTomadasPort (porque el usuario ya
        // decidió sobre él antes) nunca podría aceptarse/rechazarse desde
        // "solicitudes recibidas", así el candidato sí tenga un like pendiente.
        if (sugerencia.isEmpty() && !decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)) {
            throw new SugerenciaNoEncontradaException(
                    "No hay una sugerencia vigente de " + candidatoId + " para el usuario " + usuarioId);
        }

        // La sugerencia se consume al decidir, sea LIKE o DESCARTE (si existía en
        // la cola). Se registra también en decisionesTomadasPort para que este
        // candidato no vuelva a aparecer en un recálculo futuro de la cola
        // (ver CalcularSugerenciasService).
        colaSugerenciasPort.eliminarSugerencia(usuarioId, candidatoId);
        decisionesTomadasPort.registrarDecision(usuarioId, candidatoId);

        if (decision == DecisionMatching.DESCARTE) {
            decisionPendientePort.eliminarLike(candidatoId, usuarioId);
            log.debug("Usuario {} descartó al candidato {}", usuarioId, candidatoId);
            return Optional.empty();
        }

        if (matchRepositoryPort.existeEntre(usuarioId, candidatoId)) {
            log.debug("Match ya existente entre {} y {}", usuarioId, candidatoId);
            return matchRepositoryPort.buscarEntre(usuarioId, candidatoId);
        }

        if (decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)) {
            ScoreCompatibilidad score = sugerencia.map(Sugerencia::score)
                    .orElseGet(() -> calcularScoreFresco(usuarioId, candidatoId));
            return Optional.of(confirmarMatch(usuarioId, candidatoId, score));
        }

        decisionPendientePort.registrarLike(usuarioId, candidatoId);
        log.debug("Usuario {} dio LIKE a {}, pendiente de reciprocidad", usuarioId, candidatoId);
        return Optional.empty();
    }

    /**
     * Recalcula el score cuando se responde a una solicitud recibida sin que
     * el candidato siga en la cola propia (ver comentario en {@link #decidir}).
     */
    private ScoreCompatibilidad calcularScoreFresco(UUID usuarioId, UUID candidatoId) {
        PerfilMatching usuario = perfilUsuarioPort.buscarPorId(usuarioId)
                .orElseThrow(() -> new UsuarioNoElegibleException(
                        "Usuario " + usuarioId + " no encontrado en Usuarios"));
        PerfilMatching candidato = perfilUsuarioPort.buscarPorId(candidatoId)
                .orElseThrow(() -> new UsuarioNoElegibleException(
                        "Usuario " + candidatoId + " no encontrado en Usuarios"));
        return CalculadoraCompatibilidad.calcular(usuario, candidato);
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
