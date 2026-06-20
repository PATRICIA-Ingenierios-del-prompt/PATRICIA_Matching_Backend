package com.escuelaing.matching.domain.model;

import java.util.Set;

/**
 * Servicio de dominio puro (sin dependencias de infraestructura) que
 * calcula el {@link ScoreCompatibilidad} entre dos perfiles, según las
 * reglas de RF05:
 * <ul>
 *   <li>40% intereses en común (índice de Jaccard sobre el catálogo cerrado
 *       de {@code Interes} que mantiene Usuarios)</li>
 *   <li>30% afinidad académica (misma carrera / cercanía de semestre)</li>
 *   <li>30% disponibilidad: Usuarios no modela franjas horarias, solo un
 *       estado puntual (DISPONIBLE/OCUPADO/NO_MOLESTAR). Por eso este
 *       factor es binario: 1.0 si ambos están DISPONIBLE en el momento del
 *       cálculo, 0.0 en cualquier otro caso. Es una aproximación más simple
 *       que la disponibilidad horaria de RF05.3 — si Usuarios llega a
 *       modelar franjas semanales en el futuro, este factor se puede
 *       reemplazar sin tocar los otros dos ni la ponderación.
 * </ul>
 */
public final class CalculadoraCompatibilidad {

    private CalculadoraCompatibilidad() {
    }

    public static ScoreCompatibilidad calcular(PerfilMatching usuario, PerfilMatching candidato) {
        double scoreIntereses = scoreIntereses(usuario.intereses(), candidato.intereses());
        double scoreAcademico = scoreAcademico(usuario, candidato);
        double scoreDisponibilidad = scoreDisponibilidad(usuario.disponibilidad(), candidato.disponibilidad());

        return ScoreCompatibilidad.de(scoreIntereses, scoreAcademico, scoreDisponibilidad);
    }

    /** Índice de Jaccard entre los conjuntos de intereses: |intersección| / |unión|. */
    private static double scoreIntereses(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        long interseccion = a.stream().filter(b::contains).count();
        long union = a.size() + b.size() - interseccion;
        if (union == 0) {
            return 0.0;
        }
        return (double) interseccion / (double) union;
    }

    /** Misma carrera pesa más; cercanía de semestre suma un componente menor. */
    private static double scoreAcademico(PerfilMatching usuario, PerfilMatching candidato) {
        double scoreCarrera = mismaCarrera(usuario.carrera(), candidato.carrera()) ? 0.7 : 0.0;
        double scoreSemestre = cercaniaSemestre(usuario.semestre(), candidato.semestre());
        return scoreCarrera + (0.3 * scoreSemestre);
    }

    private static boolean mismaCarrera(String carreraA, String carreraB) {
        return carreraA != null && carreraA.equalsIgnoreCase(carreraB);
    }

    private static double cercaniaSemestre(Integer semestreA, Integer semestreB) {
        if (semestreA == null || semestreB == null) {
            return 0.0;
        }
        int diferencia = Math.abs(semestreA - semestreB);
        if (diferencia == 0) {
            return 1.0;
        }
        // decae linealmente hasta 0 a partir de 5 semestres de diferencia
        return Math.max(0.0, 1.0 - (diferencia / 5.0));
    }

    /**
     * Factor binario: ambos DISPONIBLE -> 1.0, cualquier otra combinación -> 0.0.
     * Ver nota de clase sobre la limitación frente a RF05.3.
     */
    private static double scoreDisponibilidad(DisponibilidadUsuario a, DisponibilidadUsuario b) {
        if (a == DisponibilidadUsuario.DISPONIBLE && b == DisponibilidadUsuario.DISPONIBLE) {
            return 1.0;
        }
        return 0.0;
    }
}
