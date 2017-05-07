package top.quantic.sentry.service.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LogsTfProvider implements StatsProvider {

    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://)?logs\\.tf/([0-9]+)");

    @Override
    public boolean matches(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    @Override
    public String getId(String url) {
        return URL_PATTERN.matcher(url).replaceAll("$1");
    }

    @Override
    public GameStats getGameStats(String url) {
        return null;
    }
}
