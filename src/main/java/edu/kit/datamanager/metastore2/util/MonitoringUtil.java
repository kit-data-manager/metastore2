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
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.metastore2.configuration.MonitoringConfiguration;
import edu.kit.datamanager.metastore2.dao.*;
import edu.kit.datamanager.metastore2.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for providing monitoring functionality.
 */
public class MonitoringUtil {
  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MonitoringUtil.class);

  private static MonitoringConfiguration monitoringConfiguration;

  private static IIpMonitoringDao ipMonitoringDao;


  MonitoringUtil() {
    //Utility class
  }

  /**
   * Sets the repository for the IP monitoring.
   * @param ipMonitoringDao the ipMonitoringDao to set
   */
  public static void setIpMonitoringDao(IIpMonitoringDao ipMonitoringDao) {
    MonitoringUtil.ipMonitoringDao = ipMonitoringDao;
  }
  /**
   * Sets the monitoring configuration.
   * @param monitoringConfiguration the monitoringConfiguration to set
   */
  public static void setMonitoringConfiguration(MonitoringConfiguration monitoringConfiguration) {
    MonitoringUtil.monitoringConfiguration = monitoringConfiguration;
  }
  /**
   * Returns the number of unique users.
   */
  public static long getNoOfUniqueUsers() {
    return ipMonitoringDao.count();
  }

  /**
   * Register the IP hash for the given IP address.
   * @param ip The IP address to hash.
   */
  public static void registerIp(String ip) {
    // Check if IP address is null or empty
    if (ip == null || ip.trim().isEmpty()) {
      LOG.warn("IP address is null or empty. Cannot register.");
      return;
    }

    String ipHash = ip;
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(ip.getBytes(StandardCharsets.UTF_8));
      ipHash = new String(messageDigest.digest(), StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException nsae) {
      LOG.error("Error hashing IP address: ", nsae);
    }
    IpMonitoring ipMonitoring = new IpMonitoring();
    ipMonitoring.setIpHash(ipHash);
    ipMonitoring.setLastVisit(Instant.now().truncatedTo(ChronoUnit.DAYS));
    ipMonitoringDao.save(ipMonitoring);
  }
  /**
   * Cleans up the metrics by deleting records older than the specified number of days.
   */
  public static void cleanupMetrics() {
    Instant latestDate = Instant.now().minus(monitoringConfiguration.getNoOfDaysToKeep(), ChronoUnit.DAYS);
    ipMonitoringDao.deleteAllEntriesOlderThan(latestDate);
  }

  /** Checks if monitoring is enabled.
   * @return true if monitoring is enabled, false otherwise
   */
  public static boolean isMonitoringEnabled() {
    return monitoringConfiguration.isEnabled();
  }
}

