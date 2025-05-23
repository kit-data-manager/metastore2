/*
 * Copyright 2025 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.configuration;

import edu.kit.datamanager.configuration.GenericPluginProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for Monitoring.
 */
@ConfigurationProperties(prefix = "metastore.monitoring")
@Component
@Data
@Validated
@RefreshScope
@EqualsAndHashCode(callSuper = true)
public class MonitoringConfiguration extends GenericPluginProperties {
  /**
   * Whether the monitoring is enabled or not. If set to false, the
   * scheduled task will not be executed.
   */
  private boolean enabled = false;
  /**
   * The cron expression for the schedule task to check the status of the
   * metastore.
   */
  @Value("${metastore.monitoring.cron4schedule:3 * * * *}")
  private String cron4schedule = "3 * * * *"; // every hour at 3 minutes past the hour
  /**
   * The cron expression for the schedule task to clean up the metrics for hashed ips.
   */
  private String cron4cleanUp = "0 0 * * *"; // always at midnight
  /**
   * The number of days to keep the hash of the ips.
   */
  private int noOfDaysToKeep = 28; // 4 weeks
}
