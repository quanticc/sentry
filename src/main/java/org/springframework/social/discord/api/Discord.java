package org.springframework.social.discord.api;

import org.springframework.social.ApiBinding;

public interface Discord extends ApiBinding {

    UserOperations userOperations();

    ApplicationOperations applicationOperations();

    InviteOperations inviteOperations();
}
