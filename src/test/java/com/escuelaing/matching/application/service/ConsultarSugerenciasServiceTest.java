package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarSugerenciasServiceTest {

    private final ColaSugerenciasPort colaSugerenciasPort = mock(ColaSugerenciasPort.class);
    private final CalcularSugerenciasUseCase calcularSugerenciasUseCase = mock(CalcularSugerenciasUseCase.class);

    private final ConsultarSugerenciasService service =
            new ConsultarSugerenciasService(colaSugerenciasPort, calcularSugerenciasUseCase);

    @Test
    void siLaColaYaEstaVigenteNoRecalcula() {
        UUID usuarioId = UUID.randomUUID();
        Sugerencia sugerencia = new Sugerencia(usuarioId, UUID.randomUUID(),
                ScoreCompatibilidad.de(0.8, 0.7, 1.0), Instant.now());

        when(colaSugerenciasPort.existeColaVigente(usuarioId)).thenReturn(true);
        when(colaSugerenciasPort.obtenerCola(usuarioId, 20)).thenReturn(List.of(sugerencia));

        List<Sugerencia> resultado = service.obtenerFeed(usuarioId, 20);

        assertEquals(1, resultado.size());
        verify(calcularSugerenciasUseCase, never()).recalcularPara(usuarioId);
    }

    @Test
    void siLaColaNoEstaVigenteRecalculaAntesDeTraerla() {
        UUID usuarioId = UUID.randomUUID();
        when(colaSugerenciasPort.existeColaVigente(usuarioId)).thenReturn(false);
        when(colaSugerenciasPort.obtenerCola(usuarioId, 20)).thenReturn(List.of());

        service.obtenerFeed(usuarioId, 20);

        verify(calcularSugerenciasUseCase).recalcularPara(usuarioId);
        verify(colaSugerenciasPort).obtenerCola(usuarioId, 20);
    }
}
