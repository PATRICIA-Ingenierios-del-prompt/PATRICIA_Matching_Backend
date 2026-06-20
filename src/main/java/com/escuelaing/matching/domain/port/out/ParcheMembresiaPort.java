package com.escuelaing.matching.domain.port.out;

import java.util.Set;
import java.util.UUID;

/**
 * Puerto de salida: consulta a Parches Core (fuente de verdad de grupos
 * y membresías) para enriquecer el cálculo de afinidad con la cantidad
 * de parches en común entre dos usuarios.
 */
public interface ParcheMembresiaPort {

    /** IDs de parches a los que pertenecen ambos usuarios. */
    Set<UUID> parchesEnComun(UUID usuarioId, UUID candidatoId);
}
