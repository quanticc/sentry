package top.quantic.sentry.service.util;

import java.time.ZonedDateTime;

public interface GameStats {

    Long getId();
    String getUrl();
    String getIp();
    ZonedDateTime getCreated();
}
