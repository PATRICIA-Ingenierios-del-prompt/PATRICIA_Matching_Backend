package com.escuelaing.matching.domain.model;

import java.util.Set;
import java.util.UUID;

/**
 * Datos del perfil de un usuario relevantes para el cálculo de compatibilidad.
 * Se obtiene combinando los endpoints internos de Usuarios (Usuario + Perfil);
 * Matching no almacena ni gestiona esta información, solo la consume.
 * <p>
 * Campos alineados 1:1 con lo que Usuarios realmente expone hoy:
 * {@code carrera} (no "programaAcademico"), {@code semestre}, intereses
 * como catálogo cerrado de etiquetas ({@code Interes}), y
 * {@link DisponibilidadUsuario} como estado puntual (no franjas horarias).
 */
public record PerfilMatching(
        UUID usuarioId,
        Set<String> intereses,
        String carrera,
        Integer semestre,
        DisponibilidadUsuario disponibilidad,
        EstadoUsuario estado
) {

    /** Indica si este perfil puede participar en el cálculo de sugerencias. */
    public boolean esElegible() {
        return estado == EstadoUsuario.ACTIVE
                && intereses != null && !intereses.isEmpty();
    }
}
