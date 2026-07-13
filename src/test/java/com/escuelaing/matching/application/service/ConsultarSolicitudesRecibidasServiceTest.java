package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.port.out.SolicitudesRecibidasPort;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarSolicitudesRecibidasServiceTest {

    private final SolicitudesRecibidasPort solicitudesRecibidasPort = mock(SolicitudesRecibidasPort.class);
    private final ConsultarSolicitudesRecibidasService service =
            new ConsultarSolicitudesRecibidasService(solicitudesRecibidasPort);

    @Test
    void delegaDirectamenteAlPuertoDeSalida() {
        UUID usuarioId = UUID.randomUUID();
        Set<UUID> admiradores = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(solicitudesRecibidasPort.buscarAdmiradoresDe(usuarioId)).thenReturn(admiradores);

        Set<UUID> resultado = service.obtenerPara(usuarioId);

        assertEquals(admiradores, resultado);
    }

    @Test
    void devuelveConjuntoVacioSiNadieLeHaDadoLike() {
        UUID usuarioId = UUID.randomUUID();
        when(solicitudesRecibidasPort.buscarAdmiradoresDe(usuarioId)).thenReturn(Set.of());

        assertEquals(Set.of(), service.obtenerPara(usuarioId));
    }
}
