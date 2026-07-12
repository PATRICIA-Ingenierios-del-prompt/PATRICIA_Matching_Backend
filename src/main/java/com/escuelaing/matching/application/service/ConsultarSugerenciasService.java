package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import com.escuelaing.matching.domain.port.in.ConsultarSugerenciasUseCase;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarSugerenciasService implements ConsultarSugerenciasUseCase {

    private final ColaSugerenciasPort colaSugerenciasPort;
    private final CalcularSugerenciasUseCase calcularSugerenciasUseCase;

    @Override
    public List<Sugerencia> obtenerFeed(UUID usuarioId, int limite) {
        if (!colaSugerenciasPort.existeColaVigente(usuarioId)) {
            calcularSugerenciasUseCase.recalcularPara(usuarioId);
        }
        return colaSugerenciasPort.obtenerCola(usuarioId, limite);
    }
}
