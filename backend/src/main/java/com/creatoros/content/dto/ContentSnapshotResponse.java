package com.creatoros.content.dto;

import com.creatoros.content.ContentSnapshot;
import java.time.Instant;

public record ContentSnapshotResponse(
        Instant capturedAt,
        long views,
        long likes,
        long comments,
        long shares,
        Long followersAttributed,
        Long watchTimeSeconds,
        Double avgViewDurationSeconds
) {
    public static ContentSnapshotResponse from(ContentSnapshot s) {
        return new ContentSnapshotResponse(s.getCapturedAt(), s.getViews(), s.getLikes(), s.getComments(),
                s.getShares(), s.getFollowersAttributed(), s.getWatchTimeSeconds(), s.getAvgViewDurationSeconds());
    }
}
