package com.escuelaing.matching.infrastructure.adapter.in.web;

import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import com.escuelaing.matching.domain.port.in.ConsultarSolicitudesRecibidasUseCase;
import com.escuelaing.matching.domain.port.in.ConsultarSugerenciasUseCase;
import com.escuelaing.matching.domain.port.in.DecidirSobreSugerenciaUseCase;
import com.escuelaing.matching.domain.port.in.ListarMatchesUseCase;
import com.escuelaing.matching.infrastructure.config.CorsConfig;
import com.escuelaing.matching.infrastructure.config.SecurityConfig;
import com.escuelaing.matching.infrastructure.exception.GlobalExceptionHandler;
import com.escuelaing.matching.infrastructure.security.InternalApiKeyFilter;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de regresión: fija en un test el comportamiento de seguridad que
 * antes estaba roto en producción (endpoints /matching/** abiertos sin JWT,
 * fallback a un UUID hardcodeado). Si alguien vuelve a poner permitAll en
 * /matching/** o comenta el JwtAuthenticationFilter, estos tests fallan.
 */
@WebMvcTest(controllers = {MatchingController.class, InternalMatchingController.class})
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class,
        MatchingControllerSecurityTest.TestConfig.class})
@TestPropertySource(properties = {
        "security.internal-api-key=test-internal-key",
        "jwt.secret=test-secret-test-secret-test-secret-test-secret",
        "cors.allowed-origins=https://app.patricia.io"
})
class MatchingControllerSecurityTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    @Autowired private MockMvc mockMvc;

    @MockBean private ConsultarSugerenciasUseCase consultarSugerenciasUseCase;
    @MockBean private DecidirSobreSugerenciaUseCase decidirSobreSugerenciaUseCase;
    @MockBean private ListarMatchesUseCase listarMatchesUseCase;
    @MockBean private ConsultarSolicitudesRecibidasUseCase consultarSolicitudesRecibidasUseCase;
    @MockBean private CalcularSugerenciasUseCase calcularSugerenciasUseCase;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JwtTokenParser jwtTokenParser() {
            return new JwtTokenParser(SECRET);
        }
    }

    private String validToken(UUID userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", List.of("STUDENT"))
                .signWith(key)
                .compact();
    }

    // ── /matching/** exige JWT ───────────────────────────────────────────────

    @Test
    void sugerencias_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/matching/sugerencias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sugerencias_conTokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get("/matching/sugerencias")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sugerencias_conTokenValido_retorna200() throws Exception {
        mockMvc.perform(get("/matching/sugerencias")
                        .header("Authorization", "Bearer " + validToken(UUID.randomUUID())))
                .andExpect(status().isOk());
    }

    @Test
    void matches_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/matching/matches"))
                .andExpect(status().isUnauthorized());
    }

    // ── /internal/matching/** exige X-Internal-Api-Key ──────────────────────

    @Test
    void recalcular_sinApiKey_retorna401() throws Exception {
        mockMvc.perform(post("/internal/matching/recalcular/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recalcular_conApiKeyInvalida_retorna401() throws Exception {
        mockMvc.perform(post("/internal/matching/recalcular/{id}", UUID.randomUUID())
                        .header(InternalApiKeyFilter.HEADER, "clave-incorrecta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recalcular_conApiKeyValida_retorna202() throws Exception {
        mockMvc.perform(post("/internal/matching/recalcular/{id}", UUID.randomUUID())
                        .header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isAccepted());
    }
}
