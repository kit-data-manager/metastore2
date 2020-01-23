/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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
import java.net.URL;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author jejkal
 */
@ConfigurationProperties(prefix = "metastore")
@Component
@Data
@Validated
@EqualsAndHashCode(callSuper = true)
public class ApplicationProperties extends GenericPluginProperties{

  @edu.kit.datamanager.annotations.LocalFolderURL
  @Value("${metastore.schema.schemaFolder}")
  private URL schemaFolder;

  @Value("${metastore.schema.synchronization.enabled:FALSE}")
  private boolean synchronizationEnabled;
  // @Value("${metastore.schema.synchronization.schemaSources}")
  private List<SynchronizationSource> schemaSources;

  @Value("${metastore.metadata.metadataFolder}")
  private URL metadataFolder;
  @Value("${metastore.metadata.schemaRegistries:''}")
  private String[] schemaRegistries;

}
