package top.quantic.sentry.event;

import top.quantic.sentry.domain.GameServer;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static top.quantic.sentry.service.util.Maps.entriesToMap;
import static top.quantic.sentry.service.util.Maps.entry;

public class RconRefreshFailedEvent extends SentryEvent {

    private final String reason;

    public RconRefreshFailedEvent(GameServer server, String reason) {
        super(server);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getContentId() {
        GameServer server = (GameServer) getSource();
        return getClass().getSimpleName() + "-" + server.getShortName() + "-" + reason;
    }

    @Override
    public String asContent() {
        GameServer server = (GameServer) getSource();
        return server.toString() + " Failed to refresh RCON due to: " + reason;
    }

    @Override
    public Map<String, Object> asMap() {
        GameServer server = (GameServer) getSource();
        return Collections.unmodifiableMap(
            Stream.of(
                entry("name", server.getShortName()),
                entry("address", server.getAddress()),
                entry("reason", reason)
            ).collect(entriesToMap()));
    }

    @Override
    public String toString() {
        return getContentId();
    }
}
