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
import edu.kit.datamanager.metastore2.dto.LandingPageModel;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
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
@RequestMapping(value = "/landingpage")
@Tag(name = "Landing Page")
@Schema(description = "Landing page for all digital objects stored in this repo.")
public class LandingPageControllerImpl implements ILandingPageController {

  private static final Logger LOG = LoggerFactory.getLogger(LandingPageControllerImpl.class);

  @Autowired
  private final ILinkedMetadataRecordDao metadataRecordDao;

  private final MetastoreConfiguration metadataConfig;
  @Autowired
  private final IDataResourceDao dataResourceDao;

  /**
   * Optional messagingService bean may or may not be available, depending on a
   * service's configuration. If messaging capabilities are disabled, this bean
   * should be not available. In that case, messages are only logged.
   */
  @Autowired
  private Optional<IMessagingService> messagingService;

  private final String guestToken;

  /**
   * Constructor for metadata documents controller.
   *
   * @param applicationProperties Configuration for controller.
   * @param metadataConfig Configuration for metadata documents repository.
   * @param metadataRecordDao DAO for metadata records.
   * @param dataResourceDao DAO for data resources.
   */
  public LandingPageControllerImpl(ApplicationProperties applicationProperties,
          MetastoreConfiguration metadataConfig,
          ILinkedMetadataRecordDao metadataRecordDao,
          IDataResourceDao dataResourceDao) {
    this.metadataConfig = metadataConfig;
    this.metadataRecordDao = metadataRecordDao;
    this.dataResourceDao = dataResourceDao;
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", this.metadataConfig);
    LOG.info("------------------------------------------------------");
    LOG.trace("Create guest token");
    guestToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("guest", RepoUserRole.GUEST).
            addSimpleClaim("email", "metastore@localhost").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());
    MetadataRecordUtil.setToken(guestToken);
  }

  @Override
  public ModelAndView getLandingPageOfId(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing Landing page for repo.... with ({}, {}).", id, version);

    LandingPageModel request = LandingPageModel.builder()
            .information("Information about '" + id + "' and version '" + version + "'.")
            .build();
   LOG.trace("Model: '{}'", request);

    ModelAndView model = new ModelAndView("landingpage");
    model.addObject("request", request);
    return model;
  }

}
