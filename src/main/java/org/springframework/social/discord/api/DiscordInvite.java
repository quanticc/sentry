package org.springframework.social.discord.api;

import java.io.Serializable;
import java.util.Objects;

public class DiscordInvite implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private Guild guild;
    private Channel channel;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscordInvite that = (DiscordInvite) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    public static class Guild implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String splash;
        private String icon;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSplash() {
            return splash;
        }

        public void setSplash(String splash) {
            this.splash = splash;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Guild guild = (Guild) o;
            return Objects.equals(id, guild.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    public static class Channel implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String name;
        private String type;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Channel channel = (Channel) o;
            return Objects.equals(id, channel.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }


}
