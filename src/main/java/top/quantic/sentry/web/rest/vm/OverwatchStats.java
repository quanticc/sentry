package top.quantic.sentry.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OverwatchStats {

    private RegionStats eu;
    private RegionStats kr;
    private RegionStats any;
    private RegionStats us;

    public RegionStats getEu() {
        return eu;
    }

    public void setEu(RegionStats eu) {
        this.eu = eu;
    }

    public RegionStats getKr() {
        return kr;
    }

    public void setKr(RegionStats kr) {
        this.kr = kr;
    }

    public RegionStats getAny() {
        return any;
    }

    public void setAny(RegionStats any) {
        this.any = any;
    }

    public RegionStats getUs() {
        return us;
    }

    public void setUs(RegionStats us) {
        this.us = us;
    }

    public String getRegion() {
        if (us != null) {
            return "US";
        } else if (eu != null) {
            return "EU";
        } else if (kr != null) {
            return "KR";
        } else {
            return "ANY";
        }
    }

    public RegionStats getRegionStats() {
        if (us != null) {
            return us;
        } else if (eu != null) {
            return eu;
        } else if (kr != null) {
            return kr;
        } else {
            return any;
        }
    }

    public static class RegionStats {
        private Map<String, Map<String, Object>> achievements;
        private Map<String, Map<String, Object>> heroes;
        private Map<String, ModeStats> stats;

        public Map<String, Map<String, Object>> getAchievements() {
            return achievements;
        }

        public void setAchievements(Map<String, Map<String, Object>> achievements) {
            this.achievements = achievements;
        }

        public Map<String, Map<String, Object>> getHeroes() {
            return heroes;
        }

        public void setHeroes(Map<String, Map<String, Object>> heroes) {
            this.heroes = heroes;
        }

        public Map<String, ModeStats> getStats() {
            return stats;
        }

        public void setStats(Map<String, ModeStats> stats) {
            this.stats = stats;
        }
    }

    public static class ModeStats {

        private boolean competitive;

        @JsonProperty("overall_stats")
        private Map<String, Object> overallStats;

        @JsonProperty("game_stats")
        private Map<String, Object> gameStats;

        @JsonProperty("average_stats")
        private Map<String, Object> averageStats;

        public boolean isCompetitive() {
            return competitive;
        }

        public void setCompetitive(boolean competitive) {
            this.competitive = competitive;
        }

        public Map<String, Object> getOverallStats() {
            return overallStats;
        }

        public void setOverallStats(Map<String, Object> overallStats) {
            this.overallStats = overallStats;
        }

        public Map<String, Object> getGameStats() {
            return gameStats;
        }

        public void setGameStats(Map<String, Object> gameStats) {
            this.gameStats = gameStats;
        }

        public Map<String, Object> getAverageStats() {
            return averageStats;
        }

        public void setAverageStats(Map<String, Object> averageStats) {
            this.averageStats = averageStats;
        }
    }
}
