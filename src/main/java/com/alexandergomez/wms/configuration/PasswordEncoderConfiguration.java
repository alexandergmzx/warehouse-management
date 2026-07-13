package com.alexandergomez.wms.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing configuration.
 *
 * <p>Uses Argon2id with the Spring Security v5.8 defaults (OWASP-aligned) per
 * ADR 0005. The encoded PHC string is self-describing, so verification needs no
 * external parameter configuration and later parameter increases do not
 * invalidate stored hashes.
 */
@Configuration
public class PasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
