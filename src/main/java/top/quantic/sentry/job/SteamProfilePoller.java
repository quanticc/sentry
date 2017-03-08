package top.quantic.sentry.job;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.ibasco.agql.protocols.valve.steam.webapi.pojos.SteamPlayerProfile;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import top.quantic.sentry.service.GameQueryService;

import java.util.LinkedHashSet;
import java.util.Set;

public class SteamProfilePoller implements Job {

    private static final Logger log = LoggerFactory.getLogger(SteamProfilePoller.class);

    @Autowired
    private GameQueryService gameQueryService;

    @Autowired
    private MetricRegistry metricRegistry;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String keys = dataMap.getString("keys");
        if (keys == null) {
            throw new JobExecutionException("Data map key 'keys' is missing");
        }

        Set<Long> checked = new LinkedHashSet<>();

        for (String key : keys.split(";")) {
            Long id = gameQueryService.getSteamId64(key).exceptionally(t -> {
                log.warn("Could not get profile ID from key {}", key, t);
                return null;
            }).join();
            if (id != null) {
                report(id, getPlayerProfile(id));
                checked.add(id);
            }
        }

        // clean up removed ids
        metricRegistry.removeMatching((name, metric) -> name.startsWith("steam.profile") && !checked.contains(extractId(name)));
    }

    private Long extractId(String name) {
        return Long.valueOf(name.replaceAll("^steam\\.profile\\..+\\[steamId:([0-9]+)]$", "$1"));
    }

    private void report(Long id, SteamPlayerProfile profile) {
        String statusKey = "steam.profile.status[steamId:" + id + "]";
        String lastLogOffKey = "steam.profile.lastLogOff[steamId:" + id + "]";
        metricRegistry.remove(statusKey);
        metricRegistry.register(statusKey, (Gauge<Integer>) profile::getPersonaState);
        metricRegistry.remove(lastLogOffKey);
        metricRegistry.register(lastLogOffKey, (Gauge<Long>) profile::getLastLogOff);
    }

    private SteamPlayerProfile getPlayerProfile(Long id) {
        return gameQueryService.getPlayerProfile(id).exceptionally(t -> {
            log.warn("Could not retrieve profile info for {}", id, t);
            SteamPlayerProfile dummy = new SteamPlayerProfile();
            dummy.setPersonaState(0);
            dummy.setLastLogOff(0);
            return dummy;
        }).join();
    }
}
