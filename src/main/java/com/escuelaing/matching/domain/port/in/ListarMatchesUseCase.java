package com.escuelaing.matching.domain.port.in;

import com.escuelaing.matching.domain.model.Match;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: lista los matches confirmados (PostgreSQL) en los
 * que participa un usuario.
 */
public interface ListarMatchesUseCase {

    List<Match> listarPara(UUID usuarioId);
}
