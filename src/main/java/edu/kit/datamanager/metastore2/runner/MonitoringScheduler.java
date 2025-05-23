/*
 * Copyright 2019 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.runner;

import edu.kit.datamanager.metastore2.configuration.MonitoringConfiguration;
import edu.kit.datamanager.metastore2.dao.IIpMonitoringDao;
import edu.kit.datamanager.metastore2.domain.IpMonitoring;
import edu.kit.datamanager.metastore2.service.MonitoringService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler for monitoring.
 */
public class MonitoringScheduler {
  public static final String MONITORING_DISABLED = "Monitoring is disabled.";
  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MonitoringScheduler.class);

  private final MonitoringConfiguration monitoringConfiguration;

  private final MonitoringService monitoringService;

  private final ThreadPoolTaskScheduler scheduler4Monitoring = new ThreadPoolTaskScheduler();

  MonitoringScheduler( MonitoringConfiguration monitoringConfiguration, MonitoringService monitoringService) {
    this.monitoringConfiguration = monitoringConfiguration;
    this.monitoringService = monitoringService;
  }

  @PostConstruct
  public void scheduleMonitoring() {
    if (monitoringConfiguration.isEnabled()) {
      // Intialize the ThreadPoolTaskScheduler
      scheduler4Monitoring.setPoolSize(2);
      scheduler4Monitoring.setThreadNamePrefix("monitoring-scheduler-");
      scheduler4Monitoring.initialize();
      // Schedule the tasks
      scheduler4Monitoring.schedule(this::runUpdateMetrics, cronTrigger(monitoringConfiguration.getCron4schedule()));
      scheduler4Monitoring.schedule(this::runCleanupMetrics, cronTrigger(monitoringConfiguration.getCron4cleanUp()));

    } else {
      LOG.info(MONITORING_DISABLED);
    }
  }
  private Trigger cronTrigger(String cronExpression) {
    return new Trigger() {
      @Override
      public Instant nextExecution(TriggerContext triggerContext) {
        return new CronTrigger(cronExpression).nextExecution(triggerContext);
      }
    };
  }
  private void runUpdateMetrics() {
    if (monitoringConfiguration.isEnabled()) {
      try {
        monitoringService.updateMetrics();
      } catch (Exception e) {
        LOG.error("Error updating metrics: ", e);
      }
    } else {
      LOG.info(MONITORING_DISABLED);
    }
  }

  private void runCleanupMetrics() {
    if (monitoringConfiguration.isEnabled()) {
      try {
        monitoringService.cleanUpMetrics();;
      } catch (Exception e) {
        LOG.error("Error cleaning up metrics: ", e);
      }
    } else {
      LOG.info(MONITORING_DISABLED);
    }
  }
}

