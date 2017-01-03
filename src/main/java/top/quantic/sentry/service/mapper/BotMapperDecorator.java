package top.quantic.sentry.service.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import sx.blah.discord.api.IDiscordClient;
import top.quantic.sentry.domain.Bot;
import top.quantic.sentry.service.BotService;
import top.quantic.sentry.service.dto.BotDTO;

@Component
public abstract class BotMapperDecorator implements BotMapper {

    private final BotMapper delegate;
    private final BotService botService;

    @Autowired
    public BotMapperDecorator(@Qualifier("delegate") BotMapper delegate, BotService botService) {
        this.delegate = delegate;
        this.botService = botService;
    }

    @Override
    public BotDTO botToBotDTO(Bot bot) {
        BotDTO dto = delegate.botToBotDTO(bot);
        IDiscordClient client = botService.getBotClient(bot);
        if (client != null) {
            dto.setReady(client.isReady());
            dto.setLoggedIn(client.isLoggedIn());
        }
        return dto;
    }
}
