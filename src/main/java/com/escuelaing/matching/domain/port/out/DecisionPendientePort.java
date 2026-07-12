package com.escuelaing.matching.domain.port.out;

import java.util.UUID;

/**
 * Puerto de salida: registra decisiones LIKE pendientes de reciprocidad.
 * Cuando A da LIKE a B, se guarda aquí hasta que B decida sobre A
 * (o expire); permite detectar el like mutuo sin esperar a que ambos
 * decidan en la misma petición.
 */
public interface DecisionPendientePort {

    void registrarLike(UUID usuarioId, UUID candidatoId);

    /** ¿{@code candidatoId} ya le dio LIKE a {@code usuarioId} previamente? */
    boolean existeLikeReciproco(UUID usuarioId, UUID candidatoId);

    void eliminarLike(UUID usuarioId, UUID candidatoId);
}
