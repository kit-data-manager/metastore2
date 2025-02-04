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

import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.dao.spec.dataresource.StateSpecification;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class contains 1 runner:
 * <ul><li>Runner for purging schema/metadata documents and linked database entries.
 * </li></ul>
 */
@Component
@Transactional
public class PurgeRunner {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(PurgeRunner.class);

  /**
   * Instande of metadata reository.
   *
   */
  @Autowired
  private MetastoreConfiguration metadataConfig;

  /**
   * Remove all database entries regarding given id. Migrate metadata of schema
   * document from version 1 to version 2 and store Also remove file(s) from
   * disc.
   *
   * @param purgeIds ID of the resource.
   */
  public void removeResources(Set<String> purgeIds) {
    LOG.info("Remove resource with ids: '{}'", purgeIds);
    int entriesPerPage = 2;
    int page = 0;
    Pageable pgbl = PageRequest.of(page, entriesPerPage);
    boolean purgeAll = purgeIds.contains("all");
    if (purgeAll) {
      LOG.info("Purge all resources with state 'GONE'.");
    }
    // Looking for removed data resources.
    Specification<DataResource> spec = DataResourceRecordUtil.findByState(null, DataResource.State.GONE);
    LOG.debug("Performing query for records.");
    Page<DataResource> queryDataResources;
    boolean incrementPage = true;
    do {
      queryDataResources = DataResourceRecordUtil.queryDataResources(spec, pgbl);
      LOG.trace("Found '{}' of '{}' resources marked as 'GONE'. Page: '{}', ", queryDataResources.getSize(), queryDataResources.getTotalElements(), queryDataResources.getPageable());
      LOG.trace("Pgbl: '{}' Page: '{}', pageno: '{}' ", queryDataResources.getPageable(), pgbl, page);
      for (DataResource dataResourceToRemove : queryDataResources.getContent()) {
        LOG.trace("PurgeIds: '{}'", purgeIds);
        LOG.trace("Current ID: '{}'", dataResourceToRemove.getId());
        if (purgeIds.contains(dataResourceToRemove.getId()) || purgeAll) {
          LOG.trace("Clean up data resource with id '{}'.", dataResourceToRemove.getId());
          DataResource copyDataResource = DataResourceUtils.copyDataResource(dataResourceToRemove);

          DataResourceRecordUtil.cleanUpDataResource(copyDataResource);
          incrementPage = false;
        }
      }
      if (incrementPage) {
        page++;
        pgbl = PageRequest.of(page, entriesPerPage);
      }
      incrementPage = true;
    } while (page < queryDataResources.getTotalPages());
  }
}
