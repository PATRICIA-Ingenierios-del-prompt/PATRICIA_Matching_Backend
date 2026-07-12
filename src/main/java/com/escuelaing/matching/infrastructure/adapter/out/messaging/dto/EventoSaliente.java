package com.escuelaing.matching.infrastructure.adapter.out.messaging.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventoSaliente(
        UUID eventoId,
        Instant timestamp,
        UUID usuarioId,
        String tipo,
        Map<String, Object> payload
) {
}
