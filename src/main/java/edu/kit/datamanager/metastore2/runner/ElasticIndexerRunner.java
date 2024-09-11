/*
 * Copyright 2022 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.runner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import edu.kit.datamanager.entities.RepoServiceRole;
import edu.kit.datamanager.entities.messaging.MetadataResourceMessage;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.*;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import static edu.kit.datamanager.metastore2.util.DataResourceRecordUtil.METADATA_SUFFIX;
import static edu.kit.datamanager.metastore2.util.DataResourceRecordUtil.SCHEMA_SUFFIX;
import static edu.kit.datamanager.metastore2.util.DataResourceRecordUtil.queryDataResources;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImpl;
import edu.kit.datamanager.metastore2.web.impl.SchemaRegistryControllerImpl;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.security.filter.JwtAuthenticationToken;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.LogfileMessagingService;
import edu.kit.datamanager.util.ControllerUtils;
import edu.kit.datamanager.util.JwtBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Class for indexing all metadata documents of given schemas Arguments have to
 * start with at least 'reindex' followed by all indices which have to be
 * reindexed. If no indices are given all indices will be reindexed.
 */
@Component
@Transactional
public class ElasticIndexerRunner implements CommandLineRunner {

  /**
   * ***************************************************************************
   * Parameter for migrating MetaStore version 1.x to version 2.x This should be
   * executed only once.
   * ***************************************************************************
   */
  @Parameter(names = {"--migrate2DataCite"}, description = "Migrate database from version 1.X to 2.X.")
  boolean doMigration2DataCite;

  /**
   * ***************************************************************************
   * Parameters for reindexing elasticsearch. This should be executed only once.
   * ***************************************************************************
   */
  @Parameter(names = {"--reindex"}, description = "Elasticsearch index should be build from existing documents.")
  boolean updateIndex;
  @Parameter(names = {"--indices", "-i"}, description = "Only for given indices (comma separated) or all indices if not present.")
  Set<String> indices;
  @Parameter(names = {"--updateDate", "-u"}, description = "Starting reindexing only for documents updated at earliest on update date.")
  Date updateDate;

  private static final Logger LOG = LoggerFactory.getLogger(ElasticIndexerRunner.class);
  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  @Autowired
  private IDataRecordDao dataRecordDao;
  @Autowired
  private MetastoreConfiguration schemaConfig;
  @Autowired
  private MetastoreConfiguration metadataConfig;
  @Autowired
  private IUrl2PathDao url2PathDao;

  /**
   * Optional messagingService bean may or may not be available, depending on a
   * service's configuration. If messaging capabilities are disabled, this bean
   * should be not available. In that case, messages are only logged.
   */
  @Autowired
  private Optional<IMessagingService> messagingService;

  @Override
  @SuppressWarnings({"StringSplitter", "JavaUtilDate"})
  public void run(String... args) throws Exception {
    JCommander argueParser = JCommander.newBuilder()
            .addObject(this)
            .build();
    try {
      argueParser.parse(args);
      if (updateIndex) {
        if (updateDate == null) {
          updateDate = new Date(0);
        }
        if (indices == null) {
          indices = new HashSet<>();
        }
        updateElasticsearchIndex();
      }
      if (doMigration2DataCite) {
        // Set adminitrative rights for reading.
        JwtAuthenticationToken jwtAuthenticationToken = JwtBuilder.createServiceToken("migrationTool", RepoServiceRole.SERVICE_READ).getJwtAuthenticationToken(schemaConfig.getJwtSecret());
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
        
        migrateToVersion2();
      }
    } catch (Exception ex) {
      LOG.error("Error while executing runner!", ex);
      argueParser.usage();
      System.exit(0);
    }
  }

  private void updateElasticsearchIndex() throws InterruptedException {
    LOG.info("Start ElasticIndexer Runner for indices '{}' and update date '{}'", indices, updateDate);
    LOG.info("No of schemas: '{}'", schemaRecordDao.count());
    // Try to determine URL of repository
    List<SchemaRecord> findAllSchemas = schemaRecordDao.findAll(PageRequest.of(0, 3)).getContent();
    if (!findAllSchemas.isEmpty()) {
      // There is at least one schema.
      // Try to fetch baseURL from this
      SchemaRecord get = findAllSchemas.get(0);
      Url2Path findByPath = url2PathDao.findByPath(get.getSchemaDocumentUri()).get(0);
      String baseUrl = findByPath.getUrl().split("/api/v1/schema")[0];
      LOG.trace("Found baseUrl: '{}'", baseUrl);

      determineIndices(indices);

      for (String index : indices) {
        LOG.info("Reindex '{}'", index);
        List<DataRecord> findBySchemaId = dataRecordDao.findBySchemaIdAndLastUpdateAfter(index, updateDate.toInstant());
        LOG.trace("Search for documents for schema '{}' and update date '{}'", index, updateDate);
        LOG.trace("No of documents: '{}'", findBySchemaId.size());
        for (DataRecord item : findBySchemaId) {
          MetadataRecord result = toMetadataRecord(item, baseUrl);
          LOG.trace("Sending CREATE event.");
          messagingService.orElse(new LogfileMessagingService()).
                  send(MetadataResourceMessage.factoryCreateMetadataMessage(result, this.getClass().toString(), ControllerUtils.getLocalHostname()));
        }
        indexAlternativeSchemaIds(index, baseUrl);
      }
      Thread.sleep(5000);
    }

    LOG.trace("Finished ElasticIndexerRunner!");
  }

  private void determineIndices(Set<String> indices) {
    if (indices.isEmpty()) {
      LOG.info("Reindex all indices!");
      long noOfEntries = url2PathDao.count();
      long entriesPerPage = 50;
      long page = 0;
      // add also the schema registered in the schema registry
      do {
        List<SchemaRecord> allSchemas = schemaRecordDao.findAll(PageRequest.of((int) page, (int) entriesPerPage)).getContent();
        LOG.trace("Add '{}' schemas of '{}'", allSchemas.size(), noOfEntries);
        for (SchemaRecord item : allSchemas) {
          indices.add(item.getSchemaIdWithoutVersion());
        }
        page++;
      } while (page * entriesPerPage < noOfEntries);
    }

  }

  private void indexAlternativeSchemaIds(String index, String baseUrl) {
    LOG.trace("Search for alternative schemaId (given as URL)");
    List<SchemaRecord> findSchemaBySchemaId = schemaRecordDao.findBySchemaIdStartsWithOrderByVersionDesc(index + "/");
    DataRecord templateRecord = new DataRecord();
    for (SchemaRecord debug : findSchemaBySchemaId) {
      templateRecord.setSchemaId(debug.getSchemaIdWithoutVersion());
      templateRecord.setSchemaVersion(debug.getVersion());
      List<Url2Path> findByPath1 = url2PathDao.findByPath(debug.getSchemaDocumentUri());
      for (Url2Path path : findByPath1) {
        LOG.trace("SchemaRecord: '{}'", debug);
        List<DataRecord> findBySchemaUrl = dataRecordDao.findBySchemaIdAndLastUpdateAfter(path.getUrl(), updateDate.toInstant());
        LOG.trace("Search for documents for schema '{}' and update date '{}'", path.getUrl(), updateDate);
        LOG.trace("No of documents: '{}'", findBySchemaUrl.size());
        for (DataRecord item : findBySchemaUrl) {
          templateRecord.setMetadataId(item.getMetadataId());
          templateRecord.setVersion(item.getVersion());
          MetadataRecord result = toMetadataRecord(templateRecord, baseUrl);
          LOG.trace("Sending CREATE event.");
          messagingService.orElse(new LogfileMessagingService()).
                  send(MetadataResourceMessage.factoryCreateMetadataMessage(result, this.getClass().toString(), ControllerUtils.getLocalHostname()));
        }
      }
    }

  }

  /**
   * Transform DataRecord to MetadataRecord.
   *
   * @param dataRecord DataRecord holding all information about metadata
   * document.
   * @param baseUrl Base URL for accessing service.
   * @return MetadataRecord of metadata document.
   */
  private MetadataRecord toMetadataRecord(DataRecord dataRecord, String baseUrl) {
    String metadataIdWithVersion = baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImpl.class).getMetadataDocumentById(dataRecord.getMetadataId(), dataRecord.getVersion(), null, null)).toUri();
    MetadataRecord returnValue = new MetadataRecord();
    returnValue.setId(dataRecord.getMetadataId());
    returnValue.setSchemaVersion(dataRecord.getSchemaVersion());
    returnValue.setRecordVersion(dataRecord.getVersion());
    returnValue.setMetadataDocumentUri(metadataIdWithVersion);
    returnValue.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(toSchemaUrl(dataRecord, baseUrl)));

    return returnValue;
  }

  /**
   * Transform schemaID to URL if it is an internal
   *
   * @param dataRecord DataRecord holding schemaID and schema version.
   * @param baseUrl Base URL for accessing service.
   * @return URL to Schema as String.
   */
  private String toSchemaUrl(DataRecord dataRecord, String baseUrl) {
    String schemaUrl;
    schemaUrl = baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SchemaRegistryControllerImpl.class).getSchemaDocumentById(dataRecord.getSchemaId(), dataRecord.getVersion(), null, null)).toUri();
    return schemaUrl;
  }

  private void migrateToVersion2() throws InterruptedException {
    LOG.info("Start Migrate2DataCite Runner for migrating database from version 1 to version 2.");
    // Try to determine URL of repository
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(METADATA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    spec.or(ResourceTypeSpec.toSpecification(ResourceType.createResourceType(SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL)));
    Pageable pgbl = PageRequest.of(0, 1);
    long totalElements = queryDataResources(spec, pgbl).getTotalElements();
    if (totalElements == 0) {
      // Migrate all schemas...
      migrateAllSchemasToDataciteVersion2();
      migrateAllMetadataDocumentsToDataciteVersion2();
      Thread.sleep(5000);
    }

    LOG.trace("Finished Migrate2DataCite!");
  }

  /**
   * Migrate dataresources of schemas using version 1 to version 2.
   */
  private void migrateAllSchemasToDataciteVersion2() {
    Specification spec;
    spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE, ResourceType.TYPE_GENERAL.DATASET));
    int pageNumber = 0;
    int pageSize = 1;
    Pageable pgbl = PageRequest.of(pageNumber, pageSize);
    Page<DataResource> queryDataResources;
    do {
      queryDataResources = queryDataResources(spec, pgbl);
      for (DataResource schema : queryDataResources.getContent()) {
        migrateSchemaToDataciteVersion2(schema);
      }
    } while (queryDataResources.getTotalPages() > 1);
  }

  /**
   * Migrate dataresources of schemas using version 1 to version 2.
   */
  private void migrateSchemaToDataciteVersion2(DataResource schema) {
    long version = Long.parseLong(schema.getVersion());
    String id = schema.getId();
    for (int versionNo = 1; versionNo <= version; versionNo++) {
      DataResource recordByIdAndVersion = DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, id, version);
      // Remove type from first title with type 'OTHER'
      for (Title title : recordByIdAndVersion.getTitles()) {
        if (title.getTitleType() == Title.TYPE.OTHER) {
          title.setTitleType(null);
          break;
        }
      }
      // Set resource type to  new definition of version 2 ('...'_Schema)
      ResourceType resourceType = recordByIdAndVersion.getResourceType();
      resourceType.setTypeGeneral(ResourceType.TYPE_GENERAL.MODEL);
      resourceType.setValue(recordByIdAndVersion.getFormats().iterator().next() + DataResourceRecordUtil.SCHEMA_SUFFIX);
      // Save migrated version
      dataResourceDao.save(recordByIdAndVersion);
    }
  }

  /**
   * Migrate dataresources of schemas using version 1 to version 2.
   */
  private void migrateAllMetadataDocumentsToDataciteVersion2() {
    Specification spec;
    spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataRecord.RESOURCE_TYPE, ResourceType.TYPE_GENERAL.DATASET));
    int pageNumber = 0;
    int pageSize = 1;
    Pageable pgbl = PageRequest.of(pageNumber, pageSize);
    Page<DataResource> queryDataResources;
    do {
      queryDataResources = queryDataResources(spec, pgbl);
      for (DataResource schema : queryDataResources.getContent()) {
        migrateMetadataDocumentsToDataciteVersion2(schema);
      }
    } while (queryDataResources.getTotalPages() > 1);
  }

  /**
   * Migrate dataresources of schemas using version 1 to version 2.
   */
  private void migrateMetadataDocumentsToDataciteVersion2(DataResource schema) {
    long version = Long.parseLong(schema.getVersion());
    String id = schema.getId();
    // Get resource type of schema....
    String format = null;
    for (RelatedIdentifier identifier : schema.getRelatedIdentifiers()) {
      if (identifier.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM) {
        String schemaUrl = identifier.getValue();
        Optional<Url2Path> findByUrl = url2PathDao.findByUrl(schemaUrl);
        LOG.trace("Found entry for schema:  {}", findByUrl.get().toString());
        format = findByUrl.get().getType().toString();
      }
    }
    for (int versionNo = 1; versionNo <= version; versionNo++) {
      DataResource recordByIdAndVersion = DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version);
      // Remove type from first title with type 'OTHER'
      for (Title title : recordByIdAndVersion.getTitles()) {
        if (title.getTitleType() == Title.TYPE.OTHER) {
          title.setTitleType(null);
          break;
        }
      }
      // Set resource type to  new definition of version 2 ('...'_Schema)
      ResourceType resourceType = recordByIdAndVersion.getResourceType();
      resourceType.setTypeGeneral(ResourceType.TYPE_GENERAL.MODEL);
      resourceType.setValue(format + DataResourceRecordUtil.METADATA_SUFFIX);
      // Save migrated version
      dataResourceDao.save(recordByIdAndVersion);
    }
  }
}
