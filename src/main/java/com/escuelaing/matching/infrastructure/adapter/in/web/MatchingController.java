package com.escuelaing.matching.infrastructure.adapter.in.web;

import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.port.in.ConsultarSugerenciasUseCase;
import com.escuelaing.matching.domain.port.in.DecidirSobreSugerenciaUseCase;
import com.escuelaing.matching.domain.port.in.ListarMatchesUseCase;
import com.escuelaing.matching.infrastructure.adapter.in.web.dto.request.DecisionRequest;
import com.escuelaing.matching.infrastructure.adapter.in.web.dto.response.DecisionResponse;
import com.escuelaing.matching.infrastructure.adapter.in.web.dto.response.MatchResponse;
import com.escuelaing.matching.infrastructure.adapter.in.web.dto.response.SugerenciaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints públicos de Matching. La identidad del usuario autenticado
 * llega en el header {@code X-User-Id}, propagado por el Gateway tras
 * validar el JWT con Auth (Matching no valida tokens, confía en el
 * perímetro de la plataforma).
 */
@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "Feed de sugerencias, decisiones (like/descarte) y matches confirmados")
public class MatchingController {

    private final ConsultarSugerenciasUseCase consultarSugerenciasUseCase;
    private final DecidirSobreSugerenciaUseCase decidirSobreSugerenciaUseCase;
    private final ListarMatchesUseCase listarMatchesUseCase;

    @GetMapping("/sugerencias")
    @Operation(summary = "Obtiene el feed de candidatos pre-calculados (RF05.1)",
            description = "Si no hay una cola vigente en Redis, la calcula bajo demanda antes de responder.")
    public List<SugerenciaResponse> obtenerSugerencias(
            @RequestHeader("X-User-Id") UUID usuarioId,
            @RequestParam(defaultValue = "20") int limite
    ) {
        return consultarSugerenciasUseCase.obtenerFeed(usuarioId, limite).stream()
                .map(SugerenciaResponse::desde)
                .toList();
    }

    @PostMapping("/decisiones")
    @Operation(summary = "Registra un LIKE o DESCARTE sobre un candidato sugerido",
            description = "Si ambos usuarios se dan LIKE mutuamente, confirma el match y devuelve sus datos.")
    public DecisionResponse decidir(
            @RequestHeader("X-User-Id") UUID usuarioId,
            @Valid @RequestBody DecisionRequest request
    ) {
        return decidirSobreSugerenciaUseCase
                .decidir(usuarioId, request.candidatoId(), request.decision())
                .map(match -> DecisionResponse.conMatch(MatchResponse.desde(match, usuarioId)))
                .orElseGet(DecisionResponse::sinMatch);
    }

    @GetMapping("/matches")
    @Operation(summary = "Lista los matches confirmados del usuario autenticado")
    public List<MatchResponse> listarMatches(@RequestHeader("X-User-Id") UUID usuarioId) {
        return listarMatchesUseCase.listarPara(usuarioId).stream()
                .map(match -> MatchResponse.desde(match, usuarioId))
                .toList();
    }
}
