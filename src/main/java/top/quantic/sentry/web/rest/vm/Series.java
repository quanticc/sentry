package top.quantic.sentry.web.rest.vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Series {

    private String key;
    private List<List<Number>> values = new ArrayList<>();

    public Series() {
    }

    public Series(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Series key(String key) {
        this.key = key;
        return this;
    }

    public List<List<Number>> getValues() {
        return values;
    }

    public void setValues(List<List<Number>> values) {
        this.values = values;
    }

    public Series values(List<List<Number>> values) {
        this.values = values;
        return this;
    }

    public Series add(Number x, Number y) {
        values.add(Arrays.asList(x, y));
        return this;
    }

    @Override
    public String toString() {
        return "{ key: '" + key + "', values: " + values + "}";
    }
}
