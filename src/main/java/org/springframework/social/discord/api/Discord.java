package org.springframework.social.discord.api;

import org.springframework.social.ApiBinding;
import org.springframework.web.client.RestOperations;

public interface Discord extends ApiBinding {

    UserOperations userOperations();

    ApplicationOperations applicationOperations();

    RestOperations restOperations();
}
