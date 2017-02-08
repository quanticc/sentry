package top.quantic.sentry.service;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.quantic.sentry.config.SentryProperties;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.config.Constants.USER_AGENT;

/**
 * Parser for common server operations inside a GameServers web panel.
 */
@Service
public class GameAdminService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(GameAdminService.class);

    private static final String DOMAIN = "my.gameservers.com";
    private static final String LOGIN_URL = "https://" + DOMAIN;
    private static final String HOME_URL = LOGIN_URL + "/home";
    private static final String PANEL_URL = HOME_URL + "/subscription_info.php";

    private static final int TIMEOUT = 60000;

    private final Map<String, String> session = new ConcurrentHashMap<>();
    private final RateLimiter rateLimiter = RateLimiter.create(0.4); // required to operate GameServers panel

    private final SentryProperties sentryProperties;

    private boolean enabled = true;

    @Autowired
    public GameAdminService(SentryProperties sentryProperties) {
        this.sentryProperties = sentryProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getUsername() == null || getPassword() == null) {
            throw new IllegalArgumentException("Game admin panel credentials not set!");
        }
    }

    private Parameters getHome() {
        return new Parameters(HOME_URL);
    }

    private Parameters getPanelView(String subId, String view) {
        Parameters parameters = new Parameters(PANEL_URL);
        parameters.getVariables().put("SUBID", subId);
        parameters.getVariables().put("view", view);
        return parameters;
    }

    private Parameters getPanelFunction(String subId, String function) {
        Parameters parameters = new Parameters(PANEL_URL);
        parameters.getVariables().put("SUBID", subId);
        parameters.getVariables().put("function", function);
        return parameters;
    }

    private Parameters getPanelModInstall(String subId, String function, String modId) {
        Parameters parameters = getPanelView(subId, "server_mods");
        parameters.getVariables().put("function", function);
        parameters.getVariables().put("modid", modId);
        return parameters;
    }

    private synchronized boolean login() throws IOException {
        log.info("Authenticating to GS admin panel");
        String username = getUsername();
        String password = getPassword();
        rateLimiter.acquire(2);
        Connection.Response loginForm = Jsoup.connect(LOGIN_URL)
            .method(Connection.Method.GET)
            .userAgent(USER_AGENT)
            .execute();
        if (loginForm.statusCode() == 403) {
            log.warn("Disabling panel due to 403");
            enabled = false;
        }
        rateLimiter.acquire(2);
        Document document = Jsoup.connect(LOGIN_URL)
            .userAgent(USER_AGENT)
            .data("logout", "1")
            .data("username", username)
            .data("password", password)
            .data("query_string", "")
            .cookies(loginForm.cookies())
            .post();
        session.clear();
        session.putAll(loginForm.cookies());
        return !isLoginPage(document);
    }

    @Retryable(include = {IOException.class}, backoff = @Backoff(2000L))
    private Document validate(Parameters parameters) throws IOException {
        if (!enabled) {
            return new Document("");
        }
        if (session.isEmpty()) {
            if (!login()) {
                // failed login at this point most likely means incorrect credentials
                throw new IOException("Login could not be completed");
            }
        }
        // our session might have expired
        rateLimiter.acquire();
        Document document = Jsoup.connect(parameters.getUrl())
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT)
            .data(parameters.getVariables())
            .cookies(session)
            .get();
        if (isLoginPage(document)) {
            session.clear();
            throw new IOException("Remote session has expired"); // but will retry
        }
        return document;
    }

    private boolean isLoginPage(Document document) {
        return document.title().contains("Login");
    }

    /**
     * Retrieves the available server list from the provider. It should contain the most recent data.
     *
     * @return a map containing data obtained from the provider
     * @throws IOException
     */
    @Retryable(backoff = @Backoff(2000L))
    public Map<String, Map<String, String>> getServerDetails() throws IOException {
        rateLimiter.acquire();
        Document document = validate(getHome());
        Map<String, Map<String, String>> latest = new LinkedHashMap<>();
        // parse GameServers panel and cache available game servers
        for (Element server : document.select("td.section_notabs").select("tr")) {
            String text = "";
            for (Element data : server.select("td.datatbl_row")) {
                if (!data.text().isEmpty()) {
                    text = data.text();
                }
                Elements links = data.select("a");
                // only save servers with 3 links (info, config, mods)
                if (links.size() == 3) {
                    if (text.isEmpty()) {
                        log.warn("Server name not found!");
                        continue;
                    }
                    String subid = links.first().attr("href").split("SUBID=")[1];
                    // parse name & address
                    String[] array = text.split("\\[|\\] \\(|\\)");
                    // array[1] is the address, array[2] is the name of the server
                    Map<String, String> value = new HashMap<>();
                    value.put("name", array[2]);
                    value.put("SUBID", subid);
                    latest.put(array[1], value);
                    log.info("Server found: ({}) {} [{}]", subid, array[2], array[1]);
                }
            }
        }
        return latest;
    }

    /**
     * Retrieves information about the <code>server</code> not essential for Source/RCON socket creation but for associated
     * services like an FTP server operating for the server's location.
     *
     * @param subId the internal id GameServers uses for its servers
     * @return a map with configuration key-values found in the GS web page
     * @throws IOException if the web operation could not be completed
     */
    @Retryable(backoff = @Backoff(2000L))
    public Map<String, String> getServerInfo(String subId) throws IOException {
        Map<String, String> map = new HashMap<>();
        Document document = validate(getPanelView(subId, "server_information"));
        Result result = extractResult(document.select("td.content_main").text());
        if (result != Result.OTHER) {
            map.put("error", result.toString());
        }
        Elements infos = document.select("div.server_info > a");
        String surl = infos.first().text();
        if (surl.startsWith("ftp://")) {
            URL url = new URL(surl);
            map.put("ftp-hostname", url.getHost());
            String[] userInfo = Optional.ofNullable(url.getUserInfo()).orElse("").split(":");
            if (userInfo.length == 2) {
                map.put("ftp-username", userInfo[0]);
                map.put("ftp-password", userInfo[1]);
            }
        }
        return map;
    }

    @Retryable(backoff = @Backoff(2000L))
    public String getServerConsole(String subId) throws IOException {
        String bodyHtml = validate(getPanelView(subId, "console_log")).body().toString();
        return Jsoup.clean(bodyHtml, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
    }

    @Retryable(backoff = @Backoff(2000L))
    public synchronized Map<String, String> getServerConfig(String subId) throws IOException {
        Map<String, String> map = new HashMap<>();
        Document document = validate(getPanelView(subId, "server_configuration"));
        Result result = extractResult(document.select("td.content_main").text());
        if (result != Result.OTHER) {
            map.put("error", result.name());
        }
        Elements elements = document.select("input");
        for (Element el : elements) {
            map.put(el.attr("name"), el.attr("value"));
        }
        return map;
    }

    /**
     * Retrieves information about the packages or modifications installed to the <code>server</code> that can be managed from the
     * provider service, including game updates.
     *
     * @param subId the internal id GameServers uses for its servers
     * @return a map with mods (updates) key-values found in the GS web page
     * @throws IOException if the web operation could not be completed
     */
    @Retryable(backoff = @Backoff(2000L))
    public Map<String, String> getServerMods(String subId) throws IOException {
        Map<String, String> map = new HashMap<>();
        Document document = validate(getPanelView(subId, "server_mods"));
        Elements mods = document.select("td.section_tabs table[style=\"margin-top: 10px;\"] tr");
        mods.stream().skip(1).filter(el -> {
            Elements cols = el.children();
            return cols.size() == 3 && "Server Update".equals(cols.get(0).text());
        }).forEach(el -> map.put("latest-update", extractDate(el.children().get(1).text())));
        log.debug("Latest available update: {}", map.getOrDefault("latest-update", "Not found!"));
        Elements history = document.select("span.page_subtitle + table tr");
        history.stream().skip(1).findFirst().ifPresent(el -> {
            Elements cols = el.children();
            if (cols.size() == 3) {
                String date = cols.get(0).text();
                String author = cols.get(1).text();
                String mod = cols.get(2).text();
                map.put("last-mod-date", date);
                map.put("last-mod-by", author);
                map.put("last-mod-type", mod);
                log.debug("Most recent update: {} @ {} by {}", mod, date, author);
            } else {
                log.warn("Invalid mod history row, must be size 3 (found {})", cols.size());
            }
        });
        return map;
    }

    public ZonedDateTime getLastAvailableUpdate(String subId) {
        try {
            Map<String, String> map = getServerMods(subId);
            String text = map.get("latest-update");
            if (text != null) {
                LocalDateTime localDateTime = LocalDateTime.parse(text,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
                return ZonedDateTime.of(localDateTime, ZoneOffset.ofHours(-3));
            }
        } catch (IOException e) {
            log.warn("Could not get server #{} mods data: {}", subId, e.toString());
        }
        return null;
    }

    /**
     * Sends a restart instruction to the server.
     *
     * @param subId the internal id GameServers uses for its servers
     * @return <code>true</code> if the instruction was sent, <code>false</code> otherwise, regardless of the restart being
     * successful or not.
     * @throws IOException if the web operation could not be completed
     */
    public Result restart(String subId) throws IOException {
        Document document = validate(getPanelFunction(subId, "restart"));
        Result result = extractResult(document.text());
        if (result == Result.RESTARTED) {
            log.info("*** Server RESTART in progress: {} --", subId);
            //log.debug("Response: {}", document.select("table.global").text());
        } else {
            log.warn("Could not restart server {}: {}", subId, result);
        }
        return result;
    }

    /**
     * Sends a shutdown instruction to the server.
     *
     * @param subId the internal id GameServers uses for its servers
     * @return <code>true</code> if the instruction was sent, <code>false</code> otherwise, regardless of the shutdown being
     * successful or not.
     * @throws IOException if the web operation could not be completed
     */
    public Result stop(String subId) throws IOException {
        Document document = validate(getPanelFunction(subId, "stop"));
        Result result = extractResult(document.text());
        if (result == Result.STOPPED) {
            log.info("*** Server STOP in progress: {} ***", subId);
            //log.debug("Response: {}", document.select("table.global").text());
        } else {
            log.warn("Could not stop server {}: {}", subId, result);
        }
        return result;
    }

    /**
     * Instructs the server to retrieve the latest game update.
     *
     * @param subId the internal id GameServers uses for its servers
     * @return <code>true</code> if the instruction was sent, <code>false</code> otherwise, regardless of the upgrade being
     * successful or not.
     * @throws IOException if the web operation could not be completed
     */
    public Result upgrade(String subId) throws IOException {
        Document document = validate(getPanelModInstall(subId, "addmod", "730"));
        String response = document.select("td.content_main").text();
        Result result = extractResult(response);
        if (result == Result.INSTALLING) {
            log.info("*** Server UPGRADE in progress: {} ***", subId);
            //log.debug("Response: {}", response);
        } else {
            log.warn("Could not upgrade server {}: {}", subId, result);
        }
        return result;
    }

    public Result installMod(String subId, String modId) throws IOException {
        Document document = validate(getPanelModInstall(subId, "addmod2", modId));
        String response = document.select("td.content_main").text();
        Result result = extractResult(response);
        if (result == Result.INSTALLING) {
            log.info("*** Server MOD {} INSTALL in progress: {} ***", modId, subId);
            //log.debug("Response: {}", document.select("td.content_main").text());
        } else {
            log.warn("Could not install mod to server {}: {}", subId, result);
        }
        return result;
    }

    private Result extractResult(String response) {
        if (isBlank(response)) {
            return Result.NO_RESPONSE;
        } else if (response.contains("Please wait")) {
            // This mod has been recently installed to your server. Please wait awhile before attempting another install
            return Result.TOO_MANY_INSTALLS;
        } else if (response.contains("Server is currently offline")) {
            // Server is currently offline and this action cannot be performed
            return Result.SERVER_OFFLINE;
        } else if (response.contains("We have detected an outage")) {
            /* We have detected an outage with your server and are currently working to resolve this issue.
            During this time, configuration management is disabled. Server crashes are generally solved within
            30 minutes and extended mass outages will be reported via the members area alert system. */
            return Result.OUTAGE_DETECTED;
        } else if (response.contains("Your mod is now installing")) {
            /* Your mod is now installing. Depending on the mod requirements,
            your server may be restarted after the install is complete. */
            return Result.INSTALLING;
        } else if (response.contains("Server Restarted")) {
            /*
            Server Restarted - Please allow 15-30 seconds for the server to load.
             */
            return Result.RESTARTED;
        } else if (response.contains("Server Stopped")) {
            return Result.STOPPED;
        } else if (response.contains("Could not start server")) {
            return Result.COULD_NOT_START;
        } else {
            return Result.OTHER;
        }
    }

    private String getUsername() {
        return sentryProperties.getGameAdmin().getUsername();
    }

    private String getPassword() {
        return sentryProperties.getGameAdmin().getPassword();
    }

    private String extractDate(String data) {
        String match = "(Last Updated ";
        if (!data.contains(match)) {
            return "Invalid string!";
        }
        return data.substring(data.indexOf(match) + match.length(), data.length() - 1);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void enable() {
        if (!enabled) {
            log.info("Enabling GS admin service");
            enabled = true;
        }
    }

    public enum Result {
        INSTALLING, RESTARTED, STOPPED, SERVER_OFFLINE, OUTAGE_DETECTED, TOO_MANY_INSTALLS, NO_RESPONSE,
        COULD_NOT_START, OTHER;

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(name().replace("_", " "));
        }
    }

    private static class Parameters {
        final String url;
        final Map<String, String> variables;

        Parameters(String url) {
            this(url, new LinkedHashMap<>());
        }

        Parameters(String url, Map<String, String> variables) {
            this.url = url;
            this.variables = variables;
        }

        public String getUrl() {
            return url;
        }

        public Map<String, String> getVariables() {
            return variables;
        }
    }

}
