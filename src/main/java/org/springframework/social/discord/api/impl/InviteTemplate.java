package org.springframework.social.discord.api.impl;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.discord.api.DiscordInvite;
import org.springframework.social.discord.api.InviteOperations;
import org.springframework.web.client.RestTemplate;

public class InviteTemplate extends AbstractDiscordOperations implements InviteOperations {

    public InviteTemplate(RestTemplate restTemplate, boolean isAuthorizedForUser) {
        super(restTemplate, isAuthorizedForUser);
    }

    @Override
    public DiscordInvite acceptInvite(String code) {
        ResponseEntity<DiscordInvite> responseEntity = restTemplate.exchange(buildUri("/invites/" + code),
            HttpMethod.POST, null, DiscordInvite.class);
        return responseEntity.getBody();
    }
}
