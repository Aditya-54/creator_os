package com.creatoros.social;

/**
 * Supported social platforms. Adding a new platform means adding an enum value
 * plus a {@link SocialPlatformClient} implementation - business logic never
 * branches on platform directly (see docs/architecture.md).
 */
public enum Platform {
    YOUTUBE,
    INSTAGRAM,
    MOCK
}
