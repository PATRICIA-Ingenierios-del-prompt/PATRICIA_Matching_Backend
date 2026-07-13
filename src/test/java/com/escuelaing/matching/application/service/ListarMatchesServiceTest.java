package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarMatchesServiceTest {

    private final MatchRepositoryPort matchRepositoryPort = mock(MatchRepositoryPort.class);
    private final ListarMatchesService service = new ListarMatchesService(matchRepositoryPort);

    @Test
    void delegaDirectamenteAlPuertoDeSalida() {
        UUID usuarioId = UUID.randomUUID();
        Match match = Match.confirmar(usuarioId, UUID.randomUUID(), ScoreCompatibilidad.de(0.9, 0.8, 1.0));
        when(matchRepositoryPort.buscarPorUsuario(usuarioId)).thenReturn(List.of(match));

        List<Match> resultado = service.listarPara(usuarioId);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).involucraA(usuarioId));
    }

    @Test
    void devuelveListaVaciaSiNoTieneMatches() {
        UUID usuarioId = UUID.randomUUID();
        when(matchRepositoryPort.buscarPorUsuario(usuarioId)).thenReturn(List.of());

        assertEquals(List.of(), service.listarPara(usuarioId));
    }
}
