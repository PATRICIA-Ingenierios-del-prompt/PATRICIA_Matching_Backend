package com.escuelaing.matching.domain.model;

/**
 * Espeja el enum {@code Disponibilidad} del microservicio Usuarios
 * (DISPONIBLE / OCUPADO / NO_MOLESTAR). No es un horario semanal: es un
 * estado puntual que el usuario declara en su perfil (RF02.2 / RF04).
 * <p>
 * Usuarios no expone un campo separado de "visibilidad en matching"; por
 * decisión de producto, NO_MOLESTAR se trata como señal de baja prioridad
 * para sugerencias entrantes (ver {@link PerfilMatching#esElegible()}),
 * mientras que DISPONIBLE/OCUPADO sí participan del cálculo normal.
 */
public enum DisponibilidadUsuario {
    DISPONIBLE,
    OCUPADO,
    NO_MOLESTAR
}
