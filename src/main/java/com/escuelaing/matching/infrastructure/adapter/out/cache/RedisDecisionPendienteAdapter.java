package com.escuelaing.matching.infrastructure.adapter.out.cache;

import com.escuelaing.matching.domain.port.out.DecisionPendientePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Adaptador de salida: implementa {@link DecisionPendientePort} sobre Redis.
 * <p>
 * Estructura: {@code like:{usuarioId}:{candidatoId}} -&gt; "1", con TTL.
 * Si el otro usuario no decide dentro del TTL, el like pendiente expira
 * y deja de bloquear; el usuario simplemente volverá a aparecer en una
 * futura cola de sugerencias si el recálculo lo determina así.
 */
@Component
@RequiredArgsConstructor
public class RedisDecisionPendienteAdapter implements DecisionPendientePort {

    private static final String PREFIX = "like:";

    private final StringRedisTemplate redisTemplate;

    @Value("${matching.likes.ttl-dias:30}")
    private long ttlDias;

    @Override
    public void registrarLike(UUID usuarioId, UUID candidatoId) {
        redisTemplate.opsForValue().set(key(usuarioId, candidatoId), "1", Duration.ofDays(ttlDias));
    }

    @Override
    public boolean existeLikeReciproco(UUID usuarioId, UUID candidatoId) {
        // ¿candidatoId ya le dio LIKE a usuarioId? -> key invertida
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(candidatoId, usuarioId)));
    }

    @Override
    public void eliminarLike(UUID usuarioId, UUID candidatoId) {
        redisTemplate.delete(key(usuarioId, candidatoId));
    }

    private String key(UUID usuarioId, UUID candidatoId) {
        return PREFIX + usuarioId + ":" + candidatoId;
    }
}
