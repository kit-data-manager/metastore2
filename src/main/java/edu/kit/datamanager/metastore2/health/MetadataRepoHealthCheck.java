/*
 * Copyright 2023 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.health;

import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.util.ActuatorUtil;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;

/**
 * Collect information about metadata document repository for health actuator.
 */
@Component("MetadataRepo")
public class MetadataRepoHealthCheck  implements HealthIndicator {

  /** 
   * Logger
   */
  private static final Logger LOG = LoggerFactory.getLogger(MetadataRepoHealthCheck.class);
  
  /** 
   * Configuration settings of metadata repo.
   */
  private final MetastoreConfiguration metadataConfig;

  /**
   * Constructor for initializing class.
   * @param metadataConfig Configuration settings of metadata repo.
   */
  public MetadataRepoHealthCheck(MetastoreConfiguration metadataConfig) {
    this.metadataConfig = metadataConfig;
  }

  @Override
  public Health health() {
    LOG.trace("Check for MetadataRepo health information...");

    URL basePath = metadataConfig.getBasepath();
    Map<String, String> details = ActuatorUtil.testDirectory(basePath);

    if (details.isEmpty()) {
      return Health.down().withDetail("No of metadata documents", 0).build();
    } else {
      details.put("No of metadata documents", Long.toString(MetadataRecordUtil.getNoOfDocuments()));
      return Health.up().withDetails(details).build();
    }
  }
}
