package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UgcBan {

    private String name;
    private Long memSteam;
    private Object type; // can be numeric like 3 or alphabetic like "N"

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMemSteam() {
        return memSteam;
    }

    public void setMemSteam(Long memSteam) {
        this.memSteam = memSteam;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }
}
