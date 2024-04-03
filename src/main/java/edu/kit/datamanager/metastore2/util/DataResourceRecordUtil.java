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
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.DataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import static edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE.JSON;
import static edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE.XML;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType;
import static edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType.INTERNAL;
import static edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType.URL;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.metastore2.domain.Url2Path;
import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImpl;
import edu.kit.datamanager.metastore2.web.impl.MetadataControllerImplV2;
import edu.kit.datamanager.metastore2.web.impl.SchemaRegistryControllerImplV2;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.RelatedIdentifierSpec;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Scheme;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for handling json documents
 */
public class DataResourceRecordUtil {

  public static final String RESOURCE_TYPE = "application/vnd.datacite.org+json";
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
  private static final String LOG_SEPARATOR = "-----------------------------------------";
  private static final String LOG_SCHEMA_REGISTRY = "No external schema registries defined. Try to use internal one...";
  private static final String LOG_FETCH_SCHEMA = "Try to fetch schema from '{}'.";
  private static final String PATH_SCHEMA = "schemas";
  private static final String LOG_ERROR_ACCESS = "Failed to access schema registry at '%s'. Proceeding with next registry.";
  private static final String LOG_SCHEMA_RECORD = "Found schema record: '{}'";
  private static final String ERROR_PARSING_JSON = "Error parsing json: ";

  private static MetastoreConfiguration schemaConfig;
  private static String guestToken = null;

  private static IDataRecordDao dataRecordDao;
  private static IDataResourceDao dataResourceDao;
  private static ISchemaRecordDao schemaRecordDao;
  private static IMetadataFormatDao metadataFormatDao;

  private static IUrl2PathDao url2PathDao;

  public static final String SCHEMA_SUFFIX = "_Schema";
  public static final String XML_SCHEMA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.XML + SCHEMA_SUFFIX;
  public static final String JSON_SCHEMA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.JSON + SCHEMA_SUFFIX;

  public static final String METADATA_SUFFIX = "_Metadata";
  public static final String XML_METADATA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.XML + METADATA_SUFFIX;
  public static final String JSON_METADATA_TYPE = MetadataSchemaRecord.SCHEMA_TYPE.JSON + METADATA_SUFFIX;

  DataResourceRecordUtil() {
    //Utility class
  }

  /**
   * Create/Ingest an instance of MetadataSchemaRecord.
   *
   * @param applicationProperties Settings of repository.
   * @param recordDocument Record of the schema.
   * @param document Schema document.
   * @param getSchemaDocumentById Method for creating access URL.
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
        String documentString = new String(document.getBytes());
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

    return metadataRecord;
  }

  /**
   * Create/Ingest an instance of MetadataRecord.
   *
   * @param applicationProperties Settings of repository.
   * @param recordDocument Record of the metadata.
   * @param document Schema document.
   * @param getSchemaDocumentById Method for creating access URL.
   * @return Record of registered schema document.
   */
  public static DataResource createDataResourceRecord4Metadata(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument,
          MultipartFile document) {
    DataResource metadataRecord;

    // Do some checks first.
    metadataRecord = checkParameters(recordDocument, document, true);
    Objects.requireNonNull(metadataRecord);
    if (metadataRecord.getId() != null) {
      // Optional id set. Check for valid ID
      check4validId(metadataRecord, true);
    }
    // End of parameter checks
    // validate schema document / determine type if not given
    validateMetadataDocument(applicationProperties, metadataRecord, document);
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

    return metadataRecord;
  }

//  /**
//   * Create a digital object from metadata record and metadata document.
//   *
//   * @param applicationProperties Configuration properties.
//   * @param recordDocument Metadata record.
//   * @param document Metadata document.
//   * @return Enriched metadata record.
//   */
//  public static MetadataRecord createDataResource4MetadataDocument(MetastoreConfiguration applicationProperties,
//          MultipartFile recordDocument, MultipartFile document) {
//    DataResource metadataRecord;
//    long nano1 = System.nanoTime() / 1000000;
//    // Do some checks first.
//    metadataRecord = checkParameters(recordDocument, document, true);
//
//    if (metadataRecord.getRelatedResource() == null || metadataRecord.getRelatedResource().getIdentifier() == null || metadataRecord.getSchema() == null || metadataRecord.getSchema().getIdentifier() == null) {
//      String message = "Mandatory attributes relatedResource and/or schema not found in record. Returning HTTP BAD_REQUEST.";
//      LOG.error(message);
//      throw new BadArgumentException(message);
//    }
//    // Test for schema version
//    if (metadataRecord.getSchemaVersion() == null) {
//      MetadataSchemaRecord currentSchemaRecord;
//      try {
//        currentSchemaRecord = MetadataSchemaRecordUtil.getCurrentSchemaRecord(applicationProperties, metadataRecord.getSchema());
//      } catch (ResourceNotFoundException rnfe) {
//        throw new UnprocessableEntityException("Unknown schema ID '" + metadataRecord.getSchema().getIdentifier() + "'!");
//      }
//      metadataRecord.setSchemaVersion(currentSchemaRecord.getSchemaVersion());
//    }
//
//    // validate document
//    long nano2 = System.nanoTime() / 1000000;
//    // validate schema document
//    validateMetadataDocument(applicationProperties, metadataRecord, document);
//    // set internal parameters
//    metadataRecord.setRecordVersion(1l);
//
//    long nano3 = System.nanoTime() / 1000000;
//    // create record.
//    DataResource dataResource = migrateToDataResource(applicationProperties, metadataRecord);
//    // add id as internal identifier if exists
//    // Note: DataResourceUtils.createResource will ignore id of resource. 
//    // id will be set to alternate identifier if exists. 
//    if (dataResource.getId() != null) {
//      // check for valid identifier without any chars which may be encoded
//      try {
//        String originalId = dataResource.getId();
//        String value = URLEncoder.encode(originalId, StandardCharsets.UTF_8.toString());
//        if (!value.equals(originalId)) {
//          String message = "Not a valid id! Encoded: " + value;
//          LOG.error(message);
//          throw new BadArgumentException(message);
//        }
//      } catch (UnsupportedEncodingException ex) {
//        String message = "Error encoding id " + metadataRecord.getSchemaId();
//        LOG.error(message);
//        throw new CustomInternalServerError(message);
//      }
//
//      dataResource.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(dataResource.getId()));
//    }
//    long nano4 = System.nanoTime() / 1000000;
//    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
//    long nano5 = System.nanoTime() / 1000000;
//    // store document
//    ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, t -> "somethingStupid");
//    long nano6 = System.nanoTime() / 1000000;
//    // Create additional metadata record for faster access
//    DataRecord dataRecord = new DataRecord();
//    dataRecord.setMetadataId(createResource.getId());
//    dataRecord.setVersion(metadataRecord.getRecordVersion());
//    dataRecord.setSchemaId(metadataRecord.getSchema().getIdentifier());
//    dataRecord.setSchemaVersion(metadataRecord.getSchemaVersion());
//    dataRecord.setMetadataDocumentUri(contentInformation.getContentUri());
//    dataRecord.setDocumentHash(contentInformation.getHash());
//    dataRecord.setLastUpdate(dataResource.getLastUpdate());
//    saveNewDataRecord(dataRecord);
//    long nano7 = System.nanoTime() / 1000000;
//    LOG.info("Create Record times, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1, nano6 - nano1, nano7 - nano1);
//
//    return migrateToMetadataRecord(applicationProperties, createResource, true);
//  }
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
    DataResource metadataRecord = null;
    DataResource updatedDataResource;

    // Do some checks first.
    if ((recordDocument == null || recordDocument.isEmpty()) && (document == null || document.isEmpty())) {
      String message = "Neither metadata record nor metadata document provided.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    if (!(recordDocument == null || recordDocument.isEmpty())) {
      try {
        metadataRecord = Json.mapper().readValue(recordDocument.getInputStream(), DataResource.class);
      } catch (IOException ex) {
        String message = "Can't map record document to MetadataRecord";
        if (ex instanceof JsonParseException) {
          message = message + " Reason: " + ex.getMessage();
        }
        LOG.error(ERROR_PARSING_JSON, ex);
        throw new BadArgumentException(message);
      }
    }

    LOG.trace("Obtaining most recent metadata record with id {}.", resourceId);
    DataResource dataResource = applicationProperties.getDataResourceService().findById(resourceId);
    LOG.trace("ETag: '{}'", dataResource.getEtag());
    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(eTag, dataResource);
    if (metadataRecord != null) {
      updatedDataResource = metadataRecord;
      if ((updatedDataResource.getAcls() == null) || updatedDataResource.getAcls().isEmpty()) {
        updatedDataResource.setAcls(dataResource.getAcls());
      }
      if (updatedDataResource.getRights() == null) {
        updatedDataResource.setRights(new HashSet<>());
      }
    } else {
      updatedDataResource = DataResourceUtils.copyDataResource(dataResource);
    }

    LOG.trace("ETag: '{}'", dataResource.getEtag());

    boolean noChanges = false;
    if (document != null) {
      SchemaRecord schemaRecord = getSchemaRecordFromDataResource(updatedDataResource);
      validateMetadataDocument(applicationProperties, document, schemaRecord);

      ContentInformation info;
      String fileName = document.getOriginalFilename();
      info = getContentInformationOfResource(applicationProperties, updatedDataResource);
      if (info != null) {
        fileName = info.getRelativePath();
        noChanges = true;
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
      }
      if (!noChanges) {
        // Everything seems to be fine update document and increment version
        LOG.trace("Updating schema document (and increment version)...");
        String version = dataResource.getVersion();
        if (version == null) {
          version = "0";
        }
        updatedDataResource.setVersion(Long.toString(Long.parseLong(version) + 1l));
        ContentDataUtils.addFile(applicationProperties, updatedDataResource, document, fileName, null, true, supplier);
      }

    } else {
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, updatedDataResource);
      // validate if document is still valid due to changed record settings.
      //    metadataRecord = migrateToMetadataRecord(applicationProperties, dataResource, false);
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

    }
    if (noChanges) {
      Optional<DataRecord> dataRecord = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(dataResource.getId());
      if (dataRecord.isPresent()) {
        dataRecordDao.delete(dataRecord.get());
      }
    }
    dataResource = DataResourceUtils.updateResource(applicationProperties, resourceId, updatedDataResource, eTag, supplier);

    return dataResource;
  }

  /**
   * Delete schema document.
   *
   * @param applicationProperties Settings of repository.
   * @param id ID of the schema document.
   * @param eTag E-Tag of the current schema document.
   * @param supplier Method for creating access URL.
   */
  public static void deleteMetadataSchemaRecord(MetastoreConfiguration applicationProperties,
          String id,
          String eTag,
          UnaryOperator<String> supplier) {
    // Find all versions for given id...
    int pageNo = 0;
    int pageSize = 10;
    int totalNoOfPages;
    Set<String> uris = new HashSet<>();
    Pageable pgbl;
    Page<DataResource> allVersionsOfResource;
    do {
      pgbl = PageRequest.of(pageNo, pageSize);
      allVersionsOfResource = DataResourceUtils.readAllVersionsOfResource(applicationProperties, id, pgbl);
      totalNoOfPages = allVersionsOfResource.getTotalPages();
      for (DataResource item : allVersionsOfResource.getContent()) {
        uris.add(SchemaRegistryControllerImplV2.getSchemaDocumentUri(item).toString());
      }
      pageNo++;
    } while (pageNo < totalNoOfPages);
    // Test for linked metadata documents
    Specification<DataResource> spec = RelatedIdentifierSpec.toSpecification(uris.toArray(new String[]{}));
    Optional<DataResource> findOne = dataResourceDao.findOne(spec);
    // No references to this schema available -> Ready for deletion
    if (findOne.isEmpty()) {
      DataResourceUtils.deleteResource(applicationProperties, id, eTag, supplier);
      List<SchemaRecord> listOfSchemaIds = schemaRecordDao.findBySchemaIdStartsWithOrderByVersionDesc(id + SCHEMA_VERSION_SEPARATOR);
      for (SchemaRecord item : listOfSchemaIds) {
        LOG.trace("Delete entry for path '{}'", item.getSchemaDocumentUri());
        List<Url2Path> findByPath = url2PathDao.findByPath(item.getSchemaDocumentUri());
        for (Url2Path entry : findByPath) {
          url2PathDao.delete(entry);
        }
      }
      schemaRecordDao.deleteAll(listOfSchemaIds);
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

      for (Identifier identifier : dataResource.getAlternateIdentifiers()) {
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
        LOG.trace("Found related Identifier: '{}'", relatedIds);
        if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR) {
          ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryInternalResourceIdentifier(relatedIds.getValue());
          if (relatedIds.getIdentifierType() != null) {
            resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(relatedIds.getValue(), IdentifierType.valueOf(relatedIds.getIdentifierType().name()));
          }
          LOG.trace("Set relation to '{}'", resourceIdentifier);
          metadataRecord.setRelatedResource(resourceIdentifier);
        }
        if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM) {
          ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(relatedIds.getValue(), IdentifierType.valueOf(relatedIds.getIdentifierType().name()));
          metadataRecord.setSchema(resourceIdentifier);
          if (resourceIdentifier.getIdentifierType().equals(IdentifierType.URL)) {
            //Try to fetch version from URL (only works with URLs including the version as query parameter.
            Matcher matcher = Pattern.compile(".*[&?]version=(\\d*).*").matcher(resourceIdentifier.getIdentifier());
            while (matcher.find()) {
              metadataRecord.setSchemaVersion(Long.parseLong(matcher.group(1)));
            }
          } else {
            metadataRecord.setSchemaVersion(1l);
          }
          LOG.trace("Set schema to '{}'", resourceIdentifier);
        }
      }
      if (metadataRecord.getSchema() == null) {
        String message = "Missing schema identifier for metadata document. Not a valid metadata document ID. Returning HTTP BAD_REQUEST.";
        LOG.error(message);
        throw new BadArgumentException(message);
      }
      DataRecord dataRecord = null;
      LOG.trace("Get document URI from ContentInformation.");
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, dataResource);
      if (info != null) {
        metadataRecord.setDocumentHash(info.getHash());
        metadataRecord.setMetadataDocumentUri(info.getContentUri());
        MetadataSchemaRecord currentSchemaRecord = MetadataSchemaRecordUtil.getCurrentSchemaRecord(schemaConfig, metadataRecord.getSchema());
        metadataRecord.setSchemaVersion(currentSchemaRecord.getSchemaVersion());
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
   * @param schemaRecord metadata of the schema document.
   * @param document document
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          SchemaRecord schemaRecord) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, schemaRecord, document);
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
    LOG.trace("Obtaining schema record with id {} and version {}.", recordId, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    long nano = System.nanoTime() / 1000000;
    long nano2;
    Page<DataResource> dataResource;
    try {
      dataResource = metastoreProperties.getDataResourceService().findAllVersions(recordId, null);
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
          String recordId) throws ResourceNotFoundException {
    return getContentInformationByIdAndVersion(metastoreProperties, recordId, null);
  }

  public static ContentInformation getContentInformationByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    LOG.trace("Obtaining content information record with id {} and version {}.", recordId, version);
    return metastoreProperties.getContentInformationService().getContentInformation(recordId, null, version);
  }

  public static Path getMetadataDocumentByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId) throws ResourceNotFoundException {
    return getMetadataDocumentByIdAndVersion(metastoreProperties, recordId, null);
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
        specWithSchema = specWithSchema.and(RelatedIdentifierSpec.toSpecification(allSchemaIds.toArray(String[]::new)));
      }
    }
    return specWithSchema;
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
      specWithSchema = specWithSchema.and(RelatedIdentifierSpec.toSpecification(relatedIds.toArray(String[]::new)));
    }
    return specWithSchema;
  }

  /**
   * Merge new metadata record in the existing one.
   *
   * @param managed Existing metadata record.
   * @param provided New metadata record.
   * @return Merged record
   */
//  public static MetadataRecord mergeDataResources(DataResource managed, DataResource provided) {
//    if (provided != null && managed != null) {
//      //update pid
//      managed.setAlternateIdentifiers(mergeEntry("Update record->pid", managed.getAlternateIdentifiers(), provided.getAlternateIdentifiers()));
//
//      //update acl
//      managed.setAcls(mergeAcl(managed.getAcls(), provided.getAcls()));
//      //update getRelatedResource
//      managed.setRelatedIdentifiers(mergeEntry("Updating record->relatedResource", managed.getRelatedIdentifiers(), provided.getRelatedIdentifiers()));
//      //update schemaVersion
//      managed.setVersion(mergeEntry("Updating record->schemaVersion", managed.getVersion(), provided.getVersion()));
//      managed.setRights(mergeEntry("halo", managed.getRights(), provided.getRights(), true));
//      // update licenseUri
//      managed.setRights(mergeEntry("Updating record->licenseUri", managed.getRights(), provided.getRights(), true));
//1    } else {
//      managed = (managed != null) ? managed : provided;
//    }
//    return managed;
//  }
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

  /**
   * Set DAO for data record.
   *
   * @param aDataRecordDao the dataRecordDao to set
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

  /**
   * Checks if current user is allowed to access with given AclEntries.
   *
   * @param aclEntries AclEntries of resource.
   * @param currentAcl Check current ACL (true) or new one (false).
   *
   * @return Allowed (true) or not.
   */
  public static boolean checkAccessRights(Set<AclEntry> aclEntries, boolean currentAcl) {
    boolean isAllowed = false;
    String errorMessage1 = "Error invalid ACL! Reason: Only ADMINISTRATORS are allowed to change ACL entries.";
    String errorMessage2 = "Error invalid ACL! Reason: You are not allowed to revoke your own administrator rights.";
    Authentication authentication = AuthenticationHelper.getAuthentication();
    List<String> authorizationIdentities = AuthenticationHelper.getAuthorizationIdentities();
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      authorizationIdentities.add(authority.getAuthority());
    }
    if (authorizationIdentities.contains(RepoUserRole.ADMINISTRATOR.getValue())) {
      //ROLE_ADMINISTRATOR detected -> no further permission check necessary.
      return true;
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace("Check access rights for changing ACL list!");
      for (String authority : authorizationIdentities) {
        LOG.trace("Indentity/Authority: '{}'", authority);
      }
    }
    // Check if authorized user still has ADMINISTRATOR rights
    Iterator<AclEntry> iterator = aclEntries.iterator();
    while (iterator.hasNext()) {
      AclEntry aclEntry = iterator.next();
      LOG.trace("'{}' has ’{}' rights!", aclEntry.getSid(), aclEntry.getPermission());
      if (aclEntry.getPermission().atLeast(PERMISSION.ADMINISTRATE)
              && authorizationIdentities.contains(aclEntry.getSid())) {
        isAllowed = true;
        LOG.trace("Confirm permission for updating ACL: '{}' has ’{}' rights!", aclEntry.getSid(), PERMISSION.ADMINISTRATE);
        break;
      }
    }
    if (!isAllowed) {
      String errorMessage = currentAcl ? errorMessage1 : errorMessage2;
      LOG.warn(errorMessage);
      if (schemaConfig.isAuthEnabled()) {
        if (currentAcl) {
          throw new AccessForbiddenException(errorMessage1);
        } else {
          throw new BadArgumentException(errorMessage2);
        }
      }
    }
    return isAllowed;
  }

  public static final void fixMetadataDocumentUri(MetadataRecord metadataRecord) {
    String metadataDocumentUri = metadataRecord.getMetadataDocumentUri();
    metadataRecord
            .setMetadataDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImpl.class
            ).getMetadataDocumentById(metadataRecord.getId(), metadataRecord.getRecordVersion(), null, null)).toUri().toString());
    LOG.trace("Fix metadata document Uri '{}' -> '{}'", metadataDocumentUri, metadataRecord.getMetadataDocumentUri());
  }

  public static final void fixSchemaUrl(DataResource dataresource) {
    RelatedIdentifier schemaIdentifier = getSchemaIdentifier(dataresource);
    if ((schemaIdentifier != null) && (schemaIdentifier.getIdentifierType().equals(Identifier.IDENTIFIER_TYPE.INTERNAL))) {
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

  public static void check4RelatedResource(DataResource dataResource, RelatedIdentifier relatedResource) {
    if (relatedResource != null) {
      Set<RelatedIdentifier> relatedResources = dataResource.getRelatedIdentifiers();

      if (relatedResources.isEmpty()) {
        relatedResources.add(relatedResource);
      } else {
        // Check if related resource already exists (only one related resource of each type allowed)
        for (RelatedIdentifier item : relatedResources) {
          if (item.getRelationType().equals(relatedResource.getRelationType())
                  && !item.getValue().equals(relatedResource.getValue())) {
            relatedResources.remove(item);
            relatedResources.add(relatedResource);
            break;
          }
        }
      }
    }
  }

  public static void validateRelatedResources4MetadataDocuments(DataResource dataResource) throws BadArgumentException {
    int noOfRelatedData = 0;
    int noOfRelatedSchemas = 0;
    String message = "Invalid related resources! Expected '1' related resource found '%d'. Expected '1' related schema found '%d'!";
    if (dataResource != null) {
      Set<RelatedIdentifier> relatedResources = dataResource.getRelatedIdentifiers();

      // Check if related resource already exists (only one related resource of type isMetadataFor allowed)
      for (RelatedIdentifier item : relatedResources) {
        if (item.getRelationType().equals(RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR)) {
          noOfRelatedData++;
        }
        if (item.getRelationType().equals(RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM)) {
          noOfRelatedSchemas++;
        }
      }
    }
    if (noOfRelatedData != 1 || noOfRelatedSchemas != 1) {
      String errorMessage = "";
      if (noOfRelatedData == 0) {
        errorMessage = "Mandatory attribute relatedIdentifier of type 'isMetadataFor' was not found in record. \n";
      }
      if (noOfRelatedData > 1) {
        errorMessage = "Mandatory attribute relatedIdentifier of type 'isMetadataFor' was provided more than once in record. \n";
      }
      if (noOfRelatedSchemas == 0) {
        errorMessage = errorMessage + "Mandatory attribute relatedIdentifier of type 'isDerivedFrom' was not found in record. \n";
      }
      if (noOfRelatedSchemas > 1) {
        errorMessage = errorMessage + "Mandatory attribute relatedIdentifier of type 'isDerivedFrom' was provided more than once in record. \n";
      }
      errorMessage = errorMessage + "Returning HTTP BAD_REQUEST.";
      LOG.error(message);
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
    RelatedIdentifier relatedIdentifier = getRelatedIdentifier(dataResourceRecord, RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM);
    return relatedIdentifier;
  }

  /**
   * Transform schema identifier to global available identifier (if neccessary).
   *
   * @param dataResourceRecord Metadata record hold schema identifier.
   * @return ResourceIdentifier with a global accessible identifier.
   */
  public static RelatedIdentifier getRelatedIdentifier(DataResource dataResourceRecord, RelatedIdentifier.RELATION_TYPES relationType) {
    LOG.trace("Get related identifier for '{}' of type '{}'.", dataResourceRecord.getId(), relationType);
    RelatedIdentifier relatedIdentifier = null;

    Set<RelatedIdentifier> relatedResources = dataResourceRecord.getRelatedIdentifiers();

    // Check if related resource already exists (only one related resource of type isMetadataFor allowed)
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
    String lowerCaseId = id.toLowerCase();

    if (allowUpperCase) {
      lowerCaseId = id;
    }
    metadataRecord.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(lowerCaseId));
    if (!lowerCaseId.equals(id)) {
      metadataRecord.getAlternateIdentifiers().add(Identifier.factoryIdentifier(id, Identifier.IDENTIFIER_TYPE.OTHER));
    }

    try {
      String value = URLEncoder.encode(metadataRecord.getId(), StandardCharsets.UTF_8.toString());
      if (!value.equals(metadataRecord.getId())) {
        String message = "Not a valid ID! Encoded: " + value;
        LOG.error(message);
        throw new BadArgumentException(message);
      }
    } catch (UnsupportedEncodingException ex) {
      String message = "Error encoding schemaId " + metadataRecord.getId();
      LOG.error(message);
      throw new CustomInternalServerError(message);
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

    IValidator applicableValidator = null;
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
      try {
        metadataRecord = Json.mapper().readValue(dataResourceRecord.getInputStream(), DataResource.class);
      } catch (IOException ex) {
        message = "Can't map record document to MetadataRecord";
        if (ex instanceof JsonParseException) {
          message = message + " Reason: " + ex.getMessage();
        }
        LOG.error(ERROR_PARSING_JSON, ex);
        throw new BadArgumentException(message);
      }
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
    LOG.trace("validateMetadataDocument {},SchemaID {}, Version {}, {}", metastoreProperties, schemaId, version, document);
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
//
//  /**
//   * Validate metadata document with given schema. In case of an error a runtime
//   * exception is thrown.
//   *
//   * @param metastoreProperties Configuration properties.
//   * @param document Document to validate.
//   * @param schemaRecord Record of the schema.
//   */
//  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
//          MultipartFile document,
//          SchemaRecord schemaRecord) {
//    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, schemaRecord, document);
//
//    if (document == null || document.isEmpty()) {
//      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
//      LOG.error(message);
//      throw new BadArgumentException(message);
//    }
//    try {
//      try (InputStream inputStream = document.getInputStream()) {
//        validateMetadataDocument(metastoreProperties, inputStream, schemaRecord);
//      }
//    } catch (IOException ex) {
//      String message = LOG_ERROR_READ_METADATA_DOCUMENT;
//      LOG.error(message, ex);
//      throw new UnprocessableEntityException(message);
//    }
//  }
//
//  /**
//   * Validate metadata document with given schema. In case of an error a runtime
//   * exception is thrown.
//   *
//   * @param metastoreProperties Configuration properties.
//   * @param inputStream Document to validate.
//   * @param schemaRecord Record of the schema.
//   */
//  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
//          InputStream inputStream,
//          SchemaRecord schemaRecord) throws IOException {
//    LOG.trace("validateMetadataInputStream {},{}, {}", metastoreProperties, schemaRecord, inputStream);
//
//    long nano1 = System.nanoTime() / 1000000;
//    if (schemaRecord == null || schemaRecord.getSchemaDocumentUri() == null || schemaRecord.getSchemaDocumentUri().trim().isEmpty()) {
//      String message = "Missing or invalid schema record. Returning HTTP BAD_REQUEST.";
//      LOG.error(message + " -> '{}'", schemaRecord);
//      throw new BadArgumentException(message);
//    }
//    long nano2 = System.nanoTime() / 1000000;
//    LOG.trace("Checking local schema file.");
//    Path schemaDocumentPath = Paths.get(URI.create(schemaRecord.getSchemaDocumentUri()));
//
//    if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
//      LOG.error("Schema document with schemaId '{}'at path {} either does not exist or is no file or is not readable.", schemaRecord.getSchemaId(), schemaDocumentPath);
//      throw new CustomInternalServerError("Schema document on server either does not exist or is no file or is not readable.");
//    }
//    LOG.trace("obtain validator for type");
//    IValidator applicableValidator;
//    if (schemaRecord.getType() == null) {
//      byte[] schemaDocument = FileUtils.readFileToByteArray(schemaDocumentPath.toFile());
//      applicableValidator = getValidatorForRecord(metastoreProperties, schemaRecord, schemaDocument);
//    } else {
//      applicableValidator = getValidatorForRecord(metastoreProperties, schemaRecord, null);
//    }
//    long nano3 = System.nanoTime() / 1000000;
//
//    if (applicableValidator == null) {
//      String message = "No validator found for schema type " + schemaRecord.getType();
//      LOG.error(message);
//      throw new UnprocessableEntityException(message);
//    } else {
//      LOG.trace("Validator found.");
//
//      LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", schemaRecord.getSchemaId(), schemaRecord.getVersion(), applicableValidator);
//      long nano4 = System.nanoTime() / 1000000;
//      if (!applicableValidator.validateMetadataDocument(schemaDocumentPath.toFile(), inputStream)) {
//        LOG.warn("Metadata document validation failed. -> " + applicableValidator.getErrorMessage());
//        throw new UnprocessableEntityException(applicableValidator.getErrorMessage());
//      }
//      long nano5 = System.nanoTime() / 1000000;
//      LOG.info("Validate document(schemaRecord), {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1);
//    }
//    LOG.trace("Metadata document validation succeeded.");
//  }

  /**
   * Gets SchemaRecord from identifier. Afterwards there should be a clean up.
   *
   * @see #cleanUp(edu.kit.datamanager.metastore2.domain.ResourceIdentifier,
   * edu.kit.datamanager.metastore2.domain.SchemaRecord)
   *
   * @param identifier ResourceIdentifier of type INTERNAL or URL.
   * @param version Version (may be null)
   * @return schema record.
   */
  public static SchemaRecord getSchemaRecord(ResourceIdentifier identifier, Long version) {
    LOG.trace("getSchemaRecord {},{}", identifier, version);
    SchemaRecord schemaRecord;
    if (identifier == null || identifier.getIdentifierType() == null) {
      String message = "Missing resource identifier for schema. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    switch (identifier.getIdentifierType()) {
      case INTERNAL -> {
        String schemaId = identifier.getIdentifier();
        if (schemaId == null) {
          String message = "Missing schemaID. Returning HTTP BAD_REQUEST.";
          LOG.error(message);
          throw new BadArgumentException(message);
        }
        if (version != null) {
          schemaRecord = schemaRecordDao.findBySchemaId(schemaId + SCHEMA_VERSION_SEPARATOR + version);
        } else {
          schemaRecord = schemaRecordDao.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId + SCHEMA_VERSION_SEPARATOR);
        }
      }
      case URL -> {
        schemaRecord = prepareResourceFromUrl(identifier, version);
      }
      default ->
        throw new BadArgumentException("For schema document identifier type '" + identifier.getIdentifierType() + "' is not allowed!");
    }
    if (schemaRecord != null) {
      LOG.trace("getSchemaRecord {},{}", schemaRecord.getSchemaDocumentUri(), schemaRecord.getVersion());
    } else {
      LOG.trace("No matching schema record found!");
    }
    return schemaRecord;
  }

  private static SchemaRecord getSchemaRecordFromDataResource(DataResource dataResource) {
    SchemaRecord schemaRecord = null;
    RelatedIdentifier schemaIdentifier = getSchemaIdentifier(dataResource);
    String schemaId = schemaIdentifier.getValue();
    switch (schemaIdentifier.getIdentifierType()) {
      case URL:
        schemaRecord = schemaRecordDao.findByAlternateId(schemaIdentifier.getValue());
        break;
      case INTERNAL:
        String[] split = schemaId.split(SCHEMA_VERSION_SEPARATOR);
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
    return schemaRecord;
  }

  private static SchemaRecord prepareResourceFromUrl(ResourceIdentifier identifier, Long version) {
    String url = identifier.getIdentifier();
    Path pathToFile;
    MetadataSchemaRecord.SCHEMA_TYPE type = null;
    Optional<Url2Path> findByUrl = url2PathDao.findByUrl(url);
    if (findByUrl.isPresent()) {
      url = findByUrl.get().getPath();
      type = findByUrl.get().getType();
      pathToFile = Paths.get(URI.create(url));
    } else {
      URI resourceUrl;
      try {
        resourceUrl = new URI(url);
      } catch (URISyntaxException ex) {
        String message = String.format("Invalid URL: '%s'", url);
        LOG.error(message, ex);
        throw new BadArgumentException(message);
      }
      Optional<Path> path = DownloadUtil.downloadResource(resourceUrl);
      pathToFile = path.get();
    }
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaDocumentUri(pathToFile.toUri().toString());
    schemaRecord.setType(type);
    return schemaRecord;
  }

  /**
   * Remove all downloaded files for schema Record.
   *
   * @param schemaRecord Schema record.
   */
  public static void cleanUp(ContentInformation schemaRecord) {
    LOG.trace("Clean up {}", schemaRecord);
    if (schemaRecord == null || schemaRecord.getContentUri() == null) {
      String message = "Missing resource locator for schema.";
      LOG.error(message);
    } else {
      URI uri = URI.create(schemaRecord.getContentUri());
      if (!uri.getScheme().equals("file")) {
        // remove downloaded file
      } else {
        // nothing to do
      }
    }
  }

  /**
   * Set the DAO holding url and paths.
   *
   * @param aUrl2PathDao the url2PathDao to set
   */
  public static void setUrl2PathDao(IUrl2PathDao aUrl2PathDao) {
    url2PathDao = aUrl2PathDao;
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
  public static DataResource updateMetadataSchemaRecord(MetastoreConfiguration applicationProperties,
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

    ContentInformation info;
    info = getContentInformationOfResource(applicationProperties, dataResource);
    if (schemaDocument != null) {
      // Get schema record for this schema
      validateMetadataSchemaDocument(applicationProperties, dataResource, schemaDocument);

      boolean noChanges = false;
      String fileName = schemaDocument.getOriginalFilename();
      if (info != null) {
        noChanges = true;
        fileName = info.getRelativePath();
        // Check for changes...
        try {
          byte[] currentFileContent;
          File file = new File(URI.create(info.getContentUri()));
          if (schemaDocument.getSize() == Files.size(file.toPath())) {
            currentFileContent = FileUtils.readFileToByteArray(file);
            byte[] newFileContent = schemaDocument.getBytes();
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
      }
      if (!noChanges) {
        // Everything seems to be fine update document and increment version
        LOG.trace("Updating schema document (and increment version)...");
        String version = dataResource.getVersion();
        if (version != null) {
          dataResource.setVersion(Long.toString(Long.parseLong(version) + 1l));
        }
        ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, dataResource, schemaDocument, fileName, null, true, supplier);
        SchemaRecord schemaRecord = createSchemaRecord(dataResource, contentInformation);
        MetadataSchemaRecordUtil.saveNewSchemaRecord(schemaRecord);
      }
    } else {
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
        DataResourceRecordUtil.validateMetadataSchemaDocument(applicationProperties, dataResource, schemaDoc);
      } catch (IOException ex) {
        LOG.error("Error validating file!", ex);
      }

    }
    dataResource = DataResourceUtils.updateResource(applicationProperties, dataResource.getId(), dataResource, eTag, supplier);

    return dataResource;
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param metadataRecord metadata of the document.
   * @param document document
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          DataResource metadataRecord,
          MultipartFile document) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, metadataRecord, document);
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    boolean validationSuccess = false;
    StringBuilder errorMessage = new StringBuilder();
    RelatedIdentifier schemaIdentifier = getSchemaIdentifier(metadataRecord);
    SchemaRecord findByAlternateId;
    if ((schemaIdentifier != null) && (schemaIdentifier.getValue() != null)) {
      if (schemaIdentifier.getIdentifierType() != Identifier.IDENTIFIER_TYPE.INTERNAL) {
        findByAlternateId = schemaRecordDao.findByAlternateId(schemaIdentifier.getValue());
      } else {
        String schemaId = schemaIdentifier.getValue();
        String[] split = schemaId.split(SCHEMA_VERSION_SEPARATOR);

        if (split.length > 1) {
          findByAlternateId = schemaRecordDao.findBySchemaId(schemaId);
        } else {
          findByAlternateId = schemaRecordDao.findFirstBySchemaIdStartsWithOrderByVersionDesc(split[0] + SCHEMA_VERSION_SEPARATOR);
        }
      }
      if (findByAlternateId != null) {
        try {
          validateMetadataDocument(metastoreProperties, document, findByAlternateId);
          validationSuccess = true;
        } catch (Exception ex) {
          String message = "Error validating document!";
          LOG.error(message, ex);
          errorMessage.append(ex.getMessage()).append("\n");
        }
      } else {
        errorMessage.append("No matching schema found for '" + schemaIdentifier.getValue() + "'!");
      }
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
    return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class).getMetadataDocumentById(id, Long.parseLong(version), null, null)).toUri();
  }
}
