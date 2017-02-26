package top.quantic.sentry.discord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.impl.obj.Message;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;
import top.quantic.sentry.discord.core.Command;
import top.quantic.sentry.discord.core.CommandBuilder;
import top.quantic.sentry.discord.module.CommandSupplier;
import top.quantic.sentry.discord.util.DiscordUtil;
import top.quantic.sentry.service.ScriptService;
import top.quantic.sentry.service.SettingService;
import top.quantic.sentry.service.util.Result;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static top.quantic.sentry.discord.util.DiscordUtil.*;

@Component
public class Eval implements CommandSupplier {

    private static final Logger log = LoggerFactory.getLogger(Eval.class);

    private final ScriptService scriptService;
    private final SettingService settingService;

    @Autowired
    public Eval(ScriptService scriptService, SettingService settingService) {
        this.scriptService = scriptService;
        this.settingService = settingService;
    }

    @Override
    public List<Command> getCommands() {
        return Collections.singletonList(eval());
    }

    private Command eval() {
        return CommandBuilder.of("eval")
            .describedAs("Run a script")
            .in("Administrative")
            .nonParsed()
            .secured()
            .onExecute(context -> {
                IMessage message = context.getMessage();
                IChannel channel = getTrustedChannel(settingService, message);
                String content = context.getContentAfterCommand();
                if (isBlank(content)) {
                    answer(message, "Please input a script, code blocks optional");
                    return;
                }
                RequestBuffer.RequestFuture<IMessage> header = answer(message, "Running script, please wait...");
                doEval(message).whenComplete((result, error) -> {
                    EmbedBuilder builder = new EmbedBuilder()
                        .setLenient(true)
                        .withFooterIcon(message.getAuthor().getAvatarURL())
                        .withFooterText("Requested by " + withDiscriminator(message.getAuthor()))
                        .appendField("Command", content, false);
                    String response;
                    if (result.isSuccessful()) {
                        response = (String) result.getContent().get("result");
                        builder.withColor(new Color(0x00aa00));
                    } else {
                        response = (String) result.getContent().get("error");
                        builder.withColor(new Color(0xaa0000));
                    }
                    String title = result.isSuccessful() ? "Response" : "Error";
                    if (response.length() > Message.MAX_MESSAGE_LENGTH) {
                        sendMessage(channel, builder.appendField(title, "Response too long", false).build()).get();
                        sendMessage(channel, response).get();
                    } else {
                        sendMessage(channel, builder.appendField(title, response, false).build()).get();
                    }
                    CompletableFuture.supplyAsync(header::get).thenApply(DiscordUtil::deleteMessage);
                });
            })
            .build();
    }

    private CompletableFuture<Result<Map<String, Object>>> doEval(IMessage message) {
        String script = message.getContent().split(" ", 2)[1];
        script = script.replace("`", "");
        return scriptService.executeScript(script, message)
            .handle((result, error) -> {
                log.debug("eval result: {}", result);
                if (result.containsKey("error")) {
                    return Result.error(result, "Could not execute script", (Throwable) result.get("throwable"));
                } else {
                    return Result.ok(result, "Script executed successfully");
                }
            });
    }
}
