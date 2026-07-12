package com.escuelaing.matching.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI matchingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Matching API")
                        .description("Microservice that calculates user compatibility, manages the "
                                + "suggestion queue and confirms matches via mutual like.")
                        .version("v0.0.1")
                        .contact(new Contact().name("PATRICIA - Matching")))
                .components(new Components()
                        .addHeaders("X-User-Id", new Header()
                                .description("UUID of the authenticated user, propagated by the Gateway "
                                        + "after JWT validation by Auth")
                                .schema(new StringSchema().format("uuid")))
                        .addHeaders("X-Internal-Api-Key", new Header()
                                .description("Shared secret for service-to-service calls on /internal/**")
                                .schema(new StringSchema())));
    }
}
