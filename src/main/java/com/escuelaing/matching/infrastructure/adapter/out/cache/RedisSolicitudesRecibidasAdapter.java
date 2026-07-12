package com.escuelaing.matching.infrastructure.adapter.out.cache;

import com.escuelaing.matching.domain.port.out.SolicitudesRecibidasPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador de salida: escanea las keys Redis con patrón
 * {@code like:*:{usuarioId}} para encontrar todos los usuarios que le
 * dieron LIKE al destinatario pero aún no recibieron respuesta.
 * <p>
 * Nota de rendimiento: {@code keys(pattern)} es O(N) sobre todo el keyspace
 * y no debe usarse en producción con Redis de alta carga. Si el volumen
 * crece, migrar a un índice inverso explícito (SET de admiradores por usuario).
 * Para el tamaño actual del proyecto es aceptable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSolicitudesRecibidasAdapter implements SolicitudesRecibidasPort {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Set<UUID> buscarAdmiradoresDe(UUID usuarioId) {
        String patron = "like:*:" + usuarioId;
        Set<String> keys = redisTemplate.keys(patron);

        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }

        Set<UUID> admiradores = new HashSet<>();
        for (String key : keys) {
            // key = "like:{admiradorId}:{usuarioId}"
            String[] partes = key.split(":");
            if (partes.length == 3) {
                try {
                    admiradores.add(UUID.fromString(partes[1]));
                } catch (IllegalArgumentException ex) {
                    log.warn("Key Redis malformada ignorada: {}", key);
                }
            }
        }
        return admiradores;
    }
}
