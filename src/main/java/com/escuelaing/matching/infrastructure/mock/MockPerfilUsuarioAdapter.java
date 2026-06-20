package com.escuelaing.matching.infrastructure.mock;

import com.escuelaing.matching.domain.model.DisponibilidadUsuario;
import com.escuelaing.matching.domain.model.EstadoUsuario;
import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.domain.port.out.PerfilUsuarioPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Mock temporal de Usuarios, activo solo con el perfil Spring
 * {@code mock-usuarios}. Necesario porque usuarios-service (rama
 * feature-inicial) todavía no expone un endpoint interno que combine
 * Usuario + Perfil sin exigir JWT (ver TODO_INTEGRACIONES.md). Las
 * etiquetas de interés usadas aquí son un subconjunto real del catálogo
 * cerrado {@code Interes} de usuarios-service, para que el mock sea
 * representativo del dato real una vez exista el endpoint.
 */
@Component
@Profile("mock-usuarios")
public class MockPerfilUsuarioAdapter implements PerfilUsuarioPort {

    private static final List<String> CATALOGO_INTERESES = List.of(
            "Conciertos en vivo", "Grupos de estudio", "Básquetbol", "Gimnasio",
            "Comer en campus", "Videojuegos competitivos", "Desarrollo web/app",
            "Cine & Películas", "Fotografía", "Hackathones de código",
            "Concursos académicos", "Voluntariado", "Road trips", "Meditación"
    );

    private static final List<String> CARRERAS = List.of(
            "Ingenieria de Sistemas", "Ingenieria Industrial", "Ingenieria Electronica"
    );

    private final Random random = new Random();

    @Override
    public Optional<PerfilMatching> buscarPorId(UUID usuarioId) {
        return Optional.of(perfilAleatorio(usuarioId));
    }

    @Override
    public List<PerfilMatching> buscarCandidatos(UUID usuarioId, int maxCandidatos) {
        return IntStream.range(0, Math.min(maxCandidatos, 30))
                .mapToObj(i -> perfilAleatorio(UUID.randomUUID()))
                .collect(Collectors.toList());
    }

    private PerfilMatching perfilAleatorio(UUID usuarioId) {
        Set<String> intereses = IntStream.range(0, 3 + random.nextInt(4))
                .mapToObj(i -> CATALOGO_INTERESES.get(random.nextInt(CATALOGO_INTERESES.size())))
                .collect(Collectors.toSet());

        DisponibilidadUsuario disponibilidad = DisponibilidadUsuario.values()[random.nextInt(3)];

        return new PerfilMatching(
                usuarioId,
                intereses,
                CARRERAS.get(random.nextInt(CARRERAS.size())),
                1 + random.nextInt(10),
                disponibilidad,
                EstadoUsuario.ACTIVE
        );
    }
}
