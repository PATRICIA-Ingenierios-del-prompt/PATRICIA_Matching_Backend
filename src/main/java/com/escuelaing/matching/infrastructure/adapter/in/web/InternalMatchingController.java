package com.escuelaing.matching.infrastructure.adapter.in.web;

import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints internos de Matching, protegidos por {@code X-Internal-Api-Key}
 * (ver {@code InternalApiKeyFilter}). Usados por otros microservicios para
 * forzar un recálculo puntual (p. ej. tras una corrección administrativa
 * en Usuarios que no amerite esperar al evento {@code usuario.actualizado}).
 */
@RestController
@RequestMapping("/internal/matching")
@RequiredArgsConstructor
@Tag(name = "Internal - Matching", description = "Endpoints internos servicio-a-servicio")
public class InternalMatchingController {

    private final CalcularSugerenciasUseCase calcularSugerenciasUseCase;

    @PostMapping("/recalcular/{usuarioId}")
    @Operation(summary = "Fuerza el recálculo de la cola de sugerencias de un usuario")
    public ResponseEntity<Void> recalcular(@PathVariable UUID usuarioId) {
        calcularSugerenciasUseCase.recalcularPara(usuarioId);
        return ResponseEntity.accepted().build();
    }
}
