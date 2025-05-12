package edu.kit.datamanager.metastore2.web.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.MessageDigest;

@Service
public class PreHandleInterceptor implements HandlerInterceptor {
    private final MeterRegistry meterRegistry;

    /**
     * Logger for this class.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(PreHandleInterceptor.class);

    @Autowired
    PreHandleInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
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

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(ip.getBytes());
        String ipHash = new String(messageDigest.digest());

        meterRegistry.summary("metastore.requests_served", "ipHash", ipHash).record(1);

        return true;
    }
}
