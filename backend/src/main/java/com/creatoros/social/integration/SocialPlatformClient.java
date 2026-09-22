package com.creatoros.social.integration;

import com.creatoros.social.Platform;
import java.util.List;

/**
 * Boundary between CreatorOS business logic and a specific social platform.
 * Business/analytics code never talks to YouTube, Instagram, etc. directly -
 * it always goes through this interface, so adding a new platform (TikTok, X, ...)
 * only requires a new implementation, not changes to the sync/analytics layers.
 * See docs/architecture.md "Why mock unsupported social APIs?".
 */
public interface SocialPlatformClient {

    Platform platform();

    /** Resolves a user-supplied handle into a normalized platform profile at connect time. */
    PlatformProfile fetchAccountProfile(SocialConnectRequest request);

    /** Returns the content items currently published on the account. */
    List<PlatformContentItem> fetchContent(ConnectedAccount account);

    /** Returns current-moment performance for the given platform content ids. */
    List<PlatformPerformanceSnapshot> fetchPerformance(ConnectedAccount account, List<String> platformContentIds);
}
