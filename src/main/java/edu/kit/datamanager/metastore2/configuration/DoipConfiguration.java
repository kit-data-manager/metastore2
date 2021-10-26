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
 *
 */
@ConfigurationProperties(prefix = "metastore")
@Component
@Data
@Validated
@RefreshScope
@EqualsAndHashCode(callSuper = true)
public class DoipConfiguration extends GenericPluginProperties{

  @Value("${repo.plugin.doip.port:8881}")
  private int port;

  @Value("${repo.plugin.doip.serviceId:0.NFDI4Ing/DOIPServer}")
  private String serviceId;

  @Value("${repo.plugin.doip.serviceName:TurntableAPI}")
  private String serviceName;

  @Value("${repo.plugin.doip.serviceDescription:Generic repository especially for metadata.}")
  private String serviceDescription;

  @Value("${repo.plugin.doip.address:localhost}")
  private String address;

  @Value("${repo.plugin.doip.authenticationEnabled:true}")
  private String authenticationEnabled;

  @Value("${repo.plugin.doip.defaultToken:8881}")
  private String defaultToken;

}
