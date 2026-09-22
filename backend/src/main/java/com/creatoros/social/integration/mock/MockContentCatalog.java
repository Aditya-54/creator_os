package com.creatoros.social.integration.mock;

import com.creatoros.content.ContentType;

/** Static pool of realistic titles/topics used to procedurally build a MOCK account's content list. */
final class MockContentCatalog {

    record Template(String title, String topic, String category, ContentType type, int durationSeconds) {
    }

    static final Template[] TEMPLATES = {
            new Template("Last-minute winner in the derby! 🔥", "football", "Sports", ContentType.SHORT, 45),
            new Template("Breaking down the 4-3-3 press", "football", "Sports", ContentType.VIDEO, 620),
            new Template("Ranked #1 to Grandmaster in 48 hours", "gaming", "Gaming", ContentType.VIDEO, 900),
            new Template("Clutch 1v5 ace (no reloads)", "gaming", "Gaming", ContentType.SHORT, 38),
            new Template("This anime opening hits different", "anime", "Entertainment", ContentType.REEL, 30),
            new Template("Top 10 anime plot twists of the decade", "anime", "Entertainment", ContentType.VIDEO, 780),
            new Template("I built an AI agent that codes itself", "AI", "Technology", ContentType.VIDEO, 940),
            new Template("GPT vs Claude: honest comparison", "AI", "Technology", ContentType.VIDEO, 560),
            new Template("Unboxing the new flagship phone", "technology", "Technology", ContentType.VIDEO, 500),
            new Template("5 productivity apps I can't live without", "technology", "Technology", ContentType.SHORT, 55),
            new Template("Street style haul from Lagos Fashion Week", "fashion", "Fashion", ContentType.REEL, 40),
            new Template("How to style one blazer 5 ways", "fashion", "Fashion", ContentType.POST, 0),
            new Template("Index funds explained in 60 seconds", "finance", "Finance", ContentType.SHORT, 58),
            new Template("Why I stopped day trading", "finance", "Finance", ContentType.VIDEO, 660),
            new Template("Training vlog: matchday prep", "football", "Sports", ContentType.STORY, 20),
    };

    private MockContentCatalog() {
    }
}
