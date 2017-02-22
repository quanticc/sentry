package top.quantic.sentry.discord.util;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MarkdownStripper {

    private static final Pair<Pattern, String> STRIKE_THROUGH = Pair.of(Pattern.compile("~~"), "");
    private static final Pair<Pattern, String> FENCED_CODE_BLOCK = Pair.of(Pattern.compile("`{3}.*\\n"), "");
    private static final Pair<Pattern, String> STYLING = Pair.of(Pattern.compile("([*_]{1,3})(\\S.*?\\S)\\1"), "$2");
    private static final Pair<Pattern, String> CODE_MULTILINE = Pair.of(Pattern.compile("(`{3,})(.*?)\\1", Pattern.MULTILINE), "$2");
    private static final Pair<Pattern, String> CODE = Pair.of(Pattern.compile("`(.+?)`"), "$1");

    private static final List<Pair<Pattern, String>> PATTERNS = Arrays.asList(STRIKE_THROUGH, FENCED_CODE_BLOCK, STYLING, CODE_MULTILINE, CODE);

    public static String strip(String input) {
        String str = input;
        for (Pair<Pattern, String> spec : PATTERNS) {
            str = spec.getLeft().matcher(str).replaceAll(spec.getRight());
        }
        return str;
    }

    private MarkdownStripper() {
    }
}
