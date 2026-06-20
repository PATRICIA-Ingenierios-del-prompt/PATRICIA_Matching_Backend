package com.escuelaing.matching.infrastructure.adapter.in.messaging.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Estructura de evento consumida desde el broker. Coincide con el shape
 * usado por los demás microservicios de la plataforma (ver AuthEvent en
 * Auth-Service): id de evento, timestamp, usuario afectado, tipo y payload
 * libre para datos adicionales.
 */
public record EventoEntrante(
        UUID eventoId,
        Instant timestamp,
        UUID usuarioId,
        String tipo,
        Map<String, Object> payload
) {
}
