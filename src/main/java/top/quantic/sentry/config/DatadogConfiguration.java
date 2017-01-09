package top.quantic.sentry.config;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.coursera.metrics.datadog.transport.Transport;
import org.coursera.metrics.datadog.transport.UdpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@AutoConfigureAfter(MetricsConfiguration.class)
public class DatadogConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatadogConfiguration.class);

    private final SentryProperties sentryProperties;

    @Autowired
    public DatadogConfiguration(SentryProperties sentryProperties) {
        this.sentryProperties = sentryProperties;
    }

    @Bean
    public DatadogReporter datadogReporter(MetricRegistry registry) {
        DatadogReporter reporter = null;
        if (isEnabled()) {
            reporter = enableDatadogMetrics(registry);
        } else {
            log.info("Datadog reporter is disabled");
        }
        return reporter;
    }

    private DatadogReporter enableDatadogMetrics(MetricRegistry registry) {
        log.info("Initializing Datadog reporter on host: {} with period: {} seconds", getHost(), getPeriod());
        Transport transport;
        if (getApiKey() == null) {
            // use UDP transport
            transport = new UdpTransport.Builder().build();
        } else {
            transport = new HttpTransport.Builder()
                .withApiKey(getApiKey())
                .build();
        }
        DatadogReporter reporter = DatadogReporter.forRegistry(registry)
            .withHost(getHost())
            .withTransport(transport)
            .withExpansions(expansions())
            .withTags(getTags())
            .withPrefix(getPrefix())
            .filter(metricFilter())
            .build();
        reporter.start(getPeriod(), TimeUnit.SECONDS);
        log.info("Datadog reporter successfully initialized");
        return reporter;
    }

    private EnumSet<DatadogReporter.Expansion> expansions() {
        List<String> expansions = getExpansions();
        if (expansions.isEmpty() || expansions.contains("ALL")) {
            log.debug("Datadog reporter - Using all expansions");
            return DatadogReporter.Expansion.ALL;
        } else {
            EnumSet<DatadogReporter.Expansion> set = EnumSet.copyOf(
                expansions.stream()
                    .map(DatadogReporter.Expansion::valueOf)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
            log.debug("Datadog reporter - Using expansions: {}", set);
            return set;
        }
    }

    private MetricFilter metricFilter() {
        return (name, metric) -> getInclude().stream().anyMatch(name::matches) && getExclude().stream().noneMatch(name::matches);
    }

    private boolean isEnabled() {
        return sentryProperties.getMetrics().getDatadog().isEnabled();
    }

    private String getApiKey() {
        return sentryProperties.getMetrics().getDatadog().getApiKey();
    }

    private long getPeriod() {
        return sentryProperties.getMetrics().getDatadog().getPeriod();
    }

    private String getHost() {
        return sentryProperties.getMetrics().getDatadog().getHost();
    }

    private String getPrefix() {
        return sentryProperties.getMetrics().getDatadog().getPrefix();
    }

    private List<String> getTags() {
        return sentryProperties.getMetrics().getDatadog().getTags();
    }

    private List<String> getExpansions() {
        return sentryProperties.getMetrics().getDatadog().getExpansions();
    }

    private List<String> getInclude() {
        return sentryProperties.getMetrics().getDatadog().getInclude();
    }

    private List<String> getExclude() {
        return sentryProperties.getMetrics().getDatadog().getExclude();
    }
}
