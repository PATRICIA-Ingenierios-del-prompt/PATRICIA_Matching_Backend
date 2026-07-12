package com.escuelaing.matching.infrastructure.adapter.in.web.dto.response;

public record DecisionResponse(
        boolean matchConfirmado,
        MatchResponse match
) {

    public static DecisionResponse sinMatch() {
        return new DecisionResponse(false, null);
    }

    public static DecisionResponse conMatch(MatchResponse match) {
        return new DecisionResponse(true, match);
    }
}
