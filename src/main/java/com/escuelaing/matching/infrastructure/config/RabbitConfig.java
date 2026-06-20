package com.escuelaing.matching.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Matching escucha tres routing keys de {@code patricia.usuarios} (todas
 * declaradas y publicadas por usuarios-service en
 * {@code RabbitMqConfig}/{@code UsuarioEventPublisherAdapter}):
 * <ul>
 *   <li>{@code usuario.actualizado} — cambios generales en el agregado Usuario</li>
 *   <li>{@code usuario.intereses.actualizados} — cambio específico de intereses</li>
 *   <li>{@code disponibilidad.cambiada} — cambio específico de disponibilidad</li>
 * </ul>
 * Las tres disparan el mismo recálculo completo (no hay forma barata de
 * recalcular solo un factor), así que todas se enrutan a la misma cola.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange matchingExchange(
            @Value("${messaging.exchange}") String exchange
    ) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public TopicExchange usuariosExchange(
            @Value("${messaging.usuarios-exchange}") String exchange
    ) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue usuarioActualizadoQueue(
            @Value("${messaging.queues.usuario-actualizado}") String queue
    ) {
        return new Queue(queue, true, false, false);
    }

    @Bean
    public Binding bindingUsuarioActualizado(
            @Qualifier("usuarioActualizadoQueue") Queue queue,
            @Qualifier("usuariosExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("usuario.actualizado");
    }

    @Bean
    public Binding bindingInteresesActualizados(
            @Qualifier("usuarioActualizadoQueue") Queue queue,
            @Qualifier("usuariosExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("usuario.intereses.actualizados");
    }

    @Bean
    public Binding bindingDisponibilidadCambiada(
            @Qualifier("usuarioActualizadoQueue") Queue queue,
            @Qualifier("usuariosExchange") TopicExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with("disponibilidad.cambiada");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
