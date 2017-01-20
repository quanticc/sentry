package top.quantic.sentry.service.util;

/**
 * Simple wrapper class to convey more information about a return.
 *
 * @param <T> the content type to hold as a result
 */
public class Result<T> {

    private final T content;
    private final boolean successful;
    private final String message;
    private final Throwable error;

    public static <T> Result<T> ok(T content) {
        return new Result<>(content, true, "", null);
    }

    public static <T> Result<T> ok(T content, String message) {
        return new Result<>(content, true, message, null);
    }

    public static Result<Void> empty(String message) {
        return new Result<>(null, true, message, null);
    }

    public static <T> Result<T> error(T content, String message, Throwable error) {
        return new Result<>(content, false, message, error);
    }

    public static <T> Result<T> error(String message, Throwable error) {
        return new Result<>(null, false, message, error);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(null, false, message, null);
    }

    private Result(T content, boolean successful, String message, Throwable error) {
        this.content = content;
        this.successful = successful;
        this.message = message;
        this.error = error;
    }

    public T getContent() {
        return content;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }
}
