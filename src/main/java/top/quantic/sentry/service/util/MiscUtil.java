package top.quantic.sentry.service.util;

public class MiscUtil {

    public static String humanizeBytes(long bytes) {
        int unit = 1000; // 1024 for non-SI units
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp - 1));
    }

    private MiscUtil() {}
}
