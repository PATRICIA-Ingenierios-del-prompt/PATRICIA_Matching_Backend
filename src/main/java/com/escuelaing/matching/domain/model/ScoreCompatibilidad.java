package com.escuelaing.matching.domain.model;

/**
 * Desglose ponderado del score de compatibilidad entre dos usuarios.
 * <p>
 * Pesos definidos por RF05 - Algoritmo de Matching:
 * <ul>
 *   <li>Intereses en común: 40%</li>
 *   <li>Afinidad académica (programa / semestre): 30%</li>
 *   <li>Disponibilidad horaria en común: 30%</li>
 * </ul>
 * La ubicación física (RF05.2) queda explícitamente fuera de este cálculo:
 * es responsabilidad de Eventos / un futuro servicio de geolocalización.
 */
public record ScoreCompatibilidad(
        double intereses,
        double academico,
        double disponibilidad
) {

    public static final double PESO_INTERESES = 0.40;
    public static final double PESO_ACADEMICO = 0.30;
    public static final double PESO_DISPONIBILIDAD = 0.30;

    /** Umbral por defecto a partir del cual una sugerencia se considera "alta afinidad" (RF05.4). */
    public static final double UMBRAL_AFINIDAD_ALTA = 0.90;

    public ScoreCompatibilidad {
        intereses = clamp(intereses);
        academico = clamp(academico);
        disponibilidad = clamp(disponibilidad);
    }

    public static ScoreCompatibilidad de(double intereses, double academico, double disponibilidad) {
        return new ScoreCompatibilidad(intereses, academico, disponibilidad);
    }

    /** Score final ponderado, en el rango [0, 1]. */
    public double total() {
        return (intereses * PESO_INTERESES)
                + (academico * PESO_ACADEMICO)
                + (disponibilidad * PESO_DISPONIBILIDAD);
    }

    public boolean esAfinidadAlta() {
        return total() >= UMBRAL_AFINIDAD_ALTA;
    }

    private static double clamp(double valor) {
        if (Double.isNaN(valor)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, valor));
    }
}
