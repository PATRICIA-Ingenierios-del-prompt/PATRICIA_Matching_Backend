package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.exception.SugerenciaNoEncontradaException;
import com.escuelaing.matching.domain.model.DecisionMatching;
import com.escuelaing.matching.domain.model.DisponibilidadUsuario;
import com.escuelaing.matching.domain.model.EstadoUsuario;
import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.escuelaing.matching.domain.port.out.DecisionPendientePort;
import com.escuelaing.matching.domain.port.out.DecisionesTomadasPort;
import com.escuelaing.matching.domain.port.out.EventoMatchingPort;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import com.escuelaing.matching.domain.port.out.PerfilUsuarioPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
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
    private final DecisionesTomadasPort decisionesTomadasPort = mock(DecisionesTomadasPort.class);
    private final MatchRepositoryPort matchRepositoryPort = mock(MatchRepositoryPort.class);
    private final EventoMatchingPort eventoMatchingPort = mock(EventoMatchingPort.class);
    private final PerfilUsuarioPort perfilUsuarioPort = mock(PerfilUsuarioPort.class);

    private final DecidirSobreSugerenciaService service = new DecidirSobreSugerenciaService(
            colaSugerenciasPort, decisionPendientePort, decisionesTomadasPort, matchRepositoryPort,
            eventoMatchingPort, perfilUsuarioPort
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
        verify(decisionesTomadasPort).registrarDecision(usuarioId, candidatoId);
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
        verify(decisionesTomadasPort).registrarDecision(usuarioId, candidatoId);
        verify(eventoMatchingPort, never()).publicarMatchConfirmado(any());
    }

    @Test
    void lanzaExcepcionSiNoHaySugerenciaVigenteYNoHayLikeRecibido() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.empty());
        when(decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)).thenReturn(false);

        assertThrows(SugerenciaNoEncontradaException.class,
                () -> service.decidir(usuarioId, candidatoId, DecisionMatching.LIKE));
    }

    @Test
    void aceptaSolicitudRecibidaAunSinSugerenciaVigenteEnLaColaPropia() {
        // Reproduce el caso real: candidatoId ya le dio LIKE a usuarioId (aparece
        // en "solicitudes recibidas"), pero usuarioId ya había decidido sobre él
        // antes por otra vía, así que decisionesTomadasPort lo excluyó de la cola
        // propia en el último recálculo. Aceptar la solicitud no debe fallar.
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        PerfilMatching usuario = perfil(usuarioId);
        PerfilMatching candidato = perfil(candidatoId);

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.empty());
        when(decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)).thenReturn(true);
        when(matchRepositoryPort.existeEntre(usuarioId, candidatoId)).thenReturn(false);
        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarPorId(candidatoId)).thenReturn(Optional.of(candidato));

        Optional<Match> resultado = service.decidir(usuarioId, candidatoId, DecisionMatching.LIKE);

        assertTrue(resultado.isPresent());
        verify(matchRepositoryPort).guardar(any(Match.class));
        verify(eventoMatchingPort).publicarMatchConfirmado(any(Match.class));
        verify(decisionesTomadasPort).registrarDecision(usuarioId, candidatoId);
    }

    @Test
    void rechazaSolicitudRecibidaAunSinSugerenciaVigenteEnLaColaPropia() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        when(colaSugerenciasPort.obtenerSugerencia(usuarioId, candidatoId)).thenReturn(Optional.empty());
        when(decisionPendientePort.existeLikeReciproco(usuarioId, candidatoId)).thenReturn(true);

        Optional<Match> resultado = service.decidir(usuarioId, candidatoId, DecisionMatching.DESCARTE);

        assertTrue(resultado.isEmpty());
        verify(decisionesTomadasPort).registrarDecision(usuarioId, candidatoId);
        verify(eventoMatchingPort, never()).publicarMatchConfirmado(any());
    }

    private Sugerencia sugerencia(UUID usuarioId, UUID candidatoId) {
        return new Sugerencia(usuarioId, candidatoId, ScoreCompatibilidad.de(0.8, 0.7, 0.6), Instant.now());
    }

    private PerfilMatching perfil(UUID id) {
        return new PerfilMatching(id, Set.of("Musica"), "Ingeniería de Sistemas", 5,
                DisponibilidadUsuario.DISPONIBLE, EstadoUsuario.ACTIVE);
    }
}
