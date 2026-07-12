package com.escuelaing.matching.domain.port.in;

import java.util.UUID;

/**
 * Puerto de entrada (driving port): calcula la cola de sugerencias de
 * compatibilidad para un usuario, cruzando intereses, afinidad académica
 * y disponibilidad horaria (RF05).
 */
public interface CalcularSugerenciasUseCase {

    /**
     * Recalcula y reemplaza la cola de sugerencias de {@code usuarioId}
     * en Redis. Se invoca de forma asíncrona al recibir {@code usuario.actualizado}
     * o bajo demanda cuando el usuario solicita su feed y no tiene cola vigente.
     */
    void recalcularPara(UUID usuarioId);
}
