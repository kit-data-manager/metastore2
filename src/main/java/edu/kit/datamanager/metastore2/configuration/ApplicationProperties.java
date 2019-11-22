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

import edu.kit.datamanager.configuration.GenericApplicationProperties;
import java.net.URL;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author jejkal
 */
@Component
@Data
@Validated
@EqualsAndHashCode(callSuper = true)
public class ApplicationProperties extends GenericApplicationProperties{

  @edu.kit.datamanager.annotations.LocalFolderURL
  private URL schemaFolder;
  @Value("${repo.schema.synchronization.enabled:FALSE}")
  private boolean synchronizationEnabled;

  @Value("${repo.schema.synchronization.source}")
  private Map<String, String> schemaSources;

}
