package edu.kit.datamanager.metastore2.service;

import edu.kit.datamanager.metastore2.util.MonitoringUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

@Service
public class PreHandleInterceptor implements HandlerInterceptor {
    private final Counter counter;

    /**
     * Logger for this class.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(PreHandleInterceptor.class);

    @Autowired
    PreHandleInterceptor(MeterRegistry meterRegistry) {
        Gauge.builder(MonitoringService.PREFIX_METRICS + "unique_users", MonitoringUtil::getNoOfUniqueUsers).register(meterRegistry);
        counter = Counter.builder( MonitoringService.PREFIX_METRICS + "requests_served").register(meterRegistry);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
        if (MonitoringUtil.isMonitoringEnabled()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            LOGGER.debug("X-Forwarded-For: {}", forwardedFor);
            String clientIp = null;

            if (forwardedFor != null) {
                String[] ipList = forwardedFor.split(", ");
                if (ipList.length > 0) clientIp = ipList[0];
                LOGGER.debug("Client IP from X-Forwarded-For: {}", clientIp);
            }

            String remoteIp = request.getRemoteAddr();
            LOGGER.debug("Client IP from getRemoteAddr: {}", remoteIp);
            String ip = clientIp == null ? remoteIp : clientIp;
            LOGGER.debug("Using {} for monitoring", ip);

            MonitoringUtil.registerIp(ip);

            counter.increment();
        }

        return true;
    }
}
