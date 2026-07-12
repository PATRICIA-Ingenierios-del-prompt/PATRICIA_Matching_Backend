package com.escuelaing.matching.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro JWT para los endpoints públicos ({@code /matching/**}).
 * Extrae el token del header {@code Authorization: Bearer <token>},
 * valida la firma con {@link JwtTokenParser} y puebla el
 * {@code SecurityContext} con el userId ({@code sub}) y los roles.
 * <p>
 * Mismo patrón que {@code JwtAuthenticationFilter} en usuarios-service:
 * matching-service no reimplementa autenticación, solo verifica la firma
 * con el {@code jwt.secret} compartido con auth-service.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenParser jwtTokenParser;

    public JwtAuthenticationFilter(JwtTokenParser jwtTokenParser) {
        this.jwtTokenParser = jwtTokenParser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            try {
                JwtTokenParser.ClaimsJwt claims = jwtTokenParser.parse(token);

                List<GrantedAuthority> authorities = claims.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .collect(Collectors.toList());

                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.userId(), null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception ex) {
                log.warn("Token JWT inválido en matching-service: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
