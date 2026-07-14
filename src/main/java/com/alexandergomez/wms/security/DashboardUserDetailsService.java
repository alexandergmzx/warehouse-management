package com.alexandergomez.wms.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.alexandergomez.wms.identity.AppUser;
import com.alexandergomez.wms.identity.AppUserRepository;

/**
 * Loads operator accounts for the session-based dashboard login (ADR 0007).
 * The {@code /api/v1} bearer-token surface does not use this; it authenticates
 * through {@link com.alexandergomez.wms.identity.TokenService} instead.
 */
@Service
public class DashboardUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public DashboardUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
