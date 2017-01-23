package top.quantic.sentry.discord.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MessageSplitter {

    public static final int LENGTH_LIMIT = 1990;

    private final String message;

    public MessageSplitter(String message) {
        this.message = message;
    }

    public List<String> split(int maxLength) {
        List<String> splits = new ArrayList<>();
        String str = message;
        int end;
        while (str != null && !str.isEmpty()) {
            int codeBlockTags = StringUtils.countOccurrencesOf(str, "```");
            if (str.length() <= Math.max(1, maxLength - (codeBlockTags > 0 ? 4 : 0))) {
                splits.add(str);
                str = "";
            } else {
                end = Math.min(str.length(), str.lastIndexOf("\n", maxLength));
                if (end <= 0) {
                    end = Math.min(str.length(), maxLength);
                }
                String split = str.substring(0, end);
                str = str.substring(end);
                int tagsAfterSplit = StringUtils.countOccurrencesOf(split, "```");
                if (codeBlockTags > 0 && tagsAfterSplit < codeBlockTags && tagsAfterSplit % 2 != 0) {
                    split = split + "```";
                    str = "```\n" + str;
                }
                splits.add(split);
            }
        }
        return splits;
    }
}
