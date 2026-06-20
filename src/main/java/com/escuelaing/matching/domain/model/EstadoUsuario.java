package com.escuelaing.matching.domain.model;

/**
 * Espeja el enum {@code EstadoUsuario} del microservicio Usuarios.
 * Solo los usuarios ACTIVE son elegibles para participar en matching;
 * SUSPENDED y BANNED quedan fuera del cálculo de sugerencias.
 */
public enum EstadoUsuario {
    ACTIVE,
    SUSPENDED,
    BANNED
}
