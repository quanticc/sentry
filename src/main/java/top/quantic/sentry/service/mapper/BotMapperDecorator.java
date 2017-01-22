package top.quantic.sentry.service.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import sx.blah.discord.api.IDiscordClient;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.discord.core.ClientRegistry;
import top.quantic.sentry.service.dto.BotDTO;

public abstract class BotMapperDecorator implements BotMapper {

    @Autowired
    @Qualifier("delegate")
    private BotMapper delegate;

    @Autowired
    private ClientRegistry clientRegistry;

    @Override
    public BotDTO botToBotDTO(Bot bot) {
        BotDTO dto = delegate.botToBotDTO(bot);
        IDiscordClient client = clientRegistry.getClients().get(bot);
        if (client != null) {
            dto.setCreated(true);
            dto.setReady(client.isReady());
            dto.setLoggedIn(client.isLoggedIn());
        }
        return dto;
    }
}
