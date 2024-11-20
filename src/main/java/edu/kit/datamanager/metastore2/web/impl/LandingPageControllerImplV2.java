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

import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import static edu.kit.datamanager.metastore2.util.DataResourceRecordUtil.fixSchemaUrl;
import edu.kit.datamanager.metastore2.web.ILandingPageControllerV2;
import edu.kit.datamanager.repo.domain.DataResource;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for metadata documents.
 */
@Controller
@RequestMapping(value = "")
@Tag(name = "Landing Page")
@Schema(description = "Landing page for all digital objects stored in this repo.")
public class LandingPageControllerImplV2 implements ILandingPageControllerV2 {

  private static final Logger LOG = LoggerFactory.getLogger(LandingPageControllerImpl.class);

  private final MetastoreConfiguration metadataConfig;

  private final MetastoreConfiguration schemaConfig;

  /**
   * Constructor for metadata documents controller.
   *
   * @param applicationProperties Configuration for controller.
   * @param metadataConfig Configuration for metadata documents repository.
   * @param schemaConfig Configuration for schema documents repository.
   */
  public LandingPageControllerImplV2(ApplicationProperties applicationProperties,
          MetastoreConfiguration metadataConfig,
          MetastoreConfiguration schemaConfig) {
    this.metadataConfig = metadataConfig;
    this.schemaConfig = schemaConfig;
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", this.metadataConfig);
    LOG.info("------------------------------------------------------");
  }

  @Override
  public String getLandingPageOfSchemaWithId(@RequestParam(value = "schemaId") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr,
          Model model) {
    LOG.trace("Performing getLandingPageOfSchemaWithId({}, {}).", id, version);

    //if security is enabled, include principal in query
    LOG.debug("Performing  a query for records with given id.");
    DataResource recordByIdAndVersion = DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, id, version);
    List<DataResource> recordList = new ArrayList<>();
    recordList.add(recordByIdAndVersion);
    if (version == null) {
      long totalNoOfElements = Long.parseLong(recordByIdAndVersion.getVersion());
      for (long size = totalNoOfElements - 1; size > 0; size--) {
        recordList.add(DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, id, size));
      }
    }

    LOG.trace("Fix URL for all schema records");
    List<MetadataSchemaRecord> metadataList = new ArrayList<>();
    recordList.forEach(metadataRecord -> {
      MetadataSchemaRecord metadataSchemaRecord = DataResourceRecordUtil.migrateToMetadataSchemaRecordV2(schemaConfig, metadataRecord);
      metadataSchemaRecord.setSchemaDocumentUri(DataResourceRecordUtil.getSchemaDocumentUri(id, metadataSchemaRecord.getSchemaVersion()));
      metadataList.add(metadataSchemaRecord);
    });

    model.addAttribute("records", metadataList);

    return "schema-landing-page.html";
  }

  @Override
  public String getLandingPageOfMetadataDocumentWithId(@PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr,
          Model model
  ) {

    LOG.trace("Performing getLandingPageOfMetadataDocumentWithId({}, {}).", id, version);

    //if security is enabled, include principal in query
    LOG.debug("Performing  a query for all records with given id...");
    DataResource recordByIdAndVersion = DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version);
    List<DataResource> recordList = new ArrayList<>();

    recordList.add(recordByIdAndVersion);
    if (version == null) {
      long totalNoOfElements = Long.parseLong(recordByIdAndVersion.getVersion());
      for (long size = totalNoOfElements - 1; size > 0; size--) {
        recordList.add(DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, size));
      }
    }
    List<MetadataRecord> resultList = new ArrayList<>();
    for (DataResource item : recordList) {
      DataResourceRecordUtil.fixSchemaUrl(item);
      MetadataRecord metadataRecord = DataResourceRecordUtil.migrateToMetadataRecordV2(metadataConfig, item);
      
      resultList.add(metadataRecord);
    }


    model.addAttribute("type", recordList.get(0).getFormats().iterator().next());
    model.addAttribute("records", resultList);

    return "metadata-landing-page.html";
  }

}
