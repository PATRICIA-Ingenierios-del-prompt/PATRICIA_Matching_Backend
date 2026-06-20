package com.escuelaing.matching.infrastructure.adapter.in.messaging;

import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import com.escuelaing.matching.infrastructure.adapter.in.messaging.dto.EventoEntrante;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de entrada: una única cola recibe tres routing keys de
 * usuarios-service ({@code usuario.actualizado}, {@code usuario.intereses.actualizados},
 * {@code disponibilidad.cambiada} — ver {@code RabbitConfig}), todas
 * disparando el mismo recálculo completo de la cola de sugerencias del
 * usuario afectado. El campo {@code tipo} del envelope identifica cuál
 * de los tres fue, solo para logging.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioEventConsumer {

    private final CalcularSugerenciasUseCase calcularSugerenciasUseCase;

    @RabbitListener(queues = "${messaging.queues.usuario-actualizado}")
    public void handleEventoUsuario(EventoEntrante evento) {
        UUID usuarioId = evento.usuarioId();

        if (usuarioId == null) {
            log.warn("Evento [{}] recibido sin usuarioId, se ignora", evento.tipo());
            return;
        }

        log.info("Evento [{}] recibido para usuarioId={}, recalculando sugerencias", evento.tipo(), usuarioId);
        calcularSugerenciasUseCase.recalcularPara(usuarioId);
    }
}
