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

import edu.kit.datamanager.configuration.SearchConfiguration;
import edu.kit.datamanager.metastore2.util.ActuatorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Collect information about metadata document repository for health actuator.
 */
@Component("Elasticsearch")
public class ElasticsearchHealthCheck  implements HealthIndicator {

  /** 
   * Logger
   */
  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchHealthCheck.class);
  
  /** 
   * Configuration settings of elasticsearch.
   */
  private final SearchConfiguration elasticConfig;

  /**
   * Constructor for initializing class.
   * @param elasticConfig Configuration settings of metadata repo.
   */
  public ElasticsearchHealthCheck(SearchConfiguration elasticConfig) {
    this.elasticConfig = elasticConfig;
  }

  @Override
  public Health health() {
    LOG.trace("Check for Elasticsearch health information...");
    Health health = Health.unknown().build();
    if (elasticConfig.isSearchEnabled()) {
      Map<String, String> details = ActuatorUtil.testElastic(elasticConfig.getUrl());
    if (!details.isEmpty()) {
      health = Health.up().withDetails(details).build();
    } else {
      health = Health.down().withDetail("tagline", "-").build();
    }
    } 
    return health;
  }
}
