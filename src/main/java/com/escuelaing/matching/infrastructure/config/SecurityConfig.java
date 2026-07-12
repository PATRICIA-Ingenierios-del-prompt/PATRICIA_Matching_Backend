package com.escuelaing.matching.infrastructure.config;

import com.escuelaing.matching.infrastructure.security.InternalApiKeyFilter;
import com.escuelaing.matching.infrastructure.security.JwtAuthenticationFilter;
import com.escuelaing.matching.infrastructure.security.JwtTokenParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de seguridad de matching-service.
 * Mismo patrón de tres cadenas que usuarios-service:
 *
 * <ol>
 *   <li>{@code /internal/**} → {@link InternalApiKeyFilter} (sin JWT)</li>
 *   <li>{@code /matching/**} → {@link JwtAuthenticationFilter} (JWT de auth-service)</li>
 *   <li>resto              → permitAll solo Swagger/actuator, denyAll el resto</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(
            HttpSecurity http,
            @Value("${security.internal-api-key}") String internalApiKey
    ) throws Exception {
        http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(new InternalApiKeyFilter(internalApiKey),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain matchingFilterChain(
            HttpSecurity http,
            JwtTokenParser jwtTokenParser,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http.securityMatcher("/matching/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenParser), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        // Cubre rutas públicas restantes (Swagger UI, actuator health, etc.).
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health/**"
                        ).permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }
}
