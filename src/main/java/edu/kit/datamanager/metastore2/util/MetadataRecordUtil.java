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
import edu.kit.datamanager.exceptions.*;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.domain.*;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImplV2;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.*;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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

  private static final String LOG_SCHEMA_REGISTRY = "No external schema registries defined. Try to use internal one...";
  private static final String LOG_FETCH_SCHEMA = "Try to fetch schema from '{}'.";
  private static final String PATH_SCHEMA = "schemas";
  private static final String LOG_ERROR_ACCESS = "Failed to access schema registry at '%s'. Proceeding with next registry.";
  private static final String LOG_SCHEMA_RECORD = "Found schema record: '{}'";

  private static MetastoreConfiguration schemaConfig;
  private static String guestToken = null;

  private static IDataRecordDao dataRecordDao;

  MetadataRecordUtil() {
    //Utility class
  }

  /**
   * Create a digital object from metadata record and metadata document.
   *
   * @param applicationProperties Configuration properties.
   * @param recordDocument Metadata record.
   * @param document Metadata document.
   * @return Enriched metadata record.
   */
  public static MetadataRecord createMetadataRecord(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument, MultipartFile document) {
    MetadataRecord metadataRecord;
    long nano1 = System.nanoTime() / 1000000;
    // Do some checks first.
    if (recordDocument == null || recordDocument.isEmpty() || document == null || document.isEmpty()) {
      String message = "No metadata record and/or metadata document provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      metadataRecord = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
    } catch (IOException ex) {
      String message = "No valid metadata record provided. Returning HTTP BAD_REQUEST.";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error("Error parsing json: ", ex);
      throw new BadArgumentException(message);
    }

    if (metadataRecord.getRelatedResource() == null || metadataRecord.getRelatedResource().getIdentifier() == null || metadataRecord.getSchema() == null || metadataRecord.getSchema().getIdentifier() == null) {
      String message = "Mandatory attributes relatedResource and/or schema not found in record. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    // Test for schema version
    if (metadataRecord.getSchemaVersion() == null) {
      MetadataSchemaRecord currentSchemaRecord;
      try {
        currentSchemaRecord = MetadataSchemaRecordUtil.getCurrentSchemaRecord(applicationProperties, metadataRecord.getSchema());
      } catch (ResourceNotFoundException rnfe) {
        throw new UnprocessableEntityException("Unknown schema ID '" + metadataRecord.getSchema().getIdentifier() + "'!");
      }
      metadataRecord.setSchemaVersion(currentSchemaRecord.getSchemaVersion());
    }

    // validate document
    long nano2 = System.nanoTime() / 1000000;
    // validate schema document
    validateMetadataDocument(applicationProperties, metadataRecord, document);
    // set internal parameters
    metadataRecord.setRecordVersion(1L);

    long nano3 = System.nanoTime() / 1000000;
    // create record.
    DataResource dataResource = migrateToDataResource(applicationProperties, metadataRecord);
    // add id as internal identifier if exists
    // Note: DataResourceUtils.createResource will ignore id of resource. 
    // id will be set to alternate identifier if exists. 
    if (dataResource.getId() != null) {
      // check for valid identifier without any chars which may be encoded
      String originalId = dataResource.getId();
      String value = URLEncoder.encode(originalId, StandardCharsets.UTF_8);
      if (!value.equals(originalId)) {
        String message = "Not a valid id! Encoded: " + value;
        LOG.error(message);
        throw new BadArgumentException(message);
      }

      dataResource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(dataResource.getId()));
    }
    long nano4 = System.nanoTime() / 1000000;
    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
    long nano5 = System.nanoTime() / 1000000;
    // store document
    ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, t -> "somethingStupid");
    long nano6 = System.nanoTime() / 1000000;
    // Create additional metadata record for faster access
    DataRecord dataRecord = new DataRecord();
    dataRecord.setMetadataId(createResource.getId());
    dataRecord.setVersion(metadataRecord.getRecordVersion());
    dataRecord.setSchemaId(metadataRecord.getSchema().getIdentifier());
    dataRecord.setSchemaVersion(metadataRecord.getSchemaVersion());
    dataRecord.setMetadataDocumentUri(contentInformation.getContentUri());
    dataRecord.setDocumentHash(contentInformation.getHash());
    dataRecord.setLastUpdate(dataResource.getLastUpdate());
    saveNewDataRecord(dataRecord);
    long nano7 = System.nanoTime() / 1000000;
    LOG.info("Create Record times, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1, nano6 - nano1, nano7 - nano1);

    return migrateToMetadataRecord(applicationProperties, createResource, true);
  }

  /**
   * Update a digital object with given metadata record and/or metadata
   * document.
   *
   * @param applicationProperties Configuration properties.
   * @param resourceId Identifier of digital object.
   * @param eTag ETag of the old digital object.
   * @param recordDocument Metadata record.
   * @param document Metadata document.
   * @param supplier Function for updating record.
   * @return Enriched metadata record.
   */
  public static MetadataRecord updateMetadataRecord(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          MultipartFile recordDocument,
          MultipartFile document,
          UnaryOperator<String> supplier) {
    MetadataRecord metadataRecord = null;

    // Do some checks first.
    if ((recordDocument == null || recordDocument.isEmpty()) && (document == null || document.isEmpty())) {
      String message = "Neither metadata record nor metadata document provided.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    if (!(recordDocument == null || recordDocument.isEmpty())) {
      try {
        metadataRecord = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
      } catch (IOException ex) {
        String message = "Can't map record document to MetadataRecord";
        if (ex instanceof JsonParseException) {
          message = message + " Reason: " + ex.getMessage();
        }
        LOG.error("Error parsing json: ", ex);
        throw new BadArgumentException(message);
      }
    }
    DataResource givenRecord = null;
    if (metadataRecord != null) {
      givenRecord = migrateToDataResource(applicationProperties, metadataRecord);
    }
    DataResource dataResource = DataResourceRecordUtil.updateDataResource4MetadataDocument(applicationProperties, resourceId, eTag, givenRecord, document, supplier);

    return migrateToMetadataRecord(applicationProperties, dataResource, true);
  }

  /**
   * Delete a digital object with given identifier.
   *
   * @param applicationProperties Configuration properties.
   * @param id Identifier of digital object.
   * @param eTag ETag of the old digital object.
   * @param supplier Function for updating record.
   */
  public static void deleteMetadataRecord(MetastoreConfiguration applicationProperties,
          String id,
          String eTag,
          UnaryOperator<String> supplier) {
    DataResourceUtils.deleteResource(applicationProperties, id, eTag, supplier);
    try {
      DataResourceUtils.getResourceByIdentifierOrRedirect(applicationProperties, id, null, supplier);
    } catch (ResourceNotFoundException rnfe) {
      Optional<DataRecord> dataRecord = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(id);
      while (dataRecord.isPresent()) {
        dataRecordDao.delete(dataRecord.get());
        dataRecord = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(id);
      }
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
    dataResource = getDataResource(metadataRecord);
    dataResource.setAcls(metadataRecord.getAcl());
    DataResourceRecordUtil.setCreationDate(dataResource, metadataRecord.getCreatedAt());
    if (metadataRecord.getPid() != null) {
      PrimaryIdentifier pid = PrimaryIdentifier.factoryPrimaryIdentifier();
      pid.setIdentifierType(metadataRecord.getPid().getIdentifierType().value());
      pid.setValue(metadataRecord.getPid().getIdentifier());
      dataResource.setIdentifier(pid);
    } else {
      LOG.trace("Remove existing identifier");
      dataResource.setIdentifier(null);
    }
    updateRelatedIdentifierForSchema(dataResource, metadataRecord);
    updateRelatedIdentifierForResource(dataResource, metadataRecord);
    setTitle(dataResource);

    // Set ResourceType due to new version
    ResourceIdentifier schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(schemaConfig, metadataRecord);
    String prefixDocument = MetadataSchemaRecordUtil.getCurrentSchemaRecord(schemaConfig, schemaIdentifier).getType().name();
    ResourceType resourceType = ResourceType.createResourceType(prefixDocument + DataResourceRecordUtil.METADATA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL);
    dataResource.setResourceType(resourceType);

    DataResourceRecordUtil.checkLicense(dataResource, metadataRecord.getLicenseUri());

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
    MetadataRecord metadataRecord = DataResourceRecordUtil.migrateToMetadataRecordV2(applicationProperties, dataResource);
    if ((metadataRecord != null) && provideETag) {
      metadataRecord.setETag(dataResource.getEtag());
      DataRecord dataRecord = null;
      Optional<DataRecord> dataRecordResult = dataRecordDao.findByMetadataIdAndVersion(dataResource.getId(), metadataRecord.getRecordVersion());
      if (dataRecordResult.isPresent()) {
        LOG.trace("Get document URI from DataRecord.");
        dataRecord = dataRecordResult.get();
        metadataRecord.setMetadataDocumentUri(dataRecord.getMetadataDocumentUri());
        metadataRecord.setDocumentHash(dataRecord.getDocumentHash());
        metadataRecord.setSchemaVersion(dataRecord.getSchemaVersion());
      } else {
        LOG.trace("Get document URI from ContentInformation.");
        ContentInformation info;
        info = getContentInformationOfResource(applicationProperties, dataResource);
        if (info != null) {
          metadataRecord.setDocumentHash(info.getHash());
          metadataRecord.setMetadataDocumentUri(info.getContentUri());
          MetadataSchemaRecord currentSchemaRecord = MetadataSchemaRecordUtil.getCurrentSchemaRecord(schemaConfig, metadataRecord.getSchema());
          metadataRecord.setSchemaVersion(currentSchemaRecord.getSchemaVersion());
          saveNewDataRecord(metadataRecord);
        }
      }
      // Only one license allowed. So don't worry about size of set.
      if (!dataResource.getRights().isEmpty()) {
        metadataRecord.setLicenseUri(dataResource.getRights().iterator().next().getSchemeUri());
      }
    }

    return metadataRecord;
  }

  private static ContentInformation getContentInformationOfResource(RepoBaseConfiguration applicationProperties,
          DataResource dataResource) {
    ContentInformation returnValue = null;
    IContentInformationService contentInformationService = applicationProperties.getContentInformationService();
    ContentInformation info = new ContentInformation();
    info.setParentResource(dataResource);
    List<ContentInformation> listOfFiles = contentInformationService.findAll(info, PageRequest.of(0, 100)).getContent();
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
    if (metastoreProperties.getSchemaRegistries().size() == 0) {
      LOG.trace(LOG_SCHEMA_REGISTRY);

      returnValue = MetadataSchemaRecordUtil.getRecordById(metastoreProperties, schemaId);
      success = true;
    } else {
      for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
        LOG.trace(LOG_FETCH_SCHEMA, schemaRegistry);
        URI schemaRegistryUri = URI.create(schemaRegistry);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), PATH_SCHEMA, schemaId);

        URI finalUri = builder.build().toUri();

        try {
          returnValue = SimpleServiceClient.create(finalUri.toString()).withBearerToken(guestToken).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).getResource(MetadataSchemaRecord.class
          );
          success = true;
          break;
        } catch (HttpClientErrorException ce) {
          String message = "Error accessing schema '" + schemaId + "' at '" + schemaRegistry + "'!";
          LOG.error(message, ce);
          errorMessage.append(message).append("\n");
        } catch (RestClientException ex) {
          String message = String.format(LOG_ERROR_ACCESS, schemaRegistry);
          LOG.error(message, ex);
          errorMessage.append(message).append("\n");
        }
      }
    }
    if (!success) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }
    LOG.trace(LOG_SCHEMA_RECORD, returnValue);
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
    if (metastoreProperties.getSchemaRegistries().size() == 0) {
      LOG.trace(LOG_SCHEMA_REGISTRY);

      returnValue = MetadataSchemaRecordUtil.getRecordByIdAndVersion(metastoreProperties, schemaId, version);
      success = true;
    } else {
      for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
        LOG.trace(LOG_FETCH_SCHEMA, schemaRegistry);
        URI schemaRegistryUri = URI.create(schemaRegistry);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), PATH_SCHEMA, schemaId).queryParam("version", version);

        URI finalUri = builder.build().toUri();

        try {
          returnValue = SimpleServiceClient.create(finalUri.toString()).withBearerToken(guestToken).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).getResource(MetadataSchemaRecord.class
          );
          success = true;
          break;
        } catch (HttpClientErrorException ce) {
          String message = "Error accessing schema '" + schemaId + "' at '" + schemaRegistry + "'!";
          LOG.error(message, ce);
          errorMessage.append(message).append("\n");
        } catch (RestClientException ex) {
          String message = String.format(LOG_ERROR_ACCESS, schemaRegistry);
          LOG.error(message, ex);
          errorMessage.append(message).append("\n");
        }
      }
    }
    if (!success) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }
    LOG.trace(LOG_SCHEMA_RECORD, returnValue);
    return returnValue;
  }

  /**
   * Update/create related identifier for resource to values given by metadata
   * record.
   *
   * @param dataResource data resource holding related resource.
   * @param metadataRecord record holding resource information.
   */
  private static void updateRelatedIdentifierForResource(DataResource dataResource, MetadataRecord metadataRecord) {
    RelatedIdentifier schemaRelatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    if (schemaRelatedIdentifier == null) {
      schemaRelatedIdentifier = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, null, null, null);
      dataResource.getRelatedIdentifiers().add(schemaRelatedIdentifier);
    }
    ResourceIdentifier schemaIdentifier = metadataRecord.getRelatedResource();
    schemaRelatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.valueOf(schemaIdentifier.getIdentifierType().name()));
    schemaRelatedIdentifier.setValue(schemaIdentifier.getIdentifier());
    LOG.trace("Set related identfier for resource to '{}'", schemaRelatedIdentifier);
  }

  /**
   * Update/create related identifier for schema to values given by metadata
   * record.
   *
   * @param dataResource data resource holding related schema.
   * @param metadataRecord record holding schema information.
   */
  private static void updateRelatedIdentifierForSchema(DataResource dataResource, MetadataRecord metadataRecord) {
    RelatedIdentifier schemaRelatedIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
    if (schemaRelatedIdentifier == null) {
      schemaRelatedIdentifier = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_SCHEMA_TYPE, null, null, null);
      dataResource.getRelatedIdentifiers().add(schemaRelatedIdentifier);
    }
    ResourceIdentifier schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(schemaConfig, metadataRecord);
    schemaRelatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.valueOf(schemaIdentifier.getIdentifierType().name()));
    schemaRelatedIdentifier.setValue(schemaIdentifier.getIdentifier());
    LOG.trace("Set relatedId for schema to '{}'", schemaRelatedIdentifier);
  }

  /**
   * Set title for data resource if and only if not already set.
   *
   * @param dataResource data resource
   */
  public static void setTitle(DataResource dataResource) {
    String defaultTitle = "Metadata 4 metastore";
    boolean titleExists = false;
    for (Title title : dataResource.getTitles()) {
      if (title.getTitleType() == null && title.getValue().equals(defaultTitle)) {
        titleExists = true;
      }
    }
    if (!titleExists) {
      dataResource.getTitles().add(Title.factoryTitle(defaultTitle, null));
    }
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param metadataRecord metadata of the document.
   * @param document document
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MetadataRecord metadataRecord,
          MultipartFile document) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, metadataRecord, document);
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    boolean validationSuccess = false;
    StringBuilder errorMessage = new StringBuilder();
    if (metastoreProperties.getSchemaRegistries().isEmpty() || metadataRecord.getSchema().getIdentifierType() != IdentifierType.INTERNAL) {
      LOG.trace(LOG_SCHEMA_REGISTRY);
      if (schemaConfig != null) {
        try {
          MetadataSchemaRecordUtil.validateMetadataDocument(schemaConfig, document, metadataRecord.getSchema(), metadataRecord.getSchemaVersion());
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
        LOG.trace(LOG_FETCH_SCHEMA, schemaRegistry);
        URI schemaRegistryUri = URI.create(schemaRegistry);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), PATH_SCHEMA, metadataRecord.getSchema().getIdentifier(), "validate").queryParam("version", metadataRecord.getSchemaVersion());

        URI finalUri = builder.build().toUri();

        try {
          HttpStatus status = SimpleServiceClient.create(finalUri.toString()).withBearerToken(guestToken).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).withFormParam("document", document.getInputStream()).postForm(MediaType.MULTIPART_FORM_DATA);

          if (Objects.equals(HttpStatus.NO_CONTENT, status)) {
            LOG.trace("Successfully validated document against schema {} in registry {}.", metadataRecord.getSchema().getIdentifier(), schemaRegistry);
            validationSuccess = true;
            break;
          }
        } catch (HttpClientErrorException ce) {
          //not valid 
          String message = "Failed to validate metadata document against schema " + metadataRecord.getSchema().getIdentifier() + " at '" + schemaRegistry + "' with status " + ce.getStatusCode() + ".";
          LOG.error(message, ce);
          errorMessage.append(message).append("\n");
        } catch (IOException | RestClientException ex) {
          String message = String.format(LOG_ERROR_ACCESS, schemaRegistry);
          LOG.error(message, ex);
          errorMessage.append(message).append("\n");
        }
      }
    }
    if (!validationSuccess) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }
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
    long nano2;
    MetadataRecord result = null;
    Page<DataResource> dataResource;
    try {
      dataResource = metastoreProperties.getDataResourceService().findAllVersions(recordId, null);
    } catch (ResourceNotFoundException ex) {
      ex.setDetail("Metadata document with ID '" + recordId + "' doesn't exist!");
      throw ex;
    }
    nano2 = System.nanoTime() / 1000000;
    Stream<DataResource> stream = dataResource.get();
    if (version != null) {
      stream = stream.filter(resource -> Long.parseLong(resource.getVersion()) == version);
    }
    Optional<DataResource> findFirst = stream.findFirst();
    if (findFirst.isPresent()) {
      result = migrateToMetadataRecord(metastoreProperties, findFirst.get(), supportEtag);
    } else {
      String message = String.format("Version '%d' of ID '%s' doesn't exist!", version, recordId);
      LOG.error(message);
      throw new ResourceNotFoundException(message);
    }
    long nano3 = System.nanoTime() / 1000000;
    LOG.info("getRecordByIdAndVersion {}, {}, {}", nano, (nano2 - nano), (nano3 - nano));
    return result;
  }

  /**
   * Merge new metadata record in the existing one.
   *
   * @param managed Existing metadata record.
   * @param provided New metadata record.
   * @return Merged record
   */
  public static MetadataRecord mergeRecords(MetadataRecord managed, MetadataRecord provided) {
    if (provided != null && managed != null) {
      //update pid
      managed.setPid(mergeEntry("Update record->pid", managed.getPid(), provided.getPid()));

      //update acl
      managed.setAcl(mergeAcl(managed.getAcl(), provided.getAcl()));
      //update getRelatedResource
      managed.setRelatedResource(mergeEntry("Updating record->relatedResource", managed.getRelatedResource(), provided.getRelatedResource()));
      //update schemaId
      managed.setSchema(mergeEntry("Updating record->schema", managed.getSchema(), provided.getSchema()));
      //update schemaVersion
      managed.setSchemaVersion(mergeEntry("Updating record->schemaVersion", managed.getSchemaVersion(), provided.getSchemaVersion()));
      // update licenseUri
      managed.setLicenseUri(mergeEntry("Updating record->licenseUri", managed.getLicenseUri(), provided.getLicenseUri(), true));
    } else {
      managed = (managed != null) ? managed : provided;
    }
    return managed;
  }

  /**
   * Check validity of acl list and then merge new acl list in the existing one.
   *
   * @param managed Existing metadata record.
   * @param provided New metadata record.
   * @return Merged list
   */
  public static Set<AclEntry> mergeAcl(Set<AclEntry> managed, Set<AclEntry> provided) {
    return DataResourceRecordUtil.mergeAcl(managed, provided);
  }

  /**
   * Set new value for existing one.
   *
   * @param description For logging purposes only
   * @param managed Existing value.
   * @param provided New value.
   * @return Merged record
   */
  public static <T> T mergeEntry(String description, T managed, T provided) {
    return DataResourceRecordUtil.mergeEntry(description, managed, provided);
  }

  /**
   * Set new value for existing one.
   *
   * @param description For logging purposes only
   * @param managed Existing value.
   * @param provided New value.
   * @param overwriteWithNull Allows also deletion of a value.
   * @return Merged record
   */
  public static <T> T mergeEntry(String description, T managed, T provided, boolean overwriteWithNull) {
    return DataResourceRecordUtil.mergeEntry(description, managed, provided, overwriteWithNull);
  }

  /**
   * Return the number of ingested documents. If there are two versions of the
   * same document this will be counted as two.
   *
   * @return Number of registered documents.
   */
  public static long getNoOfDocuments() {
    return dataRecordDao.count();
  }

  public static void setToken(String bearerToken) {
    guestToken = bearerToken;
  }

  /**
   * Set schema config.
   *
   * @param aSchemaConfig the schemaConfig to set
   */
  public static void setSchemaConfig(MetastoreConfiguration aSchemaConfig) {
    schemaConfig = aSchemaConfig;
  }

  /**
   * Set DAO for data record.
   *
   * @param aDataRecordDao the dataRecordDao to set
   */
  public static void setDataRecordDao(IDataRecordDao aDataRecordDao) {
    dataRecordDao = aDataRecordDao;
  }

  private static DataResource getDataResource(MetadataRecord metadataRecord) {
    DataResource dataResource;
    if (metadataRecord.getId() != null) {
      try {
        dataResource = DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, metadataRecord.getId(), metadataRecord.getRecordVersion());
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
    return dataResource;
  }

  private static void saveNewDataRecord(MetadataRecord result) {
    DataRecord dataRecord;

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

  public static final void fixMetadataDocumentUri(MetadataRecord metadataRecord) {
    String metadataDocumentUri = metadataRecord.getMetadataDocumentUri();
    metadataRecord
            .setMetadataDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class
            ).getMetadataDocumentById(metadataRecord.getId(), metadataRecord.getRecordVersion(), null, null)).toUri().toString());
    LOG.trace("Fix metadata document Uri '{}' -> '{}'", metadataDocumentUri, metadataRecord.getMetadataDocumentUri());
  }
}
