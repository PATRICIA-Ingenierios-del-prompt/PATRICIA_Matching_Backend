package com.escuelaing.matching.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {

    @Test
    void normalizaElParIndependientementeDelOrden() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ScoreCompatibilidad score = ScoreCompatibilidad.de(0.8, 0.7, 0.6);

        Match matchAB = Match.confirmar(a, b, score);
        Match matchBA = Match.confirmar(b, a, score);

        assertEquals(matchAB.usuarioMenorId(), matchBA.usuarioMenorId());
        assertEquals(matchAB.usuarioMayorId(), matchBA.usuarioMayorId());
    }

    @Test
    void rechazaMatchConsigoMismo() {
        UUID a = UUID.randomUUID();
        ScoreCompatibilidad score = ScoreCompatibilidad.de(1, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> Match.confirmar(a, a, score));
    }

    @Test
    void otroUsuarioDevuelveElParticipanteCorrecto() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Match match = Match.confirmar(a, b, ScoreCompatibilidad.de(1, 1, 1));

        assertEquals(b, match.otroUsuario(a));
        assertEquals(a, match.otroUsuario(b));
        assertTrue(match.involucraA(a));
        assertTrue(match.involucraA(b));
    }

    @Test
    void otroUsuarioLanzaExcepcionSiNoParticipa() {
        Match match = Match.confirmar(UUID.randomUUID(), UUID.randomUUID(), ScoreCompatibilidad.de(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> match.otroUsuario(UUID.randomUUID()));
    }
}
