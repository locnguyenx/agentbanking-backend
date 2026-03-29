package com.agentbanking.auth.infrastructure.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class JwtConfig {

    @Bean
    public RSAKey rsaKey() {
        try {
            return new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key", e);
        }
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey);
    }
}