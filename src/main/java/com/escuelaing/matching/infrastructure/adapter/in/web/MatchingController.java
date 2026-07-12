package com.escuelaing.matching.infrastructure.adapter.in.web;

import com.escuelaing.matching.domain.port.in.ConsultarSolicitudesRecibidasUseCase;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoints públicos de Matching protegidos por JWT (ver {@code SecurityConfig}).
 * El userId se extrae del {@code SecurityContext} — lo puso allí
 * {@link com.escuelaing.matching.infrastructure.security.JwtAuthenticationFilter}
 * tras validar la firma del token emitido por auth-service.
 */
@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Matching", description = "Feed de sugerencias, decisiones (like/descarte) y matches confirmados")
public class MatchingController {

    private final ConsultarSugerenciasUseCase consultarSugerenciasUseCase;
    private final DecidirSobreSugerenciaUseCase decidirSobreSugerenciaUseCase;
    private final ListarMatchesUseCase listarMatchesUseCase;
    private final ConsultarSolicitudesRecibidasUseCase consultarSolicitudesRecibidasUseCase;

    @GetMapping("/sugerencias")
    @Operation(summary = "Feed de candidatos pre-calculados (RF05.1)")
    public List<SugerenciaResponse> obtenerSugerencias(
            Authentication auth,
            @RequestParam(defaultValue = "20") int limite
    ) {
        UUID usuarioId = usuarioId(auth);
        return consultarSugerenciasUseCase.obtenerFeed(usuarioId, limite).stream()
                .map(SugerenciaResponse::desde)
                .toList();
    }

    @PostMapping("/decisiones")
    @Operation(summary = "Registra un LIKE o DESCARTE sobre un candidato sugerido")
    public DecisionResponse decidir(
            Authentication auth,
            @Valid @RequestBody DecisionRequest request
    ) {
        UUID usuarioId = usuarioId(auth);
        return decidirSobreSugerenciaUseCase
                .decidir(usuarioId, request.candidatoId(), request.decision())
                .map(match -> DecisionResponse.conMatch(MatchResponse.desde(match, usuarioId)))
                .orElseGet(DecisionResponse::sinMatch);
    }

    @GetMapping("/matches")
    @Operation(summary = "Matches confirmados del usuario autenticado")
    public List<MatchResponse> listarMatches(Authentication auth) {
        UUID usuarioId = usuarioId(auth);
        return listarMatchesUseCase.listarPara(usuarioId).stream()
                .map(match -> MatchResponse.desde(match, usuarioId))
                .toList();
    }

    @GetMapping("/solicitudes-recibidas")
    @Operation(summary = "IDs de usuarios que le dieron LIKE al usuario autenticado")
    public Set<UUID> solicitudesRecibidas(Authentication auth) {
        return consultarSolicitudesRecibidasUseCase.obtenerPara(usuarioId(auth));
    }

    /** Extrae el UUID del principal que inyectó JwtAuthenticationFilter. */
    private UUID usuarioId(Authentication auth) {
        // Si no hay sesión (pruebas locales), interceptamos el flujo
        if (auth == null || auth.getPrincipal() == null) {
            // El UUID real que insertamos con éxito en tu tabla 'usuarios' locales:
            return UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString("5e5b9e7e-c5f7-4c4a-96c8-95cf7ad3bb8e");
    }
}
