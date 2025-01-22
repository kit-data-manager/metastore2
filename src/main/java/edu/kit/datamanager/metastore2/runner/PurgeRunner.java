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

import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImplV2;
import edu.kit.datamanager.metastore2.web.impl.SchemaRegistryControllerImplV2;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;

import java.util.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class contains 2 runners:
 * <ul><li>Runner for indexing all metadata documents of given schemas Arguments
 * have to start with at least 'reindex' followed by all indices which have to
 * be reindexed. If no indices are given all indices will be reindexed.</li>
 * <li>Runner for migrating dataresources from version 1 to version2.
 */
@Component
@Transactional
public class PurgeRunner {

  /**
   * Determine the baseUrl of the service.
   */
  private String baseUrl;
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(PurgeRunner.class);
  /**
   * DAO for all data resources.
   */
  @Autowired
  private IDataResourceDao dataResourceDao;
  /**
   * Instance of schema repository.
   */
  @Autowired
  private MetastoreConfiguration schemaConfig;
  /**
   * Instande of metadata reository.
   *
   */
  @Autowired
  private MetastoreConfiguration metadataConfig;

  /**
   * Remove all database entries regarding given id. Migrate metadata of schema document from version 1 to version 2 and store
   * Also remove file(s) from disc.
   *
   * @param id ID of the resource.
   */
  public void removeResource(String id) {
    LOG.info("Remove resource with id: '{}'", id);
  }

  /**
   * Create a deep copy of a data resource instance.
   *
   * @param dataResource Data resource.
   * @return Deep copy of data resource.
   */
  public DataResource getCopyOfDataResource(DataResource dataResource) {
    DataResource copy = null;
    Optional<DataResource> origDataResource;
    Objects.requireNonNull(dataResource);
    Objects.requireNonNull(dataResource.getId());
    origDataResource = dataResourceDao.findById(dataResource.getId());
    if (origDataResource.isPresent()) {
      copy = DataResourceUtils.copyDataResource(origDataResource.get());
    } else {
      copy = DataResourceUtils.copyDataResource(dataResource);
    }
    return copy;
  }

  /**
   * Set base URL for accessing documents and records.
   *
   * @param baseUrl the baseUrl to set
   */
  public void setBaseUrl(String baseUrl) {
    LOG.trace("Set baseURL from '{}' to '{}'", this.baseUrl, baseUrl);
    this.baseUrl = baseUrl;
  }
}
