package top.quantic.sentry.discord.util;

import com.google.common.util.concurrent.RateLimiter;

public class DiscordLimiter {

    private static final RateLimiter WEBHOOK = RateLimiter.create(1);

    public static void acquireWebhook() {
        WEBHOOK.acquire();
    }

    private DiscordLimiter() {

    }
}
