package top.quantic.sentry.service.mapper.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ObjectMappingUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String mapToString(Map<String, Object> in) {
        if (in == null || in.isEmpty()) {
            return "";
        } else {
            try {
                return objectMapper.writeValueAsString(in);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Map<String, Object> stringToMap(String in) {
        if (in == null || in.isEmpty()) {
            return new HashMap<>();
        } else {
            try {
                return objectMapper.readValue(in, new TypeReference<Map<String, Object>>() {
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
