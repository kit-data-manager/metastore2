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
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.*;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.*;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType;
import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImplV2;
import edu.kit.datamanager.metastore2.web.impl.SchemaRegistryControllerImplV2;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.domain.*;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE.JSON;
import static edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE.XML;
import edu.kit.datamanager.repo.dao.spec.dataresource.PermissionSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.RelatedIdentifierSpec;
import edu.kit.datamanager.repo.domain.Date;
import java.nio.charset.Charset;
import java.time.Instant;

/**
 * Utility class for handling json documents
 */
public class DataResourceRecordUtil {

  public static final String RESOURCE_TYPE = "application/vnd.datacite.org+json";
  
  public static final RelatedIdentifier.RELATION_TYPES RELATED_DATA_RESOURCE_TYPE = RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR;
  public static final RelatedIdentifier.RELATION_TYPES RELATED_SCHEMA_TYPE = RelatedIdentifier.RELATION_TYPES.HAS_METADATA;
  public static final RelatedIdentifier.RELATION_TYPES RELATED_NEW_VERSION_OF = RelatedIdentifier.RELATION_TYPES.IS_NEW_VERSION_OF;
  /**
   * Mediatype for fetching a DataResource.
   */
  public static final MediaType DATA_RESOURCE_MEDIA_TYPE = MediaType.valueOf(RESOURCE_TYPE);

  /**
   * Separator for separating schemaId and schemaVersion.
   */
  public static final String SCHEMA_VERSION_SEPARATOR = "/";
  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(DataResourceRecordUtil.class);

  private static final String LOG_ERROR_READ_METADATA_DOCUMENT = "Failed to read metadata document from input stream.";
  private static final String ERROR_PARSING_JSON = "Error parsing json: ";

  private static MetastoreConfiguration schemaConfig;

  private static IDataRecordDao dataRecordDao;
  private static IDataResourceDao dataResourceDao;
  private static ISchemaRecordDao schemaRecordDao;
  private static IMetadataFormatDao metadataFormatDao;

  public static final String SCHEMA_SUFFIX = "_Schema";
  public static final String XML_SCHEMA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.XML + SCHEMA_SUFFIX;
  public static final String JSON_SCHEMA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.JSON + SCHEMA_SUFFIX;

  public static final String METADATA_SUFFIX = "_Metadata";
  public static final String XML_METADATA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.XML + METADATA_SUFFIX;
  public static final String JSON_METADATA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.JSON + METADATA_SUFFIX;

  private static String baseUrl;

  DataResourceRecordUtil() {
    //Utility class
  }

  /**
   * Checks if current user is allowed to access with given AclEntries.
   *
   * @param aclEntries AclEntries of resource.
   * @param currentAcl Check current ACL (true) or new one (false).
   *
   * @return Allowed (true) or not.
   */
  public static boolean checkAccessRights(Set<AclEntry> aclEntries, boolean currentAcl) {
    boolean isAllowed = true;
    String errorMessage1 = "Error invalid ACL! Reason: Only ADMINISTRATORS are allowed to change ACL entries.";
    String errorMessage2 = "Error invalid ACL! Reason: You are not allowed to revoke your own administrator rights.";
    List<String> authorizationIdentities = getAllAuthorizationIdentities();
    // No authentication enabled or 
    //ROLE_ADMINISTRATOR detected -> no further permission check necessary.
    if (schemaConfig.isAuthEnabled() && !authorizationIdentities.contains(RepoUserRole.ADMINISTRATOR.getValue())) {
      isAllowed = false;
      // Check if authorized user still has ADMINISTRATOR rights
      for (AclEntry aclEntry : aclEntries) {
        LOG.trace("'{}' has '{}' rights!", aclEntry.getSid(), aclEntry.getPermission());
        if (aclEntry.getPermission().atLeast(PERMISSION.ADMINISTRATE) && authorizationIdentities.contains(aclEntry.getSid())) {
          isAllowed = true;
          LOG.trace("Confirm permission for updating ACL: '{}' has '{}' rights!", aclEntry.getSid(), PERMISSION.ADMINISTRATE);
          break;
        }
      }
      if (!isAllowed) {
        if (currentAcl) {
          LOG.warn(errorMessage1);
          throw new AccessForbiddenException(errorMessage1);
        } else {
          LOG.warn(errorMessage2);
          throw new BadArgumentException(errorMessage2);
        }
      }
    }
    return isAllowed;
  }

  /**
   * Create/Ingest an instance of MetadataSchemaRecord.
   *
   * @param applicationProperties Settings of repository.
   * @param recordDocument Record of the schema.
   * @param document Schema document.
   * @return Record of registered schema document.
   */
  public static DataResource createDataResourceRecord4Schema(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument,
          MultipartFile document) {
    DataResource metadataRecord;

    // Do some checks first.
    metadataRecord = checkParameters(recordDocument, document, true);
    Objects.requireNonNull(metadataRecord);
    if (metadataRecord.getId() == null) {
      String message = "Mandatory attribute 'id' not found in record. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    // Check if id is lower case and URL encodable.
    // and save as alternate identifier. (In case of
    // upper letters in both versions (with and without
    // upper letters)
    DataResourceRecordUtil.check4validSchemaId(metadataRecord);
    // End of parameter checks
    // validate schema document / determine type if not given
    validateMetadataSchemaDocument(applicationProperties, metadataRecord, document);
    // set internal parameters
    if (metadataRecord.getResourceType() == null) {
      LOG.trace("No mimetype set! Try to determine...");
      if (document.getContentType() != null) {
        LOG.trace("Set mimetype determined from document: '{}'", document.getContentType());
        metadataRecord.getFormats().add(document.getContentType());
      }
    }
    metadataRecord.setVersion(Long.toString(1));
    // create record.
    DataResource dataResource = metadataRecord;
    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
    // store document
    ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, t -> "somethingStupid");
    // Create schema record
    SchemaRecord schemaRecord = createSchemaRecord(dataResource, contentInformation);
    MetadataSchemaRecordUtil.saveNewSchemaRecord(schemaRecord);

    // Settings for OAI PMH
    if (MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(schemaRecord.getType())) {
      try {
        MetadataFormat metadataFormat = new MetadataFormat();
        metadataFormat.setMetadataPrefix(schemaRecord.getSchemaIdWithoutVersion());
        metadataFormat.setSchema(schemaRecord.getAlternateId());
        String documentString = new String(document.getBytes(), Charset.defaultCharset());
        LOG.trace(documentString);
        String metadataNamespace = SchemaUtils.getTargetNamespaceFromSchema(document.getBytes());
        metadataFormat.setMetadataNamespace(metadataNamespace);
        metadataFormatDao.save(metadataFormat);
      } catch (IOException ex) {
        String message = LOG_ERROR_READ_METADATA_DOCUMENT;
        LOG.error(message, ex);
        throw new UnprocessableEntityException(message);
      }
    }
    // reload data resource
    metadataRecord = DataResourceRecordUtil.getRecordByIdAndVersion(applicationProperties, metadataRecord.getId(), Long.valueOf(metadataRecord.getVersion()));

    return metadataRecord;
  }

  /**
   * Create/Ingest an instance of MetadataRecord.
   *
   * @param applicationProperties Settings of repository.
   * @param recordDocument Record of the metadata.
   * @param document Schema document.
   * @return Record of registered schema document.
   */
  public static DataResource createDataResourceRecord4Metadata(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument,
          MultipartFile document) {
    DataResource dataResource;

    // Do some checks first.
    dataResource = checkParameters(recordDocument, document, true);
    Objects.requireNonNull(dataResource);
    if (dataResource.getId() != null) {
      // Optional id set. Check for valid ID
      check4validId(dataResource, true);
    }
    // End of parameter checks
    // Fix internal references, of necessary
    fixRelatedSchemaIfNeeded(dataResource);
    // validate schema document / determine or correct resource type
    validateMetadataDocument(applicationProperties, dataResource, document);

    dataResource.setVersion(getSchemaRecordFromDataResource(dataResource).getVersion().toString());
    // create record.
    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
    // store document
    ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, t -> "somethingStupid");
    dataResource = DataResourceRecordUtil.getRecordByIdAndVersion(applicationProperties, dataResource.getId(), Long.valueOf(dataResource.getVersion()));

    return dataResource;
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
  public static DataResource updateDataResource4MetadataDocument(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          MultipartFile recordDocument,
          MultipartFile document,
          UnaryOperator<String> supplier) {
    DataResource givenDataResource;
    givenDataResource = checkParameters(recordDocument, document, false);

    return updateDataResource4MetadataDocument(applicationProperties, resourceId, eTag, givenDataResource, document, supplier);
  }

  public static DataResource updateDataResource4MetadataDocument(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          DataResource givenDataResource,
          MultipartFile document,
          UnaryOperator<String> supplier) {
    DataResource updatedDataResource;

    LOG.trace("Obtaining most recent datacite record with id {}.", resourceId);
    DataResource oldDataResource = applicationProperties.getDataResourceService().findById(resourceId);
    ControllerUtils.checkEtag(eTag, oldDataResource);
    LOG.trace("ETag: '{}'", oldDataResource.getEtag());
    DataResource mergedDataResource = mergeDataResource(oldDataResource, givenDataResource);
    updatedDataResource = fixRelatedSchemaIfNeeded(mergedDataResource);

    if (document != null) {
      updateMetadataDocument(applicationProperties, updatedDataResource, document, supplier);
    } else {
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, updatedDataResource);
      // validate if document is still valid due to changed record settings.
      if (info != null) {
        String metadataDocumentUri = info.getContentUri();
        Path metadataDocumentPath = Paths.get(URI.create(metadataDocumentUri));
        if (!Files.exists(metadataDocumentPath) || !Files.isRegularFile(metadataDocumentPath) || !Files.isReadable(metadataDocumentPath)) {
          LOG.warn("Metadata document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", metadataDocumentPath);
          throw new CustomInternalServerError("Metadata document on server either does not exist or is no file or is not readable.");
        }
        // test if document is still valid for updated(?) schema.
        try {
          InputStream inputStream = Files.newInputStream(metadataDocumentPath);
          SchemaRecord schemaRecord = DataResourceRecordUtil.getSchemaRecordFromDataResource(updatedDataResource);
          MetadataSchemaRecordUtil.validateMetadataDocument(applicationProperties, inputStream, schemaRecord);
        } catch (IOException ex) {
          LOG.error("Error validating file!", ex);
        }
      } else {
        throw new CustomInternalServerError("Metadata document on server does not exist!");
      }

    }
    oldDataResource = DataResourceUtils.updateResource(applicationProperties, resourceId, updatedDataResource, eTag, supplier);

    return oldDataResource;
  }

  /**
   * Add or replace link to predecessor.
   *
   * @param newDataResource Data resource holding the new version.
   */
  public static void addProvenance(DataResource newDataResource) {
    if (Long.parseLong(newDataResource.getVersion()) > 1L) {
      replaceIsDerivedFrom(newDataResource);
    }
  }

  /**
   * Replace outdated link to predecessor with new one.
   *
   * @param newDataResource Data resource holding the new version.
   */
  public static void replaceIsDerivedFrom(DataResource newDataResource) {
    boolean foundOldIdentifier = false;
    long oldVersion = Long.parseLong(newDataResource.getVersion()) - 1L;
    String urlToPredecessor = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class).
            getMetadataDocumentById(newDataResource.getId(), oldVersion, null, null)).
            toUri().
            toString();
    for (RelatedIdentifier item : newDataResource.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(DataResourceRecordUtil.RELATED_NEW_VERSION_OF)) {
        String oldUrl = item.getValue();
        item.setValue(urlToPredecessor);
        item.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
        LOG.trace("Fix related identifier 'isDerivedFrom' : '{}' -> '{}'", oldUrl, urlToPredecessor);
        foundOldIdentifier = true;
      }
    }
    if (!foundOldIdentifier) {
      RelatedIdentifier newRelatedIdentifier = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_NEW_VERSION_OF, urlToPredecessor, null, null);
      newRelatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      newDataResource.getRelatedIdentifiers().add(newRelatedIdentifier);
    }
  }

  /**
   * Delete a digital object with given identifier.
   *
   * @param applicationProperties Configuration properties.
   * @param id Identifier of digital object.
   * @param eTag ETag of the old digital object.
   * @param supplier Function for updating record.
   */
  public static void deleteDataResourceRecord(MetastoreConfiguration applicationProperties,
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
   * Migrate data resource to metadata record.
   *
   * @param applicationProperties Configuration settings of repository.
   * @param dataResource Data resource to migrate.
   * @return Metadata record of data resource.
   */
  public static MetadataRecord migrateToMetadataRecordV2(RepoBaseConfiguration applicationProperties,
          DataResource dataResource) {
    MetadataRecord metadataRecord = new MetadataRecord();
    if (dataResource != null) {
      metadataRecord.setId(dataResource.getId());
      metadataRecord.setAcl(dataResource.getAcls());

      metadataRecord.setCreatedAt(getCreationDate(dataResource));
      metadataRecord.setLastUpdate(dataResource.getLastUpdate());

      PrimaryIdentifier pid = dataResource.getIdentifier();
      if ((pid != null) && pid.hasDoi()) {
        metadataRecord.setPid(ResourceIdentifier.factoryResourceIdentifier(pid.getValue(), IdentifierType.valueOf(pid.getIdentifierType())));
      }
      metadataRecord.setRecordVersion(getVersion(dataResource));

      setSchemaAndVersion(metadataRecord, dataResource);
      setRelatedResource(metadataRecord, dataResource);

      if (metadataRecord.getSchema() == null) {
        String message = "Missing schema identifier for metadata document. Not a valid metadata document ID. Returning HTTP BAD_REQUEST.";
        LOG.error(message);
        throw new BadArgumentException(message);
      }
      LOG.trace("Get document URI from ContentInformation.");
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, dataResource);
      if (info != null) {
        metadataRecord.setDocumentHash(info.getHash());
        metadataRecord.setMetadataDocumentUri(info.getContentUri());
      }
      // Only one license allowed. So don't worry about size of set.
      if (!dataResource.getRights().isEmpty()) {
        metadataRecord.setLicenseUri(dataResource.getRights().iterator().next().getSchemeUri());
      }
    }

    return metadataRecord;
  }

  /**
   * Migrate data resource to metadata record.
   *
   * @param applicationProperties Configuration settings of repository.
   * @param dataResource Data resource to migrate.
   * @return Metadata record of data resource.
   */
  public static MetadataSchemaRecord migrateToMetadataSchemaRecordV2(RepoBaseConfiguration applicationProperties,
          DataResource dataResource) {
    MetadataSchemaRecord metadataSchemaRecord = new MetadataSchemaRecord();
    if (dataResource != null) {
      metadataSchemaRecord.setSchemaId(dataResource.getId());
      metadataSchemaRecord.setAcl(dataResource.getAcls());

      metadataSchemaRecord.setCreatedAt(getCreationDate(dataResource));
      metadataSchemaRecord.setLastUpdate(dataResource.getLastUpdate());

      PrimaryIdentifier pid = dataResource.getIdentifier();
      if ((pid != null) && pid.hasDoi()) {
        metadataSchemaRecord.setPid(ResourceIdentifier.factoryResourceIdentifier(pid.getValue(), IdentifierType.valueOf(pid.getIdentifierType())));
      }
      metadataSchemaRecord.setSchemaVersion(getVersion(dataResource));

      LOG.trace("Get document URI from ContentInformation.");
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, dataResource);
      if (info != null) {
        metadataSchemaRecord.setSchemaHash(info.getHash());
        metadataSchemaRecord.setSchemaDocumentUri(info.getContentUri());
      }
      // Only one license allowed. So don't worry about size of set.
      if (!dataResource.getRights().isEmpty()) {
        metadataSchemaRecord.setLicenseUri(dataResource.getRights().iterator().next().getSchemeUri());
      }
    }

    return metadataSchemaRecord;
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
   * Validate metadata document with given schema.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param schemaRecord metadata of the schema document.
   * @param document document
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          SchemaRecord schemaRecord) {
    LOG.trace("validateMetadataDocument (schemaRecord) {},{}, {}", metastoreProperties, schemaRecord, document);
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    URI pathToSchemaFile = URI.create(schemaRecord.getSchemaDocumentUri());
    try {
      switch (pathToSchemaFile.getScheme()) {
        case "file":
          // check file
          Path schemaDocumentPath = Paths.get(pathToSchemaFile);
          if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
            LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
            String errorMessage = "Schema document on server either does not exist or is no file or is not readable.";
            throw new CustomInternalServerError(errorMessage);
          }
          byte[] schemaDocument = FileUtils.readFileToByteArray(schemaDocumentPath.toFile());
          IValidator applicableValidator;
          String mediaType = null;
          switch (schemaRecord.getType()) {
            case JSON:
              mediaType = MediaType.APPLICATION_JSON_VALUE;
              break;
            case XML:
              mediaType = MediaType.APPLICATION_XML_VALUE;
              break;
            default:
              LOG.error("Unkown schema type: '" + schemaRecord.getType() + "'");
          }
          applicableValidator = getValidatorForRecord(metastoreProperties, mediaType, schemaDocument);
          if (applicableValidator == null) {
            String message = "No validator found for schema type " + mediaType;
            LOG.error(message);
            throw new UnprocessableEntityException(message);
          } else {
            LOG.trace("Validator found.");
            LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", schemaRecord.getSchemaId(), schemaRecord.getVersion(), applicableValidator);
            if (!applicableValidator.validateMetadataDocument(schemaDocumentPath.toFile(), document.getInputStream())) {
              LOG.warn("Metadata document validation failed. -> " + applicableValidator.getErrorMessage());
              throw new UnprocessableEntityException(applicableValidator.getErrorMessage());
            }
          }
          LOG.trace("Metadata document validation succeeded.");
          break;
        case "http":
        case "https":
        default:
          throw new CustomInternalServerError("Protocol of schema ('" + pathToSchemaFile.getScheme() + "') is not supported yet!");
      }
    } catch (IOException ex) {
      java.util.logging.Logger.getLogger(DataResourceRecordUtil.class.getName()).log(Level.SEVERE, null, ex);
      throw new CustomInternalServerError("Schema '" + pathToSchemaFile + "' is not accessible!");
    }
  }

  public static DataResource getRecordById(MetastoreConfiguration metastoreProperties,
          String recordId) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(metastoreProperties, recordId, null);
  }

  public static DataResource getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    LOG.trace("Obtaining record with id {} and version {}.", recordId, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    long nano = System.nanoTime() / 1000000;
    long nano2;
    Page<DataResource> dataResource;
    try {
      dataResource = metastoreProperties.getDataResourceService().findAllVersions(recordId, null);
      if (LOG.isTraceEnabled()) {
        for (DataResource item : dataResource.getContent()) {
          LOG.trace("Id: '{}' - Version: '{}' -> Type: '{}'", item.getId(), item.getVersion(), item.getResourceType().toString());
        }
      }
    } catch (ResourceNotFoundException ex) {
      ex.setDetail("Document with ID '" + recordId + "' doesn't exist!");
      throw ex;
    }
    nano2 = System.nanoTime() / 1000000;
    Stream<DataResource> stream = dataResource.get();
    if (version != null) {
      stream = stream.filter(resource -> Long.parseLong(resource.getVersion()) == version);
    }
    Optional<DataResource> findFirst = stream.findFirst();
    if (findFirst.isEmpty()) {
      String message = String.format("Version '%d' of ID '%s' doesn't exist!", version, recordId);
      LOG.error(message);
      throw new ResourceNotFoundException(message);
    }
    long nano3 = System.nanoTime() / 1000000;
    LOG.info("getRecordByIdAndVersion {}, {}, {}", nano, (nano2 - nano), (nano3 - nano));
    return findFirst.get();
  }

  public static ContentInformation getContentInformationByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    LOG.trace("Obtaining content information record with id {} and version {}.", recordId, version);
    return metastoreProperties.getContentInformationService().getContentInformation(recordId, null, version);
  }

  public static Path getMetadataDocumentByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    LOG.trace("Obtaining content information record with id {} and version {}.", recordId, version);
    ContentInformation metadataRecord = getContentInformationByIdAndVersion(metastoreProperties, recordId, version);

    URI metadataDocumentUri = URI.create(metadataRecord.getContentUri());

    Path metadataDocumentPath = Paths.get(metadataDocumentUri);
    if (!Files.exists(metadataDocumentPath) || !Files.isRegularFile(metadataDocumentPath) || !Files.isReadable(metadataDocumentPath)) {
      LOG.warn("Metadata document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", metadataDocumentPath);
      throw new CustomInternalServerError("Metadata document on server either does not exist or is no file or is not readable.");
    }
    return metadataDocumentPath;
  }

  public static Specification<DataResource> findByAccessRights(Specification<DataResource> spec) {
    // Add authentication if enabled
    if (schemaConfig.isAuthEnabled()) {
      boolean isAdmin;
      isAdmin = AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString());
      // Add authorization for non administrators
      if (!isAdmin) {
        List<String> authorizationIdentities = AuthenticationHelper.getAuthorizationIdentities();
        if (authorizationIdentities != null) {
          LOG.trace("Creating (READ) permission specification. '{}'", authorizationIdentities);
          Specification<DataResource> permissionSpec = PermissionSpecification.toSpecification(authorizationIdentities, PERMISSION.READ);
          spec = spec.and(permissionSpec);
        } else {
          LOG.trace("No permission information provided. Skip creating permission specification.");
        }
      }
    }
    return spec;
  }

  /**
   * Create specification for all listed schemaIds.
   *
   * @param specification Specification for search.
   * @param schemaIds Provided schemaIDs...
   * @return Specification with schemaIds added.
   */
  public static Specification<DataResource> findBySchemaId(Specification<DataResource> specification, List<String> schemaIds) {
    Specification<DataResource> specWithSchema = specification;
    if (schemaIds != null) {
      List<String> allSchemaIds = new ArrayList<>();
      for (String schemaId : schemaIds) {
        allSchemaIds.add(schemaId);
        List<SchemaRecord> allVersions = schemaRecordDao.findBySchemaIdStartsWithOrderByVersionDesc(schemaId + SCHEMA_VERSION_SEPARATOR);
        for (SchemaRecord schemaRecord : allVersions) {
          allSchemaIds.add(schemaRecord.getAlternateId());
        }
      }
      if (!allSchemaIds.isEmpty()) {
        specWithSchema = specWithSchema.and(RelatedIdentifierSpec.toSpecification(DataResourceRecordUtil.RELATED_SCHEMA_TYPE, allSchemaIds.toArray(String[]::new)));
      }
    }
    return specWithSchema;
  }

  public static final Specification<DataResource> findByMimetypes(List<String> mimeTypes) {
    // Search for both mimetypes (xml & json)
    ResourceType resourceType;
    final int JSON = 1; // bit 0
    final int XML = 2;  // bit 1
    // 
    int searchFor = 0; // 1 - JSON, 2 - XML, 3 - both
    if (mimeTypes != null) {
      for (String mimeType : mimeTypes) {
        if (mimeType.contains("json")) {
          searchFor |= JSON;
        }
        if (mimeType.contains("xml")) {
          searchFor |= XML;
        }
      }
    } else {
      searchFor = JSON | XML;
    }
    resourceType = switch (searchFor) {
      // 1 -> search for JSON only
      case JSON ->
        ResourceType.createResourceType(DataResourceRecordUtil.JSON_SCHEMA_TYPE, ResourceType.TYPE_GENERAL.MODEL);
      // 2 -> search for XML only
      case XML ->
        ResourceType.createResourceType(DataResourceRecordUtil.XML_SCHEMA_TYPE, ResourceType.TYPE_GENERAL.MODEL);
      // 3 -> Search for both mimetypes (xml & json)
      case JSON | XML ->
        ResourceType.createResourceType(DataResourceRecordUtil.SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL);
      // 0 -> Unknown mimetype
      default ->
        ResourceType.createResourceType("unknown");
    };

    return ResourceTypeSpec.toSpecification(resourceType);
  }

  /**
   * Create specification for all listed schemaIds.
   *
   * @param specification Specification for search.
   * @param relatedIds Provided schemaIDs...
   * @return Specification with schemaIds added.
   */
  public static Specification<DataResource> findByRelatedId(Specification<DataResource> specification, List<String> relatedIds) {
    Specification<DataResource> specWithSchema = specification;
    if ((relatedIds != null) && !relatedIds.isEmpty()) {
      specWithSchema = specWithSchema.and(RelatedIdentifierSpec.toSpecification(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, relatedIds.toArray(String[]::new)));
    }
    return specWithSchema;
  }

  /**
   * Check validity of acl list and then merge new acl list in the existing one.
   *
   * @param managed Existing metadata record.
   * @param provided New metadata record.
   * @return Merged list
   */
  public static Set<AclEntry> mergeAcl(Set<AclEntry> managed, Set<AclEntry> provided) {
    // Check for null parameters (which shouldn't happen)
    managed = (managed == null) ? new HashSet<>() : managed;
    provided = (provided == null) ? new HashSet<>() : provided;
    if (!provided.isEmpty()) {
      if (!provided.equals(managed)) {
        // check for special access rights
        // - only administrators are allowed to change ACL
        checkAccessRights(managed, true);
        // - at least principal has to remain as ADMIN
        checkAccessRights(provided, false);
        LOG.trace("Updating record acl from {} to {}.", managed, provided);
        managed = provided;
      } else {
        LOG.trace("Provided ACL is still the same -> Continue using old one.");
      }
    } else {
      LOG.trace("Provided ACL is empty -> Continue using old one.");
    }
    return managed;
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
    return mergeEntry(description, managed, provided, false);
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
    if ((provided != null && !provided.equals(managed))
            || overwriteWithNull) {
      LOG.trace(description + " from '{}' to '{}'", managed, provided);
      managed = provided;
    }
    return managed;
  }

  /**
   * Return the number of ingested documents. If there are two versions of the
   * same document this will be counted as one.
   *
   * @return Number of registered documents.
   */
  public static long getNoOfMetadataDocuments() {
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(METADATA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    Pageable pgbl = PageRequest.of(0, 1);
    return queryDataResources(spec, pgbl).getTotalElements();
  }

  /**
   * Return the number of ingested schema documents. If there are two versions
   * of the same document this will be counted as one.
   *
   * @return Number of registered documents.
   */
  public static long getNoOfSchemaDocuments() {
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    Pageable pgbl = PageRequest.of(0, 1);
    return queryDataResources(spec, pgbl).getTotalElements();
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

  /**
   * Set DAO for data record.
   *
   * @param aDataResourceDao the dataResourceDao to set
   */
  public static void setDataResourceDao(IDataResourceDao aDataResourceDao) {
    dataResourceDao = aDataResourceDao;
  }

  /**
   * Set the DAO for MetadataFormat.
   *
   * @param aMetadataFormatDao the metadataFormatDao to set
   */
  public static void setMetadataFormatDao(IMetadataFormatDao aMetadataFormatDao) {
    metadataFormatDao = aMetadataFormatDao;
  }

  /**
   * Set the DAO for SchemaRecord.
   *
   * @param aSchemaRecordDao the schemaRecordDao to set
   */
  public static void setSchemaRecordDao(ISchemaRecordDao aSchemaRecordDao) {
    schemaRecordDao = aSchemaRecordDao;
  }

  public static final void fixMetadataDocumentUri(MetadataRecord metadataRecord) {
    String metadataDocumentUri = metadataRecord.getMetadataDocumentUri();
    metadataRecord
            .setMetadataDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class
            ).getMetadataDocumentById(metadataRecord.getId(), metadataRecord.getRecordVersion(), null, null)).toUri().toString());
    LOG.trace("Fix metadata document Uri '{}' -> '{}'", metadataDocumentUri, metadataRecord.getMetadataDocumentUri());
  }

  public static final void fixSchemaUrl(DataResource dataresource) {
    RelatedIdentifier schemaIdentifier = getSchemaIdentifier(dataresource);
    fixSchemaUrl(schemaIdentifier);
  }

  public static final void fixSchemaUrl(RelatedIdentifier schemaIdentifier) {
    if (schemaIdentifier != null && schemaIdentifier.getIdentifierType().equals(Identifier.IDENTIFIER_TYPE.INTERNAL)) {
      String value = schemaIdentifier.getValue();
      StringTokenizer tokenizer = new StringTokenizer(schemaIdentifier.getValue(), SCHEMA_VERSION_SEPARATOR);
      Long version = null;
      String schemaId = null;
      SchemaRecord schemaRecord = null;
      switch (tokenizer.countTokens()) {
        case 2:
          schemaId = tokenizer.nextToken();
          version = Long.parseLong(tokenizer.nextToken());
          schemaRecord = schemaRecordDao.findBySchemaId(schemaId + SCHEMA_VERSION_SEPARATOR + version);
          break;
        case 1:
          schemaId = tokenizer.nextToken();
          schemaRecord = schemaRecordDao.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId + SCHEMA_VERSION_SEPARATOR);
          break;
        default:
          throw new CustomInternalServerError("Invalid schemaId!");
      }

      schemaIdentifier.setValue(schemaRecord.getAlternateId());
      schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      LOG.trace("Fix scheme Url '{}' -> '{}'", value, schemaIdentifier.getValue());
    }
  }

  public static void checkLicense(DataResource dataResource, String licenseUri) {
    if (licenseUri != null) {
      Set<Scheme> rights = dataResource.getRights();
      String licenseId = licenseUri.substring(licenseUri.lastIndexOf(SCHEMA_VERSION_SEPARATOR));
      Scheme license = Scheme.factoryScheme(licenseId, licenseUri);
      if (rights.isEmpty()) {
        rights.add(license);
      } else {
        // Check if license already exists (only one license allowed)
        if (!rights.contains(license)) {
          rights.clear();
          rights.add(license);
        }
      }
    } else {
      // Remove license
      dataResource.getRights().clear();
    }
  }

  /**
   * Test if exactly one schema and one related resource exists. This method
   * does NOT check the correctness of the references.
   *
   * @param dataResource Data resource of a metadata document.
   * @throws BadArgumentException Related resources are not defined as expected.
   */
  public static void validateRelatedResources4MetadataDocuments(DataResource dataResource) throws BadArgumentException {
    int noOfRelatedData = 0;
    int noOfRelatedSchemas = 0;
    if (dataResource != null) {
      Set<RelatedIdentifier> relatedResources = dataResource.getRelatedIdentifiers();

      // Check if related resource already exists (only one related resource of type hasMetadata is allowed)
      for (RelatedIdentifier item : relatedResources) {
        if (item.getRelationType() == DataResourceRecordUtil.RELATED_SCHEMA_TYPE) {
          noOfRelatedSchemas++;
        }
        if (item.getRelationType() == DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE) {
          noOfRelatedData++;
        }
      }
    }
    checkNoOfRelatedIdentifiers(noOfRelatedData, noOfRelatedSchemas);
  }
  /** Validate related identifiers.
   * There has to be exactly one schema (hasMetadata) 
   * and at *least* one related data resource.
   * @param noOfRelatedData No of related data resources.
   * @param noOfRelatedSchemas No of related schemas.
   */
  private static void checkNoOfRelatedIdentifiers(int noOfRelatedData, int noOfRelatedSchemas) {
    if ((noOfRelatedSchemas != 1) || (noOfRelatedData == 0)) {
      String errorMessage = "";
      if (noOfRelatedSchemas == 0) {
        errorMessage = "Mandatory attribute relatedIdentifier of type '" + DataResourceRecordUtil.RELATED_SCHEMA_TYPE + "' was not found in record. \n";
      }       
      if (noOfRelatedSchemas > 1) {
        errorMessage = "Mandatory attribute relatedIdentifier of type '" + DataResourceRecordUtil.RELATED_SCHEMA_TYPE + "' was provided more than once in record. \n";
      }
      if (noOfRelatedData == 0) {
        errorMessage = errorMessage + "Mandatory attribute relatedIdentifier of type '" + DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE + "' was not found in record. \n";
      }
      errorMessage = errorMessage + "Returning HTTP BAD_REQUEST.";
      LOG.error(errorMessage);
      throw new BadArgumentException(errorMessage);
    }
  }

  /**
   * Get schema identifier of data resource.
   *
   * @param dataResourceRecord Metadata record hold schema identifier.
   * @return RelatedIdentifier with a global accessible identifier.
   */
  public static RelatedIdentifier getSchemaIdentifier(DataResource dataResourceRecord) {
    LOG.trace("Get schema identifier for '{}'.", dataResourceRecord.getId());
    return getRelatedIdentifier(dataResourceRecord, DataResourceRecordUtil.RELATED_SCHEMA_TYPE);
  }

  /**
   * Transform schema identifier to global available identifier (if neccessary).
   *
   * @param dataResourceRecord Metadata record hold schema identifier.
   * @param relationType Relation type of the identifier.
   * @return ResourceIdentifier with a global accessible identifier.
   */
  public static RelatedIdentifier getRelatedIdentifier(DataResource dataResourceRecord, RelatedIdentifier.RELATION_TYPES relationType) {
    LOG.trace("Get related identifier for '{}' of type '{}'.", dataResourceRecord.getId(), relationType);
    RelatedIdentifier relatedIdentifier = null;

    Set<RelatedIdentifier> relatedResources = dataResourceRecord.getRelatedIdentifiers();

    // Check if related resource already exists (only one related resource of type DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE allowed)
    for (RelatedIdentifier item : relatedResources) {
      if (item.getRelationType().equals(relationType)) {
        relatedIdentifier = item;
      }
    }
    return relatedIdentifier;
  }

  /**
   * Check if ID for schema is valid. Requirements: - shouldn't change if URL
   * encoded - should be lower case If it's not lower case the original ID will
   * we set as an alternate ID.
   *
   * @param metadataRecord Datacite Record.
   */
  public static final void check4validSchemaId(DataResource metadataRecord) {
    // schema id should be lower case due to elasticsearch
    // alternate identifier is used to set id to a given id.
    check4validId(metadataRecord, false);
  }

  public static final void check4validId(DataResource metadataRecord, boolean allowUpperCase) {
    String id = metadataRecord.getId();
    String lowerCaseId;
    lowerCaseId = id.toLowerCase(Locale.getDefault());

    if (allowUpperCase) {
      lowerCaseId = id;
    }
    metadataRecord.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(lowerCaseId));
    if (!lowerCaseId.equals(id)) {
      metadataRecord.getAlternateIdentifiers().add(Identifier.factoryIdentifier(id, Identifier.IDENTIFIER_TYPE.OTHER));
    }

    String value = URLEncoder.encode(metadataRecord.getId(), StandardCharsets.UTF_8);
    if (!value.equals(metadataRecord.getId())) {
      String message = "Not a valid ID! Encoded: " + value;
      LOG.error(message);
      throw new BadArgumentException(message);
    }

  }

  private static void validateMetadataSchemaDocument(MetastoreConfiguration metastoreProperties, DataResource schemaRecord, MultipartFile document) {
    LOG.debug("Validate metadata schema document...");
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata schema document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      validateMetadataSchemaDocument(metastoreProperties, schemaRecord, document.getBytes());
    } catch (IOException ex) {
      String message = LOG_ERROR_READ_METADATA_DOCUMENT;
      LOG.error(message, ex);
      throw new UnprocessableEntityException(message);
    }
  }

  private static void validateMetadataSchemaDocument(MetastoreConfiguration metastoreProperties, DataResource dataResource, byte[] document) {
    LOG.debug("Validate metadata schema document...");
    if (document == null || document.length == 0) {
      String message = "Missing metadata schema document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }

    IValidator applicableValidator;
    try {
      applicableValidator = getValidatorForRecord(metastoreProperties, dataResource, document);

      if (applicableValidator == null) {
        String message = "No validator found for schema type " + dataResource.getResourceType().getValue() + ". Returning HTTP UNPROCESSABLE_ENTITY.";
        LOG.error(message);
        throw new UnprocessableEntityException(message);
      } else {
        LOG.trace("Validator found. Checking provided schema file.");
        LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", dataResource.getId(), dataResource.getVersion(), applicableValidator);
        try (InputStream inputStream = new ByteArrayInputStream(document)) {
          if (!applicableValidator.isSchemaValid(inputStream)) {
            String message = "Metadata schema document validation failed. Returning HTTP UNPROCESSABLE_ENTITY.";
            LOG.warn(message);
            if (LOG.isTraceEnabled()) {
              LOG.trace("Schema: \n'{}'", new String(document, StandardCharsets.UTF_8));
            }
            throw new UnprocessableEntityException(message);
          }
        }
      }
    } catch (IOException ex) {
      String message = LOG_ERROR_READ_METADATA_DOCUMENT;
      LOG.error(message, ex);
      throw new UnprocessableEntityException(message);
    }

    LOG.trace("Schema document is valid!");
  }

  private static IValidator getValidatorForRecord(MetastoreConfiguration metastoreProperties, String mimeType, byte[] schemaDocument) {
    IValidator applicableValidator = null;

    //obtain/guess record type
    if (mimeType == null) {
      String formatDetected = SchemaUtils.guessMimetype(schemaDocument);
      if (formatDetected == null) {
        String message = "Unable to detect schema type automatically. Please provide a valid type";
        LOG.error(message);
        throw new UnprocessableEntityException(message);
      } else {
        String type;
        if (formatDetected.contains("json")) {
          type = JSON + SCHEMA_SUFFIX;
        } else {
          type = XML + SCHEMA_SUFFIX;
        }
        mimeType = formatDetected;
        LOG.debug("Automatically detected mimetype of schema: '{}' -> '{}'.", formatDetected, type);
      }
    }
    for (IValidator validator : metastoreProperties.getValidators()) {
      if (validator.supportsMimetype(mimeType)) {
        applicableValidator = validator.getInstance();
        LOG.trace("Found validator for mime type: '{}'", mimeType);
        return applicableValidator;
      }
    }
    return applicableValidator;
  }

  private static IValidator getValidatorForRecord(MetastoreConfiguration metastoreProperties, DataResource schemaRecord, byte[] schemaDocument) {
    IValidator applicableValidator = null;
    //obtain/guess record type
    if ((schemaRecord.getResourceType() == null)
            || (schemaRecord.getResourceType().getValue() == null)) {
      String formatDetected = SchemaUtils.guessMimetype(schemaDocument);
      if (formatDetected == null) {
        String message = "Unable to detect schema type automatically. Please provide a valid type";
        LOG.error(message);
        throw new UnprocessableEntityException(message);
      } else {
        String type;
        if (formatDetected.contains("json")) {
          type = JSON + SCHEMA_SUFFIX;
        } else {
          type = XML + SCHEMA_SUFFIX;
        }
        schemaRecord.setResourceType(ResourceType.createResourceType(type, ResourceType.TYPE_GENERAL.MODEL));
        LOG.debug("Automatically detected mimetype of schema: '{}' -> '{}'.", formatDetected, type);
      }
    }
    String schemaType = schemaRecord.getResourceType().getValue().replace(SCHEMA_SUFFIX, "").replace(METADATA_SUFFIX, "");
    for (IValidator validator : metastoreProperties.getValidators()) {
      if (validator.supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE.valueOf(schemaType))) {
        applicableValidator = validator.getInstance();
        LOG.trace("Found validator for schema: '{}'", schemaType);
        return applicableValidator;
      }
    }
    return applicableValidator;
  }

  private static DataResource checkParameters(MultipartFile dataResourceRecord, MultipartFile document, boolean bothRequired) {
    boolean recordNotAvailable;
    boolean documentNotAvailable;
    DataResource metadataRecord = null;

    recordNotAvailable = dataResourceRecord == null || dataResourceRecord.isEmpty();
    documentNotAvailable = document == null || document.isEmpty();
    String message = null;
    if (bothRequired && (recordNotAvailable || documentNotAvailable)) {
      message = "No data resource record and/or metadata document provided. Returning HTTP BAD_REQUEST.";
    } else {
      if (!bothRequired && recordNotAvailable && documentNotAvailable) {
        message = "Neither metadata record nor metadata document provided.";
      }
    }
    if (message != null) {
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    // Do some checks first.
    if (!recordNotAvailable) {
      metadataRecord = getDataResourceFromBody(dataResourceRecord);
    }
    return metadataRecord;
  }

  private static DataResource getDataResourceFromBody(MultipartFile dataResourceRecord) {
    DataResource metadataRecord = null;
    try {
      metadataRecord = Json.mapper().readValue(dataResourceRecord.getInputStream(), DataResource.class);
    } catch (IOException ex) {
      String message = "Can't map record document to DataResource";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error(ERROR_PARSING_JSON, ex);
      throw new BadArgumentException(message);
    }
    return metadataRecord;
  }

  /**
   * Validate metadata document with given schema. In case of an error a runtime
   * exception is thrown.
   *
   * @param metastoreProperties Configuration properties.
   * @param document Document to validate.
   * @param schemaId SchemaId of schema.
   * @param version Version of the document.
   */
  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          String schemaId,
          Long version) {
    LOG.trace("validateMetadataDocument (schemaId) {},SchemaID {}, Version {}, {}", metastoreProperties, schemaId, version, document);
    SchemaRecord schemaRecord;
    DataResource dataResource = DataResourceRecordUtil.getRecordById(metastoreProperties, schemaId);
    if (dataResource == null) {
      String message = "Unknown schemaID '" + schemaId + "'!";
      LOG.error(message);
      throw new ResourceNotFoundException(message);
    }
    schemaId = dataResource.getId();
    if (version != null) {
      schemaRecord = schemaRecordDao.findBySchemaId(schemaId + SCHEMA_VERSION_SEPARATOR + version);
    } else {
      schemaRecord = schemaRecordDao.findBySchemaIdStartsWithOrderByVersionDesc(schemaId + SCHEMA_VERSION_SEPARATOR).get(0);
    }
    if (schemaRecord == null) {
      String message = "Unknown version '" + version + "' for schemaID '" + schemaId + "'!";
      LOG.error(message);
      throw new ResourceNotFoundException(message);
    }
    validateMetadataDocument(metastoreProperties, document, schemaRecord);
  }

  private static SchemaRecord getSchemaRecordFromDataResource(DataResource dataResource) {
    SchemaRecord schemaRecord = null;
    RelatedIdentifier schemaIdentifier = getSchemaIdentifier(dataResource);
    if ((schemaIdentifier != null) && (schemaIdentifier.getValue() != null)) {
      String schemaId = schemaIdentifier.getValue();
      LOG.trace("getSchemaRecordFromDataResource: related identifier:  '{}'", schemaIdentifier);
      LOG.trace("getSchemaRecordFromDataResource: '{}'", schemaId);
      switch (schemaIdentifier.getIdentifierType()) {
        case URL:
          schemaRecord = schemaRecordDao.findByAlternateId(schemaIdentifier.getValue());
          break;
        case INTERNAL:
          String[] split;
          split = schemaId.split(SCHEMA_VERSION_SEPARATOR, -1);
          if (split.length == 1) {
            schemaRecord = schemaRecordDao.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId + SCHEMA_VERSION_SEPARATOR);
          } else {
            schemaRecord = schemaRecordDao.findBySchemaId(schemaId);
          }
          break;
        default:
          String message = "Unsupported identifier type: '" + schemaIdentifier.getIdentifierType() + "'!";
          LOG.error(message);
          throw new ResourceNotFoundException(message);
      }
    }
    return schemaRecord;
  }

  /**
   * Update schema document.
   *
   * @param applicationProperties Settings of repository.
   * @param resourceId ID of the schema document.
   * @param eTag E-Tag of the current schema document.
   * @param recordDocument Record of the schema.
   * @param schemaDocument Schema document.
   * @param supplier Method for creating access URL.
   * @return Record of updated schema document.
   */
  public static DataResource updateDataResource4SchemaDocument(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          MultipartFile recordDocument,
          MultipartFile schemaDocument,
          UnaryOperator<String> supplier) {
    DataResource metadataRecord;
    metadataRecord = checkParameters(recordDocument, schemaDocument, false);

    LOG.trace("Obtaining most recent metadata schema record with id {}.", resourceId);
    DataResource dataResource = applicationProperties.getDataResourceService().findById(resourceId);
    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(eTag, dataResource);
    if (metadataRecord != null) {
      metadataRecord.setVersion(dataResource.getVersion());
      metadataRecord.setId(dataResource.getId());
      dataResource = metadataRecord;
    } else {
      dataResource = DataResourceUtils.copyDataResource(dataResource);
    }
    return updateDataResource4SchemaDocument(applicationProperties, resourceId, eTag, dataResource, schemaDocument, supplier);
  }

  /**
   * Update schema document.
   *
   * @param applicationProperties Settings of repository.
   * @param resourceId ID of the schema document.
   * @param eTag E-Tag of the current schema document.
   * @param givenDataResource Record of the schema.
   * @param schemaDocument Schema document.
   * @param supplier Method for creating access URL.
   * @return Record of updated schema document.
   */
  public static DataResource updateDataResource4SchemaDocument(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          DataResource givenDataResource,
          MultipartFile schemaDocument,
          UnaryOperator<String> supplier) {
    DataResource updatedDataResource;
    LOG.trace("Obtaining most recent datacite record with id {}.", resourceId);
    DataResource oldDataResource = applicationProperties.getDataResourceService().findById(resourceId);
    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(eTag, oldDataResource);
    LOG.trace("ETag: '{}'", oldDataResource.getEtag());
    updatedDataResource = mergeDataResource(oldDataResource, givenDataResource);
    if (schemaDocument != null) {
      updateSchemaDocument(applicationProperties, updatedDataResource, schemaDocument, supplier);
    } else {
      updateOnlyMetadata4SchemaDocument(applicationProperties, updatedDataResource);
    }
    updatedDataResource = DataResourceUtils.updateResource(applicationProperties, updatedDataResource.getId(), updatedDataResource, eTag, supplier);

    return updatedDataResource;
  }

  private static void updateMetadataDocument(MetastoreConfiguration applicationProperties,
          DataResource updatedDataResource,
          MultipartFile document,
          UnaryOperator<String> supplier) {
    SchemaRecord schemaRecord = getSchemaRecordFromDataResource(updatedDataResource);
    validateMetadataDocument(applicationProperties, document, schemaRecord);

    ContentInformation info;
    String fileName;
    info = getContentInformationOfResource(applicationProperties, updatedDataResource);
    fileName = (info != null) ? info.getRelativePath() : document.getOriginalFilename();
    boolean noChanges = checkDocumentForChanges(info, document);

    if (!noChanges) {
      // Everything seems to be fine update document and increment version
      LOG.trace("Updating schema document (and increment version)...");
      String version = updatedDataResource.getVersion();
      if (version == null) {
        version = "0";
      }
      updatedDataResource.setVersion(Long.toString(Long.parseLong(version) + 1L));
      addProvenance(updatedDataResource);
      ContentDataUtils.addFile(applicationProperties, updatedDataResource, document, fileName, null, true, supplier);
    }
  }

  private static void updateSchemaDocument(MetastoreConfiguration applicationProperties,
          DataResource updatedDataResource,
          MultipartFile schemaDocument,
          UnaryOperator<String> supplier) {
    ContentInformation info;
    info = getContentInformationOfResource(applicationProperties, updatedDataResource);
    // Get schema record for this schema
    validateMetadataSchemaDocument(applicationProperties, updatedDataResource, schemaDocument);

    boolean noChanges;
    String fileName;

    fileName = (info != null) ? info.getRelativePath() : schemaDocument.getOriginalFilename();
    noChanges = checkDocumentForChanges(info, schemaDocument);

    if (!noChanges) {
      // Everything seems to be fine update document and increment version
      LOG.trace("Updating schema document (and increment version)...");
      String version = updatedDataResource.getVersion();
      if (version != null) {
        updatedDataResource.setVersion(Long.toString(Long.parseLong(version) + 1L));
      }
      addProvenance(updatedDataResource);
      ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, updatedDataResource, schemaDocument, fileName, null, true, supplier);
      SchemaRecord schemaRecord = createSchemaRecord(updatedDataResource, contentInformation);
      MetadataSchemaRecordUtil.saveNewSchemaRecord(schemaRecord);
    }

  }

  private static void updateOnlyMetadata4SchemaDocument(MetastoreConfiguration applicationProperties,
          DataResource updatedDataResource) {
    ContentInformation info;
    info = getContentInformationOfResource(applicationProperties, updatedDataResource);
    // validate if document is still valid due to changed record settings.
    Objects.requireNonNull(info);
    URI schemaDocumentUri = URI.create(info.getContentUri());

    Path schemaDocumentPath = Paths.get(schemaDocumentUri);
    if (!Files.exists(schemaDocumentPath)
            || !Files.isRegularFile(schemaDocumentPath)
            || !Files.isReadable(schemaDocumentPath)) {
      LOG.warn("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
      throw new CustomInternalServerError("Schema document on server either does not exist or is no file or is not readable.");
    }

    try {
      byte[] schemaDoc = Files.readAllBytes(schemaDocumentPath);
      validateMetadataSchemaDocument(applicationProperties, updatedDataResource, schemaDoc);
    } catch (IOException ex) {
      LOG.error("Error validating file!", ex);
    }

  }

  private static DataResource mergeDataResource(DataResource oldDataResource, DataResource givenDataResource) {
    DataResource updatedDataResource;

    if (givenDataResource != null) {
      LOG.trace("new DataResource: '{}'", givenDataResource);
      givenDataResource.setVersion(oldDataResource.getVersion());
      givenDataResource.setId(oldDataResource.getId());
      updatedDataResource = givenDataResource;
      mergeCreators(oldDataResource, updatedDataResource);
      mergePublicationYear(oldDataResource, updatedDataResource);
      mergePublisher(oldDataResource, updatedDataResource);
      mergeAcl(oldDataResource, updatedDataResource);
      fixEmptyRights(updatedDataResource);
      mergeState(oldDataResource, updatedDataResource);
      mergeResourceType(oldDataResource, updatedDataResource);
      mergeCreateDate(oldDataResource, updatedDataResource);
    } else {
      updatedDataResource = DataResourceUtils.copyDataResource(oldDataResource);
    }
    return updatedDataResource;
  }

  private static DataResource mergeCreators(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null) {
      if ((updatedDataResource.getCreators() == null) || updatedDataResource.getCreators().isEmpty()) {
        updatedDataResource.setCreators(oldDataResource.getCreators());
      } else {
        LOG.trace("Update creators!");
      }
    }
    return updatedDataResource;
  }

  private static DataResource mergePublicationYear(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null && updatedDataResource.getPublicationYear() == null) {
      updatedDataResource.setPublicationYear(oldDataResource.getPublicationYear());

    }
    return updatedDataResource;
  }

  private static DataResource mergePublisher(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null && updatedDataResource.getPublisher() == null) {
      updatedDataResource.setPublisher(oldDataResource.getPublisher());

    }
    return updatedDataResource;
  }

  private static DataResource mergeAcl(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null) {
      if ((updatedDataResource.getAcls() == null) || updatedDataResource.getAcls().isEmpty()) {
        updatedDataResource.setAcls(oldDataResource.getAcls());
      } else {
        // Check for access rights for changing ACL (need ADMINISTRATION rights!)
        checkAccessRights(oldDataResource.getAcls(), true);
        // Check if access rights still valid afterwards (at least one user with ADMINISTRATION rights should be available!)
        checkAccessRights(updatedDataResource.getAcls(), false);
      }
    }
    return updatedDataResource;
  }

  /**
   * Fix rights to an empty array if no rights are provided.
   *
   * @param updatedDataResource data resource to check.
   * @return Fixed data resource.
   */
  private static DataResource fixEmptyRights(DataResource updatedDataResource) {
    if (updatedDataResource != null && updatedDataResource.getRights() == null) {
      updatedDataResource.setRights(new HashSet<>());
    }
    return updatedDataResource;
  }

  private static DataResource mergeState(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null && updatedDataResource.getState() == null) {
      updatedDataResource.setState(oldDataResource.getState());
    }
    return updatedDataResource;
  }

  private static DataResource mergeResourceType(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null && updatedDataResource.getResourceType() == null) {
      updatedDataResource.setResourceType(oldDataResource.getResourceType());
    }
    return updatedDataResource;
  }

  private static DataResource mergeCreateDate(DataResource oldDataResource, DataResource updatedDataResource) {
    if (updatedDataResource != null) {
      // Set create date
      Date createDate = null;
      for (Date date : oldDataResource.getDates()) {
        if (date.getType().equals(Date.DATE_TYPE.CREATED)) {
          createDate = date;
        }
      }
      Date newCreateDate = null;
      for (Date date : updatedDataResource.getDates()) {
        if (date.getType().equals(Date.DATE_TYPE.CREATED)) {
          newCreateDate = date;
        }
      }
      if (newCreateDate != null) {
        updatedDataResource.getDates().remove(newCreateDate);
      }
      updatedDataResource.getDates().add(createDate);
    }
    return updatedDataResource;
  }

  public static boolean checkDocumentForChanges(ContentInformation info, MultipartFile document) {
    boolean noChanges = true;
    if (info != null) {
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
        throw new BadArgumentException("Error reading schema document!");
      }
    } else {
      throw new CustomInternalServerError("No content information provided!");
    }
    return noChanges;
  }
  // Check for changes...

  /**
   * Migrate schema from INTERNAL type to URL type (if necessary)
   *
   * @param dataResource Data resource which should be fixed.
   * @return Fixed data resource.
   */
  private static DataResource fixRelatedSchemaIfNeeded(DataResource dataResource) {
    RelatedIdentifier relatedIdentifier = getSchemaIdentifier(dataResource);
    SchemaRecord schemaRecord = getSchemaRecordFromDataResource(dataResource);
    if (schemaRecord != null) {
      if (relatedIdentifier != null && relatedIdentifier.getIdentifierType() == Identifier.IDENTIFIER_TYPE.INTERNAL) {
        relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
        // schemaRecord should never be null for internal schema!
        relatedIdentifier.setValue(schemaRecord.getAlternateId());
      }
    } else {
      String identifier = relatedIdentifier != null ? relatedIdentifier.getValue() : "is not defined and therefor";
      throw new UnprocessableEntityException("Schema '" + identifier + "' is not known!");
    }
    return dataResource;
  }

  /**
   * Validate metadata document with given schema. Determine type if not already
   * given or check type.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param dataResource Data resource record of the document.
   * @param document Document of data resource.
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          DataResource dataResource,
          MultipartFile document) {
    LOG.trace("validateMetadataDocument (dataresource) {},{}, {}", metastoreProperties, dataResource, document);
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    boolean validationSuccess = false;
    StringBuilder errorMessage = new StringBuilder();
    SchemaRecord findByAlternateId;
    findByAlternateId = getSchemaRecordFromDataResource(dataResource);
    if (findByAlternateId != null) {
      try {
        validateMetadataDocument(metastoreProperties, document, findByAlternateId);
        validationSuccess = true;
        // After successful validation set type for metadata document resource.
        MetadataSchemaRecord.SCHEMA_TYPE type = findByAlternateId.getType();
        dataResource.setResourceType(ResourceType.createResourceType(type + METADATA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
        //
      } catch (Exception ex) {
        String message = "Error validating document!";
        LOG.error(message, ex);
        errorMessage.append(ex.getMessage()).append("\n");
      }
    } else {
      errorMessage.append("No matching schema found for '");
      RelatedIdentifier schemaIdentifier = getSchemaIdentifier(dataResource);
      errorMessage = schemaIdentifier != null ? errorMessage.append(schemaIdentifier.getValue()) : errorMessage.append("missing schema identifier");
      errorMessage.append("'!");
    }
    if (!validationSuccess) {
      LOG.error(errorMessage.toString());
      throw new UnprocessableEntityException(errorMessage.toString());
    }
  }

  /**
   * Create schema record from DataResource and ContentInformation.
   *
   * @param dataResource Data resource
   * @param contentInformation Content information
   * @return schema record
   */
  public static final SchemaRecord createSchemaRecord(DataResource dataResource, ContentInformation contentInformation) {
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(dataResource.getId());
    String type = dataResource.getResourceType().getValue();
    if (type.equals(JSON + SCHEMA_SUFFIX)) {
      schemaRecord.setType(JSON);
    } else {
      if (type.equals(XML + SCHEMA_SUFFIX)) {
        schemaRecord.setType(XML);

      } else {
        throw new BadArgumentException("Please provide a valid resource type for data resource '" + schemaRecord.getSchemaIdWithoutVersion() + "'!\n"
                + "One of ['" + JSON + SCHEMA_SUFFIX + "', '" + XML + SCHEMA_SUFFIX + "']");
      }
    }
    Long currentVersion = Long.valueOf(dataResource.getVersion());
    String schemaUrl = getSchemaDocumentUri(dataResource.getId(), currentVersion);
    schemaRecord.setVersion(currentVersion);
    schemaRecord.setSchemaDocumentUri(contentInformation.getContentUri());
    schemaRecord.setDocumentHash(contentInformation.getHash());
    schemaRecord.setAlternateId(schemaUrl);

    return schemaRecord;
  }

  /**
   * Get creation date of data resource.
   *
   * @param dataResource data resource.
   * @return creation date.
   */
  public static final Instant getCreationDate(DataResource dataResource) {
    Instant creationDate = null;
    for (edu.kit.datamanager.repo.domain.Date d : dataResource.getDates()) {
      if (edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())) {
        LOG.trace("Creation date entry found.");
        creationDate = d.getValue();
        break;
      }
    }
    return creationDate;
  }

  /**
   * Set creation date for data resource if and only if creation date doesn't
   * exist.
   *
   * @param dataResource data resource.
   * @param creationDate creation date
   */
  public static final void setCreationDate(DataResource dataResource, Instant creationDate) {
    if (creationDate != null) {
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
        dataResource.getDates().add(Date.factoryDate(creationDate, Date.DATE_TYPE.CREATED));
      }
    }
  }

  /**
   * Get version of data resource.
   *
   * @param dataResource data resource.
   * @return version or 1 if no version is available.
   */
  public static final Long getVersion(DataResource dataResource) {
    Long recordVersion = 1L;
    if (dataResource.getVersion() != null) {
      recordVersion = Long.valueOf(dataResource.getVersion());
    }
    return recordVersion;
  }

  public static final void setSchemaAndVersion(MetadataRecord metadataRecord, DataResource dataResource) {
    RelatedIdentifier relatedId = getSchemaIdentifier(dataResource);
    if (relatedId != null) {
      ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(relatedId.getValue(), IdentifierType.valueOf(relatedId.getIdentifierType().name()));
      metadataRecord.setSchema(resourceIdentifier);
      if (resourceIdentifier.getIdentifierType().equals(IdentifierType.URL)) {
        //Try to fetch version from URL (only works with URLs including the version as query parameter.
        Matcher matcher = Pattern.compile(".*[&?]version=(\\d*).*").matcher(resourceIdentifier.getIdentifier());
        while (matcher.find()) {
          metadataRecord.setSchemaVersion(Long.valueOf(matcher.group(1)));
        }
      } else {
        // set to current version of schema
        SchemaRecord currentSchema = schemaRecordDao.findFirstBySchemaIdStartsWithOrderByVersionDesc(resourceIdentifier.getIdentifier());
        if (currentSchema != null) {
          metadataRecord.setSchemaVersion(currentSchema.getVersion());
        } else {
          metadataRecord.setSchemaVersion(1L);
        }
      }
      LOG.trace("Set schema to '{}'", resourceIdentifier);
    }
  }

  public static final void setRelatedResource(MetadataRecord metadataRecord, DataResource dataResource) {
    RelatedIdentifier relatedId = getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    if (relatedId != null) {
      ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryInternalResourceIdentifier(relatedId.getValue());
      if (relatedId.getIdentifierType() != null) {
        resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(relatedId.getValue(), IdentifierType.valueOf(relatedId.getIdentifierType().name()));
      }
      LOG.trace("Set relation to '{}'", resourceIdentifier);
      metadataRecord.setRelatedResource(resourceIdentifier);
    }
  }

  /**
   * Get String (URL) for accessing schema document via schemaId and version.
   *
   * @param schemaId schemaId.
   * @param version version.
   * @return String for accessing schema document.
   */
  public static final String getSchemaDocumentUri(String schemaId, Long version) {
    return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SchemaRegistryControllerImplV2.class).getSchemaDocumentById(schemaId, version, null, null)).toUri().toString();
  }

  /**
   * Get String (URL) for accessing metadata document via id and version.
   *
   * @param id id.
   * @param version version.
   * @return URI for accessing schema document.
   */
  public static final URI getMetadataDocumentUri(String id, String version) {
    URI toUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class).getMetadataDocumentById(id, Long.valueOf(version), null, null)).toUri();
    if (toUri.getScheme() == null) {
      toUri = URI.create(baseUrl + toUri.toString());
    }
    return toUri;
  }

  /**
   * Query for data resources with provided specification or all if no
   * specification is provided.
   *
   * @param spec Specification of the data resources.
   * @param pgbl The pageable object containing pagination information.
   * @return Pageable Object holding all data resources fulfilling the
   * specification.
   */
  public static Page<DataResource> queryDataResources(Specification<DataResource> spec, Pageable pgbl) {
    Page<DataResource> records = null;
    try {
      records = spec != null ? dataResourceDao.findAll(spec, pgbl) : dataResourceDao.findAll(pgbl);
    } catch (Exception ex) {
      LOG.error("Error finding data resource records by specification!", ex);
      throw ex;
    }
    return records;
  }

  /**
   * Get all authorization identities.
   *
   * @return List with all identities.
   */
  private static List<String> getAllAuthorizationIdentities() {
    Authentication authentication = AuthenticationHelper.getAuthentication();
    List<String> authorizationIdentities = AuthenticationHelper.getAuthorizationIdentities();
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      authorizationIdentities.add(authority.getAuthority());
    }
    return authorizationIdentities;
  }

  /**
   * Set base URL for accessing instances.
   *
   * @param aBaseUrl the baseUrl to set
   */
  public static void setBaseUrl(String aBaseUrl) {
    baseUrl = aBaseUrl;
  }
}
