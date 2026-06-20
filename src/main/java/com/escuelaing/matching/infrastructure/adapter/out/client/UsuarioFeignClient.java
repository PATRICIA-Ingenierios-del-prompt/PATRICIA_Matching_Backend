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
 * Cliente Feign hacia endpoints internos de Usuarios protegidos por
 * {@code X-Internal-Api-Key} (mismo mecanismo que {@code /internal/usuarios/**}
 * en usuarios-service: ver {@code InternalApiKeyFilter}).
 * <p>
 * <b>IMPORTANTE — contrato propuesto, aún no implementado en Usuarios:</b>
 * el repo actual de usuarios-service (rama feature-inicial) solo expone
 * {@code /internal/usuarios/find-or-create}, {@code /internal/usuarios/{id}},
 * {@code /internal/usuarios/buscar} y {@code /internal/usuarios/{id}/estado}
 * (que devuelven solo datos de {@code Usuario}: email, nombre, roles, estado).
 * Los datos de {@code Perfil} (carrera, semestre, intereses, disponibilidad)
 * solo se exponen hoy en {@code /api/v1/usuarios/{id}/perfil}, protegido con
 * JWT del propio usuario — inutilizable para una llamada servicio-a-servicio.
 * <p>
 * Mientras Usuarios no agregue {@code /internal/usuarios/{id}/perfil-matching}
 * y {@code /internal/usuarios/candidatos-matching} (composición interna de
 * Usuario + Perfil, sin exigir JWT), Matching corre con
 * {@code MockPerfilUsuarioAdapter} (perfil {@code mock-usuarios}). Ver
 * TODO_INTEGRACIONES.md para el detalle de lo que falta agregar en Usuarios.
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
