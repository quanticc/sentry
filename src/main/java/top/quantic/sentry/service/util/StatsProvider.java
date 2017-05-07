package top.quantic.sentry.service.util;

public interface StatsProvider {

    boolean matches(String url);
    String getId(String url);
    GameStats getGameStats(String url);
}
