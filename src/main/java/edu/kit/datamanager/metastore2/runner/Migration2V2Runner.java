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
public class Migration2V2Runner {

  /**
   * Determine the baseUrl of the service.
   */
  private String baseUrl;
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(Migration2V2Runner.class);
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
   * Migrate metadata of schema document from version 1 to version 2 and store
   * new version in the database.
   *
   * @param id ID of the schema document.
   * @param version Version of the schema document.
   */
  public void saveSchema(String id, long version) {
    LOG.info("Migrate datacite for schema document with id: '{}' / version: '{}'", id, version);
    DataResource currentDataResource = DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, id, version);
    DataResource recordByIdAndVersion = DataResourceUtils.copyDataResource(currentDataResource);

    // Remove type from first title with type 'OTHER'
    removeTitleType(recordByIdAndVersion.getTitles());
    // Move PID from alternateIdentifier to identifier
    movePidFromAlternateToPrimaryIdentifier(recordByIdAndVersion);
    // Set resource type to  new definition of version 2 ('...'_Schema)
    ResourceType resourceType = recordByIdAndVersion.getResourceType();
    resourceType.setTypeGeneral(ResourceType.TYPE_GENERAL.MODEL);
    resourceType.setValue(recordByIdAndVersion.getFormats().iterator().next() + DataResourceRecordUtil.SCHEMA_SUFFIX);
    // Migrate relation type from 'isDerivedFrom' to 'hasMetadata'
    for (RelatedIdentifier item : recordByIdAndVersion.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM)) {
        item.setRelationType(DataResourceRecordUtil.RELATED_SCHEMA_TYPE);
      }
    }
    // Add provenance
    if (version > 1) {
      String schemaUrl = baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SchemaRegistryControllerImplV2.class).getSchemaDocumentById(id, version - 1l, null, null)).toString();
      RelatedIdentifier provenance = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_NEW_VERSION_OF, schemaUrl, null, null);
      recordByIdAndVersion.getRelatedIdentifiers().add(provenance);
      LOG.trace("Add provenance to datacite record: '{}'", schemaUrl);
    } else {
      long currentVersion = metadataConfig.getAuditService().getCurrentVersion(id);
      if (currentVersion == 1l) {
        // Create an additional version for JaVers if only one version exists.
        currentDataResource.setPublisher("migrationTool");
        metadataConfig.getAuditService().captureAuditInformation(currentDataResource, "Just4fun");
      }
    }
    // Save migrated version
    LOG.trace("Persisting created schema document resource.");
    DataResource migratedDataResource = dataResourceDao.save(recordByIdAndVersion);

    //Capture state change
    LOG.trace("Capturing audit information.");
    schemaConfig.getAuditService().captureAuditInformation(migratedDataResource, "migration2version2");

  }

  /**
   * Migrate metadata of metadata document from version 1 to version 2 and store
   * new version in the database.
   *
   * @param id ID of the metadata document.
   * @param version Version of the metadata document.
   * @param format Format of the metadata document. (XML/JSON)
   * @return Persisted data resource.
   */
  public DataResource saveMetadata(String id, long version, String format) {
    LOG.trace("Migrate datacite for metadata document with id: '{}' / version: '{}' and format: '{}'", id, version, format);
    DataResource currentDataResource = DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version);
    DataResource recordByIdAndVersion = DataResourceUtils.copyDataResource(currentDataResource);
    // Remove type from first title with type 'OTHER'
    removeTitleType(recordByIdAndVersion.getTitles());

    // Move PID from alternateIdentifier to identifier
    movePidFromAlternateToPrimaryIdentifier(recordByIdAndVersion);
    // Set resource type to  new definition of version 2 ('...'_Metadata)
    ResourceType resourceType = recordByIdAndVersion.getResourceType();
    resourceType.setTypeGeneral(ResourceType.TYPE_GENERAL.MODEL);
    resourceType.setValue(format + DataResourceRecordUtil.METADATA_SUFFIX);
    // Migrate relation type from 'isDerivedFrom' to 'hasMetadata'
    for (RelatedIdentifier item : recordByIdAndVersion.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM)) {
        item.setRelationType(DataResourceRecordUtil.RELATED_SCHEMA_TYPE);
        String replaceFirst = item.getValue().replaceFirst("/api/v1/schemas/", "/api/v2/schemas/");
        item.setValue(replaceFirst);
      }
    }
    // Add provenance
    if (version > 1) {
      String schemaUrl = baseUrl + WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class).getMetadataDocumentById(id, version - 1l, null, null)).toString();
      RelatedIdentifier provenance = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_NEW_VERSION_OF, schemaUrl, null, null);
      recordByIdAndVersion.getRelatedIdentifiers().add(provenance);
      LOG.trace("Add provenance to datacite record: '{}'", schemaUrl);
    } else {
      long currentVersion = metadataConfig.getAuditService().getCurrentVersion(id);
      if (currentVersion == 1l) {
        // Create an additional version for JaVers if only one version exists.
        currentDataResource.setPublisher("migrationTool");
        metadataConfig.getAuditService().captureAuditInformation(currentDataResource, "Just4fun");
      }
    }
    // Save migrated version
    LOG.trace("Persisting created metadata document resource.");
    DataResource migratedDataResource = dataResourceDao.save(recordByIdAndVersion);

    //capture state change
    LOG.trace("Capturing audit information.");
    metadataConfig.getAuditService().captureAuditInformation(migratedDataResource, "migration2version2");

    return migratedDataResource;
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

  private void movePidFromAlternateToPrimaryIdentifier(DataResource dataResource) {
    Identifier pid = null;
    // Move PID from alternateIdentifier to identifier
    for (Identifier altIdentifier : dataResource.getAlternateIdentifiers()) {
      if (altIdentifier.getIdentifierType() == Identifier.IDENTIFIER_TYPE.DOI) {
        PrimaryIdentifier primaryIdentifier = PrimaryIdentifier.factoryPrimaryIdentifier(altIdentifier.getValue());
        dataResource.setIdentifier(primaryIdentifier);
        pid = altIdentifier;
        break;
      }
    }
    if (pid != null) {
      dataResource.getAlternateIdentifiers().remove(pid);
    }
  }

  protected void removeTitleType(Set<Title> titles) {
    for (Title title : titles) {
      if (title.getTitleType() == Title.TYPE.OTHER) {
        title.setTitleType(null);
        break;
      }
    }
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
