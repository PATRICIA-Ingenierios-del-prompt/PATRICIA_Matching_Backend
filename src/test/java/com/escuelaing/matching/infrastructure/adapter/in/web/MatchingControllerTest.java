package com.escuelaing.matching.infrastructure.adapter.in.web;

import com.escuelaing.matching.domain.model.DecisionMatching;
import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.in.ConsultarSolicitudesRecibidasUseCase;
import com.escuelaing.matching.domain.port.in.ConsultarSugerenciasUseCase;
import com.escuelaing.matching.domain.port.in.DecidirSobreSugerenciaUseCase;
import com.escuelaing.matching.domain.port.in.ListarMatchesUseCase;
import com.escuelaing.matching.infrastructure.config.CorsConfig;
import com.escuelaing.matching.infrastructure.config.SecurityConfig;
import com.escuelaing.matching.infrastructure.exception.GlobalExceptionHandler;
import com.escuelaing.matching.infrastructure.security.JwtTokenParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comportamiento de MatchingController: mapeo dominio -> DTO y delegación a
 * los casos de uso. La seguridad (401/403) ya está cubierta en
 * {@link MatchingControllerSecurityTest}; aquí se asume siempre un JWT válido.
 */
@WebMvcTest(controllers = MatchingController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class,
        MatchingControllerTest.TestConfig.class})
@TestPropertySource(properties = {
        "security.internal-api-key=test-internal-key",
        "jwt.secret=test-secret-test-secret-test-secret-test-secret",
        "cors.allowed-origins=https://app.patricia.io"
})
class MatchingControllerTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    @Autowired private MockMvc mockMvc;

    @MockBean private ConsultarSugerenciasUseCase consultarSugerenciasUseCase;
    @MockBean private DecidirSobreSugerenciaUseCase decidirSobreSugerenciaUseCase;
    @MockBean private ListarMatchesUseCase listarMatchesUseCase;
    @MockBean private ConsultarSolicitudesRecibidasUseCase consultarSolicitudesRecibidasUseCase;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JwtTokenParser jwtTokenParser() {
            return new JwtTokenParser(SECRET);
        }
    }

    private String tokenPara(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", List.of("STUDENT"))
                .signWith(key)
                .compact();
    }

    @Test
    void sugerencias_mapeaElFeedAlDtoDeRespuesta() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        Sugerencia sugerencia = new Sugerencia(usuarioId, candidatoId,
                ScoreCompatibilidad.de(0.8, 0.6, 1.0), Instant.now());
        when(consultarSugerenciasUseCase.obtenerFeed(eq(usuarioId), eq(20))).thenReturn(List.of(sugerencia));

        mockMvc.perform(get("/matching/sugerencias")
                        .header("Authorization", "Bearer " + tokenPara(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidatoId").value(candidatoId.toString()))
                .andExpect(jsonPath("$[0].scoreTotal").exists());
    }

    @Test
    void sugerencias_respetaElParametroLimite() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(consultarSugerenciasUseCase.obtenerFeed(eq(usuarioId), eq(5))).thenReturn(List.of());

        mockMvc.perform(get("/matching/sugerencias")
                        .param("limite", "5")
                        .header("Authorization", "Bearer " + tokenPara(usuarioId)))
                .andExpect(status().isOk());
    }

    @Test
    void decidir_sinMatchMutuoDevuelveMatchConfirmadoFalse() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        when(decidirSobreSugerenciaUseCase.decidir(eq(usuarioId), eq(candidatoId), eq(DecisionMatching.LIKE)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/matching/decisiones")
                        .header("Authorization", "Bearer " + tokenPara(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidatoId\":\"" + candidatoId + "\",\"decision\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchConfirmado").value(false))
                .andExpect(jsonPath("$.match").doesNotExist());
    }

    @Test
    void decidir_conLikeReciprocoDevuelveElMatchConfirmado() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        Match match = Match.confirmar(usuarioId, candidatoId, ScoreCompatibilidad.de(0.9, 0.8, 1.0));
        when(decidirSobreSugerenciaUseCase.decidir(eq(usuarioId), eq(candidatoId), eq(DecisionMatching.LIKE)))
                .thenReturn(Optional.of(match));

        mockMvc.perform(post("/matching/decisiones")
                        .header("Authorization", "Bearer " + tokenPara(usuarioId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidatoId\":\"" + candidatoId + "\",\"decision\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchConfirmado").value(true))
                .andExpect(jsonPath("$.match.otroUsuarioId").value(candidatoId.toString()));
    }

    @Test
    void decidir_sinCandidatoIdRetorna400() throws Exception {
        mockMvc.perform(post("/matching/decisiones")
                        .header("Authorization", "Bearer " + tokenPara(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"LIKE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void matches_mapeaLosMatchesAlDtoConElOtroUsuario() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID otroId = UUID.randomUUID();
        Match match = Match.confirmar(usuarioId, otroId, ScoreCompatibilidad.de(0.9, 0.8, 1.0));
        when(listarMatchesUseCase.listarPara(usuarioId)).thenReturn(List.of(match));

        mockMvc.perform(get("/matching/matches")
                        .header("Authorization", "Bearer " + tokenPara(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].otroUsuarioId").value(otroId.toString()));
    }

    @Test
    void solicitudesRecibidas_devuelveElConjuntoDeIds() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        UUID admiradorId = UUID.randomUUID();
        when(consultarSolicitudesRecibidasUseCase.obtenerPara(usuarioId)).thenReturn(Set.of(admiradorId));

        mockMvc.perform(get("/matching/solicitudes-recibidas")
                        .header("Authorization", "Bearer " + tokenPara(usuarioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(admiradorId.toString()));
    }

    // ── Rama defensiva de usuarioId(Authentication) ──────────────────────────
    // No debería dispararse nunca en producción (SecurityConfig exige
    // authenticated() y JwtAuthenticationFilter siempre puebla un UUID
    // válido), pero al ser código de seguridad se prueba directo, sin pasar
    // por el filtro real, instanciando el controller a mano.

    @Test
    void usuarioId_conPrincipalQueNoEsUuid_lanzaAccessDenied() {
        MatchingController controller = new MatchingController(
                consultarSugerenciasUseCase, decidirSobreSugerenciaUseCase,
                listarMatchesUseCase, consultarSolicitudesRecibidasUseCase);

        Authentication authConPrincipalInvalido = mock(Authentication.class);
        when(authConPrincipalInvalido.getPrincipal()).thenReturn("no-es-un-uuid");

        assertThrows(AccessDeniedException.class,
                () -> controller.obtenerSugerencias(authConPrincipalInvalido, 20));
    }

    @Test
    void usuarioId_conAuthenticationNula_lanzaAccessDenied() {
        MatchingController controller = new MatchingController(
                consultarSugerenciasUseCase, decidirSobreSugerenciaUseCase,
                listarMatchesUseCase, consultarSolicitudesRecibidasUseCase);

        assertThrows(AccessDeniedException.class,
                () -> controller.obtenerSugerencias(null, 20));
    }
}
