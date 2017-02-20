package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcDivision {

    private Long divId;
    private String divName;
    private Long ladderId;
    private Long divOrder;
    private String divStatus;
    private List<String> aliases = new ArrayList<>();

    public static UgcDivision parse(Long id, String spec) {
        String[] tokens = spec.split(";");
        if (tokens.length < 4) {
            throw new IllegalArgumentException("Bad division spec: " + spec);
        }
        UgcDivision div = new UgcDivision();
        div.setDivId(id);
        div.setDivName(tokens[0]);
        div.setLadderId(Long.parseLong(tokens[1]));
        div.setDivOrder(Long.parseLong(tokens[2]));
        div.setDivStatus(tokens[3]);
        if (tokens.length > 4) {
            div.setAliases(Arrays.asList(Arrays.copyOfRange(tokens, 4, tokens.length)));
        }
        return div;
    }

    public Long getDivId() {
        return divId;
    }

    public void setDivId(Long divId) {
        this.divId = divId;
    }

    public String getDivName() {
        return divName;
    }

    public void setDivName(String divName) {
        this.divName = divName;
    }

    public Long getLadderId() {
        return ladderId;
    }

    public void setLadderId(Long ladderId) {
        this.ladderId = ladderId;
    }

    public Long getDivOrder() {
        return divOrder;
    }

    public void setDivOrder(Long divOrder) {
        this.divOrder = divOrder;
    }

    public String getDivStatus() {
        return divStatus;
    }

    public void setDivStatus(String divStatus) {
        this.divStatus = divStatus;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
}
