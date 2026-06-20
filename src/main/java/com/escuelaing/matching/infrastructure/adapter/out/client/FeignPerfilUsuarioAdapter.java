package com.escuelaing.matching.infrastructure.adapter.out.client;

import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.domain.port.out.PerfilUsuarioPort;
import com.escuelaing.matching.infrastructure.adapter.out.client.dto.UsuarioPerfilMatchingResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida: implementa {@link PerfilUsuarioPort} consultando
 * sincrónicamente al microservicio Usuarios vía Feign.
 */
@Slf4j
@Component
@Profile("!mock-usuarios")
@RequiredArgsConstructor
public class FeignPerfilUsuarioAdapter implements PerfilUsuarioPort {

    private final UsuarioFeignClient usuarioFeignClient;
    private final UsuarioClientMapper mapper;

    @Value("${security.internal-api-key}")
    private String internalApiKey;

    @Override
    public Optional<PerfilMatching> buscarPorId(UUID usuarioId) {
        try {
            UsuarioPerfilMatchingResponse response = usuarioFeignClient.buscarPerfilMatching(usuarioId, internalApiKey);
            return Optional.ofNullable(mapper.aDominio(response));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        } catch (FeignException ex) {
            log.error("Error consultando perfil de matching para usuarioId={}: {}", usuarioId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<PerfilMatching> buscarCandidatos(UUID usuarioId, int maxCandidatos) {
        try {
            List<UsuarioPerfilMatchingResponse> respuestas =
                    usuarioFeignClient.buscarCandidatos(usuarioId, maxCandidatos, internalApiKey);
            return respuestas.stream()
                    .map(mapper::aDominio)
                    .toList();
        } catch (FeignException ex) {
            log.error("Error consultando candidatos de matching para usuarioId={}: {}", usuarioId, ex.getMessage());
            return List.of();
        }
    }
}
