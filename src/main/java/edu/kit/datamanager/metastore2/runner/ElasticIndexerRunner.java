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
import edu.kit.datamanager.entities.messaging.MetadataResourceMessage;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.*;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImpl;
import edu.kit.datamanager.metastore2.web.impl.SchemaRegistryControllerImpl;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.LogfileMessagingService;
import edu.kit.datamanager.util.ControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Class for indexing all metadata documents of given schemas Arguments have to
 * start with at least 'reindex' followed by all indices which have to be
 * reindexed. If no indices are given all indices will be reindexed.
 */
@Component
public class ElasticIndexerRunner implements CommandLineRunner {

  @Parameter(names = {"--reindex"}, description = "Elasticsearch index should be build from existing documents.")
  boolean updateIndex;
  @Parameter(names = {"--indices", "-i"}, description = "Only for given indices (comma separated) or all indices if not present.")
  Set<String> indices;
  @Parameter(names = {"--updateDate", "-u"}, description = "Starting reindexing only for documents updated at earliest on update date.")
  Date updateDate;

  private static final Logger LOG = LoggerFactory.getLogger(ElasticIndexerRunner.class);
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  @Autowired
  private IDataRecordDao dataRecordDao;
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
      if (updateDate == null) {
        updateDate = new Date(0);
      }
      if (indices == null) {
        indices = new HashSet<>();
      }
    } catch (Exception ex) {
      argueParser.usage();
      System.exit(0);
    }
    if (updateIndex) {
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

        for (String index : indices) {
          LOG.info("Reindex '{}'", index);
          List<DataRecord> findBySchemaId = dataRecordDao.findBySchemaIdAndLastUpdateAfter(index, updateDate.toInstant());
          List<SchemaRecord> findSchemaBySchemaId = schemaRecordDao.findBySchemaIdStartsWithOrderByVersionDesc(index + "/");
          LOG.trace("Search for documents for schema '{}' and update date '{}'", index, updateDate);
          LOG.trace("No of documents: '{}'", findBySchemaId.size());
          for (DataRecord item : findBySchemaId) {
            MetadataRecord result = toMetadataRecord(item, baseUrl);
            LOG.trace("Sending CREATE event.");
            messagingService.orElse(new LogfileMessagingService()).
                    send(MetadataResourceMessage.factoryCreateMetadataMessage(result, this.getClass().toString(), ControllerUtils.getLocalHostname()));
          }
          LOG.trace("Search for alternative schemaId (given as URL)");
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
        Thread.sleep(5000);
      }

      LOG.trace("Finished ElasticIndexerRunner!");
    }
  }

  /**
   * Transform DataRecord to MetadataRecord.
   *
   * @param dataRecord DataRecord holding all information about metadata document.
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
}
