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

import edu.kit.datamanager.entities.messaging.MetadataResourceMessage;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.DataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.metastore2.domain.Url2Path;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImpl;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.LogfileMessagingService;
import edu.kit.datamanager.util.ControllerUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

/**
 * Class for indexing all metadata documents of given schemas Arguments have to
 * start with at least 'reindex' followed by all indices which have to be
 * reindexed. If no indices are given all indices will be reindexed.
 */
@Component
public class ElasticIndexerRunner implements CommandLineRunner {

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
  public void run(String... args) throws Exception {
    Set<String> indices = new HashSet<>();
    for (String argument : args) {
      LOG.trace("Start ElasticIndexerRunner with param: '{}'.", argument);
    }
    if (args.length > 0) {
      if (args[0].equalsIgnoreCase("reindex")) {
        LOG.info("Start ElasticIndexer Runner");
        // Try to determine URL of repository
        List<SchemaRecord> findAll = schemaRecordDao.findAll();
        if (!findAll.isEmpty()) {
          // There is at least one schema.
          // Try to fetch baseURL from this
          SchemaRecord get = findAll.get(0);
          Url2Path findByPath = url2PathDao.findByPath(get.getSchemaDocumentUri()).get(0);
          String baseUrl = findByPath.getUrl().split("/api/v1/schema")[0];
          LOG.trace("Found baseUrl: '{}'", baseUrl);

          if (args.length > 1) {
            for (int index = 1; index < args.length; index++) {
              LOG.trace("Reindex '{}'", args[index]);
              indices.add(args[index]);
            }
          } else {
            LOG.info("Reindex all indices!");
            for (SchemaRecord item : schemaRecordDao.findAll()) {
              indices.add(item.getSchemaId());
            }
          }

          for (String index : indices) {
            LOG.info("Reindex '{}'", index);
            List<DataRecord> findBySchemaId = dataRecordDao.findBySchemaId(index);
            for (DataRecord item : findBySchemaId) {
              MetadataRecord result = toMetadataRecord(item, baseUrl);
              LOG.trace("Sending CREATE event.");
              messagingService.orElse(new LogfileMessagingService()).
                      send(MetadataResourceMessage.factoryCreateMetadataMessage(result, this.getClass().toString(), ControllerUtils.getLocalHostname()));
            }

          }
          Thread.sleep(5000);
        }
      }
      LOG.trace("Finished ElasticIndexerRunner!");
    }
  }

  private MetadataRecord toMetadataRecord(DataRecord record, String baseUrl) {
    MetadataRecord returnValue = new MetadataRecord();
    returnValue.setId(record.getMetadataId());
    returnValue.setMetadataDocumentUri(baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImpl.class).getMetadataDocumentById(record.getMetadataId(), record.getVersion(), null, null)).toUri().toString());
    returnValue.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(record.getSchemaId()));
    LOG.trace("MetadataRecord: '{}'", returnValue);
    return returnValue;
  }
}
