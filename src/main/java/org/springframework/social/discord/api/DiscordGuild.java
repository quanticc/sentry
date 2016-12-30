package org.springframework.social.discord.api;

import java.io.Serializable;

public class DiscordGuild implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean owner;
    private long permissions;
    private String icon;
    private final String id;
    private String name;

    public DiscordGuild(String id) {
        this.id = id;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public long getPermissions() {
        return permissions;
    }

    public void setPermissions(long permissions) {
        this.permissions = permissions;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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
