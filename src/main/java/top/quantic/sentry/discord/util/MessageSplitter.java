package top.quantic.sentry.discord.util;

import net.logstash.logback.encoder.org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MessageSplitter {

    private static final Logger log = LoggerFactory.getLogger(MessageSplitter.class);

    private final String message;

    public MessageSplitter(String message) {
        this.message = message;
    }

    public List<String> split(int maxLength) {
        return splitString(message, maxLength);
    }

    private List<String> splitString(String str, int maxLength) {
        List<String> splits = new ArrayList<>();
        String codeBlock = "```";
        StringTokenizer tokenizer = new StringTokenizer(str, "\n", true);
        StringBuilder builder = new StringBuilder();
        boolean hasUnmatchedCodeBlock = false;
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (builder.length() > 0 && builder.length() + line.length() > maxLength - (hasUnmatchedCodeBlock ? codeBlock.length() : 0)) {
                if (hasUnmatchedCodeBlock) {
                    builder.append(codeBlock);
                }
                splits.add(builder.toString());
                builder = new StringBuilder();
                if (hasUnmatchedCodeBlock) {
                    builder.append(codeBlock).append('\n');
                }
            }
            if (line.contains(codeBlock)) {
                hasUnmatchedCodeBlock = !hasUnmatchedCodeBlock;
            }
            builder.append(line);
        }
        if (builder.length() > maxLength) {
            splits.addAll(splitString(WordUtils.wrap(builder.toString(), maxLength), maxLength));
        } else {
            splits.add(builder.toString());
        }
        return splits;
    }
}
