package com.escuelaing.matching.domain.port.out;

import com.escuelaing.matching.domain.model.Sugerencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: persistencia de la cola de sugerencias en Redis.
 * Las sugerencias son efímeras (TTL) por diseño: son un caché del
 * cálculo del algoritmo, no la fuente de verdad de relaciones.
 */
public interface ColaSugerenciasPort {

    void reemplazarCola(UUID usuarioId, List<Sugerencia> sugerencias);

    List<Sugerencia> obtenerCola(UUID usuarioId, int limite);

    Optional<Sugerencia> obtenerSugerencia(UUID usuarioId, UUID candidatoId);

    void eliminarSugerencia(UUID usuarioId, UUID candidatoId);

    boolean existeColaVigente(UUID usuarioId);
}
