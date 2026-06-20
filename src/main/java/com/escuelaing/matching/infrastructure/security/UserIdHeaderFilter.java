package com.escuelaing.matching.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Autentica las peticiones a {@code /matching/**} confiando en el header
 * {@code X-User-Id}, propagado por el Gateway/Auth tras validar el JWT
 * del usuario final. Matching no valida tokens JWT directamente: esa
 * responsabilidad es exclusiva de Auth, evitando solapar funciones.
 * <p>
 * Si el header está ausente o no es un UUID válido, la petición sigue
 * sin autenticación y la cadena de seguridad la rechaza más adelante.
 */
@Component
public class UserIdHeaderFilter extends OncePerRequestFilter {

    private static final String USER_PATH_PREFIX = "/matching/";
    private static final String HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().startsWith(USER_PATH_PREFIX)) {

            String userIdHeader = request.getHeader(HEADER);

            if (userIdHeader != null && esUuidValido(userIdHeader)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        userIdHeader,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean esUuidValido(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
