package org.springframework.social.discord.api;

import java.io.Serializable;

public class DiscordConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    private long visibility;
    private boolean friendSync;
    private String type;
    private final String id;
    private String name;

    public DiscordConnection(String id) {
        this.id = id;
    }

    public long getVisibility() {
        return visibility;
    }

    public void setVisibility(long visibility) {
        this.visibility = visibility;
    }

    public boolean isFriendSync() {
        return friendSync;
    }

    public void setFriendSync(boolean friendSync) {
        this.friendSync = friendSync;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
