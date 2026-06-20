package com.escuelaing.matching.infrastructure.config;

import com.escuelaing.matching.infrastructure.security.InternalApiKeyFilter;
import com.escuelaing.matching.infrastructure.security.UserIdHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalApiKeyFilter internalApiKeyFilter;
    private final UserIdHeaderFilter userIdHeaderFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                )

                .authorizeHttpRequests(auth -> auth

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health"
                        ).permitAll()

                        .requestMatchers(
                                "/internal/**"
                        ).hasRole("INTERNAL")

                        .requestMatchers(
                                "/matching/**"
                        ).hasRole("USER")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        internalApiKeyFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        userIdHeaderFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .httpBasic(httpBasic -> httpBasic.disable())

                .formLogin(form -> form.disable());

        return http.build();
    }
}
