package com.escuelaing.matching.infrastructure.adapter.out.persistence;

import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import com.escuelaing.matching.infrastructure.adapter.out.persistence.entity.MatchEntity;
import com.escuelaing.matching.infrastructure.adapter.out.persistence.mapper.MatchPersistenceMapper;
import com.escuelaing.matching.infrastructure.adapter.out.persistence.repository.MatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaMatchRepositoryAdapter implements MatchRepositoryPort {

    private final MatchJpaRepository jpaRepository;
    private final MatchPersistenceMapper mapper;

    @Override
    public Match guardar(Match match) {
        MatchEntity guardada = jpaRepository.save(mapper.aEntidad(match));
        return mapper.aDominio(guardada);
    }

    @Override
    public Optional<Match> buscarEntre(UUID usuarioA, UUID usuarioB) {
        UUID menor = menor(usuarioA, usuarioB);
        UUID mayor = mayor(usuarioA, usuarioB);
        return jpaRepository.findByUsuarioMenorIdAndUsuarioMayorId(menor, mayor)
                .map(mapper::aDominio);
    }

    @Override
    public List<Match> buscarPorUsuario(UUID usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::aDominio)
                .toList();
    }

    @Override
    public boolean existeEntre(UUID usuarioA, UUID usuarioB) {
        return jpaRepository.existsByUsuarioMenorIdAndUsuarioMayorId(menor(usuarioA, usuarioB), mayor(usuarioA, usuarioB));
    }

    private UUID menor(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private UUID mayor(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }
}
