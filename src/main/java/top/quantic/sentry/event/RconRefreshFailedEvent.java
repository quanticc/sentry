package top.quantic.sentry.event;

import top.quantic.sentry.domain.GameServer;

import java.util.HashMap;
import java.util.Map;

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
        Map<String, Object> map = new HashMap<>();
        map.put("name", server.getShortName());
        map.put("address", server.getAddress());
        map.put("reason", reason);
        return map;
    }

    @Override
    public String toString() {
        return getContentId();
    }
}
