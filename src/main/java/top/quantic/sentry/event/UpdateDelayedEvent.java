package top.quantic.sentry.event;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import top.quantic.sentry.domain.GameServer;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static top.quantic.sentry.service.util.DateUtil.formatRelative;
import static top.quantic.sentry.service.util.MiscUtil.inflect;

public class UpdateDelayedEvent extends SentryEvent {

    private final List<GameServer> delaying;

    public UpdateDelayedEvent(Integer version, List<GameServer> delaying) {
        super(version);
        Objects.requireNonNull(delaying);
        this.delaying = delaying;
    }

    @Override
    public Integer getSource() {
        return (Integer) super.getSource();
    }

    public List<GameServer> getDelaying() {
        return delaying;
    }

    @Override
    public String getContentId() {
        return Integer.toHexString(Objects.hash(getSource(), Arrays.hashCode(delaying.toArray())));
    }

    @Override
    public String asContent(Map<String, Object> dataMap) {
        return "**" + title() + "**\n" + body();
    }

    @Override
    public Map<String, Object> asMap(Map<String, Object> dataMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title());
        map.put("text", body());
        map.put("tags", tags());
        map.put("alert_type", alertType());
        map.put("aggregation_key", "game_updates");
        return map;
    }

    @Override
    public EmbedObject asEmbed(Map<String, Object> dataMap) {
        return null;
    }

    private String title() {
        return "Update to version " + getSource() + " is still in progress";
    }

    private String body() {
        if (delaying.isEmpty()) {
            return "---";
        } else {
            return delaying.stream()
                .map(server -> "• " + server.toString() + " has " + inflect(server.getUpdateAttempts(), "update attempt")
                    + " since " + server.getLastUpdateStart() + " (" + formatRelative(server.getLastUpdateStart()) + ")")
                .collect(Collectors.joining("\n"));
        }
    }

    private String alertType() {
        boolean tooManyAttempts = highestAttemptCount() >= 10;
        boolean tooSlowUpdate = !Duration.between(earliestUpdateStart(), ZonedDateTime.now()).minusMinutes(30).isNegative();
        if (tooManyAttempts && tooSlowUpdate) {
            return "error";
        } else if (tooManyAttempts || tooSlowUpdate) {
            return "warning";
        } else {
            return "info";
        }
    }

    private int highestAttemptCount() {
        return delaying.stream()
            .map(GameServer::getUpdateAttempts)
            .max(Comparator.naturalOrder())
            .orElse(1);
    }

    private ZonedDateTime earliestUpdateStart() {
        return delaying.stream()
            .map(GameServer::getLastUpdateStart)
            .sorted()
            .findFirst()
            .orElse(ZonedDateTime.now());
    }

    private List<String> tags() {
        return delaying.stream()
            .map(server -> Arrays.asList("region:" + server.getShortRegion(), "game:" + server.getShortName()))
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
    }
}
