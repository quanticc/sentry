package top.quantic.sentry.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.service.GameServerService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GameServerHealthIndicator implements HealthIndicator {

    private final GameServerService gameServerService;

    @Autowired
    public GameServerHealthIndicator(GameServerService gameServerService) {
        this.gameServerService = gameServerService;
    }

    @Override
    public Health health() {
        /*
        Perform a series of checks:
        - Servers must be up (0 < delay < 1000 and State == GOOD)
        - Servers must have the latest version
        - Servers must have a valid RCON password (non-null)
         */
        Map<String, Object> details = new LinkedHashMap<>();
        List<GameServer> unresponsive = gameServerService.findUnhealthyServers();
        if (!unresponsive.isEmpty()) {
            details.put("unresponsive", unresponsive.stream()
                .map(GameServer::toString)
                .collect(Collectors.toList()));
        }
        List<GameServer> outdated = gameServerService.findOutdatedServers();
        if (!outdated.isEmpty()) {
            details.put("outdated", outdated.stream()
                .map(GameServer::toString)
                .collect(Collectors.toList()));
        }
        List<GameServer> missingRcon = gameServerService.findServersWithoutRcon();
        if (!missingRcon.isEmpty()) {
            details.put("missingRcon", missingRcon.stream()
                .map(GameServer::toString)
                .collect(Collectors.toList()));
        }
        Health.Builder builder;
        if (details.isEmpty()) {
            builder = Health.up();
            gameServerService.getSummary().forEach(builder::withDetail);
        } else {
            builder = Health.outOfService();
            details.forEach(builder::withDetail);
        }
        return builder.build();
    }
}
