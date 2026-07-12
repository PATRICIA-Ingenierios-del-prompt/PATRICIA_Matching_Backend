package com.escuelaing.matching.infrastructure.adapter.in.web.dto.request;

import com.escuelaing.matching.domain.model.DecisionMatching;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DecisionRequest(
        @NotNull(message = "candidatoId es obligatorio")
        UUID candidatoId,

        @NotNull(message = "decision es obligatoria")
        DecisionMatching decision
) {
}
