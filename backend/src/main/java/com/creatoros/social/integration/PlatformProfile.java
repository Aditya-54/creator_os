package com.creatoros.social.integration;

/** Normalized account/profile info returned when connecting a social account. */
public record PlatformProfile(String platformAccountId, String username, String displayName) {
}
