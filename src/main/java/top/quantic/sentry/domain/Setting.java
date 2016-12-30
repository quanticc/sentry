package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Setting.
 */

@Document(collection = "setting")
public class Setting extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("guild")
    private String guild;

    @NotNull
    @Field("key")
    private String key;

    @NotNull
    @Field("value")
    private String value;

    @Field("type")
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGuild() {
        return guild;
    }

    public Setting guild(String guild) {
        this.guild = guild;
        return this;
    }

    public void setGuild(String guild) {
        this.guild = guild;
    }

    public String getKey() {
        return key;
    }

    public Setting key(String key) {
        this.key = key;
        return this;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public Setting value(String value) {
        this.value = value;
        return this;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public Setting type(String type) {
        this.type = type;
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Setting setting = (Setting) o;
        if (setting.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, setting.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Setting{" +
            "id=" + id +
            ", guild='" + guild + "'" +
            ", key='" + key + "'" +
            ", value='" + value + "'" +
            ", type='" + type + "'" +
            '}';
    }
}
