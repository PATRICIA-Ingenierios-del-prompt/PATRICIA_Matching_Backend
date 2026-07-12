package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.port.in.ConsultarSolicitudesRecibidasUseCase;
import com.escuelaing.matching.domain.port.out.SolicitudesRecibidasPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarSolicitudesRecibidasService implements ConsultarSolicitudesRecibidasUseCase {

    private final SolicitudesRecibidasPort solicitudesRecibidasPort;

    @Override
    public Set<UUID> obtenerPara(UUID usuarioId) {
        return solicitudesRecibidasPort.buscarAdmiradoresDe(usuarioId);
    }
}
