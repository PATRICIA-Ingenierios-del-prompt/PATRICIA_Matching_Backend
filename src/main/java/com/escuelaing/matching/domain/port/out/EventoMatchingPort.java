package com.escuelaing.matching.domain.port.out;

import com.escuelaing.matching.domain.model.Match;

/**
 * Puerto de salida: publicación de eventos de dominio de Matching
 * hacia el broker (RabbitMQ). Matching solo publica {@code match.confirmado};
 * el resto de servicios (p. ej. Notificaciones) reaccionan a este evento.
 */
public interface EventoMatchingPort {

    void publicarMatchConfirmado(Match match);
}
