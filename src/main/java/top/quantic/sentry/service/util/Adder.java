package top.quantic.sentry.service.util;

public class Adder {

    private final long total;
    private final int count;

    public static Adder sum(Adder adder1, Adder adder2) {
        return new Adder(adder1.total + adder2.total, adder1.count + adder2.count);
    }

    public Adder(long total) {
        this(total, 1);
    }

    public Adder(long total, int count) {
        this.total = total;
        this.count = count;
    }

    public Long average() {
        return total / count;
    }

    public long getTotal() {
        return total;
    }

    public int getCount() {
        return count;
    }
}
