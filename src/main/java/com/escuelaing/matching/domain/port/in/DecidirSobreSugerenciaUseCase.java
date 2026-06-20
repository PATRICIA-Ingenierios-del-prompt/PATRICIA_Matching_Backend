package com.escuelaing.matching.domain.port.in;

import com.escuelaing.matching.domain.model.DecisionMatching;
import com.escuelaing.matching.domain.model.Match;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de entrada: registra la decisión (LIKE/DESCARTE) de un usuario
 * sobre un candidato sugerido. Si ambos usuarios emiten LIKE mutuamente,
 * confirma el match, lo persiste y publica {@code match.confirmado}.
 */
public interface DecidirSobreSugerenciaUseCase {

    /**
     * @return el {@link Match} confirmado si esta decisión completó un like
     *         mutuo, o vacío si quedó pendiente de la decisión del otro usuario
     *         (o si fue un descarte).
     */
    Optional<Match> decidir(UUID usuarioId, UUID candidatoId, DecisionMatching decision);
}
