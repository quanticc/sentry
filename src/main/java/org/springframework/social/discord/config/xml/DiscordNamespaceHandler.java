package org.springframework.social.discord.config.xml;

import org.springframework.social.config.xml.AbstractProviderConfigBeanDefinitionParser;
import org.springframework.social.config.xml.AbstractProviderConfigNamespaceHandler;

public class DiscordNamespaceHandler extends AbstractProviderConfigNamespaceHandler {

    @Override
    protected AbstractProviderConfigBeanDefinitionParser getProviderConfigBeanDefinitionParser() {
        return new DiscordConfigBeanDefinitionParser();
    }

}
