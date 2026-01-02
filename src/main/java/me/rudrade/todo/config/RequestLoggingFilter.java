package me.rudrade.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import java.util.Set;

@Component
public class RequestLoggingFilter extends AbstractRequestLoggingFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final Set<String> excludedUrls = Set.of("/auth");

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        boolean isExcluded = excludedUrls.stream().anyMatch(str -> request.getRequestURI().contains(str));
        if (isExcluded)
            return false;

        return LOGGER.isDebugEnabled();
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        LOGGER.debug(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        LOGGER.debug(message);
    }
}
