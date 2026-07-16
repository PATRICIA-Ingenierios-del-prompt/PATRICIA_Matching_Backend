package com.escuelaing.matching.domain.port.out;

import java.util.UUID;

/**
 * Puerto de salida: registra que un usuario ya tomó una decisión (LIKE o
 * DESCARTE) sobre un candidato, sin importar si terminó en match. Se usa
 * para excluir a ese candidato de futuros recálculos de la cola de
 * sugerencias — a diferencia de {@link DecisionPendientePort}, que solo
 * rastrea LIKEs pendientes de reciprocidad y se limpia al confirmarse el
 * match, este registro existe para no volver a mostrar a alguien sobre
 * quien el usuario ya se pronunció.
 */
public interface DecisionesTomadasPort {

    void registrarDecision(UUID usuarioId, UUID candidatoId);

    /** ¿{@code usuarioId} ya decidió (LIKE o DESCARTE) sobre {@code candidatoId}? */
    boolean yaDecidioSobre(UUID usuarioId, UUID candidatoId);
}
