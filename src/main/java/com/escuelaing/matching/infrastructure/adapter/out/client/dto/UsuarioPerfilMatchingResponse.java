package com.escuelaing.matching.infrastructure.adapter.out.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Contrato consumido de Usuarios vía {@code GET /internal/usuarios/{id}/perfil-matching}
 * y {@code GET /internal/usuarios/candidatos-matching} (ver {@code InternalUsuarioController}
 * en el repo de usuarios-service, DTO {@code PerfilMatchingResponse}).
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}: usuarios-service agregó
 * {@code urlFotoPerfil}/{@code tienePersonaEnFoto}/{@code franjasDisponibilidad}
 * a ese contrato para otros consumidores; matching-service no los necesita
 * (no participan del cálculo de compatibilidad), pero sin esta anotación
 * Jackson lanza {@code UnrecognizedPropertyException} en cada respuesta
 * (falla por defecto ante campos desconocidos), y el Feign client lo atrapa
 * como {@code FeignException} devolviendo listas/perfiles vacíos en
 * silencio — vaciando el pool de candidatos sin ningún error visible.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsuarioPerfilMatchingResponse(
        UUID id,
        String estado,
        List<String> intereses,
        String carrera,
        Integer semestre,
        String disponibilidad
) {
}
