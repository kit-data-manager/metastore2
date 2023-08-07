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
package edu.kit.datamanager.metastore2.web.impl;

import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.dto.LandingPageModel;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.ILandingPageController;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.service.IMessagingService;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for metadata documents.
 */
@Controller
@RequestMapping(value = "/api/v1/landingpage")
@Tag(name = "Landing Page")
@Schema(description = "Landing page for all digital objects stored in this repo.")
public class LandingPageControllerImpl implements ILandingPageController {

  private static final Logger LOG = LoggerFactory.getLogger(LandingPageControllerImpl.class);

  private final MetastoreConfiguration metadataConfig;

  private final MetastoreConfiguration schemaConfig;
  
  private static final String PLACEHOLDER_ID = "$(id)";
  private static final String PLACEHOLDER_VERSION = "$(version)";

  /**
   * Constructor for metadata documents controller.
   *
   * @param applicationProperties Configuration for controller.
   * @param metadataConfig Configuration for metadata documents repository.
   * @param schemaConfig Configuration for schema documents repository.
   * @param metadataRecordDao DAO for metadata records.
   * @param dataResourceDao DAO for data resources.
   */
  public LandingPageControllerImpl(ApplicationProperties applicationProperties,
          MetastoreConfiguration metadataConfig,
          MetastoreConfiguration schemaConfig) {
    this.metadataConfig = metadataConfig;
    this.schemaConfig = schemaConfig;
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", this.metadataConfig);
    LOG.info("------------------------------------------------------");
  }

  @Override
  public ModelAndView getLandingPageOfId(
          @RequestParam(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing Landing page for repo.... with ({}, {}).", id, version);
    String redirectUrl = null;
    try {
      MetadataRecord recordByIdAndVersion = MetadataRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version);
    id = recordByIdAndVersion.getId();
    version = recordByIdAndVersion.getRecordVersion();
    redirectUrl = metadataConfig.getLandingPage();
    } catch (Throwable tw) {
      // No metadata document found? 
      // Search for a appropriate schema...
      MetadataSchemaRecord recordByIdAndVersion = MetadataSchemaRecordUtil.getRecordByIdAndVersion(schemaConfig, id, version);
     id = recordByIdAndVersion.getSchemaId();
     version = recordByIdAndVersion.getSchemaVersion();
    redirectUrl = schemaConfig.getLandingPage();
    }
    redirectUrl = redirectUrl.replace(PLACEHOLDER_ID, id);
    redirectUrl = "redirect:" + redirectUrl.replace(PLACEHOLDER_VERSION, version.toString());
    
    LOG.trace("Redirect to '{}'", redirectUrl);
    
    return new ModelAndView(redirectUrl);
  }

}
