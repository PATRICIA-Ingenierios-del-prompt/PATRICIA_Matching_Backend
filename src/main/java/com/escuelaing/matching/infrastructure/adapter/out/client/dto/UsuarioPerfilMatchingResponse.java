package com.escuelaing.matching.infrastructure.adapter.out.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * Contrato esperado de un endpoint interno de Usuarios que HOY NO EXISTE
 * en el repo (ver TODO_INTEGRACIONES.md). Se propone como:
 * {@code GET /internal/usuarios/{id}/perfil-matching}.
 * <p>
 * Composición sugerida en Usuarios: {@code UsuarioResponse} (estado) +
 * {@code PerfilResponse} (carrera, semestre, intereses, disponibilidad),
 * que hoy se sirven por separado vía {@code /internal/usuarios/{id}}
 * (API key) y {@code /api/v1/usuarios/{id}/perfil} (JWT de usuario final,
 * no apto para llamadas servicio-a-servicio).
 */
public record UsuarioPerfilMatchingResponse(
        UUID id,
        String estado,
        List<String> intereses,
        String carrera,
        Integer semestre,
        String disponibilidad
) {
}
