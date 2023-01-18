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
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 *
 * @author hartmann-v
 */
@Component("SchemaRepo")
public class SchemaRepoHealthCheck extends HealthCheck {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRepoHealthCheck.class);

  private final MetastoreConfiguration schemaConfig;

  @Autowired
  private final ISchemaRecordDao schemaRecordDao;

  /**
   *
   * @param schemaConfig
   * @param schemaRecordDao
   */
  public SchemaRepoHealthCheck(MetastoreConfiguration schemaConfig,
          ISchemaRecordDao schemaRecordDao) {
    this.schemaConfig = schemaConfig;
    this.schemaRecordDao = schemaRecordDao;
  }

  @Override
  public Health health() {
    LOG.trace("Check for SchemaRepo health information...");

    URL basePath = schemaConfig.getBasepath();
    Map<String, String> details = testDirectory(basePath);

    if (details.isEmpty()) {
      return Health.down().withDetail("No of metadata documents", 0).build();
    } else {
      details.put("No of schema documents", Long.toString(schemaRecordDao.count()));
      return Health.up().withDetails(details).build();
    }
  }

  @Override
  public void contribute(Info.Builder builder) {
    LOG.trace("Check for SchemaRepo information...");

    URL basePath = schemaConfig.getBasepath();
    Map<String, String> details = testDirectory(basePath);

    if (!details.isEmpty()) {
      details.put("No of schema documents", Long.toString(schemaRecordDao.count()));
      builder.withDetail("schemaRepo", details);
    }
  }
}
