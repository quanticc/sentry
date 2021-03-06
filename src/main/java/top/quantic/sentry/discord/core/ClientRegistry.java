package top.quantic.sentry.discord.core;

import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import top.quantic.sentry.domain.Bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientRegistry {

    private final Map<Bot, IDiscordClient> clients = new ConcurrentHashMap<>();

    public Map<Bot, IDiscordClient> getClients() {
        return clients;
    }
}
