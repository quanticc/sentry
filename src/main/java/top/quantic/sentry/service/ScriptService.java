package top.quantic.sentry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.transform.ThreadInterrupt;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.devcake.groovy.autoimports.AutoImportsGroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import sx.blah.discord.handle.obj.IMessage;
import top.quantic.sentry.discord.core.ClientRegistry;
import top.quantic.sentry.discord.core.CommandRegistry;
import top.quantic.sentry.repository.GameServerRepository;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@Service
public class ScriptService {

    private static final Logger log = LoggerFactory.getLogger(ScriptService.class);

    private final ApplicationContext context;
    private final SettingService settingService;
    private final PermissionService permissionService;
    private final GameServerService gameServerService;
    private final GameQueryService gameQueryService;
    private final GameExpiryService gameExpiryService;
    private final GameAdminService gameAdminService;
    private final GameServerRepository gameServerRepository;
    private final RestTemplate restTemplate;
    private final ClientRegistry clientRegistry;
    private final CommandRegistry commandRegistry;

    @Autowired
    public ScriptService(ApplicationContext context, SettingService settingService, PermissionService permissionService,
                         GameServerService gameServerService, GameQueryService gameQueryService,
                         GameExpiryService gameExpiryService, GameAdminService gameAdminService,
                         GameServerRepository gameServerRepository, RestTemplate restTemplate,
                         ClientRegistry clientRegistry, CommandRegistry commandRegistry) {
        this.context = context;
        this.settingService = settingService;
        this.permissionService = permissionService;
        this.gameServerService = gameServerService;
        this.gameQueryService = gameQueryService;
        this.gameExpiryService = gameExpiryService;
        this.gameAdminService = gameAdminService;
        this.gameServerRepository = gameServerRepository;
        this.restTemplate = restTemplate;
        this.clientRegistry = clientRegistry;
        this.commandRegistry = commandRegistry;
    }

    private GroovyShell createShell(Map<String, Object> bindingValues) {
        bindingValues.put("context", context);
        bindingValues.put("setting", settingService);
        bindingValues.put("permission", permissionService);
        bindingValues.put("gameServer", gameServerService);
        bindingValues.put("gameQuery", gameQueryService);
        bindingValues.put("gameExpiry", gameExpiryService);
        bindingValues.put("gameAdmin", gameAdminService);
        bindingValues.put("gameServerRepository", gameServerRepository);
        bindingValues.put("mapper", new ObjectMapper());
        bindingValues.put("rest", restTemplate);
        bindingValues.put("clientRegistry", clientRegistry);
        bindingValues.put("commandRegistry", commandRegistry);

        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));

        ImportCustomizer importCustomizer = new ImportCustomizer();
        settingService.findByGuild("import").forEach(setting -> {
            String key = setting.getKey();
            String value = setting.getValue();
            switch (key) {
                case "alias": {
                    String[] tokens = value.split(",");
                    for (String definition : tokens) {
                        String[] steps = definition.split(":");
                        if (steps.length > 1) {
                            importCustomizer.addImport(steps[0], steps[1]);
                        } else {
                            log.warn("Ignoring badly defined alias, must use Alias:class notation: {}", value);
                        }
                    }
                    break;
                }
                case "import": {
                    String[] tokens = value.split(",");
                    importCustomizer.addImports(tokens);
                    break;
                }
                case "star": {
                    String[] tokens = value.split(",");
                    importCustomizer.addStarImports(tokens);
                    break;
                }
                case "static": {
                    String[] tokens = value.split(",");
                    for (String definition : tokens) {
                        String[] steps = definition.split(":");
                        if (steps.length > 2) {
                            importCustomizer.addStaticImport(steps[0], steps[1], steps[2]);
                        } else if (steps.length > 1) {
                            importCustomizer.addStaticImport(steps[0], steps[1]);
                        } else {
                            log.warn("Ignoring badly defined static import, must use Alias:class:Field or class:Field notation: {}", value);
                        }
                    }
                    break;
                }
                case "static-star":
                case "staticStar": {
                    String[] tokens = value.split(",");
                    importCustomizer.addStaticStars(tokens);
                    break;
                }
            }
        });
        config.addCompilationCustomizers(importCustomizer);

        return new AutoImportsGroovyShell(null, new Binding(bindingValues), config);
    }

    public CompletableFuture<Map<String, Object>> executeScript(String script, IMessage message) {
        log.info("Executing Script: " + script);
        try {
            return CompletableFuture.supplyAsync(() -> eval(script, message));
        } catch (Throwable t) {
            log.warn("Error while evaluating script", t);
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("error", t.getMessage());
            return CompletableFuture.completedFuture(resultMap);
        }
    }

    public Map<String, Object> eval(String script, IMessage message) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        Map<String, Object> scope = new LinkedHashMap<>();
        resultMap.put("script", script);
        resultMap.put("startedAt", ZonedDateTime.now());
        scope.put("bot", message.getClient().getOurUser());
        scope.put("client", message.getClient());
        scope.put("message", message);
        scope.put("content", message.getContent());
        scope.put("server", message.getChannel().isPrivate() ? null : message.getChannel().getGuild());
        scope.put("channel", message.getChannel());
        scope.put("author", message.getAuthor());
        FutureTask<String> evalTask = new FutureTask<>(() -> evalWithScope(script, scope));
        try {
            evalTask.run();
            resultMap.put("result", evalTask.get(1, TimeUnit.MINUTES));
        } catch (Throwable t) {
            log.error("Could not bind result", t);
            resultMap.put("throwable", t);
            resultMap.put("error", t.getMessage());
            evalTask.cancel(true);
        }
        resultMap.put("stoppedAt", ZonedDateTime.now());
        return resultMap;
    }

    public String evalWithScope(String script, Map<String, Object> scope) {
        Object result = createShell(scope).evaluate(script);
        String resultString = result != null ? result.toString() : "null";
        log.trace("eval() result: " + resultString);
        return resultString;
    }

}
