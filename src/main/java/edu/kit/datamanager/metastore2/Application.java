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
import edu.kit.datamanager.configuration.SearchConfiguration;
import edu.kit.datamanager.entities.messaging.IAMQPSubmittable;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.configuration.OaiPmhConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.repo.configuration.DateBasedStorageProperties;
import edu.kit.datamanager.repo.configuration.IdBasedStorageProperties;
import edu.kit.datamanager.repo.configuration.StorageServiceProperties;
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
import edu.kit.datamanager.security.filter.KeycloakJwtProperties;
import edu.kit.datamanager.security.filter.KeycloakTokenFilter;
import edu.kit.datamanager.security.filter.KeycloakTokenValidator;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.RabbitMQMessagingService;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Main class starting spring boot service of MetaStore.
 */
@SpringBootApplication
@EnableScheduling
@EntityScan("edu.kit.datamanager")
@EnableJpaRepositories("edu.kit.datamanager")
@ComponentScan({"edu.kit.datamanager"})
public class Application {

  private static final String DEFAULT = "simple";
  private static final String DEFAULT_VERSIONING = DEFAULT;
  private static final String DEFAULT_STORAGE = DEFAULT;
  private static final String LIST_ITEM = ".... '{}'";

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  @Autowired
  private Javers javers;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private List<IRepoVersioningService> versioningServices;
  @Autowired
  private List<IRepoStorageService>storageServices;
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  @Autowired
  private IDataRecordDao dataRecordDao;
  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private IUrl2PathDao url2PathDao;
  @Autowired
  private IMetadataFormatDao metadataFormatDao;
  @Autowired
  private List<IValidator> validators;

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

  @Bean
  public StorageServiceProperties storageServiceProperties() {
    return new StorageServiceProperties();
  }

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
  @ConditionalOnProperty(prefix = "repo.messaging", name = "enabled", havingValue = "true")
  public IMessagingService messagingService() {
    LOG.trace("LOAD RabbitMQ");
    return new RabbitMQMessagingService();
  }

  @Bean(name = "messagingService")
  @ConditionalOnProperty(prefix = "repo.messaging", name = "enabled", havingValue = "false")
  public IMessagingService dummyMessagingService() {
    LOG.trace("LOAD DUMMY RabbitMQ");
    return new IMessagingService() {
      @Override
      public void send(IAMQPSubmittable iamqps) {
        LOG.trace("RabbitMQ send dummy");
      }

      @Override
      public Health health() {
        LOG.trace("RabbitMQ health dummy");
        return new Health.Builder().up().build();
      }
    };
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
  @ConfigurationProperties("repo")
  public ApplicationProperties applicationProperties() {
    return new ApplicationProperties();
  }

  @Bean
  @ConfigurationProperties("repo.search")
  @ConditionalOnProperty(prefix = "repo.search", name = "enabled", havingValue = "true")
  public SearchConfiguration searchConfiguration() {
    return new SearchConfiguration();
  }

  @Bean
  public KeycloakJwtProperties keycloakProperties() {
    return new KeycloakJwtProperties();
  }

  @Bean
  @ConditionalOnProperty(
          value = "repo.auth.enabled",
          havingValue = "true",
          matchIfMissing = false)
  public KeycloakTokenFilter keycloaktokenFilterBean() throws Exception {
    return new KeycloakTokenFilter(KeycloakTokenValidator.builder()
            .readTimeout(keycloakProperties().getReadTimeoutms())
            .connectTimeout(keycloakProperties().getConnectTimeoutms())
            .sizeLimit(keycloakProperties().getSizeLimit())
            .jwtLocalSecret(applicationProperties().getJwtSecret())
            .build(keycloakProperties().getJwkUrl(), keycloakProperties().getResource(), keycloakProperties().getJwtClaim()));
  }

  @Bean
  public MetastoreConfiguration schemaConfig() {

    IAuditService<DataResource> auditServiceDataResource;
    IAuditService<ContentInformation> contentAuditService;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(this.applicationProperties.getSchemaFolder());
    rbc.setAuthEnabled(this.applicationProperties.isAuthEnabled());
    rbc.setJwtSecret(this.applicationProperties.getJwtSecret());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(schemaResourceService());
    rbc.setContentInformationService(schemaInformationService());
    rbc.setEventPublisher(eventPublisher);
    LOG.trace("Looking for versioningServices....");
    for (IRepoVersioningService versioningService : this.versioningServices) {
      LOG.trace(LIST_ITEM, versioningService.getServiceName());
      if (Objects.equals(versioningService.getServiceName(), DEFAULT_VERSIONING)) {
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    LOG.trace("Looking for storageServices....");
    for (IRepoStorageService storageService : this.storageServices) {
      LOG.trace(LIST_ITEM, storageService.getServiceName());
      if (Objects.equals(storageService.getServiceName(), DEFAULT_STORAGE)) {
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
    schemaResourceService().configure(rbc);
    schemaInformationService().configure(rbc);
    rbc.setAuditService(auditServiceDataResource);
    rbc.setMaxJaversScope(this.applicationProperties.getMaxJaversScope());
    rbc.setSchemaRegistries(checkRegistries(applicationProperties.getSchemaRegistries()));
    rbc.setValidators(validators);
    MetadataRecordUtil.setSchemaConfig(rbc);
    MetadataRecordUtil.setDataRecordDao(dataRecordDao);
    MetadataSchemaRecordUtil.setSchemaRecordDao(schemaRecordDao);
    MetadataSchemaRecordUtil.setMetadataFormatDao(metadataFormatDao);
    MetadataSchemaRecordUtil.setUrl2PathDao(url2PathDao);
    MetadataSchemaRecordUtil.setDataRecordDao(dataRecordDao);
    DataResourceRecordUtil.setDataRecordDao(dataRecordDao);
    DataResourceRecordUtil.setDataResourceDao(dataResourceDao);
    DataResourceRecordUtil.setMetadataFormatDao(metadataFormatDao);
    DataResourceRecordUtil.setSchemaRecordDao(schemaRecordDao);
    DataResourceRecordUtil.setSchemaConfig(rbc);

    fixBasePath(rbc);

    printSettings(rbc);
    LOG.trace("Content audit service: '{}'", contentAuditService);

    return rbc;
  }

  @Bean
  public MetastoreConfiguration metadataConfig() {

    IAuditService<DataResource> auditServiceDataResource;
    IAuditService<ContentInformation> contentAuditService;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(applicationProperties.getMetadataFolder());
    rbc.setAuthEnabled(this.applicationProperties.isAuthEnabled());
    rbc.setJwtSecret(this.applicationProperties.getJwtSecret());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(dataResourceService());
    rbc.setContentInformationService(contentInformationService());
    rbc.setEventPublisher(eventPublisher);
    LOG.trace("Looking for versioningServices....");
    for (IRepoVersioningService versioningService : this.versioningServices) {
      LOG.trace(LIST_ITEM, versioningService.getServiceName());
      if (Objects.equals(versioningService.getServiceName(), DEFAULT_VERSIONING)) {
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    LOG.trace("Looking for storageService '{}'....", this.applicationProperties.getStoragePattern());
    for (IRepoStorageService storageService : this.storageServices) {
      LOG.trace(LIST_ITEM, storageService.getServiceName());
      if (Objects.equals(storageService.getServiceName(), DEFAULT_STORAGE)) {
        rbc.setStorageService(storageService); // Should be used as default
      }
      if (this.applicationProperties.getStoragePattern().equals(storageService.getServiceName())) {
        LOG.trace("Configure '{}' with '{}'", storageService.getServiceName(), storageServiceProperties());
        storageService.configure(storageServiceProperties());
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
    dataResourceService().configure(rbc);
    contentInformationService().configure(rbc);
    rbc.setAuditService(auditServiceDataResource);
    rbc.setMaxJaversScope(this.applicationProperties.getMaxJaversScope());
    rbc.setSchemaRegistries(checkRegistries(applicationProperties.getSchemaRegistries()));
    rbc.setValidators(validators);
    
    fixBasePath(rbc);

    printSettings(rbc);
    LOG.trace("Content audit service: '{}'", contentAuditService);

    return rbc;
  }

  /**
   * Print current settings for repository
   *
   * @param config Settings.
   */
  public void printSettings(MetastoreConfiguration config) {
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", config);
    LOG.info("------------------------------------------------------");
    LOG.info("Versioning service: {}", config.getVersioningService().getServiceName());
    LOG.info("Storage service: {}", config.getStorageService().getServiceName());
    LOG.info("Basepath metadata repository: {}", config.getBasepath().toString());
    int noOfSchemaRegistries = config.getSchemaRegistries().size();
    LOG.info("Number of registered external schema registries: {}", noOfSchemaRegistries);
    for (int index1 = 0; index1 < noOfSchemaRegistries; index1++) {
      LOG.info("Schema registry '{}': {}", index1 + 1, config.getSchemaRegistries().get(index1));
    }

  }

  /**
   * Check settings for empty entries and remove them.
   *
   * @param currentRegistries Current list of schema registries.
   * @return Fitered list of schema registries.
   */
  public List<String> checkRegistries(List<String> currentRegistries) {
    List<String> allRegistries = new ArrayList<>();
    for (String schemaRegistry : currentRegistries) {
      if (!schemaRegistry.trim().isEmpty()) {
        allRegistries.add(schemaRegistry);
      }
    }
    return allRegistries;
  }

  /**
   * Fix base path on Windows system due to missing drive in case of relative
   * paths.
   *
   * @param config Configuration holding setting of repository.
   */
  private void fixBasePath(MetastoreConfiguration config) {
    String basePath = config.getBasepath().toString();
    LOG.trace("fixBasePath: '{}'", basePath);
    try {
      basePath = MetadataSchemaRecordUtil.fixRelativeURI(basePath);
      LOG.trace("fixBasePath: --> '{}'", basePath);
      config.setBasepath(URI.create(basePath).toURL());
    } catch (MalformedURLException ex) {
      LOG.error("Error fixing base path '{}'", basePath);
      LOG.error("Invalid base path!", ex);
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
    LOG.info("Spring is running!");
  }

}
