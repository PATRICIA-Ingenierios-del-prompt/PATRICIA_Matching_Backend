package com.escuelaing.matching.domain.port.out;

import com.escuelaing.matching.domain.model.PerfilMatching;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (driven port): acceso de solo lectura a los perfiles
 * de usuario relevantes para matching (intereses, carrera, semestre,
 * disponibilidad, estado). La fuente de verdad es el microservicio
 * Usuarios; Matching nunca escribe a través de este puerto.
 */
public interface PerfilUsuarioPort {

    Optional<PerfilMatching> buscarPorId(UUID usuarioId);

    /**
     * Candidatos elegibles para cruzar contra {@code usuarioId}, ya
     * excluyendo al propio usuario. El filtrado fino (estado, intereses
     * mínimos) se aplica en el dominio.
     */
    List<PerfilMatching> buscarCandidatos(UUID usuarioId, int maxCandidatos);
}
