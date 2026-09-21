package com.cozyfireplace.server.auth.security;

import com.cozyfireplace.server.auth.profile.Profile;
import com.cozyfireplace.server.auth.profile.ProfileRepository;
import com.cozyfireplace.server.util.exception.TokenInvalidException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_NAME = "Authorization";
    public static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ProfileRepository profileRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String jwt = extractJwtFromHeader(request);

        String refreshToken = extractRefreshTokenFromCookie(request);

        // Проверяем, является ли запрос к эндпоинту refresh
        // Если да, то фильтр может не обрабатывать access токен, так как он не нужен для обновления
        if (isRefreshEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtUtil.extractUsername(jwt);

                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    Profile profile = profileRepository.findByUsername(username)
                            .orElse(null);

                    if (profile != null && jwtUtil.isTokenValid(jwt, profile)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } else {
                        throw new TokenInvalidException("Token is invalid");
                    }
                } else {
                    throw new TokenInvalidException("Could not extract username from token");
                }
            } catch (ExpiredJwtException e) {
                log.info("JWT token expired: {}", e.getMessage());
                throw new TokenInvalidException("Token expired");
            } catch (JwtException e) {
                log.info("JWT validation failed: {}", e.getMessage());
                throw new TokenInvalidException("Token is invalid");
            }
        } else {
            log.info("No JWT token found in request header or SecurityContext already populated.");
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(HEADER_NAME);
        if (StringUtils.isNotEmpty(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isRefreshEndpoint(HttpServletRequest request) {
        return "/api/v1/auth/refresh".equals(request.getRequestURI()) ||
                "/auth/refresh".equals(request.getRequestURI());
    }
}