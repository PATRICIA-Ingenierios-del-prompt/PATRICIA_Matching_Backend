package com.escuelaing.matching.infrastructure.adapter.out.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UsuarioPerfilMatchingResponseTest {

    /**
     * Reproduce el incidente: usuarios-service agregó urlFotoPerfil,
     * tienePersonaEnFoto y franjasDisponibilidad a PerfilMatchingResponse,
     * pero este DTO (su espejo en matching-service) no los declara. Sin
     * @JsonIgnoreProperties(ignoreUnknown = true), Jackson lanza
     * UnrecognizedPropertyException con la config por defecto (fail-on-unknown
     * no está deshabilitado en application.yml), y el pool de candidatos
     * queda vacío en silencio.
     */
    @Test
    void ignoraCamposDesconocidosDelContratoDeUsuarios() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "estado": "ACTIVE",
                  "intereses": ["Musica"],
                  "carrera": "Ingeniería de Sistemas",
                  "semestre": 5,
                  "disponibilidad": "DISPONIBLE",
                  "urlFotoPerfil": "https://cdn.example.com/foto.jpg",
                  "tienePersonaEnFoto": false,
                  "franjasDisponibilidad": []
                }
                """;

        UsuarioPerfilMatchingResponse response = assertDoesNotThrow(
                () -> mapper.readValue(json, UsuarioPerfilMatchingResponse.class));

        assertEquals("ACTIVE", response.estado());
        assertEquals("Ingeniería de Sistemas", response.carrera());
    }
}
