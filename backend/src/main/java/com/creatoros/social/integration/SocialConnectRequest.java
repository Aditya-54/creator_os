package com.creatoros.social.integration;

/**
 * Input needed to connect an account for a given platform. {@code handle} is the
 * platform-native identifier a user supplies (e.g. a YouTube channel ID/handle);
 * {@code accessToken}/{@code refreshToken} are populated once real OAuth is wired
 * up for a platform (currently unused by the YouTube/Mock/Instagram adapters).
 */
public record SocialConnectRequest(String handle, String accessToken, String refreshToken) {
}
