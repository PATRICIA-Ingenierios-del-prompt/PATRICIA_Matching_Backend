package com.escuelaing.matching.infrastructure.adapter.out.cache;

import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de salida: implementa {@link ColaSugerenciasPort} sobre Redis.
 * <p>
 * Estructura:
 * <pre>
 *   sugerencias:{usuarioId}            -&gt; HASH { candidatoId -&gt; SugerenciaCacheDto (JSON) }   TTL
 * </pre>
 * Se usa un HASH (no un SORTED SET con el score como puntaje) porque
 * además del score total se necesita el desglose por factor para la
 * respuesta de API; el orden se resuelve en memoria al leer, ya que el
 * tamaño de la cola es acotado (configurable, por defecto 50).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisColaSugerenciasAdapter implements ColaSugerenciasPort {

    private static final String PREFIX = "sugerencias:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${matching.sugerencias.ttl-horas:24}")
    private long ttlHoras;

    // TTL corto y separado del de la cola real: un pool vacío suele ser
    // transitorio (fallo de un servicio externo, usuario recién creado,
    // etc.), así que no conviene "recordarlo" 24h como si fuera una cola
    // con resultados legítimos. Con esto, un falso vacío se autocorrige
    // solo en minutos en vez de quedar pegado un día entero.
    @Value("${matching.sugerencias.empty-ttl-minutos:15}")
    private long emptyTtlMinutos;

    @Override
    public void reemplazarCola(UUID usuarioId, List<Sugerencia> sugerencias) {
        String key = key(usuarioId);

        redisTemplate.delete(key);

        if (sugerencias.isEmpty()) {
            redisTemplate.opsForValue().set(key + ":empty", "1", Duration.ofMinutes(emptyTtlMinutos));
            return;
        }

        Map<String, String> entradas = sugerencias.stream()
                .collect(Collectors.toMap(
                        s -> s.candidatoId().toString(),
                        this::serializar
                ));

        redisTemplate.opsForHash().putAll(key, entradas);
        redisTemplate.expire(key, Duration.ofHours(ttlHoras));
    }

    @Override
    public List<Sugerencia> obtenerCola(UUID usuarioId, int limite) {
        Map<Object, Object> entradas = redisTemplate.opsForHash().entries(key(usuarioId));

        return entradas.values().stream()
                .map(valor -> deserializar((String) valor))
                .sorted(Comparator.comparingDouble((Sugerencia s) -> s.score().total()).reversed())
                .limit(limite)
                .toList();
    }

    @Override
    public Optional<Sugerencia> obtenerSugerencia(UUID usuarioId, UUID candidatoId) {
        Object valor = redisTemplate.opsForHash().get(key(usuarioId), candidatoId.toString());
        if (valor == null) {
            return Optional.empty();
        }
        return Optional.of(deserializar((String) valor));
    }

    @Override
    public void eliminarSugerencia(UUID usuarioId, UUID candidatoId) {
        redisTemplate.opsForHash().delete(key(usuarioId), candidatoId.toString());
    }

    @Override
    public boolean existeColaVigente(UUID usuarioId) {
        String key = key(usuarioId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key))
                || Boolean.TRUE.equals(redisTemplate.hasKey(key + ":empty"));
    }

    private String key(UUID usuarioId) {
        return PREFIX + usuarioId;
    }

    @SneakyThrows
    private String serializar(Sugerencia sugerencia) {
        return objectMapper.writeValueAsString(SugerenciaCacheDto.desde(sugerencia));
    }

    @SneakyThrows
    private Sugerencia deserializar(String json) {
        return objectMapper.readValue(json, SugerenciaCacheDto.class).aDominio();
    }
}
