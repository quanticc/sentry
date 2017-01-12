package top.quantic.sentry.config;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DefaultMetricNameFormatter;
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
import java.util.regex.Pattern;
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
        log.info("Initializing Datadog reporter on host: {} with period: {} seconds",
            getHost() == null ? "localhost" : getHost(), getPeriod());
        Transport transport = getApiKey() == null ?
            new UdpTransport.Builder().build() : new HttpTransport.Builder().withApiKey(getApiKey()).build();
        DatadogReporter reporter = DatadogReporter.forRegistry(registry)
            .withHost(getHost())
            .withTransport(transport)
            .withExpansions(expansions())
            .withTags(getTags())
            .withPrefix(getPrefix())
            .filter(getFilter())
            .withMetricNameFormatter(new CustomMetricNameFormatter())
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

    private boolean isUseRegexFilters() {
        return sentryProperties.getMetrics().getDatadog().isUseRegexFilters();
    }

    private List<String> getIncludes() {
        return sentryProperties.getMetrics().getDatadog().getIncludes();
    }

    private List<String> getExcludes() {
        return sentryProperties.getMetrics().getDatadog().getExcludes();
    }

    ///////////////////////////////////////////////
    // Based on Dropwizard's BaseReporterFactory //
    ///////////////////////////////////////////////

    private static final ContainsMatchingStrategy CONTAINS_STRATEGY = new ContainsMatchingStrategy();
    private static final RegexMatchingStrategy REGEX_STRATEGY = new RegexMatchingStrategy();

    private MetricFilter getFilter() {
        StringMatchingStrategy strategy = isUseRegexFilters() ? REGEX_STRATEGY : CONTAINS_STRATEGY;
        return (name, metric) -> !strategy.containsMatch(ImmutableSet.copyOf(getExcludes()), name) &&
            (getIncludes().isEmpty() || strategy.containsMatch(ImmutableSet.copyOf(getIncludes()), name));
    }

    private interface StringMatchingStrategy {
        boolean containsMatch(ImmutableSet<String> matchExpressions, String metricName);
    }

    private static class ContainsMatchingStrategy implements StringMatchingStrategy {
        @Override
        public boolean containsMatch(ImmutableSet<String> matchExpressions, String metricName) {
            return matchExpressions.stream().anyMatch(metricName::contains);
        }
    }

    private static class RegexMatchingStrategy implements StringMatchingStrategy {
        private final LoadingCache<String, Pattern> patternCache;

        private RegexMatchingStrategy() {
            patternCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, Pattern>() {
                    @Override
                    public Pattern load(String regex) throws Exception {
                        return Pattern.compile(regex);
                    }
                });
        }

        @Override
        public boolean containsMatch(ImmutableSet<String> matchExpressions, String metricName) {
            for (String regexExpression : matchExpressions) {
                if (patternCache.getUnchecked(regexExpression).matcher(metricName).matches()) {
                    // just need to match on a single value - return as soon as we do
                    return true;
                }
            }
            return false;
        }
    }

    private static class CustomMetricNameFormatter extends DefaultMetricNameFormatter {
        @Override
        public String format(String name, String... path) {
            String newName = name
                .replace("com.codahale.metrics.servlet.", "")
                .replace("top.quantic.sentry.", "");
            return super.format(newName, path);
        }
    }
}
