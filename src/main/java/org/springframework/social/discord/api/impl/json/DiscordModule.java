package org.springframework.social.discord.api.impl.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.social.discord.api.DiscordApplicationInfo;
import org.springframework.social.discord.api.DiscordConnection;
import org.springframework.social.discord.api.DiscordGuild;
import org.springframework.social.discord.api.DiscordUser;

public class DiscordModule extends SimpleModule {

    public DiscordModule() {
        super("DiscordModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(DiscordUser.class, DiscordUserProfileMixin.class);
        context.setMixInAnnotations(DiscordGuild.class, DiscordGuildMixin.class);
        context.setMixInAnnotations(DiscordConnection.class, DiscordConnectionMixin.class);
        context.setMixInAnnotations(DiscordApplicationInfo.class, DiscordApplicationInfoMixin.class);
    }
}
