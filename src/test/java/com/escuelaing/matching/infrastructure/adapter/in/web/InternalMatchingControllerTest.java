package com.escuelaing.matching.infrastructure.adapter.in.web;

import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import com.escuelaing.matching.infrastructure.config.CorsConfig;
import com.escuelaing.matching.infrastructure.config.SecurityConfig;
import com.escuelaing.matching.infrastructure.exception.GlobalExceptionHandler;
import com.escuelaing.matching.infrastructure.security.InternalApiKeyFilter;
import com.escuelaing.matching.infrastructure.security.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comportamiento de InternalMatchingController: delega correctamente al caso
 * de uso con el usuarioId de la ruta. La seguridad (401 sin/​con API key
 * inválida) ya está cubierta en {@link MatchingControllerSecurityTest}.
 */
@WebMvcTest(controllers = InternalMatchingController.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class,
        InternalMatchingControllerTest.TestConfig.class})
@TestPropertySource(properties = {
        "security.internal-api-key=test-internal-key",
        "jwt.secret=test-secret-test-secret-test-secret-test-secret",
        "cors.allowed-origins=https://app.patricia.io"
})
class InternalMatchingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CalcularSugerenciasUseCase calcularSugerenciasUseCase;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JwtTokenParser jwtTokenParser() {
            return new JwtTokenParser("test-secret-test-secret-test-secret-test-secret");
        }
    }

    @Test
    void recalcular_delegaAlCasoDeUsoConElUsuarioIdDeLaRuta() throws Exception {
        UUID usuarioId = UUID.randomUUID();

        mockMvc.perform(post("/internal/matching/recalcular/{id}", usuarioId)
                        .header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isAccepted());

        verify(calcularSugerenciasUseCase).recalcularPara(usuarioId);
    }
}
