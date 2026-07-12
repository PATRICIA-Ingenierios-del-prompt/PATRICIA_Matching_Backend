package com.escuelaing.matching.infrastructure.adapter.out.client;

import com.escuelaing.matching.infrastructure.adapter.out.client.dto.UsuarioPerfilMatchingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Cliente Feign hacia los endpoints internos de Usuarios protegidos por
 * {@code X-Internal-Api-Key}. Contratos implementados en
 * {@code InternalUsuarioController} (rama develop de usuarios-service):
 * <ul>
 *   <li>{@code GET /internal/usuarios/{id}/perfil-matching}</li>
 *   <li>{@code GET /internal/usuarios/candidatos-matching}</li>
 * </ul>
 */
@FeignClient(name = "usuario-service", url = "${usuario-service.url}")
public interface UsuarioFeignClient {

    @GetMapping("/internal/usuarios/{id}/perfil-matching")
    UsuarioPerfilMatchingResponse buscarPerfilMatching(
            @PathVariable("id") UUID usuarioId,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );

    @GetMapping("/internal/usuarios/candidatos-matching")
    List<UsuarioPerfilMatchingResponse> buscarCandidatos(
            @RequestParam("excluirUsuarioId") UUID excluirUsuarioId,
            @RequestParam("limite") int limite,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}
