package com.escuelaing.matching.infrastructure.adapter.out.persistence.repository;

import com.escuelaing.matching.infrastructure.adapter.out.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchJpaRepository extends JpaRepository<MatchEntity, UUID> {

    Optional<MatchEntity> findByUsuarioMenorIdAndUsuarioMayorId(UUID usuarioMenorId, UUID usuarioMayorId);

    boolean existsByUsuarioMenorIdAndUsuarioMayorId(UUID usuarioMenorId, UUID usuarioMayorId);

    @Query("""
            SELECT m FROM MatchEntity m
            WHERE m.usuarioMenorId = :usuarioId OR m.usuarioMayorId = :usuarioId
            ORDER BY m.confirmadoEn DESC
            """)
    List<MatchEntity> findByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
