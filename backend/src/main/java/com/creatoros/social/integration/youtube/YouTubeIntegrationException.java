package com.creatoros.social.integration.youtube;

/** Raised for any YouTube Data API failure; caught by the sync orchestrator and recorded on the sync job. */
public class YouTubeIntegrationException extends RuntimeException {

    public YouTubeIntegrationException(String message) {
        super(message);
    }

    public YouTubeIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
