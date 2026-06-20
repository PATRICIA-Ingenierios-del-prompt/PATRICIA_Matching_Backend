package com.escuelaing.matching.infrastructure.adapter.out.client;

import com.escuelaing.matching.infrastructure.adapter.out.client.dto.ParchesEnComunResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Cliente Feign hacia un endpoint interno de Parches Core que HOY NO EXISTE.
 * <p>
 * El repo actual de Parches Core (paquete {@code ingprompt.patricia.parches},
 * rama feature-profilePhotoForParche) no tiene ninguna ruta {@code /internal/**}
 * ni protección por API key: todo vive bajo {@code /api/parches/**}. Tampoco
 * existe una consulta de membresías por usuario — {@code ParcheRepositoryOutPort}
 * solo permite {@code findById(parcheId)}; los miembros viven como
 * {@code Set<UUID>} embebido dentro del agregado {@code Parche}, sin índice
 * por miembro. Para soportar esta llamada, Parches Core necesitaría agregar
 * como mínimo: una query "parches por miembro" + un endpoint interno con
 * autenticación de servicio (ver TODO_INTEGRACIONES.md).
 * <p>
 * Mientras tanto, {@link com.escuelaing.matching.domain.port.out.ParcheMembresiaPort}
 * se resuelve con {@code MockParcheMembresiaAdapter} (perfil
 * {@code mock-parches}), y el bonus de parches en común en el algoritmo de
 * matching está desactivado por defecto ({@code matching.parches-comun.habilitado=false}).
 */
@FeignClient(name = "parches-core-service", url = "${parches-core-service.url}")
public interface ParcheCoreFeignClient {

    @GetMapping("/internal/parches/en-comun")
    ParchesEnComunResponse buscarParchesEnComun(
            @RequestParam("usuarioA") UUID usuarioA,
            @RequestParam("usuarioB") UUID usuarioB,
            @RequestHeader("X-Internal-Api-Key") String apiKey
    );
}
