package top.quantic.sentry.service.dto;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;


/**
 * A DTO for the Subscriber entity.
 */
public class SubscriberDTO implements Serializable {

    private String id;

    private String name;

    @NotNull
    private String channel;

    @NotNull
    private String type;

    @NotNull
    private String typeParameters;


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
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(String typeParameters) {
        this.typeParameters = typeParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubscriberDTO subscriberDTO = (SubscriberDTO) o;

        if ( ! Objects.equals(id, subscriberDTO.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SubscriberDTO{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", channel='" + channel + "'" +
            ", type='" + type + "'" +
            ", typeParameters='" + typeParameters + "'" +
            '}';
    }
}
