package com.alexandergomez.wms.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import com.alexandergomez.wms.identity.TokenService;

/**
 * Stateless bearer-token security for the {@code /api/v1} REST surface. Login,
 * logout, and the health probe are open; administration paths require the
 * {@code ADMIN} role; everything else requires authentication. CSRF is disabled
 * because the API is token-authenticated and holds no session (ADR 0005). The
 * browser dashboard's cookie/CSRF story is a later phase.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, TokenService tokenService,
            WmsAuthenticationEntryPoint authenticationEntryPoint,
            WmsAccessDeniedHandler accessDeniedHandler) {
        try {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                            .requestMatchers(HttpMethod.POST,
                                    "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated())
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint(authenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler))
                    .addFilterBefore(new BearerTokenAuthenticationFilter(tokenService),
                            AuthorizationFilter.class);
            return http.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build the API security filter chain", ex);
        }
    }
}
