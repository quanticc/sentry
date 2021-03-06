package org.springframework.social.discord.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.social.discord.api.ApplicationOperations;
import org.springframework.social.discord.api.Discord;
import org.springframework.social.discord.api.InviteOperations;
import org.springframework.social.discord.api.UserOperations;
import org.springframework.social.discord.api.impl.json.DiscordModule;
import org.springframework.social.oauth2.AbstractOAuth2ApiBinding;

public class DiscordTemplate extends AbstractOAuth2ApiBinding implements Discord {

    private UserOperations userOperations;
    private ApplicationOperations applicationOperations;
    private InviteOperations inviteOperations;

    public DiscordTemplate() {
        super();
        initSubApis();
    }

    public DiscordTemplate(String accessToken) {
        super(accessToken);
        initSubApis();
    }

    @Override
    protected MappingJackson2HttpMessageConverter getJsonMessageConverter() {
        MappingJackson2HttpMessageConverter converter = super.getJsonMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new DiscordModule());
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    @Override
    public UserOperations userOperations() {
        return userOperations;
    }

    @Override
    public ApplicationOperations applicationOperations() {
        return applicationOperations;
    }

    @Override
    public InviteOperations inviteOperations() {
        return inviteOperations;
    }

    private void initSubApis() {
        this.userOperations = new UserTemplate(getRestTemplate(), isAuthorized());
        this.applicationOperations = new ApplicationTemplate(getRestTemplate(), isAuthorized());
        this.inviteOperations = new InviteTemplate(getRestTemplate(), isAuthorized());
    }
}
