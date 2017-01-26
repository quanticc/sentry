package top.quantic.sentry.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import top.quantic.sentry.discord.core.ClientRegistry;
import top.quantic.sentry.domain.Bot;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BotHealthIndicator implements HealthIndicator {

    private final ClientRegistry clientRegistry;

    public BotHealthIndicator(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    @Override
    public Health health() {
        int count = 0;
        int ready = 0;
        Map<String, Object> details = new LinkedHashMap<>();
        for (Map.Entry<Bot, IDiscordClient> entry : clientRegistry.getClients().entrySet()) {
            count++;
            Map<String, Object> bot = new LinkedHashMap<>();
            IDiscordClient client = entry.getValue();
            if (client.isReady()) {
                ready++;
                bot.put("status", "ready");
                bot.put("id", client.getOurUser().getID());
                bot.put("name", client.getOurUser().getName());
                bot.put("shards", client.getShardCount());
                bot.put("guilds", client.getGuilds().size());
                bot.put("channels", client.getChannels().size());
                bot.put("users", client.getUsers().size());
                bot.put("roles", client.getRoles().size());
            } else if (client.isLoggedIn()) {
                bot.put("status", "logged-in");
            } else {
                bot.put("status", "offline");
            }
            details.put(entry.getKey().getName(), bot);
        }
        Health.Builder builder = count == 0 ? Health.unknown() : (count == ready ? Health.up() : Health.outOfService());
        details.put("count", count);
        details.put("ready", ready);
        details.forEach(builder::withDetail);
        return builder.build();
    }
}
