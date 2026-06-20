package com.escuelaing.matching.domain.port.in;

import com.escuelaing.matching.domain.model.Sugerencia;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: obtiene el feed de sugerencias pendientes de un
 * usuario (RF05.1) desde la cola de Redis, calculándola bajo demanda
 * si aún no existe o expiró.
 */
public interface ConsultarSugerenciasUseCase {

    List<Sugerencia> obtenerFeed(UUID usuarioId, int limite);
}
