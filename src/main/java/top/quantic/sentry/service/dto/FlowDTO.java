package top.quantic.sentry.service.dto;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;


/**
 * A DTO for the Flow entity.
 */
public class FlowDTO implements Serializable {

    private String id;

    private String name;

    @NotNull
    private String input;

    private String inputParameters;

    @NotNull
    private String message;

    @NotNull
    private String translator;

    @NotNull
    private String output;

    private Boolean enabled;


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
    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
    public String getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(String inputParameters) {
        this.inputParameters = inputParameters;
    }
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public String getTranslator() {
        return translator;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }
    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FlowDTO flowDTO = (FlowDTO) o;

        if ( ! Objects.equals(id, flowDTO.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "FlowDTO{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", input='" + input + "'" +
            ", inputParameters='" + inputParameters + "'" +
            ", message='" + message + "'" +
            ", translator='" + translator + "'" +
            ", output='" + output + "'" +
            ", enabled='" + enabled + "'" +
            '}';
    }
}
