package top.quantic.sentry.aop.limiter;

import com.google.common.util.concurrent.RateLimiter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import top.quantic.sentry.web.rest.errors.RateLimitException;
import top.quantic.sentry.web.rest.util.RateLimited;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Aspect
@Component
public class RateLimiterAspect {

    private static final String RATE_LIMIT_PRECONDITION_FAIL = "HttpServletRequest parameter is missing!";

    private final Map<String, RateLimiter> limiterMap = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimited)")
    public void rateLimited(JoinPoint jp, RateLimited rateLimited) {
        RateLimiter limiter = limiterMap.computeIfAbsent(createKey(jp), createLimiter(rateLimited));
        boolean acquired = limiter.tryAcquire(0, TimeUnit.SECONDS);
        if (!acquired) {
            throw new RateLimitException("Rate limit exceeded");
        }
    }

    private Function<String, RateLimiter> createLimiter(RateLimited limit) {
        return name -> RateLimiter.create(limit.value());
    }

    private String createKey(JoinPoint jp) {
        Object[] args = jp.getArgs();

        if (args.length <= 0) {
            throw new IllegalArgumentException(RATE_LIMIT_PRECONDITION_FAIL);
        }

        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) arg;

                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null) {
                    ipAddress = request.getRemoteAddr();
                }
                return ipAddress;
            }
        }

        throw new IllegalArgumentException(RATE_LIMIT_PRECONDITION_FAIL);
    }
}
