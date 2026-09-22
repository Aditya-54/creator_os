package com.creatoros.support;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared singleton Postgres container for integration tests. Reused across test
 * classes in the same JVM to avoid paying container startup cost per class;
 * Testcontainers' Ryuk reaper cleans it up when the JVM exits.
 */
public final class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("creatoros_test")
                    .withUsername("creatoros_test")
                    .withPassword("creatoros_test")
                    .withReuse(true);

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {
    }

    public static PostgreSQLContainer<?> instance() {
        return INSTANCE;
    }
}
