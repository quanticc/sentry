package top.quantic.sentry.service.dto;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;


/**
 * A DTO for the Setting entity.
 */
public class SettingDTO implements Serializable {

    private String id;

    @NotNull
    private String guild;

    @NotNull
    private String key;

    @NotNull
    private String value;

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

    public void setGuild(String guild) {
        this.guild = guild;
    }
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public String getType() {
        return type;
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

        SettingDTO settingDTO = (SettingDTO) o;

        if ( ! Objects.equals(id, settingDTO.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SettingDTO{" +
            "id=" + id +
            ", guild='" + guild + "'" +
            ", key='" + key + "'" +
            ", value='" + value + "'" +
            ", type='" + type + "'" +
            '}';
    }
}
