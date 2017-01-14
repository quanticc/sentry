package top.quantic.sentry.service.mapper.util;

import org.springframework.stereotype.Component;

import java.time.Period;

@Component
public class TimeMappingUtil {

    public String periodToString(Period in) {
        if (in == null) {
            return "";
        } else {
            return in.toString();
        }
    }

    public Period stringToPeriod(String in) {
        if (in == null || in.isEmpty()) {
            return Period.ofDays(1);
        } else {
            return Period.parse(in);
        }
    }
}
