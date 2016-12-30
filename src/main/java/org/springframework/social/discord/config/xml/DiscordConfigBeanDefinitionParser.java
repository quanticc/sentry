package org.springframework.social.discord.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.social.config.xml.AbstractProviderConfigBeanDefinitionParser;
import org.springframework.social.discord.config.support.DiscordApiHelper;
import org.springframework.social.discord.connect.DiscordConnectionFactory;
import org.springframework.social.discord.security.DiscordAuthenticationService;
import org.springframework.social.security.provider.SocialAuthenticationService;

import java.util.Map;

class DiscordConfigBeanDefinitionParser extends AbstractProviderConfigBeanDefinitionParser {

    public DiscordConfigBeanDefinitionParser() {
        super(DiscordConnectionFactory.class, DiscordApiHelper.class);
    }

    @Override
    protected Class<? extends SocialAuthenticationService<?>> getAuthenticationServiceClass() {
        return DiscordAuthenticationService.class;
    }

    @Override
    protected BeanDefinition getConnectionFactoryBeanDefinition(String appId, String appSecret, Map<String, Object> allAttributes) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DiscordConnectionFactory.class).addConstructorArgValue(appId).addConstructorArgValue(appSecret);
        return builder.getBeanDefinition();
    }
}
