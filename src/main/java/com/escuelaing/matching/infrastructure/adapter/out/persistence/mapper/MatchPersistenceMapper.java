package com.escuelaing.matching.infrastructure.adapter.out.persistence.mapper;

import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.infrastructure.adapter.out.persistence.entity.MatchEntity;
import org.springframework.stereotype.Component;

@Component
public class MatchPersistenceMapper {

    public MatchEntity aEntidad(Match match) {
        return new MatchEntity(
                match.id(),
                match.usuarioMenorId(),
                match.usuarioMayorId(),
                match.score().intereses(),
                match.score().academico(),
                match.score().disponibilidad(),
                match.confirmadoEn()
        );
    }

    public Match aDominio(MatchEntity entity) {
        ScoreCompatibilidad score = ScoreCompatibilidad.de(
                entity.getScoreIntereses(),
                entity.getScoreAcademico(),
                entity.getScoreDisponibilidad()
        );
        return Match.reconstruir(
                entity.getId(),
                entity.getUsuarioMenorId(),
                entity.getUsuarioMayorId(),
                score,
                entity.getConfirmadoEn()
        );
    }
}
