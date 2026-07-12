package com.escuelaing.matching.domain.model;

/**
 * Decisión que toma un usuario sobre una {@link Sugerencia}.
 * Un match se confirma cuando ambos usuarios involucrados emiten LIKE
 * sobre el otro (like mutuo).
 */
public enum DecisionMatching {
    LIKE,
    DESCARTE
}
