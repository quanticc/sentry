package top.quantic.sentry.job;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import top.quantic.sentry.event.FeedUpdatedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static top.quantic.sentry.service.util.DateUtil.formatRelative;
import static top.quantic.sentry.service.util.DateUtil.instantToSystem;
import static top.quantic.sentry.service.util.MiscUtil.humanizeBytes;

public class FeedPoller implements Job {

    private static final Logger log = LoggerFactory.getLogger(FeedPoller.class);

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String url = jobDataMap.getString("url");
        log.debug("Retrieving feed at {}", url);
        try {
            HttpCacheContext cacheContext = HttpCacheContext.create();
            HttpGet method = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(method, cacheContext);
                 InputStream stream = response.getEntity().getContent()) {

                CacheResponseStatus responseStatus = cacheContext.getCacheResponseStatus();
                log.debug("Got {} with status: {} {}", humanizeBytes(response.getEntity().getContentLength()),
                    response.getStatusLine(), responseStatus);

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(stream));
                Optional<SyndEntry> mostRecent = feed.getEntries().stream().findFirst();
                if (mostRecent.isPresent()) {
                    SyndEntry entry = mostRecent.get();
                    log.info("{} on {} ({})", entry.getTitle(),
                        instantToSystem(entry.getPublishedDate().toInstant()),
                        formatRelative(entry.getPublishedDate().toInstant()));
                    if (responseStatus == CacheResponseStatus.CACHE_MISS) {
                        publisher.publishEvent(new FeedUpdatedEvent(feed));
                    }
                }
                context.setResult(responseStatus);
            } catch (FeedException e) {
                log.warn("Could not retrieve feed contents", e);
            }
        } catch (IOException e) {
            log.warn("Could not perform HTTP request", e);
        }
    }
}
