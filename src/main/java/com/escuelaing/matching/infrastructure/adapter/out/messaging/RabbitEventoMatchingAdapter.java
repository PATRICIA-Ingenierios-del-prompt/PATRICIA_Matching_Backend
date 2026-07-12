package com.escuelaing.matching.infrastructure.adapter.out.messaging;

import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.port.out.EventoMatchingPort;
import com.escuelaing.matching.infrastructure.adapter.out.messaging.dto.EventoSaliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de salida: publica eventos de Matching al exchange
 * {@code patricia.matching}. Único evento publicado por este servicio:
 * {@code match.confirmado}, consumido por Notificaciones para alertar
 * a ambos usuarios (RF05.4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEventoMatchingAdapter implements EventoMatchingPort {

    private static final String ROUTING_KEY_MATCH_CONFIRMADO = "match.confirmado";

    private final RabbitTemplate rabbitTemplate;

    @Value("${messaging.exchange}")
    private String exchange;

    @Override
    public void publicarMatchConfirmado(Match match) {
        publicarPara(match.usuarioMenorId(), match);
        publicarPara(match.usuarioMayorId(), match);
    }

    private void publicarPara(UUID destinatarioId, Match match) {
        EventoSaliente evento = new EventoSaliente(
                UUID.randomUUID(),
                Instant.now(),
                destinatarioId,
                ROUTING_KEY_MATCH_CONFIRMADO,
                Map.of(
                        "matchId", match.id(),
                        "otroUsuarioId", match.otroUsuario(destinatarioId),
                        "scoreTotal", match.score().total()
                )
        );
        rabbitTemplate.convertAndSend(exchange, ROUTING_KEY_MATCH_CONFIRMADO, evento);
        log.debug("Evento match.confirmado publicado para destinatario={} matchId={}", destinatarioId, match.id());
    }
}
