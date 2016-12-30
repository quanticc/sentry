package org.springframework.social.discord.api;

import java.io.Serializable;
import java.util.List;

public class DiscordApplicationInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String description;
    private String icon;
    private List<String> rpcOrigins;
    private long flags;
    private DiscordUser owner;

    public DiscordApplicationInfo(String id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getRpcOrigins() {
        return rpcOrigins;
    }

    public void setRpcOrigins(List<String> rpcOrigins) {
        this.rpcOrigins = rpcOrigins;
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    public DiscordUser getOwner() {
        return owner;
    }

    public void setOwner(DiscordUser owner) {
        this.owner = owner;
    }

}
