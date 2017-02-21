package top.quantic.sentry.discord;

import com.google.common.collect.Lists;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.config.SentryProperties;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.domain.GameServer;
import top.quantic.sentry.service.GameServerService;
import top.quantic.sentry.service.SettingService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static top.quantic.sentry.discord.util.DiscordUtil.*;
import static top.quantic.sentry.service.util.DateUtil.humanizeShort;
import static top.quantic.sentry.service.util.MiscUtil.*;

@Component
public class GameFiles implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(GameFiles.class);

    private final GameServerService gameServerService;
    private final SentryProperties sentryProperties;
    private final SettingService settingService;

    @Autowired
    public GameFiles(GameServerService gameServerService, SentryProperties sentryProperties, SettingService settingService) {
        this.gameServerService = gameServerService;
        this.sentryProperties = sentryProperties;
        this.settingService = settingService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(get());
    }

    private Command get() {
        OptionParser parser = new OptionParser();
        OptionSpec<String> afterSpec = parser.acceptsAll(asList("after", "since", "newer-than"),
            "Download only files newer than the specified time").withRequiredArg().describedAs("timex").defaultsTo("now-2days");
        OptionSpec<String> beforeSpec = parser.acceptsAll(asList("before", "until", "older-than"),
            "Download only files older than the specified time").withRequiredArg().describedAs("timex");
        OptionSpec<String> sizeRangeSpec = parser.acceptsAll(asList("size", "size-range"),
            "Download only files with size in specified range").withRequiredArg().describedAs("range");
        OptionSpec<Void> dryRunSpec = parser.acceptsAll(asList("preview", "test"), "Only prints, no files downloaded");
        OptionSpec<String> nonOptSpec = parser.nonOptions("Expects at least 2 arguments: " +
            "(1) One of the keywords 'logs' or 'stv', (2) A list of GameServers (IP address, name or region), " +
            "separated by commas. The following argument is an optional glob pattern and is used to only include files matching it");
        return CommandBuilder.of("get")
            .describedAs("Get SourceTV demos or log files from GameServers")
            .in("Files")
            .withExamples(getExamples())
            .parsedBy(parser)
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                IChannel replyChannel = getTrustedChannel(settingService, message);
                OptionSet o = context.getOptionSet();
                List<String> nonOptions = o.valuesOf(nonOptSpec);
                if (nonOptions.size() < 2) {
                    answerToChannel(replyChannel, "Please specify at least 2 non-option arguments: logs/stv and server");
                    return;
                }
                List<GameServer> targets = gameServerService.findServersMultiple(asList(nonOptions.get(1).split(",|;")));
                if (targets.isEmpty()) {
                    answerToChannel(replyChannel, "Must at least match 1 GameServer. Use IP address, name or region.");
                    return;
                }
                String mode = nonOptions.get(0).toLowerCase();
                List<String> command = baseCommand();
                String remote;
                String glob;
                String range;
                if ("logs".equals(mode) || "log".equals(mode)) {
                    remote = "/orangebox/tf/logs";
                    glob = "*.log";
                    range = "100k-10m";
                } else if ("stv".equals(mode) || "sourcetv".equals(mode)) {
                    remote = "/orangebox/tf";
                    glob = "*.dem";
                    range = "0-50m";
                } else {
                    answerToChannel(replyChannel, "Invalid operation mode - Must be one of: logs, stv");
                    return;
                }
                if (nonOptions.size() >= 3) {
                    // discards 4th argument and beyond
                    glob = mergeGlob(nonOptions.get(2), glob);
                }
                if (o.has(sizeRangeSpec)) {
                    range = sanitizeRange(o.valueOf(sizeRangeSpec));
                }
                String mirror = "${out}mirror -v -r -c -I ${in}" + glob + "${in} --size-range=${in}" + range + "${in}"
                    + " --newer-than=${in}" + o.valueOf(afterSpec) + "${in}";
                if (o.has(beforeSpec)) {
                    mirror += " --older-than=${in}" + o.valueOf(beforeSpec) + "${in}";
                }
                if (o.has(dryRunSpec)) {
                    mirror += " --dry-run";
                }
                mirror += " " + remote;
                Path local = Paths.get(sentryProperties.getGameAdmin().getDownloadsDir());
                try {
                    Files.createDirectories(local);
                } catch (IOException e) {
                    log.warn("Could not create downloads directory", e);
                    answerToChannel(replyChannel, "Could not create local directory");
                    return;
                }
                String job = RandomStringUtils.randomAlphanumeric(16);
                long start = System.currentTimeMillis();
                int code = 0;
                RequestBuffer.RequestFuture<IMessage> header = answerToChannel(replyChannel, "Retrieving files, please wait...");
                RequestBuffer.RequestFuture<IMessage> status = answerToChannel(replyChannel, "...");
                Map<String, List<String>> files = new LinkedHashMap<>();
                for (GameServer target : targets) {
                    String key = target.getShortName();
                    List<String> commands = new ArrayList<>(command);
                    String targetMirror = mirror + " " + job + "/" + key + ";bye${out}";
                    targetMirror = targetMirror.replace("${out}", SystemUtils.IS_OS_WINDOWS ? "\"" : "")
                        .replace("${in}", SystemUtils.IS_OS_WINDOWS ? "'" : "\"");
                    commands.add(targetMirror);
                    commands.add(getIPAddress(target.getAddress()));
                    log.debug("Retrieving files from {} server using: {}", target.getShortName(), commands.stream()
                        .filter(this::containsPublicInfo)
                        .collect(Collectors.joining(" ")));
                    updateMessage(header, "Retrieving files from **" + target.getShortName() + "** (" + target.getAddress() + ") ...");
                    Process process = null;
                    try {
                        process = new ProcessBuilder(commands)
                            .directory(local.toFile())
                            .start();
                        startErrorReader(process, target);
                        try (BufferedReader input = newProcessInputReader(process)) {
                            String line;
                            while ((line = input.readLine()) != null) {
                                if (containsPublicInfo(line)) {
                                    log.debug("[" + target + "] " + line);
                                    if (line.startsWith("Transferring file")) {
                                        String filename = line.replaceAll("^.*`(.+)'$", "$1");
                                        files.computeIfAbsent(key, k -> new ArrayList<>()).add(filename);
                                        refreshFiles(status, files);
                                    }
                                }
                            }
                        }
                        process.waitFor(5, TimeUnit.MINUTES);
                        code += process.exitValue();
                        log.debug("Process completed with exit code: {}", process.exitValue());
                    } catch (IOException e) {
                        log.warn("Could not execute process", e);
                        answerToChannel(replyChannel, "Could not execute process");
                        return;
                    } catch (InterruptedException e) {
                        log.warn("Process was interrupted", e);
                        answerToChannel(replyChannel, "Process was interrupted");
                        return;
                    } finally {
                        if (process != null && process.isAlive()) {
                            process.destroyForcibly();
                        }
                    }
                }
                if (!o.has(dryRunSpec)) {
                    answerToChannel(replyChannel, "Your requested files are available under https://quantic.top/files/" + job);
                } else {
                    answerToChannel(replyChannel, "Operation completed " + (code > 0 ? "with errors" : "successfully"));
                }
                long millis = System.currentTimeMillis() - start;
                Path dir = local.resolve(job);
                if (Files.exists(dir)) {
                    String size = humanizeBytes(FileUtils.sizeOf(dir.toFile()));
                    updateMessage(header, "Got " + size + " in " + humanizeShort(Duration.ofMillis(millis)));
                } else {
                    updateMessage(header, "Completed in " + humanizeShort(Duration.ofMillis(millis)));
                }
            })
            .onAuthorDenied(CommandBuilder.noPermission())
            .build();

    }

    private CompletableFuture<RequestBuffer.RequestFuture<IMessage>> refreshFiles(RequestBuffer.RequestFuture<IMessage> message, Map<String, List<String>> files) {
        return updateMessage(message, files.entrySet().stream()
            .map(entry -> "• **" + entry.getKey() + "**: " + entry.getValue().stream().collect(Collectors.joining(", ")))
            .collect(Collectors.joining("\n")));
    }

    private void startErrorReader(Process process, GameServer target) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader input = newProcessErrorReader(process)) {
                String line;
                while ((line = input.readLine()) != null) {
                    if (containsPublicInfo(line)) {
                        log.warn("[" + target + "] " + line);
                    }
                }
            } catch (IOException e) {
                log.warn("Could not read error stream", e);
            }
        });
    }

    private BufferedReader newProcessInputReader(Process p) {
        return new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.forName("UTF-8")));
    }

    private BufferedReader newProcessErrorReader(Process p) {
        return new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.forName("UTF-8")));
    }

    private String sanitizeRange(String range) {
        if (!range.contains("-")) {
            return "0-" + range;
        }
        if (range.startsWith("-")) {
            return "0" + range;
        }
        if (range.endsWith("-")) {
            return range + "100M";
        }
        return range;
    }

    private String mergeGlob(String glob, String base) {
        if (glob.endsWith(base)) {
            return glob.startsWith("*") ? glob : "*" + glob;
        }
        if (glob.endsWith("*")) {
            String glo = glob.substring(0, glob.length() - 1);
            return (glob.startsWith("*") ? glo : "*" + glo) + base;
        }
        return (glob.startsWith("*") ? glob : "*" + glob) + base;
    }

    private List<String> baseCommand() {
        SentryProperties.GameAdmin settings = sentryProperties.getGameAdmin();
        if (settings.getConfigFile() == null) {
            return Lists.newArrayList("lftp", "-u", settings.getUsername() + "," + settings.getPassword(), "-e");
        } else {
            return Lists.newArrayList("lftp", "--rcfile", settings.getConfigFile(), "-u", settings.getUsername() + "," + settings.getPassword(), "-e");
        }
    }

    private boolean containsPublicInfo(String str) {
        SentryProperties.GameAdmin settings = sentryProperties.getGameAdmin();
        return !str.contains(settings.getUsername()) && !str.contains(settings.getPassword());
    }

    private String getExamples() {
        return "Usage: **get** <**logs**|**stv**> <__server__> [__file-glob__] [**after** __timex__] [**before** __timex__] [**size** __range__] [**test**]\n\n" +
            "Many options come with defaults so there is no need to configure each one, for example:\n" +
            "Command ``get stv chi4`` will download all SourceTV demos since 2 days ago from chi4 server.\n" +
            "Command ``get logs chi`` will download all Log files since 2 days ago from all Chicago servers.\n\n" +
            "Further parameter information:\n" +
            "- The first parameter determines what files are retrieved: .log files for **logs** and .dem files for **stv**\n" +
            "- The second parameter determines the servers from where files will be retrieved. This is a list of GameServers (IP address, name or region), separated by commas.\n" +
            "- The third parameter is optional, and represents a glob of files to filter by name. For example using 'pl_upward' as parameter when using the 'stv' mode, will only download demos matching pl_upward in their names.\n" +
            "- A series of optional arguments can be included to filter the downloaded files, for example:\n" +
            "1. You can use 'after' or 'before' followed by an 'at' time expression. Examples of these are 'now-2days', 'now-7days', '\"week ago\"' (within quotes if using spaces) or '2016-02-07 15:00'. " +
            "Guidelines for the specification are present in <http://pubs.opengroup.org/onlinepubs/9699919799/utilities/at.html>\n" +
            "2. You can use 'size' with a range to only download the files that are within that range. For example using '1k-10k' will only get files within 1 KB and 10 KB. Sensible defaults are applied if this option is omitted.\n" +
            "3. You can include the word 'test' to only check which files would be downloaded (dry-run mode).\n";
    }

    @Scheduled(cron = "0 0 0 * * ?")
    void deleteOlderDirectories() {
        SentryProperties.GameAdmin settings = sentryProperties.getGameAdmin();
        Path dir = Paths.get(settings.getDownloadsDir());
        int threshold = 30;
        List<Path> toDelete = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, path -> Files.isDirectory(path))) {
            for (Path path : stream) {
                Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                if (Duration.between(lastModified, Instant.now()).toDays() >= threshold) {
                    toDelete.add(path);
                }
            }
        } catch (IOException e) {
            log.warn("Could not check downloads folder", e);
        }
        for (Path path : toDelete) {
            log.info("Removing folder {} as it is over {} old", path, inflect(threshold, "day"));
            try {
                FileUtils.deleteDirectory(path.toFile());
            } catch (IOException e) {
                log.warn("Could not delete folder", e);
            }
        }
    }
}
