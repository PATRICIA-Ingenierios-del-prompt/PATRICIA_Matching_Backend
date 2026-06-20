package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.exception.SugerenciaNoEncontradaException;
import com.escuelaing.matching.domain.model.DecisionMatching;
import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.escuelaing.matching.domain.port.out.DecisionPendientePort;
import com.escuelaing.matching.domain.port.out.EventoMatchingPort;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DecidirSobreSugerenciaServiceTest {

    private final ColaSugerenciasPort colaSugerenciasPort = mock(ColaSugerenciasPort.class);
    private final DecisionPendientePort decisionPendientePort = mock(DecisionPendientePort.class);
    private final MatchRepositoryPort matchRepositoryPort = mock(MatchRepositoryPort.class);
    private final EventoMatchingPort eventoMatchingPort = mock(EventoMatchingPort.class);

    private final DecidirSobreSugerenciaService service = new DecidirSobreSugerenciaService(
            colaSugerenciasPort, decisionPendientePort, matchRepositoryPort, eventoMatchingPort
    );

    @Test
    void primerLikeQuedaPendienteSinConfirmarMatch() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        Sugerencia sugerencia = sugerencia(usuarioId, candidatoId);

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.of(sugerencia));
        when(matchRepositoryPort.existeEntre(usuarioId, candidatoId)).thenReturn(false);
        when(decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)).thenReturn(false);

        Optional<Match> resultado = service.decidir(usuarioId, candidatoId, DecisionMatching.LIKE);

        assertTrue(resultado.isEmpty());
        verify(decisionPendientePort).registrarLike(usuarioId, candidatoId);
        verify(eventoMatchingPort, never()).publicarMatchConfirmado(any());
    }

    @Test
    void likeReciprocoConfirmaElMatchYPublicaElEvento() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        Sugerencia sugerencia = sugerencia(usuarioId, candidatoId);

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.of(sugerencia));
        when(matchRepositoryPort.existeEntre(usuarioId, candidatoId)).thenReturn(false);
        when(decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)).thenReturn(true);

        Optional<Match> resultado = service.decidir(usuarioId, candidatoId, DecisionMatching.LIKE);

        assertTrue(resultado.isPresent());
        verify(matchRepositoryPort).guardar(any(Match.class));
        verify(eventoMatchingPort).publicarMatchConfirmado(any(Match.class));
        verify(decisionPendientePort).eliminarLike(candidatoId, usuarioId);
        verify(decisionPendientePort).eliminarLike(usuarioId, candidatoId);
    }

    @Test
    void descarteNoRegistraLikeNiPublicaEvento() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        Sugerencia sugerencia = sugerencia(usuarioId, candidatoId);

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.of(sugerencia));

        Optional<Match> resultado = service.decidir(usuarioId, candidatoId, DecisionMatching.DESCARTE);

        assertTrue(resultado.isEmpty());
        verify(decisionPendientePort, never()).registrarLike(any(), any());
        verify(eventoMatchingPort, never()).publicarMatchConfirmado(any());
    }

    @Test
    void lanzaExcepcionSiNoHaySugerenciaVigente() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.empty());

        assertThrows(SugerenciaNoEncontradaException.class,
                () -> service.decidir(usuarioId, candidatoId, DecisionMatching.LIKE));
    }

    private Sugerencia sugerencia(UUID usuarioId, UUID candidatoId) {
        return new Sugerencia(usuarioId, candidatoId, ScoreCompatibilidad.de(0.8, 0.7, 0.6), Instant.now());
    }
}
