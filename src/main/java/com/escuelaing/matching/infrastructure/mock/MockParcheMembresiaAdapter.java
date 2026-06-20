package com.escuelaing.matching.infrastructure.mock;

import com.escuelaing.matching.domain.port.out.ParcheMembresiaPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Mock temporal de Parches Core.
 * <p>
 * Activo solo con el perfil Spring {@code mock-parches}. Reemplazar por
 * {@link com.escuelaing.matching.infrastructure.adapter.out.client.FeignParcheMembresiaAdapter}
 * (ya implementado) cuando Parches Core esté disponible: basta con no
 * activar este perfil.
 */
@Component
@Profile("mock-parches")
public class MockParcheMembresiaAdapter implements ParcheMembresiaPort {

    @Override
    public Set<UUID> parchesEnComun(UUID usuarioId, UUID candidatoId) {
        return Set.of();
    }
}
