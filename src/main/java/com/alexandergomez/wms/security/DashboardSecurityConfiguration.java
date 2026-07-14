package com.alexandergomez.wms.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Session-based, form-login security for the admin dashboard (ADR 0007),
 * separate from the stateless {@code /api/v1} bearer-token chain in
 * {@link SecurityConfiguration}. CSRF stays enabled (Spring's default login
 * page carries the token); the dashboard issues no other state-changing
 * requests. Evaluated first ({@link Order} 1) so {@code /dashboard/**} and the
 * login endpoint never fall through to the catch-all API chain.
 *
 * <p>The access-denied handler sets the status directly instead of using the
 * default {@code response.sendError(403)}: {@code sendError} triggers a
 * servlet-container forward to {@code /error}, which re-enters the security
 * filter chain as a request that no longer matches {@code /dashboard/**} and
 * falls through to the stateless bearer chain, turning the 403 into a 401.
 * {@code /default-ui.css} (Spring's default login page stylesheet) is matched
 * for the same reason: any path this chain doesn't claim falls through to the
 * bearer chain and 401s instead of serving the asset.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class DashboardSecurityConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain dashboardSecurityFilterChain(HttpSecurity http) {
        try {
            http
                    .securityMatcher("/dashboard/**", "/login", "/default-ui.css")
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/login", "/default-ui.css").permitAll()
                            .anyRequest().hasRole("ADMIN"))
                    .formLogin(form -> form.defaultSuccessUrl("/dashboard", true).permitAll())
                    .exceptionHandling(exceptions -> exceptions
                            .accessDeniedHandler((request, response, accessDeniedException) ->
                                    response.setStatus(HttpServletResponse.SC_FORBIDDEN)));
            return http.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build the dashboard security filter chain", ex);
        }
    }
}
