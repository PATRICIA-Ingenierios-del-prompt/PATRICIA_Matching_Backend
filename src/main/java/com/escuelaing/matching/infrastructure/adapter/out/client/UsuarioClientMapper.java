package com.escuelaing.matching.infrastructure.adapter.out.client;

import com.escuelaing.matching.domain.model.DisponibilidadUsuario;
import com.escuelaing.matching.domain.model.EstadoUsuario;
import com.escuelaing.matching.domain.model.PerfilMatching;
import com.escuelaing.matching.infrastructure.adapter.out.client.dto.UsuarioPerfilMatchingResponse;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UsuarioClientMapper {

    public PerfilMatching aDominio(UsuarioPerfilMatchingResponse response) {
        if (response == null) {
            return null;
        }

        Set<String> intereses = response.intereses() == null
                ? Set.of()
                : new HashSet<>(response.intereses());

        return new PerfilMatching(
                response.id(),
                intereses,
                response.carrera(),
                response.semestre(),
                parsearDisponibilidad(response.disponibilidad()),
                parsearEstado(response.estado())
        );
    }

    private DisponibilidadUsuario parsearDisponibilidad(String valor) {
        try {
            return valor == null ? DisponibilidadUsuario.OCUPADO : DisponibilidadUsuario.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            return DisponibilidadUsuario.OCUPADO;
        }
    }

    private EstadoUsuario parsearEstado(String valor) {
        try {
            return valor == null ? EstadoUsuario.SUSPENDED : EstadoUsuario.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            return EstadoUsuario.SUSPENDED;
        }
    }
}
