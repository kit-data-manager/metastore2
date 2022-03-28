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
package edu.kit.datamanager.metastore2.util;

import com.fasterxml.jackson.core.JsonParseException;
import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoServiceRole;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.domain.DataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for handling json documents
 */
public class MetadataRecordUtil {

  /**
   * Separator for separating schemaId and schemaVersion.
   */
  public static final String SCHEMA_VERSION_SEPARATOR = ":";
  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MetadataRecordUtil.class);

  private static MetastoreConfiguration schemaConfig;
  /**
   * Encoding for strings/inputstreams.
   */
  private static final String ENCODING = "UTF-8";
  private static String guestToken = null;

  private static IDataRecordDao dataRecordDao;

  public static MetadataRecord createMetadataRecord(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument, MultipartFile document) {
    MetadataRecord result = null;
    MetadataRecord record;
    long nano1 = System.nanoTime() / 1000000;
    // Do some checks first.
    if (recordDocument == null || recordDocument.isEmpty() || document == null || document.isEmpty()) {
      String message = "No metadata record and/or metadata document provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
    } catch (IOException ex) {
      String message = "No valid metadata record provided. Returning HTTP BAD_REQUEST.";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error("Error parsing json: ", ex);
      throw new BadArgumentException(message);
    }

    if (record.getRelatedResource() == null || record.getRelatedResource().getIdentifier() == null || record.getSchema() == null || record.getSchema().getIdentifier() == null) {
      String message = "Mandatory attributes relatedResource and/or schema not found in record. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    // Test for schema version
    if (record.getSchemaVersion() == null) {
      MetadataSchemaRecord currentSchemaRecord;
      try {
        currentSchemaRecord = MetadataSchemaRecordUtil.getCurrentSchemaRecord(applicationProperties, record.getSchema());
      } catch (ResourceNotFoundException rnfe) {
        throw new UnprocessableEntityException("Unknown schema ID '" + record.getSchema().getIdentifier() + "'!");
      }
      record.setSchemaVersion(currentSchemaRecord.getSchemaVersion());
    }

    // validate document
    long nano2 = System.nanoTime() / 1000000;
    // validate schema document
    validateMetadataDocument(applicationProperties, record, document);
    // set internal parameters
    record.setRecordVersion(1l);

    long nano3 = System.nanoTime() / 1000000;
    // create record.
    DataResource dataResource = migrateToDataResource(applicationProperties, record);
    // add id as internal identifier if exists
    // Note: DataResourceUtils.createResource will ignore id of resource. 
    // id will be set to alternate identifier if exists. 
    if (dataResource.getId() != null) {
      // check for valid identifier without any chars which may be encoded
      try {
        String originalId = dataResource.getId();
        String value = URLEncoder.encode(originalId, StandardCharsets.UTF_8.toString());
        if (!value.equals(originalId)) {
          String message = "Not a valid id! Encoded: " + value;
          LOG.error(message);
          throw new BadArgumentException(message);
        }
      } catch (UnsupportedEncodingException ex) {
        String message = "Error encoding id " + record.getSchemaId();
        LOG.error(message);
        throw new CustomInternalServerError(message);
      }

      dataResource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(dataResource.getId()));
    }
    long nano4 = System.nanoTime() / 1000000;
    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
    long nano5 = System.nanoTime() / 1000000;
    // store document
    ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, (t) -> {
      return "somethingStupid";
    });
    long nano6 = System.nanoTime() / 1000000;
    // Create additional metadata record for faster access
    DataRecord dataRecord = new DataRecord();
    dataRecord.setMetadataId(createResource.getId());
    dataRecord.setVersion(record.getRecordVersion());
    dataRecord.setSchemaId(record.getSchema().getIdentifier());
    dataRecord.setSchemaVersion(record.getSchemaVersion());
    dataRecord.setMetadataDocumentUri(contentInformation.getContentUri());
    dataRecord.setDocumentHash(contentInformation.getHash());
    dataRecord.setLastUpdate(dataResource.getLastUpdate());
    saveNewDataRecord(dataRecord);
    long nano7 = System.nanoTime() / 1000000;
    LOG.info("Create Record times, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1, nano6 - nano1, nano7 - nano1);

    return migrateToMetadataRecord(applicationProperties, createResource, true);
  }

  public static MetadataRecord updateMetadataRecord(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          MultipartFile recordDocument,
          MultipartFile document,
          Function<String, String> supplier) {
    MetadataRecord record = null;
    MetadataRecord existingRecord;
    DataResource newResource;

    // Do some checks first.
    if ((recordDocument == null || recordDocument.isEmpty()) && (document == null || document.isEmpty())) {
      String message = "Neither metadata record nor metadata document provided.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    if (!(recordDocument == null || recordDocument.isEmpty())) {
      try {
        record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
      } catch (IOException ex) {
        String message = "Can't map record document to MetadataRecord";
        if (ex instanceof JsonParseException) {
          message = message + " Reason: " + ex.getMessage();
        }
        LOG.error("Error parsing json: ", ex);
        throw new BadArgumentException(message);
      }
    }

    LOG.trace("Obtaining most recent metadata record with id {}.", resourceId);
    DataResource dataResource = applicationProperties.getDataResourceService().findById(resourceId);
    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(eTag, dataResource);
    if (record != null) {
      existingRecord = migrateToMetadataRecord(applicationProperties, dataResource, false);
      existingRecord = mergeRecords(existingRecord, record);
      dataResource = migrateToDataResource(applicationProperties, existingRecord);
    } else {
      dataResource = DataResourceUtils.copyDataResource(dataResource);
    }

    boolean noChanges = true;
    if (document != null) {
      record = migrateToMetadataRecord(applicationProperties, dataResource, false);
      validateMetadataDocument(applicationProperties, record, document);

      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, dataResource);
      // Check for changes...
      try {
        byte[] currentFileContent;
        File file = new File(URI.create(info.getContentUri()));
        if (document.getSize() == Files.size(file.toPath())) {
          currentFileContent = FileUtils.readFileToByteArray(file);
          byte[] newFileContent = document.getBytes();
          for (int index = 0; index < currentFileContent.length; index++) {
            if (currentFileContent[index] != newFileContent[index]) {
              noChanges = false;
              break;
            }
          }
        } else {
          noChanges = false;
        }
      } catch (IOException ex) {
        LOG.error("Error reading current file!", ex);
      }
      if (noChanges == false) {
        // Everything seems to be fine update document and increment version
        LOG.trace("Updating schema document (and increment version)...");
        String version = dataResource.getVersion();
        if (version != null) {
          dataResource.setVersion(Long.toString(Long.parseLong(version) + 1l));
        }
        ContentDataUtils.addFile(applicationProperties, dataResource, document, info.getRelativePath(), null, true, supplier);
      }
    } else {
      // validate if document is still valid due to changed record settings.
      record = migrateToMetadataRecord(applicationProperties, dataResource, false);
      URI metadataDocumentUri = URI.create(record.getMetadataDocumentUri());

      Path metadataDocumentPath = Paths.get(metadataDocumentUri);
      if (!Files.exists(metadataDocumentPath) || !Files.isRegularFile(metadataDocumentPath) || !Files.isReadable(metadataDocumentPath)) {
        LOG.warn("Metadata document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", metadataDocumentPath);
        throw new CustomInternalServerError("Metadata document on server either does not exist or is no file or is not readable.");
      }

      try {
        InputStream inputStream = Files.newInputStream(metadataDocumentPath);
        SchemaRecord schemaRecord = MetadataSchemaRecordUtil.getSchemaRecord(record.getSchema(), record.getSchemaVersion());
        MetadataSchemaRecordUtil.validateMetadataDocument(applicationProperties, inputStream, schemaRecord);
      } catch (IOException ex) {
        LOG.error("Error validating file!", ex);
      }

    }
    if (noChanges) {
      Optional<DataRecord> dataRecord = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(dataResource.getId());
      if (dataRecord.isPresent()) {
        dataRecordDao.delete(dataRecord.get());
      }
    }
    dataResource = DataResourceUtils.updateResource(applicationProperties, resourceId, dataResource, eTag, supplier);

    return migrateToMetadataRecord(applicationProperties, dataResource, true);
  }

  public static void deleteMetadataRecord(MetastoreConfiguration applicationProperties,
          String id,
          String eTag,
          Function<String, String> supplier) {
    DataResourceUtils.deleteResource(applicationProperties, id, eTag, supplier);
    Optional<DataRecord> dataRecord = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(id);
    while (dataRecord.isPresent()) {
      dataRecordDao.delete(dataRecord.get());
      dataRecord = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(id);
    }
  }

  /**
   * Migrate metadata record to data resource.
   *
   * @param applicationProperties Configuration settings of repository.
   * @param metadataRecord Metadata record to migrate.
   * @return Data resource of metadata record.
   */
  public static DataResource migrateToDataResource(RepoBaseConfiguration applicationProperties,
          MetadataRecord metadataRecord) {
    DataResource dataResource;
    if (metadataRecord.getId() != null) {
      try {
        dataResource = applicationProperties.getDataResourceService().findById(metadataRecord.getId(), metadataRecord.getRecordVersion());
        dataResource = DataResourceUtils.copyDataResource(dataResource);
      } catch (ResourceNotFoundException rnfe) {
        LOG.error("Error catching DataResource for " + metadataRecord.getId() + " -> " + rnfe.getMessage());
        dataResource = DataResource.factoryNewDataResource(metadataRecord.getId());
        dataResource.setVersion("1");
      }
    } else {
      dataResource = new DataResource();
      dataResource.setVersion("1");
    }
    dataResource.setAcls(metadataRecord.getAcl());
    if (metadataRecord.getCreatedAt() != null) {
      boolean createDateExists = false;
      Set<Date> dates = dataResource.getDates();
      for (edu.kit.datamanager.repo.domain.Date d : dates) {
        if (edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())) {
          LOG.trace("Creation date entry found.");
          createDateExists = true;
          break;
        }
      }
      if (!createDateExists) {
        dataResource.getDates().add(Date.factoryDate(metadataRecord.getCreatedAt(), Date.DATE_TYPE.CREATED));
      }
    }
    Set<Identifier> identifiers = dataResource.getAlternateIdentifiers();
    if (metadataRecord.getPid() != null) {
      ResourceIdentifier identifier = metadataRecord.getPid();
      MetadataSchemaRecordUtil.checkAlternateIdentifier(identifiers, identifier.getIdentifier(), Identifier.IDENTIFIER_TYPE.valueOf(identifier.getIdentifierType().name()));
    } else {
      LOG.trace("Remove existing identifiers (others than URL)...");
      for (Identifier item : identifiers) {
        if (item.getIdentifierType() != Identifier.IDENTIFIER_TYPE.URL) {
          LOG.trace("... {},  {}", item.getValue(), item.getIdentifierType());
          identifiers.remove(item);
        }
      }
    }
    boolean relationFound = false;
    boolean schemaIdFound = false;
    for (RelatedIdentifier relatedIds : dataResource.getRelatedIdentifiers()) {
      if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR) {
        LOG.trace("Set relation to '{}'", metadataRecord.getRelatedResource());
        relatedIds.setValue(metadataRecord.getRelatedResource().getIdentifier());
        relationFound = true;
      }
      if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM) {
        updateRelatedIdentifierForSchema(relatedIds, metadataRecord);
        schemaIdFound = true;
      }
    }
    if (!relationFound) {
      RelatedIdentifier relatedResource = RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR, metadataRecord.getRelatedResource().getIdentifier(), null, null);
      dataResource.getRelatedIdentifiers().add(relatedResource);
    }
    if (!schemaIdFound) {
      RelatedIdentifier schemaId = updateRelatedIdentifierForSchema(null, metadataRecord);
      dataResource.getRelatedIdentifiers().add(schemaId);
    }
    String defaultTitle = "Metadata 4 metastore";
    boolean titleExists = false;
    for (Title title : dataResource.getTitles()) {
      if (title.getTitleType() == Title.TYPE.OTHER && title.getValue().equals(defaultTitle)) {
        titleExists = true;
      }
    }
    if (!titleExists) {
      dataResource.getTitles().add(Title.factoryTitle(defaultTitle, Title.TYPE.OTHER));
    }
    dataResource.setResourceType(ResourceType.createResourceType(MetadataRecord.RESOURCE_TYPE));

    return dataResource;
  }

  /**
   * Migrate data resource to metadata record.
   *
   * @param applicationProperties Configuration settings of repository.
   * @param dataResource Data resource to migrate.
   * @param provideETag Flag for calculating etag.
   * @return Metadata record of data resource.
   */
  public static MetadataRecord migrateToMetadataRecord(RepoBaseConfiguration applicationProperties,
          DataResource dataResource,
          boolean provideETag) {
    long nano1 = System.nanoTime() / 1000000;
    MetadataRecord metadataRecord = new MetadataRecord();
    if (dataResource != null) {
      metadataRecord.setId(dataResource.getId());
      if (provideETag) {
        metadataRecord.setETag(dataResource.getEtag());
      }
      metadataRecord.setAcl(dataResource.getAcls());

      for (edu.kit.datamanager.repo.domain.Date d : dataResource.getDates()) {
        if (edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())) {
          LOG.trace("Creation date entry found.");
          metadataRecord.setCreatedAt(d.getValue());
          break;
        }
      }
      if (dataResource.getLastUpdate() != null) {
        metadataRecord.setLastUpdate(dataResource.getLastUpdate());
      }

      Iterator<Identifier> iterator = dataResource.getAlternateIdentifiers().iterator();
      while (iterator.hasNext()) {
        Identifier identifier = iterator.next();
        if (identifier.getIdentifierType() != Identifier.IDENTIFIER_TYPE.URL) {
          if (identifier.getIdentifierType() != Identifier.IDENTIFIER_TYPE.INTERNAL) {
            ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(identifier.getValue(), ResourceIdentifier.IdentifierType.valueOf(identifier.getIdentifierType().getValue()));
            LOG.trace("Set PID to '{}' of type '{}'", resourceIdentifier.getIdentifier(), resourceIdentifier.getIdentifierType());
            metadataRecord.setPid(resourceIdentifier);
            break;
          } else {
            LOG.debug("'INTERNAL' identifier shouldn't be used! Migrate them to 'URL' if possible.");
          }
        }
      }

      Long recordVersion = 1l;
      if (dataResource.getVersion() != null) {
        recordVersion = Long.parseLong(dataResource.getVersion());
      }
      metadataRecord.setRecordVersion(recordVersion);

      for (RelatedIdentifier relatedIds : dataResource.getRelatedIdentifiers()) {
        if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR) {
          LOG.trace("Set relation to '{}'", relatedIds.getValue());
          metadataRecord.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier(relatedIds.getValue()));
        }
        if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM) {
          ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(relatedIds.getValue(), IdentifierType.valueOf(relatedIds.getIdentifierType().name()));
          metadataRecord.setSchema(resourceIdentifier);
          metadataRecord.setSchemaVersion(1l);
          LOG.trace("Set schema to '{}'", metadataRecord.getSchema());
        }
      }
      DataRecord dataRecord = null;
      long nano2 = System.nanoTime() / 1000000;
      Optional<DataRecord> dataRecordResult = dataRecordDao.findByMetadataIdAndVersion(dataResource.getId(), recordVersion);
      long nano3 = System.nanoTime() / 1000000;
      long nano4 = nano3;
      boolean isAvailable = false;
      boolean saveDataRecord = false;
      if (dataRecordResult.isPresent()) {
        LOG.trace("Get document URI from DataRecord.");
        dataRecord = dataRecordResult.get();
        nano4 = System.nanoTime() / 1000000;
        metadataRecord.setMetadataDocumentUri(dataRecord.getMetadataDocumentUri());
        metadataRecord.setDocumentHash(dataRecord.getDocumentHash());
        metadataRecord.setSchemaVersion(dataRecord.getSchemaVersion());
        isAvailable = true;
      } else {
        saveDataRecord = true;
      }
      if (!isAvailable) {
        LOG.trace("Get document URI from ContentInformation.");
        ContentInformation info;
        info = getContentInformationOfResource(applicationProperties, dataResource);
        nano4 = System.nanoTime() / 1000000;
        if (info != null) {
          metadataRecord.setDocumentHash(info.getHash());
          metadataRecord.setMetadataDocumentUri(info.getContentUri());
          MetadataSchemaRecord currentSchemaRecord = MetadataSchemaRecordUtil.getCurrentSchemaRecord(schemaConfig, metadataRecord.getSchema());
          metadataRecord.setSchemaVersion(currentSchemaRecord.getSchemaVersion());
          if (saveDataRecord) {
            saveNewDataRecord(metadataRecord);
          }
        }
      }
      long nano5 = System.nanoTime() / 1000000;
      LOG.info("Migrate to MetadataRecord, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1, provideETag);
    }

    return metadataRecord;
  }

  private static ContentInformation getContentInformationOfResource(RepoBaseConfiguration applicationProperties,
          DataResource dataResource) {
    ContentInformation returnValue = null;
    long nano1 = System.nanoTime() / 1000000;
    IContentInformationService contentInformationService = applicationProperties.getContentInformationService();
    ContentInformation info = new ContentInformation();
    info.setParentResource(dataResource);
    long nano2 = System.nanoTime() / 1000000;
    List<ContentInformation> listOfFiles = contentInformationService.findAll(info, PageRequest.of(0, 100)).getContent();
    long nano3 = System.nanoTime() / 1000000;
    if (LOG.isTraceEnabled()) {
      LOG.trace("Found {} files for resource '{}'", listOfFiles.size(), dataResource.getId());
      for (ContentInformation ci : listOfFiles) {
        DataResource parentResource = ci.getParentResource();
        ci.setParentResource(null);
        LOG.trace("ContentInformation: {}", ci);
        ci.setParentResource(parentResource);
      }
    }
    if (!listOfFiles.isEmpty()) {
      returnValue = listOfFiles.get(0);
    }
    LOG.info("Get content information of resource, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1);
    return returnValue;
  }

  /**
   * Returns schema record with the current version.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param schemaId SchemaID of the schema.
   * @return MetadataSchemaRecord ResponseEntity in case of an error.
   */
  public static MetadataSchemaRecord getCurrentInternalSchemaRecord(MetastoreConfiguration metastoreProperties,
          String schemaId) {
    LOG.trace("Get current internal schema record for id '{}'.", schemaId);
    MetadataSchemaRecord returnValue = null;
    boolean success = false;
    StringBuilder errorMessage = new StringBuilder();
    if (metastoreProperties.getSchemaRegistries().length == 0) {
      LOG.trace("No external schema registries defined. Try to use internal one...");

      returnValue = MetadataSchemaRecordUtil.getRecordById(metastoreProperties, schemaId);
      success = true;
    } else {
      for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
        LOG.trace("Try to fetch schema from '{}'.", schemaRegistry);
        URI schemaRegistryUri = URI.create(schemaRegistry);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), "schemas", schemaId);

        URI finalUri = builder.build().toUri();

        try {
          returnValue = SimpleServiceClient.create(finalUri.toString()).withBearerToken(guestToken).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).getResource(MetadataSchemaRecord.class);
          success = true;
          break;
        } catch (HttpClientErrorException ce) {
          String message = "Error accessing schema '" + schemaId + "' at '" + schemaRegistry + "'!";
          LOG.error(message, ce);
          errorMessage.append(message).append("\n");
        } catch (RestClientException ex) {
          String message = "Failed to access schema registry at '" + schemaRegistry + "'. Proceeding with next registry.";
          LOG.error(message, ex);
          errorMessage.append(message).append("\n");
        }
      }
    }
    if (!success) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }
    LOG.trace("Found schema record: '{}'", returnValue);
    return returnValue;
  }

  /**
   * Returns schema record with the current version.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param schemaId SchemaID of the schema.
   * @param version Version of the schema.
   * @return MetadataSchemaRecord ResponseEntity in case of an error.
   */
  public static MetadataSchemaRecord getInternalSchemaRecord(MetastoreConfiguration metastoreProperties,
          String schemaId,
          Long version) {
    MetadataSchemaRecord returnValue = null;
    boolean success = false;
    StringBuilder errorMessage = new StringBuilder();
    LOG.trace("Get internal schema record for id '{}'.", schemaId);
    if (metastoreProperties.getSchemaRegistries().length == 0) {
      LOG.trace("No external schema registries defined. Try to use internal one...");

      returnValue = MetadataSchemaRecordUtil.getRecordByIdAndVersion(metastoreProperties, schemaId, version);
      success = true;
    } else {
      for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
        LOG.trace("Try to fetch schema from '{}'.", schemaRegistry);
        URI schemaRegistryUri = URI.create(schemaRegistry);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), "schemas", schemaId).queryParam("version", version);

        URI finalUri = builder.build().toUri();

        try {
          returnValue = SimpleServiceClient.create(finalUri.toString()).withBearerToken(guestToken).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).getResource(MetadataSchemaRecord.class);
          success = true;
          break;
        } catch (HttpClientErrorException ce) {
          String message = "Error accessing schema '" + schemaId + "' at '" + schemaRegistry + "'!";
          LOG.error(message, ce);
          errorMessage.append(message).append("\n");
        } catch (RestClientException ex) {
          String message = "Failed to access schema registry at '" + schemaRegistry + "'. Proceeding with next registry.";
          LOG.error(message, ex);
          errorMessage.append(message).append("\n");
        }
      }
    }
    if (!success) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }
    LOG.trace("Found schema record: '{}'", returnValue);
    return returnValue;
  }

  /**
   * Update/create related identifier to values given by metadata record.
   *
   * @param relatedIdentifier related identifier (if null create a new one)
   * @param metadataRecord record holding schema information.
   * @return updated/created related identifier.
   */
  private static RelatedIdentifier updateRelatedIdentifierForSchema(RelatedIdentifier relatedIdentifier, MetadataRecord metadataRecord) {
    if (relatedIdentifier == null) {
      relatedIdentifier = RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM, null, null, null);
    }
    ResourceIdentifier schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(schemaConfig, metadataRecord);
    relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.valueOf(schemaIdentifier.getIdentifierType().name()));
    relatedIdentifier.setValue(schemaIdentifier.getIdentifier());
    LOG.trace("Set relatedId for schema to '{}'", relatedIdentifier);

    return relatedIdentifier;
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param record metadata of the document.
   * @param document document
   * @throws Exception In case of any error or invalid document.
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MetadataRecord record,
          MultipartFile document) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, record, document);
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    boolean validationSuccess = false;
    StringBuilder errorMessage = new StringBuilder();
    if (metastoreProperties.getSchemaRegistries().length == 0 || record.getSchema().getIdentifierType() != IdentifierType.INTERNAL) {
      LOG.trace("No external schema registries defined. Try to use internal one...");
      if (schemaConfig != null) {
        try {
          MetadataSchemaRecordUtil.validateMetadataDocument(schemaConfig, document, record.getSchema(), record.getSchemaVersion());
          validationSuccess = true;
        } catch (Exception ex) {
          String message = "Error validating document!";
          LOG.error(message, ex);
          errorMessage.append(ex.getMessage()).append("\n");
        }
      } else {
        throw new CustomInternalServerError("No schema registries defined! ");
      }
    } else {
      for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
        LOG.trace("Try to fetch schema from '{}'.", schemaRegistry);
        URI schemaRegistryUri = URI.create(schemaRegistry);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), "schemas", record.getSchema().getIdentifier(), "validate").queryParam("version", record.getSchemaVersion());

        URI finalUri = builder.build().toUri();

        try {
          HttpStatus status = SimpleServiceClient.create(finalUri.toString()).withBearerToken(guestToken).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).withFormParam("document", document.getInputStream()).postForm(MediaType.MULTIPART_FORM_DATA);

          if (Objects.equals(HttpStatus.NO_CONTENT, status)) {
            LOG.trace("Successfully validated document against schema {} in registry {}.", record.getSchema().getIdentifier(), schemaRegistry);
            validationSuccess = true;
            break;
          }
        } catch (HttpClientErrorException ce) {
          //not valid 
          String message = "Failed to validate metadata document against schema " + record.getSchema().getIdentifier() + " at '" + schemaRegistry + "' with status " + ce.getStatusCode() + ".";
          LOG.error(message, ce);
          errorMessage.append(message).append("\n");
        } catch (IOException | RestClientException ex) {
          String message = "Failed to access schema registry at '" + schemaRegistry + "'. Proceeding with next registry.";
          LOG.error(message, ex);
          errorMessage.append(message).append("\n");
        }
      }
    }
    if (!validationSuccess) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }

    return;
  }

  public static MetadataRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(metastoreProperties, recordId, null, false);
  }

  public static MetadataRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(metastoreProperties, recordId, version, false);
  }

  public static MetadataRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version, boolean supportEtag) throws ResourceNotFoundException {
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    long nano = System.nanoTime() / 1000000;
    MetadataRecord result = null;
    Page<DataResource> dataResource = metastoreProperties.getDataResourceService().findAllVersions(recordId, null);
    long nano2 = System.nanoTime() / 1000000;
    Stream<DataResource> stream = dataResource.get();
    if (version != null) {
      stream = stream.filter(resource -> Long.parseLong(resource.getVersion()) == version);
    }
    Optional<DataResource> findFirst = stream.findFirst();
    if (findFirst.isPresent()) {
      result = migrateToMetadataRecord(metastoreProperties, findFirst.get(), supportEtag);
    } else {
      String message = String.format("ID '%s' or version '%d' doesn't exist!", recordId, version.longValue());
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    long nano3 = System.nanoTime() / 1000000;
    LOG.info("getRecordByIdAndVersion {}, {}, {}", nano, (nano2 - nano), (nano3 - nano));
    return result;
  }

  public static Path getMetadataDocumentByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId) throws ResourceNotFoundException {
    return getMetadataDocumentByIdAndVersion(metastoreProperties, recordId, null);
  }

  public static Path getMetadataDocumentByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    LOG.trace("Obtaining metadata record with id {} and version {}.", recordId, version);
    MetadataRecord record = getRecordByIdAndVersion(metastoreProperties, recordId, version);

    URI metadataDocumentUri = URI.create(record.getMetadataDocumentUri());

    Path metadataDocumentPath = Paths.get(metadataDocumentUri);
    if (!Files.exists(metadataDocumentPath) || !Files.isRegularFile(metadataDocumentPath) || !Files.isReadable(metadataDocumentPath)) {
      LOG.warn("Metadata document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", metadataDocumentPath);
      throw new CustomInternalServerError("Metadata document on server either does not exist or is no file or is not readable.");
    }
    return metadataDocumentPath;
  }

  public static MetadataRecord mergeRecords(MetadataRecord managed, MetadataRecord provided) {
    if (provided != null && managed != null) {
      //update pid
      if (provided.getPid() != null) {
        if (!provided.getPid().equals(managed.getPid())) {
          LOG.trace("Updating record pid from {} to {}.", managed.getPid(), provided.getPid());
          managed.setPid(provided.getPid());
        }
      }
      //update acl
      if (!provided.getAcl().isEmpty()) {
        if (!provided.getAcl().equals(managed.getAcl())) {
          // check for special access rights 
          // - only administrators are allowed to change ACL
          // - at least principal has to remain as ADMIN 
          if (checkAccessRights(provided.getAcl())) {
            LOG.trace("Updating record acl from {} to {}.", managed.getAcl(), provided.getAcl());
            managed.setAcl(provided.getAcl());
          }
        }
      }
      //update getRelatedResource
      if (provided.getRelatedResource() != null) {
        if (!provided.getRelatedResource().equals(managed.getRelatedResource())) {
          LOG.trace("Updating related resource from {} to {}.", managed.getRelatedResource(), provided.getRelatedResource());
          managed.setRelatedResource(provided.getRelatedResource());
        }
      }
      //update schemaId
      if (provided.getSchema() != null) {
        if (!provided.getSchema().equals(managed.getSchema())) {
          LOG.trace("Updating record schema from {} to {}.", managed.getSchema(), provided.getSchema());
          managed.setSchema(provided.getSchema());
        }
      }
      //update schemaVersion
      if (provided.getSchemaVersion() != null) {
        if (!provided.getSchemaVersion().equals(managed.getSchemaVersion())) {
          LOG.trace("Updating record schemaVersion from {} to {}.", managed.getSchemaVersion(), provided.getSchemaVersion());
          managed.setSchemaVersion(provided.getSchemaVersion());
        }
      }
    } else {
      managed = (managed != null) ? managed : provided;
    }
//    LOG.trace("Setting lastUpdate to now().");
//    managed.setLastUpdate(Instant.now());
    return managed;
  }

  public static void setToken(String bearerToken) {
    guestToken = bearerToken;
  }

  /**
   * @param aSchemaConfig the schemaConfig to set
   */
  public static void setSchemaConfig(MetastoreConfiguration aSchemaConfig) {
    schemaConfig = aSchemaConfig;
  }

  /**
   * @param aDataRecordDao the dataRecordDao to set
   */
  public static void setDataRecordDao(IDataRecordDao aDataRecordDao) {
    dataRecordDao = aDataRecordDao;
  }

  private static void saveNewDataRecord(MetadataRecord result) {
    DataRecord dataRecord = null;

    // Create shortcut for access.
    LOG.trace("Save new data record!");
    dataRecord = transformToDataRecord(result);

    saveNewDataRecord(dataRecord);
  }

  private static DataRecord transformToDataRecord(MetadataRecord result) {
    DataRecord dataRecord = null;
    if (result != null) {
      LOG.trace("Transform to data record!");
      dataRecord = new DataRecord();
      dataRecord.setMetadataId(result.getId());
      dataRecord.setVersion(result.getRecordVersion());
      dataRecord.setSchemaId(result.getSchema().getIdentifier());
      dataRecord.setSchemaVersion(result.getSchemaVersion());
      dataRecord.setDocumentHash(result.getDocumentHash());
      dataRecord.setMetadataDocumentUri(result.getMetadataDocumentUri());
      dataRecord.setLastUpdate(result.getLastUpdate());
    }
    return dataRecord;
  }

  private static void saveNewDataRecord(DataRecord dataRecord) {
    if (dataRecordDao != null) {
      try {
        dataRecordDao.save(dataRecord);
      } catch (Exception ex) {
        LOG.error("Error saving data record", ex);
      }
      LOG.trace("Data record saved: {}", dataRecord);
    }
  }

  public static boolean checkAccessRights(Set<AclEntry> provided) {
    LOG.trace("Check access rights for changing ACL list!");
    boolean isAllowed = false;
    String principal = AuthenticationHelper.getPrincipal();
    Authentication authentication = AuthenticationHelper.getAuthentication();
    if (AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString()) ||
        AuthenticationHelper.hasAuthority(RepoServiceRole.SERVICE_WRITE.toString())) {
      // User is allowed to change ACLs. 
      List<String> authorizationIdentities = AuthenticationHelper.getAuthorizationIdentities();
      Iterator<AclEntry> iterator = provided.iterator();
      // Check if ADMINISTRATOR is still ADMINISTRATOR
      while (iterator.hasNext()) {
        AclEntry aclEntry = iterator.next();
        if (aclEntry.getPermission().atLeast(PERMISSION.ADMINISTRATE)) {
          if (authorizationIdentities.contains(aclEntry.getSid())) {
            isAllowed = true;
            LOG.trace("ACL list is OK, ready to set new ACL list.");
            break;
          }
        }
      }
    } else {
      LOG.warn("Only ADMINISTRATORS are allowed to change ACL entries");
    }
    return isAllowed;
  }
}
