package top.quantic.sentry.service.mapper.util;

import com.google.common.base.Splitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StringMappingUtil {

    private final String separator = ";";

    public String joining(List<String> in) {
        if (in == null || in.isEmpty()) {
            return "";
        } else {
            return in.stream().collect(Collectors.joining(separator));
        }
    }

    public List<String> splitting(String in) {
        if (in == null || in.isEmpty()) {
            return new ArrayList<>();
        } else {
            return Splitter.on(separator)
                .omitEmptyStrings()
                .trimResults()
                .splitToList(in);
        }
    }
}
