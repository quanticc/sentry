package top.quantic.sentry.service;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.quantic.sentry.discord.util.MessageSplitter;

import java.util.List;
import java.util.stream.Collectors;

public class SplitUnitTest {

    private static final Logger log = LoggerFactory.getLogger(SplitUnitTest.class);

    @Test
    public void splitTest() {
        int maxLength = 2000;
        List<String> splits1 = new MessageSplitter(textWithNewlines()).split(maxLength);
        log.info("Sizes: {}", splits1.stream().map(s -> s.length() + "").collect(Collectors.joining(", ")));
        log.info("{}", splits1.stream().collect(Collectors.joining("\n==\n")));
        List<String> splits2 = new MessageSplitter(textWithoutNewlines()).split(maxLength);
        log.info("Sizes: {}", splits2.stream().map(s -> s.length() + "").collect(Collectors.joining(", ")));
        log.info("{}", splits2.stream().collect(Collectors.joining("\n==\n")));
    }

    private String textWithNewlines() {
        return "Usage: **delete** [**last** __number__] [**from** __user__] [**after** __timex__] [**before** __timex__] [**like** __content__] [**matching** __regex__] [**depth** __number__] [**include-this**] [**test**] [__user1__ [__user2__ ...]]\n\n" +
            "This command lets you set a bunch of criteria, so let me explain this step by step:\n" +
            "- Option **last** specifies the maximum number of messages to be deleted. For example, if you set 1 it will delete the most recent message matching your criteria. If you omit this it defaults to 100.\n" +
            "- Option **from** lets you set the user that will have their messages deleted. It can be an ID, a name, a nickname or a mention.\n" +
            "- Options **after** and **before** expect a temporal expression like \"2 hours ago\" or a date/time. Take note that Discord does not allow deleting messages older than 2 weeks in bulk and they must be deleted manually or with 'last 1' option.\n" +
            "- Option **like** lets you specify a string (wrap within quotes if you need spaces) to delete all messages (until depth or limit is reached) that contain it.\n" +
            "- Option **matching** lets you specify a regex (regular expression) to delete all messages (until depth or limit is reached) that match it.\n" +
            "- Option **depth** lets you set the maximum number of messages searched. If omitted it defaults to 1000.\n" +
            "- Option **include-this** makes your own command to be included in the search criteria, otherwise it will be ignored, even if it matches the given criteria.\n" +
            "- Option **test** won't delete any message, instead it will PM you what would be deleted with the given criteria.\n" +
            "- After all options, you can set additional users by ID, name, nickname or mention in case you want to delete messages from multiple users.\n\n" +
            "To delete all messages in the past hour: `delete after \"1 hour ago\"` (see the quotes in order to use spaces)\n" +
            "```\nTo delete last 10 messages from user named beepboop: `delete last 10 from beepboop` (no need to use mention)\n" +
            "To delete last 100 messages containing the word 'human': `delete last 100 like human`\n" +
            "To delete\n```\n all messages older than 10 days: `delete before \"10 days ago\"` (uses default for last and depth)\n";
    }

    private String textWithoutNewlines() {
        return "Usage: **delete** [**last** __number__] [**from** __user__] [**after** __timex__] [**before** __timex__] [**like** __content__] [**matching** __regex__] [**depth** __number__] [**include-this**] [**test**] [__user1__ [__user2__ ...]]" +
            "This command lets you set a bunch of criteria, so let me explain this step by step:" +
            "- Option **last** specifies the maximum number of messages to be deleted. For example, if you set 1 it will delete the most recent message matching your criteria. If you omit this it defaults to 100." +
            "- Option **from** lets you set the user that will have their messages deleted. It can be an ID, a name, a nickname or a mention." +
            "- Options **after** and **before** expect a temporal expression like \"2 hours ago\" or a date/time. Take note that Discord does not allow deleting messages older than 2 weeks in bulk and they must be deleted manually or with 'last 1' option." +
            "- Option **like** lets you specify a string (wrap within quotes if you need spaces) to delete all messages (until depth or limit is reached) that contain it." +
            "- Option **matching** lets you specify a regex (regular expression) to delete all messages (until depth or limit is reached) that match it." +
            "- Option **depth** lets you set the maximum number of messages searched. If omitted it defaults to 1000." +
            "- Option **include-this** makes your own command to be included in the search criteria, otherwise it will be ignored, even if it matches the given criteria." +
            "- Option **test** won't delete any message, instead it will PM you what would be deleted with the given criteria." +
            "- After all options, you can set additional users by ID, name, nickname or mention in case you want to delete messages from multiple users." +
            "To delete all messages in the past hour: `delete after \"1 hour ago\"` (see the quotes in order to use spaces)" +
            "To delete last 10 messages from user named beepboop: `delete last 10 from beepboop` (no need to use mention)" +
            "To delete last 100 messages containing the word 'human': `delete last 100 like human`" +
            "To delete all messages older than 10 days: `delete before \"10 days ago\"` (uses default for last and depth)";
    }
}
