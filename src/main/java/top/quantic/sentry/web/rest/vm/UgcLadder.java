package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcLadder {

    private Long ladderId;
    private String ladderName;
    private String shortName;
    private List<String> aliases = new ArrayList<>();

    public static UgcLadder parse(Long id, String spec) {
        String[] tokens = spec.split(";");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Bad ladder spec: " + spec);
        }
        UgcLadder lad = new UgcLadder();
        lad.setLadderId(id);
        lad.setLadderName(tokens[0]);
        lad.setShortName(tokens[1]);
        if (tokens.length > 2) {
            lad.setAliases(Arrays.asList(Arrays.copyOfRange(tokens, 2, tokens.length)));
        }
        return lad;
    }

    public Long getLadderId() {
        return ladderId;
    }

    public void setLadderId(Long ladderId) {
        this.ladderId = ladderId;
    }

    public String getLadderName() {
        return ladderName;
    }

    public void setLadderName(String ladderName) {
        this.ladderName = ladderName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }
}
