package com.creatoros.analytics;

import com.creatoros.content.Content;
import com.creatoros.content.ContentSnapshot;

/** Pairing of a content item with its most recent performance snapshot. */
public record ContentWithSnapshot(Content content, ContentSnapshot latest) {

    public double engagementRate() {
        long views = latest.getViews();
        if (views <= 0) {
            return 0;
        }
        return (latest.getLikes() + latest.getComments() + latest.getShares()) / (double) views;
    }
}
