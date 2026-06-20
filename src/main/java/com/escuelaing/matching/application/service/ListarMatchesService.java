package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.model.Match;
import com.escuelaing.matching.domain.port.in.ListarMatchesUseCase;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListarMatchesService implements ListarMatchesUseCase {

    private final MatchRepositoryPort matchRepositoryPort;

    @Override
    public List<Match> listarPara(UUID usuarioId) {
        return matchRepositoryPort.buscarPorUsuario(usuarioId);
    }
}
