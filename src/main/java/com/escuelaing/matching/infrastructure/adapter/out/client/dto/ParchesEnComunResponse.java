package com.escuelaing.matching.infrastructure.adapter.out.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Contrato esperado del endpoint interno de Parches Core:
 * {@code GET /internal/parches/en-comun?usuarioA={a}&usuarioB={b}}.
 */
public record ParchesEnComunResponse(
        List<UUID> parcheIds
) {
}
