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
        if (auth == null || !(auth.getPrincipal() instanceof UUID uuid)) {
            // No debería ocurrir nunca: SecurityConfig exige authenticated()
            // en /matching/** y JwtAuthenticationFilter siempre puebla el
            // principal con un UUID cuando la autenticación es válida. Si
            // esto se dispara, es un bug de configuración — hay que
            // rechazar la petición, jamás suplantar a un usuario real.
            throw new org.springframework.security.access.AccessDeniedException(
                    "No se pudo determinar el usuario autenticado");
        }
        return uuid;
    }
}
