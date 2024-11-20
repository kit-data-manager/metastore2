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
import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.configuration.SearchConfiguration;
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
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImplV2;
import edu.kit.datamanager.metastore2.web.impl.SchemaRegistryControllerImplV2;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.StateSpecification;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.security.filter.JwtAuthenticationToken;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.LogfileMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;

/**
 * This class contains 2 runners:
 * <ul><li>Runner for indexing all metadata documents of given schemas Arguments
 * have to start with at least 'reindex' followed by all indices which have to
 * be reindexed. If no indices are given all indices will be reindexed.</li>
 * <li>Runner for migrating dataresources from version 1 to version2.
 */
@Component
public class ElasticIndexerRunner implements CommandLineRunner {

  /**
   * ***************************************************************************
   * Parameter for migrating MetaStore version 1.x to version 2.x This should be
   * executed only once.
   * ***************************************************************************
   */
  /**
   * Start migration to version 2
   */
  @Parameter(names = {"--migrate2DataCite"}, description = "Migrate database from version 1.X to 2.X.")
  boolean doMigration2DataCite;
  /**
   * Start migration to version 2
   */
  @Parameter(names = {"--prefixIndices", "-p"}, description = "Prefix used for the indices inside elastic.")
  String prefixIndices;

  /**
   * ***************************************************************************
   * Parameters for reindexing elasticsearch. This should be executed only once.
   * ***************************************************************************
   */
  /**
   * Start reindexing...
   */
  @Parameter(names = {"--reindex"}, description = "Elasticsearch index should be build from existing documents.")
  boolean updateIndex;
  /**
   * Restrict reindexing to provided indices only.
   */
  @Parameter(names = {"--indices", "-i"}, description = "Only for given indices (comma separated) or all indices if not present.")
  Set<String> indices;
  /**
   * Restrict reindexing to dataresources new than given date.
   */
  @Parameter(names = {"--updateDate", "-u"}, description = "Starting reindexing only for documents updated at earliest on update date.")
  Date updateDate;
  /**
   * Determine the baseUrl of the service.
   */
  private String baseUrl;
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ElasticIndexerRunner.class);

  /**
   * DAO for all schema records.
   */
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  /**
   * DAO for all data records.
   */
  @Autowired
  private IDataRecordDao dataRecordDao;
  /**
   * DAO for linking URLS to files and format.
   */
  @Autowired
  private IUrl2PathDao url2PathDao;
  /**
   * Instance of schema repository.
   */
  @Autowired
  private MetastoreConfiguration schemaConfig;

  /**
   * Optional messagingService bean may or may not be available, depending on a
   * service's configuration. If messaging capabilities are disabled, this bean
   * should be not available. In that case, messages are only logged.
   */
  @Autowired
  private Optional<IMessagingService> messagingService;
  @Autowired
  private Migration2V2Runner migrationTool;

  @Autowired
  private SearchConfiguration searchConfiguration;

  /**
   * Start runner for actions before starting service.
   *
   * @param args Arguments for the runner.
   * @throws Exception Something went wrong.
   */
  @Override
  @SuppressWarnings({"StringSplitter", "JavaUtilDate"})
  public void run(String... args) throws Exception {
    // Set defaults for cli arguments.
    prefixIndices = "metastore-";
    updateDate = new Date(0);
    indices = new HashSet<>();
    
    JCommander argueParser = JCommander.newBuilder()
            .addObject(this)
            .build();
    try {
      LOG.trace("Parse arguments: '{}'", (Object)args);
      argueParser.parse(args);
      LOG.trace("doMigration2DataCite: '{}'", doMigration2DataCite);
      LOG.trace("PrefixIndices: '{}'", prefixIndices);
      LOG.trace("Update index: '{}'", updateIndex);
      LOG.trace("update date: '{}'", updateDate.toString());
      LOG.trace("Indices: '{}'", indices);
      LOG.trace("Find all schemas...");
      List<SchemaRecord> findAllSchemas = schemaRecordDao.findAll(PageRequest.of(0, 1)).getContent();
      if (!findAllSchemas.isEmpty()) {
        // There is at least one schema.
        // Try to fetch baseURL from this
        SchemaRecord get = findAllSchemas.get(0);
        Url2Path findByPath = url2PathDao.findByPath(get.getSchemaDocumentUri()).get(0);
        baseUrl = findByPath.getUrl().split("/api/v1/schema")[0];
        LOG.trace("Found baseUrl: '{}'", baseUrl);
        migrationTool.setBaseUrl(baseUrl);
        DataResourceRecordUtil.setBaseUrl(baseUrl);
      }
      if (updateIndex) {
        updateElasticsearchIndex();
      }
      if (doMigration2DataCite) {
        migrateToVersion2();
      }
    } catch (Exception ex) {
      LOG.error("Error while executing runner!", ex);
      argueParser.usage();
      System.exit(0);
    }
  }

  /**
   * Start runner to reindex dataresources according to the given parameters.
   *
   * @throws InterruptedException Something went wrong.
   */
  private void updateElasticsearchIndex() throws InterruptedException {
    LOG.info("Start ElasticIndexer Runner for indices '{}' and update date '{}'", indices, updateDate);
    LOG.info("No of schemas: '{}'", schemaRecordDao.count());
    // Try to determine URL of repository

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

    LOG.trace("Finished ElasticIndexerRunner!");
  }

  /**
   * Determine all indices if an empty set is provided. Otherwise return
   * provided set without any change.
   *
   * @param indices Indices which should be reindexed.
   */
  private void determineIndices(Set<String> indices) {
    if (indices.isEmpty()) {
      LOG.info("Reindex all indices!");
      // Search for all indices...
      // Build Specification
      ResourceType resourceType = ResourceType.createResourceType(DataResourceRecordUtil.SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL);
      Specification<DataResource> spec = ResourceTypeSpec.toSpecification(resourceType);
      // Add authentication if enabled
      if (updateDate != null) {
        spec = spec.and(LastUpdateSpecification.toSpecification(updateDate.toInstant(), null));
      }
      // Hide revoked and gone data resources. 
      DataResource.State[] states = {DataResource.State.FIXED, DataResource.State.VOLATILE};
      List<DataResource.State> stateList = Arrays.asList(states);
      spec = spec.and(StateSpecification.toSpecification(stateList));
      int entriesPerPage = 20;
      int page = 0;
      LOG.debug("Performing query for records.");
      Pageable pgbl = PageRequest.of(page, entriesPerPage);
      Page<DataResource> records = DataResourceRecordUtil.queryDataResources(spec, pgbl);
      int noOfEntries = records.getNumberOfElements();
      int noOfPages = records.getTotalPages();

      LOG.debug("Find '{}' schemas!", noOfEntries);
      // add also the schema registered in the schema registry
      for (page = 0; page < noOfPages; page++) {
        for (DataResource schema : records.getContent()) {
          indices.add(schema.getId());
        }
      }
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
          LOG.trace("Sending CREATE event (alternativeSchemaId: '{}').", index);
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
    String metadataIdWithVersion = baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class).getMetadataDocumentById(dataRecord.getMetadataId(), dataRecord.getVersion(), null, null)).toUri();
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
    schemaUrl = baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SchemaRegistryControllerImplV2.class).getSchemaDocumentById(dataRecord.getSchemaId(), dataRecord.getVersion(), null, null)).toUri();
    return schemaUrl;
  }

  /**
   * Migrate all data resources from version 1 to version 2.
   *
   * @throws InterruptedException Process was interrupted.
   */
  private void migrateToVersion2() throws InterruptedException {
    LOG.info("Start Migrate2DataCite Runner for migrating database from version 1 to version 2.");
    // Set adminitrative rights for reading.
    JwtAuthenticationToken jwtAuthenticationToken = JwtBuilder.createServiceToken("migrationTool", RepoServiceRole.SERVICE_READ).getJwtAuthenticationToken(schemaConfig.getJwtSecret());
    SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
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
    Specification<DataResource> spec;
    spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE, ResourceType.TYPE_GENERAL.DATASET));
    int pageNumber = 0;
    int pageSize = 1;
    Pageable pgbl = PageRequest.of(pageNumber, pageSize);
    Page<DataResource> queryDataResources;
    do {
      queryDataResources = queryDataResources(spec, pgbl);
      for (DataResource schema : queryDataResources.getContent()) {
        migrateSchemaToDataciteVersion2(schema);
        removeAllIndexedEntries(schema.getId());
      }
    } while (queryDataResources.getTotalPages() > 1);
  }

  /**
   * Remove all indexed entries (indexed with V1) for given schema. (If search is enabled)
   *
   * example: POST /metastore-schemaid/_delete_by_query { "query": { "range": {
   * "metadataRecord.schemaVersion": { "gte": 1 } } } }
   *
   *
   * @param schemaId schema
   */
  private void removeAllIndexedEntries(String schemaId) {
    // Delete all entries in elastic (if available)
    LOG.trace("Remove all indexed entries for '{}'...", schemaId);
    if (searchConfiguration.isSearchEnabled()) {
      String prefix4Indices = prefixIndices;

      LOG.trace(searchConfiguration.toString());
      LOG.trace("Remove all entries for index: '{}'", prefix4Indices + schemaId);
      SimpleServiceClient client = SimpleServiceClient.create(searchConfiguration.getUrl() + "/" + prefix4Indices + schemaId + "/_delete_by_query");
      String query = "{ \"query\": { \"range\" : { \"metadataRecord.schemaVersion\" : { \"gte\" : 1} } } }";
      LOG.trace("Query: '{}'", query);                       
      client.withContentType(MediaType.APPLICATION_JSON);
      try {
        String postResource = client.postResource(query, String.class);
        LOG.trace(postResource);
      } catch (HttpClientErrorException hcee) {
        LOG.error(hcee.getMessage());
      }
    }
  }

  /**
   * Migrate dataresources of schemas using version 1 to version 2.
   *
   * @param schema Current version of schema document.
   */
  private void migrateSchemaToDataciteVersion2(DataResource schema) {
    long version = Long.parseLong(schema.getVersion());
    String id = schema.getId();
    // Migrate all versions of schema.
    for (long versionNo = 1; versionNo <= version; versionNo++) {
      migrationTool.saveSchema(id, versionNo);
    }
    LOG.info("Migration for schema document with ID: '{}', finished! No of versions: '{}'", id, version);
  }

  /**
   * Migrate dataresources of metadata documents from version 1 to version 2.
   */
  private void migrateAllMetadataDocumentsToDataciteVersion2() {
    Specification<DataResource> spec;
    spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataRecord.RESOURCE_TYPE, ResourceType.TYPE_GENERAL.DATASET));
    int pageNumber = 0;
    int pageSize = 10;
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
   * Migrate all versions of a dataresource of metadata documents from version 1
   * to version 2.
   *
   * @param metadataDocument Current version of metadata document.
   */
  private void migrateMetadataDocumentsToDataciteVersion2(DataResource metadataDocument) {
    long version = Long.parseLong(metadataDocument.getVersion());
    String id = metadataDocument.getId();

    DataResource copy = migrationTool.getCopyOfDataResource(metadataDocument);

    // Get resource type of schema....
    String format = null;
    RelatedIdentifier identifier = DataResourceRecordUtil.getRelatedIdentifier(copy, DataResourceRecordUtil.RELATED_NEW_VERSION_OF);
    if (identifier != null) {
      String schemaUrl = identifier.getValue();
      Optional<Url2Path> findByUrl = url2PathDao.findByUrl(schemaUrl);
      LOG.trace("Found entry for schema:  {}", findByUrl.get().toString());
      format = findByUrl.get().getType().toString();
    }
    // Migrate all versions of data resource.
    for (int versionNo = 1; versionNo <= version; versionNo++) {
      DataResource saveMetadata = migrationTool.saveMetadata(id, versionNo, format);
      if (versionNo == 1) {
        LOG.trace("Sending CREATE event.");
        messagingService.orElse(new LogfileMessagingService()).
                send(MetadataResourceMessage.factoryCreateMetadataMessage(saveMetadata, AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
      } else {
        LOG.trace("Sending UPDATE event.");
        messagingService.orElse(new LogfileMessagingService()).
                send(MetadataResourceMessage.factoryUpdateMetadataMessage(saveMetadata, AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));
      }
    }
    LOG.info("Migration for metadata document with ID: '{}', finished! No of versions: '{}'", id, version);
  }
}
