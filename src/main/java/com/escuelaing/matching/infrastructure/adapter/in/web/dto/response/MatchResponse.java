package com.escuelaing.matching.infrastructure.adapter.in.web.dto.response;

import com.escuelaing.matching.domain.model.Match;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
        UUID matchId,
        UUID otroUsuarioId,
        double scoreTotal,
        Instant confirmadoEn
) {

    public static MatchResponse desde(Match match, UUID usuarioSolicitanteId) {
        return new MatchResponse(
                match.id(),
                match.otroUsuario(usuarioSolicitanteId),
                match.score().total(),
                match.confirmadoEn()
        );
    }
}
