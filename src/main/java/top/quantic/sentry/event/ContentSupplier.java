package top.quantic.sentry.event;

import java.util.Map;

public interface ContentSupplier {
    String getContentId();
    String asContent();
    Map<String, Object> asMap();
}
