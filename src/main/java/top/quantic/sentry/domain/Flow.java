package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A Flow.
 */

@Document(collection = "flow")
public class Flow extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("name")
    private String name;

    @NotNull
    @Field("input")
    private String input;

    @NotNull
    @Field("message")
    private String message;

    @NotNull
    @Field("translator")
    private String translator;

    @NotNull
    @Field("output")
    private String output;

    @Field("enabled")
    private Boolean enabled = true;

    @Field("variables")
    private Map<String, Object> variables = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Flow name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInput() {
        return input;
    }

    public Flow input(String input) {
        this.input = input;
        return this;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getMessage() {
        return message;
    }

    public Flow message(String message) {
        this.message = message;
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTranslator() {
        return translator;
    }

    public Flow translator(String translator) {
        this.translator = translator;
        return this;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }

    public String getOutput() {
        return output;
    }

    public Flow output(String output) {
        this.output = output;
        return this;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Flow enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Flow flow = (Flow) o;
        if (flow.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, flow.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Flow{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", input='" + input + "'" +
            ", message='" + message + "'" +
            ", translator='" + translator + "'" +
            ", output='" + output + "'" +
            ", enabled='" + enabled + "'" +
            ", variables=" + variables +
            '}';
    }
}
