package top.quantic.sentry.service.util;

import java.util.Objects;

public class Point {
    private final Number x;
    private final Number y;

    public Point(Number x, Number y) {
        this.x = x;
        this.y = y;
    }

    public Number getX() {
        return x;
    }

    public Number getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Objects.equals(x, point.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x);
    }
}
