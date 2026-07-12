package com.escuelaing.matching.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraCompatibilidadTest {

    @Test
    void perfilesIdenticosObtienenScoreMaximoEnInteresesYAcademico() {
        PerfilMatching usuario = perfil(
                Set.of("musica", "cine", "deporte"),
                "Ingenieria de Sistemas", 5, DisponibilidadUsuario.DISPONIBLE
        );
        PerfilMatching candidato = perfil(
                Set.of("musica", "cine", "deporte"),
                "Ingenieria de Sistemas", 5, DisponibilidadUsuario.DISPONIBLE
        );

        ScoreCompatibilidad score = CalculadoraCompatibilidad.calcular(usuario, candidato);

        assertEquals(1.0, score.intereses(), 0.001);
        assertEquals(1.0, score.academico(), 0.001);
        assertEquals(1.0, score.disponibilidad(), 0.001);
    }

    @Test
    void sinInteresesEnComunElFactorInteresesEsCero() {
        PerfilMatching usuario = perfil(Set.of("musica"), "Sistemas", 1, DisponibilidadUsuario.DISPONIBLE);
        PerfilMatching candidato = perfil(Set.of("deporte"), "Sistemas", 1, DisponibilidadUsuario.DISPONIBLE);

        ScoreCompatibilidad score = CalculadoraCompatibilidad.calcular(usuario, candidato);

        assertEquals(0.0, score.intereses(), 0.001);
    }

    @Test
    void elTotalRespetaLaPonderacion40_30_30() {
        ScoreCompatibilidad score = ScoreCompatibilidad.de(1.0, 1.0, 1.0);
        assertEquals(1.0, score.total(), 0.001);

        ScoreCompatibilidad soloIntereses = ScoreCompatibilidad.de(1.0, 0.0, 0.0);
        assertEquals(0.40, soloIntereses.total(), 0.001);
    }

    @Test
    void disponibilidadEsCeroSiAlgunoNoEstaDisponible() {
        PerfilMatching usuario = perfil(Set.of("musica"), "Sistemas", 1, DisponibilidadUsuario.DISPONIBLE);
        PerfilMatching candidato = perfil(Set.of("musica"), "Sistemas", 1, DisponibilidadUsuario.OCUPADO);

        ScoreCompatibilidad score = CalculadoraCompatibilidad.calcular(usuario, candidato);

        assertEquals(0.0, score.disponibilidad(), 0.001);
    }

    @Test
    void disponibilidadEsUnoSiAmbosEstanDisponibles() {
        PerfilMatching usuario = perfil(Set.of("musica"), "Sistemas", 1, DisponibilidadUsuario.DISPONIBLE);
        PerfilMatching candidato = perfil(Set.of("musica"), "Sistemas", 1, DisponibilidadUsuario.DISPONIBLE);

        ScoreCompatibilidad score = CalculadoraCompatibilidad.calcular(usuario, candidato);

        assertEquals(1.0, score.disponibilidad(), 0.001);
    }

    private PerfilMatching perfil(Set<String> intereses, String carrera, int semestre, DisponibilidadUsuario disponibilidad) {
        return new PerfilMatching(UUID.randomUUID(), intereses, carrera, semestre, disponibilidad, EstadoUsuario.ACTIVE);
    }
}
