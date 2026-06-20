package com.escuelaing.matching.infrastructure.adapter.out.client;

import com.escuelaing.matching.domain.port.out.ParcheMembresiaPort;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Adaptador de salida: implementa {@link ParcheMembresiaPort} consultando
 * sincrónicamente a Parches Core vía Feign. Si el servicio falla, se
 * degrada a "sin parches en común" en vez de tumbar el cálculo de
 * sugerencias: el factor de parches en común es un bonus, no un
 * requisito del algoritmo.
 */
@Slf4j
@Component
@Profile("!mock-parches")
@RequiredArgsConstructor
public class FeignParcheMembresiaAdapter implements ParcheMembresiaPort {

    private final ParcheCoreFeignClient parcheCoreFeignClient;

    @Value("${security.internal-api-key}")
    private String internalApiKey;

    @Override
    public Set<UUID> parchesEnComun(UUID usuarioId, UUID candidatoId) {
        try {
            var response = parcheCoreFeignClient.buscarParchesEnComun(usuarioId, candidatoId, internalApiKey);
            return response.parcheIds() == null ? Set.of() : Set.copyOf(response.parcheIds());
        } catch (FeignException ex) {
            log.warn("Error consultando parches en común entre {} y {}: {}", usuarioId, candidatoId, ex.getMessage());
            return Set.of();
        }
    }
}
