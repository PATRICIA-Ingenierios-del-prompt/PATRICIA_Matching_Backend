package com.escuelaing.matching.domain.port.out;

import java.util.Set;
import java.util.UUID;

/**
 * Puerto de salida: consulta quién le ha dado LIKE a un usuario dado,
 * sin que éste haya respondido aún. Permite construir la bandeja de
 * "solicitudes recibidas" del tab de Matching en el frontend.
 */
public interface SolicitudesRecibidasPort {

    /** IDs de usuarios que le dieron LIKE a {@code usuarioId} y aún esperan respuesta. */
    Set<UUID> buscarAdmiradoresDe(UUID usuarioId);
}
