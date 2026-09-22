package com.creatoros.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "creatoros.jwt")
public record JwtProperties(String secret, long accessTokenTtlMinutes) {
}
