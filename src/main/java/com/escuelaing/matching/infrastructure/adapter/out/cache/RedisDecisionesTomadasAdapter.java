package com.escuelaing.matching.infrastructure.adapter.out.cache;

import com.escuelaing.matching.domain.port.out.DecisionesTomadasPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Adaptador de salida: implementa {@link DecisionesTomadasPort} sobre Redis.
 * <p>
 * Estructura: {@code decidido:{usuarioId}:{candidatoId}} -&gt; "1", con TTL
 * largo (por defecto 180 días) para que, en la práctica, un candidato sobre
 * el que ya se decidió no reaparezca en la cola de sugerencias de ese
 * usuario.
 */
@Component
@RequiredArgsConstructor
public class RedisDecisionesTomadasAdapter implements DecisionesTomadasPort {

    private static final String PREFIX = "decidido:";

    private final StringRedisTemplate redisTemplate;

    @Value("${matching.decisiones.ttl-dias:180}")
    private long ttlDias;

    @Override
    public void registrarDecision(UUID usuarioId, UUID candidatoId) {
        redisTemplate.opsForValue().set(key(usuarioId, candidatoId), "1", Duration.ofDays(ttlDias));
    }

    @Override
    public boolean yaDecidioSobre(UUID usuarioId, UUID candidatoId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(usuarioId, candidatoId)));
    }

    private String key(UUID usuarioId, UUID candidatoId) {
        return PREFIX + usuarioId + ":" + candidatoId;
    }
}
