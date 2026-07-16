package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.exception.UsuarioNoElegibleException;
import com.escuelaing.matching.domain.model.DisponibilidadUsuario;
import com.escuelaing.matching.domain.model.EstadoUsuario;
import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.escuelaing.matching.domain.port.out.DecisionesTomadasPort;
import com.escuelaing.matching.domain.port.out.MatchRepositoryPort;
import com.escuelaing.matching.domain.port.out.ParcheMembresiaPort;
import com.escuelaing.matching.domain.port.out.PerfilUsuarioPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CalcularSugerenciasServiceTest {

    private final PerfilUsuarioPort perfilUsuarioPort = mock(PerfilUsuarioPort.class);
    private final ParcheMembresiaPort parcheMembresiaPort = mock(ParcheMembresiaPort.class);
    private final ColaSugerenciasPort colaSugerenciasPort = mock(ColaSugerenciasPort.class);
    private final DecisionesTomadasPort decisionesTomadasPort = mock(DecisionesTomadasPort.class);
    private final MatchRepositoryPort matchRepositoryPort = mock(MatchRepositoryPort.class);

    private CalcularSugerenciasService service;

    @BeforeEach
    void setUp() {
        service = new CalcularSugerenciasService(perfilUsuarioPort, parcheMembresiaPort, colaSugerenciasPort,
                decisionesTomadasPort, matchRepositoryPort);
        // @Value no se inyecta fuera de un contexto Spring — se fija a mano
        // con los mismos defaults que declara application.yml.
        ReflectionTestUtils.setField(service, "maxPoolCandidatos", 200);
        ReflectionTestUtils.setField(service, "tamanoCola", 50);
        ReflectionTestUtils.setField(service, "bonusParchesComunHabilitado", false);
        ReflectionTestUtils.setField(service, "bonusPorParcheComun", 0.05);
    }

    @Test
    void lanzaExcepcionSiUsuarioNoExisteEnUsuarios() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoElegibleException.class, () -> service.recalcularPara(usuarioId));
        verifyNoInteractions(colaSugerenciasPort);
    }

    @Test
    void usuarioNoElegibleVaciaLaColaSinBuscarCandidatos() {
        UUID usuarioId = UUID.randomUUID();
        PerfilMatching noElegible = perfil(usuarioId, EstadoUsuario.SUSPENDED, Set.of("Musica"));
        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(noElegible));

        service.recalcularPara(usuarioId);

        verify(colaSugerenciasPort).reemplazarCola(usuarioId, List.of());
        verify(perfilUsuarioPort, never()).buscarCandidatos(any(), anyInt());
    }

    @Test
    void usuarioSinInteresesTampocoEsElegible() {
        UUID usuarioId = UUID.randomUUID();
        PerfilMatching sinIntereses = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of());
        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(sinIntereses));

        service.recalcularPara(usuarioId);

        verify(colaSugerenciasPort).reemplazarCola(usuarioId, List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculaYOrdenaSugerenciasPorScoreDescendente() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoBajoId = UUID.randomUUID();
        UUID candidatoAltoId = UUID.randomUUID();
        UUID noElegibleId = UUID.randomUUID();

        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica", "Cine"));
        PerfilMatching candidatoBajo = perfil(candidatoBajoId, EstadoUsuario.ACTIVE, Set.of("Deportes"));
        PerfilMatching candidatoAlto = perfil(candidatoAltoId, EstadoUsuario.ACTIVE, Set.of("Musica", "Cine"));
        PerfilMatching noElegible = perfil(noElegibleId, EstadoUsuario.SUSPENDED, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200))
                .thenReturn(List.of(candidatoBajo, candidatoAlto, noElegible));

        service.recalcularPara(usuarioId);

        ArgumentCaptor<List<Sugerencia>> captor = ArgumentCaptor.forClass(List.class);
        verify(colaSugerenciasPort).reemplazarCola(eq(usuarioId), captor.capture());
        List<Sugerencia> sugerencias = captor.getValue();

        assertEquals(2, sugerencias.size(), "el no elegible debe quedar filtrado");
        assertEquals(candidatoAltoId, sugerencias.get(0).candidatoId(), "mayor afinidad de intereses primero");
        assertTrue(sugerencias.get(0).score().total() >= sugerencias.get(1).score().total());
    }

    @Test
    @SuppressWarnings("unchecked")
    void excluyeCandidatoSobreElQueYaSeDecidio() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoDecididoId = UUID.randomUUID();
        UUID candidatoNuevoId = UUID.randomUUID();

        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching candidatoDecidido = perfil(candidatoDecididoId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching candidatoNuevo = perfil(candidatoNuevoId, EstadoUsuario.ACTIVE, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200))
                .thenReturn(List.of(candidatoDecidido, candidatoNuevo));
        when(decisionesTomadasPort.yaDecidioSobre(usuarioId, candidatoDecididoId)).thenReturn(true);

        service.recalcularPara(usuarioId);

        ArgumentCaptor<List<Sugerencia>> captor = ArgumentCaptor.forClass(List.class);
        verify(colaSugerenciasPort).reemplazarCola(eq(usuarioId), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(candidatoNuevoId, captor.getValue().get(0).candidatoId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void excluyeCandidatoConElQueYaHayMatchConfirmado() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoMatcheadoId = UUID.randomUUID();
        UUID candidatoNuevoId = UUID.randomUUID();

        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching candidatoMatcheado = perfil(candidatoMatcheadoId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching candidatoNuevo = perfil(candidatoNuevoId, EstadoUsuario.ACTIVE, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200))
                .thenReturn(List.of(candidatoMatcheado, candidatoNuevo));
        when(matchRepositoryPort.existeEntre(usuarioId, candidatoMatcheadoId)).thenReturn(true);

        service.recalcularPara(usuarioId);

        ArgumentCaptor<List<Sugerencia>> captor = ArgumentCaptor.forClass(List.class);
        verify(colaSugerenciasPort).reemplazarCola(eq(usuarioId), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(candidatoNuevoId, captor.getValue().get(0).candidatoId());
    }

    @Test
    void excluyeAlPropioUsuarioDeLosCandidatos() {
        UUID usuarioId = UUID.randomUUID();
        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching elMismo = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200)).thenReturn(List.of(elMismo));

        service.recalcularPara(usuarioId);

        verify(colaSugerenciasPort).reemplazarCola(usuarioId, List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void respetaElLimiteDeTamanoDeCola() {
        ReflectionTestUtils.setField(service, "tamanoCola", 1);
        UUID usuarioId = UUID.randomUUID();
        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching c1 = perfil(UUID.randomUUID(), EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching c2 = perfil(UUID.randomUUID(), EstadoUsuario.ACTIVE, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200)).thenReturn(List.of(c1, c2));

        service.recalcularPara(usuarioId);

        ArgumentCaptor<List<Sugerencia>> captor = ArgumentCaptor.forClass(List.class);
        verify(colaSugerenciasPort).reemplazarCola(eq(usuarioId), captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aplicaBonusDeParchesEnComunCuandoEstaHabilitado() {
        ReflectionTestUtils.setField(service, "bonusParchesComunHabilitado", true);

        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching candidato = perfil(candidatoId, EstadoUsuario.ACTIVE, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200)).thenReturn(List.of(candidato));
        when(parcheMembresiaPort.parchesEnComun(usuarioId, candidatoId))
                .thenReturn(Set.of(UUID.randomUUID(), UUID.randomUUID()));

        service.recalcularPara(usuarioId);

        verify(parcheMembresiaPort).parchesEnComun(usuarioId, candidatoId);
        ArgumentCaptor<List<Sugerencia>> captor = ArgumentCaptor.forClass(List.class);
        verify(colaSugerenciasPort).reemplazarCola(eq(usuarioId), captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    void noConsultaParchesEnComunCuandoElBonusEstaDeshabilitado() {
        UUID usuarioId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        PerfilMatching usuario = perfil(usuarioId, EstadoUsuario.ACTIVE, Set.of("Musica"));
        PerfilMatching candidato = perfil(candidatoId, EstadoUsuario.ACTIVE, Set.of("Musica"));

        when(perfilUsuarioPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(perfilUsuarioPort.buscarCandidatos(usuarioId, 200)).thenReturn(List.of(candidato));

        service.recalcularPara(usuarioId);

        verifyNoInteractions(parcheMembresiaPort);
    }

    private PerfilMatching perfil(UUID id, EstadoUsuario estado, Set<String> intereses) {
        return new PerfilMatching(id, intereses, "Ingeniería de Sistemas", 5,
                DisponibilidadUsuario.DISPONIBLE, estado);
    }
}
