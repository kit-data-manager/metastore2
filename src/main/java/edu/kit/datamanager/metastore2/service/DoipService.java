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
package edu.kit.datamanager.metastore2.service;

import com.google.gson.JsonObject;
import edu.kit.datamanager.metastore2.configuration.DoipConfiguration;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.service.doip.DoipProcessor4MetaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import net.dona.doip.InDoipSegment;
import net.dona.doip.server.DoipProcessor;
import net.dona.doip.server.DoipServer;
import net.dona.doip.server.DoipServerConfig;
import net.dona.doip.server.DoipServerRequest;
import net.dona.doip.server.DoipServerResponse;

/**
 * Service for processing DOIP requests.
 *
 */
@Component
public class DoipService implements InitializingBean {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(DoipService.class);
  
  public DoipService() {
    LOGGER.info("DOIP service ready to use...");
  }
  
  @Autowired
  private DoipConfiguration doipConfiguration;
  @Autowired
  Environment environment;
  
  @Autowired
  private MetastoreConfiguration schemaConfig;
  
  @Autowired
  private MetastoreConfiguration metadataConfig;
  
  @Override
  public void afterPropertiesSet() throws Exception {
    LOGGER.info("*********************************************************************");
    if (doipConfiguration.isEnabled()) {
      LOGGER.info("Setup DOIP service...");
      init();
    } else {
      LOGGER.info("DOIP service is not enabled!");
    }
    LOGGER.info("*********************************************************************");
  }

  /**
   * Initialize DOIP server.
   *
   * @see DoipConfiguration
   */
  public void init() {
    DoipServerConfig config = new DoipServerConfig();
    config.listenAddress = doipConfiguration.getAddress();
    
    config.port = doipConfiguration.getPort();
    config.processorClass = "edu.kit.datamanager.metastore2.service.doip.DoipProcessor4MetaStore";
    config.processorConfig = new JsonObject();
    config.processorConfig.addProperty("serviceId", doipConfiguration.getServiceId());
    config.processorConfig.addProperty("serviceName", doipConfiguration.getServiceName());
    config.processorConfig.addProperty("serviceDescription", doipConfiguration.getServiceDescription());
    config.processorConfig.addProperty("authenticationEnabled", doipConfiguration.getAuthenticationEnabled());
    config.processorConfig.addProperty("defaultToken", doipConfiguration.getDefaultToken());
    
    DoipServerConfig.TlsConfig tlsConfig = new DoipServerConfig.TlsConfig();
    tlsConfig.id = config.processorConfig.get("serviceId").getAsString();
    config.tlsConfig = tlsConfig;
    DoipProcessor4MetaStore doipProcessor = new DoipProcessor4MetaStore(schemaConfig, metadataConfig);
    doipProcessor.init(config.processorConfig);
    DoipServer server = new DoipServer(config, doipProcessor);
    try {
      server.init();
      Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
      LOGGER.info("Address: '{}'", config.listenAddress);
      LOGGER.info("Port: '{}'", config.port);
      LOGGER.info("Service Name: '{}'", doipConfiguration.getServiceName());
      LOGGER.info("Service ID: '{}'", doipConfiguration.getServiceId());
      LOGGER.info("Service Description: '{}'", doipConfiguration.getServiceDescription());
      LOGGER.info("Authentication Enabled: '{}'", doipConfiguration.getAuthenticationEnabled());
      LOGGER.info("Default Token: '{}'", doipConfiguration.getDefaultToken());
      LOGGER.info("*********************************************************************");
      LOGGER.info("DOIP Server is up and running!");
    } catch (Exception ex) {
      LOGGER.error("Error during setup of DOIP server!", ex);
    }
  }
  
}
