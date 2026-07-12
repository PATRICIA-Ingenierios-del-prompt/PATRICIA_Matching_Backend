package com.escuelaing.matching.domain.port.in;

import java.util.Set;
import java.util.UUID;

/**
 * Puerto de entrada: obtiene los IDs de usuarios que le dieron LIKE al
 * usuario autenticado y aún no han recibido respuesta. Corresponde al
 * tab "Solicitudes recibidas" del frontend.
 */
public interface ConsultarSolicitudesRecibidasUseCase {

    Set<UUID> obtenerPara(UUID usuarioId);
}
