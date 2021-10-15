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
package edu.kit.datamanager.metastore2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.configuration.OaiPmhConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.repo.configuration.DateBasedStorageProperties;
import edu.kit.datamanager.repo.configuration.IdBasedStorageProperties;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.IRepoStorageService;
import edu.kit.datamanager.repo.service.IRepoVersioningService;
import edu.kit.datamanager.repo.service.impl.ContentInformationAuditService;
import edu.kit.datamanager.repo.service.impl.ContentInformationService;
import edu.kit.datamanager.repo.service.impl.DataResourceAuditService;
import edu.kit.datamanager.repo.service.impl.DataResourceService;
import edu.kit.datamanager.repo.service.impl.IdBasedStorageService;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.RabbitMQMessagingService;
import java.util.ArrayList;
import java.util.List;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 */
@SpringBootApplication
@EnableScheduling
@EntityScan("edu.kit.datamanager")
@EnableJpaRepositories("edu.kit.datamanager")
@ComponentScan({"edu.kit.datamanager"})
public class Application {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  @Autowired
  private Javers javers;
  @Autowired
  private IDataResourceService schemaResourceService;
  @Autowired
  private IContentInformationService schemaInformationService;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private IRepoVersioningService[] versioningServices;
  @Autowired
  private IRepoStorageService[] storageServices;

  private MetastoreConfiguration metastoreProperties;
  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  @Autowired
  private IDataRecordDao dataRecordDao;
  @Autowired
  private IUrl2PathDao url2PathDao;
  @Autowired
  private IMetadataFormatDao metadataFormatDao;
  @Autowired
  private OaiPmhConfiguration oaiPmhConfiguration;
  @Autowired
  private IValidator[] validators;
  @Autowired
  private IDataResourceService dataResourceService;
  @Autowired
  private IContentInformationService contentInformationService;

  @Bean
  @Scope("prototype")
  public Logger logger(InjectionPoint injectionPoint) {
    Class<?> targetClass = injectionPoint.getMember().getDeclaringClass();
    return LoggerFactory.getLogger(targetClass.getCanonicalName());
  }

  @Bean(name = "OBJECT_MAPPER_BEAN")
  public ObjectMapper jsonObjectMapper() {
    return Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Don’t include null values
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //ISODate
            .modules(new JavaTimeModule())
            .build();
  }

  @Bean
  @ConfigurationProperties("metastore")
  public MetastoreConfiguration metastoreConfiguration() {
    return new MetastoreConfiguration();
  }

//  @Bean
//  public IdBasedStorageProperties idBasedStorageProperties() {
//    return new IdBasedStorageProperties();
//  }
  @Bean
  public DateBasedStorageProperties dateBasedStorageProperties() {
    return new DateBasedStorageProperties();
  }

  @Bean
  public IdBasedStorageProperties idBasedStorageProperties() {
    return new IdBasedStorageProperties();
  }

  @Bean
  public OaiPmhConfiguration oaiPmhConfiguration() {
    return new OaiPmhConfiguration();
  }

  @Bean
  public IMessagingService messagingService() {
    return new RabbitMQMessagingService();
  }

  @Bean
  public IDataResourceService dataResourceService() {
    return new DataResourceService();
  }

  @Bean
  public IDataResourceService schemaResourceService() {
    return new DataResourceService();
  }

  @Bean
  public IContentInformationService contentInformationService() {
    return new ContentInformationService();
  }

  @Bean
  public IContentInformationService schemaInformationService() {
    return new ContentInformationService();
  }

  @Bean
  public MetastoreConfiguration schemaConfig() {

    IAuditService<DataResource> auditServiceDataResource;
    IAuditService<ContentInformation> contentAuditService;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(this.applicationProperties.getSchemaFolder());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(schemaResourceService);
    rbc.setContentInformationService(schemaInformationService);
    rbc.setEventPublisher(eventPublisher);
    for (IRepoVersioningService versioningService : this.versioningServices) {
      if ("simple".equals(versioningService.getServiceName())) {
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    for (IRepoStorageService storageService : this.storageServices) {
      if ("simple".equals(storageService.getServiceName())) {
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
    schemaResourceService.configure(rbc);
    schemaInformationService.configure(rbc);
    rbc.setAuditService(auditServiceDataResource);
    rbc.setSchemaRegistries(checkRegistries(applicationProperties.getSchemaRegistries()));
    rbc.setValidators(validators);
    MetadataRecordUtil.setSchemaConfig(rbc);
    MetadataRecordUtil.setDataRecordDao(dataRecordDao);
    MetadataSchemaRecordUtil.setSchemaRecordDao(schemaRecordDao);
    MetadataSchemaRecordUtil.setMetadataFormatDao(metadataFormatDao);
    MetadataSchemaRecordUtil.setUrl2PathDao(url2PathDao);
    
    printSettings(rbc);
    
    return rbc;
  }

  @Bean
  public MetastoreConfiguration metadataConfig() {

    IAuditService<DataResource> auditServiceDataResource;
    IAuditService<ContentInformation> contentAuditService;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(applicationProperties.getMetadataFolder());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(dataResourceService);
    rbc.setContentInformationService(contentInformationService);
    rbc.setEventPublisher(eventPublisher);
    for (IRepoVersioningService versioningService : this.versioningServices) {
      if ("simple".equals(versioningService.getServiceName())) {
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    for (IRepoStorageService storageService : this.storageServices) {
      if (IdBasedStorageService.SERVICE_NAME.equals(storageService.getServiceName())) {
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
    dataResourceService.configure(rbc);
    contentInformationService.configure(rbc);
    rbc.setAuditService(auditServiceDataResource);
    rbc.setSchemaRegistries(checkRegistries(applicationProperties.getSchemaRegistries()));
    rbc.setValidators(validators);
    
    printSettings(rbc);
    
    return rbc;
  }
  /**
   * Print current settings for repository
   * @param config Settings.
   */
  public void printSettings(MetastoreConfiguration config) {
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", config);
    LOG.info("------------------------------------------------------");
    LOG.info("Versioning service: {}", config.getVersioningService().getServiceName());
    LOG.info("Storage service: {}", config.getStorageService().getServiceName());
    LOG.info("Basepath metadata repository: {}", config.getBasepath().toString());
    int noOfSchemaRegistries = config.getSchemaRegistries().length;
    LOG.info("Number of registered external schema registries: {}", noOfSchemaRegistries);
    for (int index1 = 0; index1 < noOfSchemaRegistries; index1++) {
      LOG.info("Schema registry '{}': {}", index1 + 1, config.getSchemaRegistries()[index1]);
    }
    
  }
  /** 
   * Check settings for empty entries and remove them.
   * @param currentRegistries Current list of schema registries.
   * @return Fitered list of schema registries.
   */
  public String[] checkRegistries(String[] currentRegistries) {
    List<String> allRegistries = new ArrayList<>();
    for (String schemaRegistry : currentRegistries){
      if (!schemaRegistry.trim().isEmpty()) { 
       allRegistries.add(schemaRegistry);
      }
    }
    String[] array = allRegistries.toArray(new String[0]);
    return array;
  }

  public static void main(String[] args) {
    ApplicationContext ctx = SpringApplication.run(Application.class, args);
    System.out.println("Spring is running!");
  }

}
