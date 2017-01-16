package top.quantic.sentry.event;

import org.springframework.context.ApplicationEvent;

public abstract class SentryEvent extends ApplicationEvent implements ContentSupplier {

    SentryEvent(Object source) {
        super(source);
    }
}
