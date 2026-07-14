package com.alexandergomez.wms.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.identity.AuthenticatedUser;
import com.alexandergomez.wms.identity.TokenAuthentication;
import com.alexandergomez.wms.identity.TokenFailure;
import com.alexandergomez.wms.identity.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates requests carrying an opaque bearer token. On success the caller
 * is placed in the security context with a role authority; on failure the
 * specific {@link ProblemCode} is stored for the entry point and the request
 * continues unauthenticated (so permit-all paths such as logout still run). The
 * filter never writes a response itself.
 */
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String PROBLEM_ATTRIBUTE = "wms.auth.problemCode";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    public BearerTokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String rawToken = header.substring(BEARER_PREFIX.length()).trim();
            TokenAuthentication result = tokenService.authenticate(rawToken);
            if (result.isAuthenticated()) {
                authenticate(request, result.user());
            } else {
                request.setAttribute(PROBLEM_ATTRIBUTE, toProblemCode(result.failure()));
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static void authenticate(HttpServletRequest request, AuthenticatedUser principal) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static ProblemCode toProblemCode(TokenFailure failure) {
        return switch (failure) {
            case EXPIRED -> ProblemCode.TOKEN_EXPIRED;
            case REVOKED -> ProblemCode.TOKEN_REVOKED;
            case INVALID -> ProblemCode.INVALID_TOKEN;
        };
    }
}
