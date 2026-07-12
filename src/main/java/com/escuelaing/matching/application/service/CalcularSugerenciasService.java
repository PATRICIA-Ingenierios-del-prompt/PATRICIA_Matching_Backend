package com.escuelaing.matching.application.service;

import com.escuelaing.matching.domain.exception.UsuarioNoElegibleException;
import com.escuelaing.matching.domain.model.CalculadoraCompatibilidad;
import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.domain.model.ScoreCompatibilidad;
import com.escuelaing.matching.domain.model.Sugerencia;
import com.escuelaing.matching.domain.port.in.CalcularSugerenciasUseCase;
import com.escuelaing.matching.domain.port.out.ColaSugerenciasPort;
import com.escuelaing.matching.domain.port.out.ParcheMembresiaPort;
import com.escuelaing.matching.domain.port.out.PerfilUsuarioPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalcularSugerenciasService implements CalcularSugerenciasUseCase {

    private final PerfilUsuarioPort perfilUsuarioPort;
    private final ParcheMembresiaPort parcheMembresiaPort;
    private final ColaSugerenciasPort colaSugerenciasPort;

    @Value("${matching.candidatos.max-pool:200}")
    private int maxPoolCandidatos;

    @Value("${matching.sugerencias.tamano-cola:50}")
    private int tamanoCola;

    /**
     * Bonus aplicado al factor académico por cada parche en común.
     * Desactivado por defecto: Parches Core (rama feature-profilePhotoForParche)
     * todavía no expone ninguna forma de consultar membresías por usuario, solo
     * por parcheId (ver TODO_INTEGRACIONES.md). Mientras tanto
     * ParcheMembresiaPort se resuelve con MockParcheMembresiaAdapter, que
     * siempre devuelve conjunto vacío, así que este bonus es naturalmente
     * un no-op aunque el flag esté en true. Se deja el flag explícito para
     * poder desactivarlo también a nivel de configuración el día que el
     * endpoint real exista pero se quiera apagar el bonus por otra razón.
     */
    @Value("${matching.parches-comun.habilitado:false}")
    private boolean bonusParchesComunHabilitado;

    @Value("${matching.parches-comun.bonus-por-parche:0.05}")
    private double bonusPorParcheComun;

    @Override
    public void recalcularPara(UUID usuarioId) {
        PerfilMatching usuario = perfilUsuarioPort.buscarPorId(usuarioId)
                .orElseThrow(() -> new UsuarioNoElegibleException(
                        "Usuario " + usuarioId + " no encontrado en Usuarios"));

        if (!usuario.esElegible()) {
            log.info("Usuario {} no es elegible para matching (estado/intereses), vaciando cola", usuarioId);
            colaSugerenciasPort.reemplazarCola(usuarioId, List.of());
            return;
        }

        List<PerfilMatching> candidatos = perfilUsuarioPort.buscarCandidatos(usuarioId, maxPoolCandidatos);

        List<Sugerencia> sugerencias = candidatos.stream()
                .filter(PerfilMatching::esElegible)
                .filter(candidato -> !candidato.usuarioId().equals(usuarioId))
                .map(candidato -> construirSugerencia(usuario, candidato))
                .sorted(Comparator.comparingDouble((Sugerencia s) -> s.score().total()).reversed())
                .limit(tamanoCola)
                .toList();

        colaSugerenciasPort.reemplazarCola(usuarioId, sugerencias);
        log.info("Cola de sugerencias recalculada para usuario={} candidatos={} sugerencias={}",
                usuarioId, candidatos.size(), sugerencias.size());
    }

    private Sugerencia construirSugerencia(PerfilMatching usuario, PerfilMatching candidato) {
        ScoreCompatibilidad scoreBase = CalculadoraCompatibilidad.calcular(usuario, candidato);
        ScoreCompatibilidad scoreFinal = bonusParchesComunHabilitado
                ? aplicarBonusParchesEnComun(usuario.usuarioId(), candidato.usuarioId(), scoreBase)
                : scoreBase;

        return new Sugerencia(usuario.usuarioId(), candidato.usuarioId(), scoreFinal, Instant.now());
    }

    /**
     * Refuerza el factor académico con un bonus proporcional a los parches
     * en común. Se modela como un ajuste del factor académico (no como un
     * cuarto factor) para no romper la ponderación 40/30/30 definida en
     * RF05. Inactivo por defecto: ver javadoc de {@code bonusParchesComunHabilitado}.
     */
    private ScoreCompatibilidad aplicarBonusParchesEnComun(UUID usuarioId, UUID candidatoId, ScoreCompatibilidad base) {
        int parchesEnComun = parcheMembresiaPort.parchesEnComun(usuarioId, candidatoId).size();
        if (parchesEnComun == 0) {
            return base;
        }
        double academicoAjustado = Math.min(1.0, base.academico() + (parchesEnComun * bonusPorParcheComun));
        return ScoreCompatibilidad.de(base.intereses(), academicoAjustado, base.disponibilidad());
    }
}
