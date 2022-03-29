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
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.metastore2.domain.Url2Path;
import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.Description;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility class for handling json documents
 */
public class MetadataSchemaRecordUtil {

  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MetadataSchemaRecordUtil.class);
  /**
   * Encoding for strings/inputstreams.
   */
  private static final String ENCODING = "UTF-8";
  /**
   * Mapper for parsing json.
   */
  private static ObjectMapper mapper = new ObjectMapper();
  
  private static ISchemaRecordDao schemaRecordDao;
  
  private static IMetadataFormatDao metadataFormatDao;
  
  private static IUrl2PathDao url2PathDao;
  
  public static MetadataSchemaRecord createMetadataSchemaRecord(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument,
          MultipartFile document,
          BiFunction<String, Long, String> getSchemaDocumentById) {
    MetadataSchemaRecord result = null;
    MetadataSchemaRecord record;

    // Do some checks first.
    if (recordDocument == null || recordDocument.isEmpty() || document == null || document.isEmpty()) {
      String message = "No metadata record and/or metadata document provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataSchemaRecord.class);
    } catch (IOException ex) {
      String message = "No valid metadata record provided. Returning HTTP BAD_REQUEST.";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error("Error parsing json: ", ex);
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    
    if (record.getSchemaId() == null) {
      String message = "Mandatory attributes schemaId not found in record. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    } else {
      try {
        String value = URLEncoder.encode(record.getSchemaId(), StandardCharsets.UTF_8.toString());
        if (!value.equals(record.getSchemaId())) {
          String message = "Not a valid schema id! Encoded: " + value;
          LOG.error(message);
          throw new BadArgumentException(message);
        }
      } catch (UnsupportedEncodingException ex) {
        String message = "Error encoding schemaId " + record.getSchemaId();
        LOG.error(message);
        throw new CustomInternalServerError(message);
      }
    }
    // Create schema record
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(record.getSchemaId());
    schemaRecord.setType(record.getType());

    // End of parameter checks
    // validate schema document / determine type if not given
    validateMetadataSchemaDocument(applicationProperties, schemaRecord, document);
    // set internal parameters
    record.setType(schemaRecord.getType());
    if (record.getMimeType() == null) {
      LOG.trace("No mimetype set! Try to determine...");
      if (document.getContentType() != null) {
        LOG.trace("Set mimetype determined from document: '{}'", document.getContentType());
        record.setMimeType(document.getContentType());
      } else {
        LOG.trace("Set mimetype according to type '{}'.", record.getType());
        switch (record.getType()) {
          case JSON:
            record.setMimeType(MediaType.APPLICATION_JSON_VALUE);
            break;
          case XML:
            record.setMimeType(MediaType.APPLICATION_XML_VALUE);
            break;
          default:
            throw new BadArgumentException("Please provide mimetype for type '" + record.getType() + "'");
        }
      }
    }
    record.setSchemaVersion(1l);
    // create record.
    DataResource dataResource = migrateToDataResource(applicationProperties, record);
    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
    // store document
    ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, (t) -> {
      return "somethingStupid";
    });
    schemaRecord.setVersion(applicationProperties.getAuditService().getCurrentVersion(dataResource.getId()));
    schemaRecord.setSchemaDocumentUri(contentInformation.getContentUri());
    schemaRecord.setDocumentHash(contentInformation.getHash());
    saveNewSchemaRecord(schemaRecord);
    // Settings for OAI PMH
    if (MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(schemaRecord.getType())) {
      try {
        MetadataFormat metadataFormat = new MetadataFormat();
        metadataFormat.setMetadataPrefix(schemaRecord.getSchemaId());
        metadataFormat.setSchema(getSchemaDocumentById.apply(schemaRecord.getSchemaId(), schemaRecord.getVersion()));
        String metadataNamespace = SchemaUtils.getTargetNamespaceFromSchema(document.getBytes());
        metadataFormat.setMetadataNamespace(metadataNamespace);
        metadataFormatDao.save(metadataFormat);
      } catch (IOException ex) {
        String message = "Failed to read metadata document from input stream.";
        LOG.error(message, ex);
        throw new UnprocessableEntityException(message);
      }
    }
    
    return migrateToMetadataSchemaRecord(applicationProperties, createResource, true);
  }
  
  public static MetadataSchemaRecord updateMetadataSchemaRecord(MetastoreConfiguration applicationProperties,
          String resourceId,
          String eTag,
          MultipartFile recordDocument,
          MultipartFile schemaDocument,
          Function<String, String> supplier) {
    MetadataSchemaRecord record = null;

    // Do some checks first.
    if ((recordDocument == null || recordDocument.isEmpty()) && (schemaDocument == null || schemaDocument.isEmpty())) {
      String message = "Neither metadata record nor metadata document provided.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    if (!(recordDocument == null || recordDocument.isEmpty())) {
      try {
        record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataSchemaRecord.class);
      } catch (IOException ex) {
        String message = "Can't map record document to MetadataSchemaRecord";
        if (ex instanceof JsonParseException) {
          message = message + " Reason: " + ex.getMessage();
        }
        LOG.error("Error parsing json: ", ex);
        throw new BadArgumentException(message);
      }
    }
    
    LOG.trace("Obtaining most recent metadata schema record with id {}.", resourceId);
    DataResource dataResource = applicationProperties.getDataResourceService().findById(resourceId);
    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(eTag, dataResource);
    SchemaRecord schemaRecord = schemaRecordDao.findFirstBySchemaIdOrderByVersionDesc(dataResource.getId());
    if (record != null) {
      MetadataSchemaRecord existingRecord = migrateToMetadataSchemaRecord(applicationProperties, dataResource, false);
      existingRecord = mergeRecords(existingRecord, record);
      mergeSchemaRecord(schemaRecord, existingRecord);
      dataResource = migrateToDataResource(applicationProperties, existingRecord);
    } else {
      dataResource = DataResourceUtils.copyDataResource(dataResource);
    }
    
    if (schemaDocument != null) {
      // Get schema record for this schema
      validateMetadataSchemaDocument(applicationProperties, schemaRecord, schemaDocument);
      
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, dataResource);
      // Check for changes...
      boolean noChanges = true;
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
      if (noChanges == false) {
        // Everything seems to be fine update document and increment version
        LOG.trace("Updating schema document (and increment version)...");
        String version = dataResource.getVersion();
        if (version != null) {
          dataResource.setVersion(Long.toString(Long.parseLong(version) + 1l));
        }
        ContentInformation addFile = ContentDataUtils.addFile(applicationProperties, dataResource, schemaDocument, info.getRelativePath(), null, true, supplier);
      } else {
        schemaRecordDao.delete(schemaRecord);
      }
    } else {
      schemaRecordDao.delete(schemaRecord);
      // validate if document is still valid due to changed record settings.
      record = migrateToMetadataSchemaRecord(applicationProperties, dataResource, false);
      URI schemaDocumentUri = URI.create(record.getSchemaDocumentUri());
      
      Path schemaDocumentPath = Paths.get(schemaDocumentUri);
      if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
        LOG.warn("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
        throw new CustomInternalServerError("Schema document on server either does not exist or is no file or is not readable.");
      }
      
      try {
        byte[] schemaDoc = Files.readAllBytes(schemaDocumentPath);
        MetadataSchemaRecordUtil.validateMetadataSchemaDocument(applicationProperties, schemaRecord, schemaDoc);
      } catch (IOException ex) {
        LOG.error("Error validating file!", ex);
      }
      
    }
    dataResource = DataResourceUtils.updateResource(applicationProperties, resourceId, dataResource, eTag, supplier);
    
    return migrateToMetadataSchemaRecord(applicationProperties, dataResource, true);
  }
  
  public static void deleteMetadataSchemaRecord(MetastoreConfiguration applicationProperties,
          String id,
          String eTag,
          Function<String, String> supplier) {
    DataResourceUtils.deleteResource(applicationProperties, id, eTag, supplier);
    List<SchemaRecord> listOfSchemaIds = schemaRecordDao.findBySchemaIdOrderByVersionDesc(id);
    for (SchemaRecord item : listOfSchemaIds) {
      LOG.trace("Delete entry for path '{}'", item.getSchemaDocumentUri());
      List<Url2Path> findByPath = url2PathDao.findByPath(item.getSchemaDocumentUri());
      for (Url2Path entry : findByPath) {
        url2PathDao.delete(entry);
      }
    }
    schemaRecordDao.deleteAll(listOfSchemaIds);
  }
  
  public static DataResource migrateToDataResource(RepoBaseConfiguration applicationProperties,
          MetadataSchemaRecord metadataSchemaRecord) {
    DataResource dataResource = null;
    if (metadataSchemaRecord != null) {
      if (metadataSchemaRecord.getSchemaId() != null) {
        try {
          dataResource = applicationProperties.getDataResourceService().findById(metadataSchemaRecord.getSchemaId(), metadataSchemaRecord.getSchemaVersion());
          dataResource = DataResourceUtils.copyDataResource(dataResource);
        } catch (ResourceNotFoundException | NullPointerException rnfe) {
          LOG.error("Error catching DataResource for " + metadataSchemaRecord.getSchemaId() + " -> " + rnfe.getMessage());
          dataResource = DataResource.factoryNewDataResource(metadataSchemaRecord.getSchemaId());
          dataResource.setVersion("1");
        }
      } else {
        dataResource = new DataResource();
        dataResource.setVersion("1");
      }
      dataResource.setAcls(metadataSchemaRecord.getAcl());
      if (metadataSchemaRecord.getCreatedAt() != null) {
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
          dataResource.getDates().add(Date.factoryDate(metadataSchemaRecord.getCreatedAt(), Date.DATE_TYPE.CREATED));
        }
      }
      Set<Identifier> identifiers = dataResource.getAlternateIdentifiers();
      if (metadataSchemaRecord.getPid() != null) {
        ResourceIdentifier identifier = metadataSchemaRecord.getPid();
        checkAlternateIdentifier(identifiers, identifier.getIdentifier(), Identifier.IDENTIFIER_TYPE.valueOf(identifier.getIdentifierType().name()));
      } else {
        LOG.trace("Remove existing identifiers (others than INTERNAL)...");
        for (Identifier item : identifiers) {
          if (item.getIdentifierType() != Identifier.IDENTIFIER_TYPE.INTERNAL) {
            LOG.trace("... {},  {}", item.getValue(), item.getIdentifierType());
          }
        }
      }
      String defaultTitle = metadataSchemaRecord.getMimeType();
      boolean titleExists = false;
      for (Title title : dataResource.getTitles()) {
//        if (title.getTitleType() == Title.TYPE.OTHER && title.getValue().equals(defaultTitle)) {
        if (title.getTitleType() == Title.TYPE.OTHER) {
          title.setValue(defaultTitle);
          titleExists = true;
        }
      }
      if (!titleExists) {
        dataResource.getTitles().add(Title.factoryTitle(defaultTitle, Title.TYPE.OTHER));
      }
      dataResource.setResourceType(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE));
      dataResource.getFormats().clear();
      dataResource.getFormats().add(metadataSchemaRecord.getType().name());
    }
    // label      -> description of type (OTHER)
    // definition -> description of type (TECHNICAL_INFO)
    // comment    -> description of type (ABSTRACT)
    Set<Description> descriptions = dataResource.getDescriptions();
    
    checkDescription(descriptions, metadataSchemaRecord.getLabel(), Description.TYPE.OTHER);
    checkDescription(descriptions, metadataSchemaRecord.getDefinition(), Description.TYPE.TECHNICAL_INFO);
    checkDescription(descriptions, metadataSchemaRecord.getComment(), Description.TYPE.ABSTRACT);
    
    return dataResource;
  }

  /**
   * Test if description exists. If description is null remove existing
   * description type. if description added/changed add/change description with
   * given type.
   *
   * @param descriptions all descriptions
   * @param description Content of (new) description
   * @param type Type of the description
   */
  private static void checkDescription(Set<Description> descriptions, String description, Description.TYPE type) {
    Iterator<Description> iterator = descriptions.iterator();
    Description item = null;
    while (iterator.hasNext()) {
      Description next = iterator.next();
      
      if (next.getType().compareTo(type) == 0) {
        item = next;
        break;
      }
    }
    if (item != null) {
      if (description != null) {
        if (!description.equals(item.getDescription())) {
          item.setDescription(description);
        }
      } else {
        descriptions.remove(item);
      }
    } else {
      if (description != null) {
        item = Description.factoryDescription(description, type);
        descriptions.add(item);
      }
    }
  }

  /**
   * Test if alternate identifier exists. If alternate identifier is null remove
   * existing alternate identifier type. if alternate identifier added/changed
   * add/change alternate identifier with given type.
   *
   * @param identifiers all alternate identifiers
   * @param identifier Content of (new) alternate identifier
   * @param type Type of the alternate identifier
   */
  public static void checkAlternateIdentifier(Set<Identifier> identifiers, String identifier, Identifier.IDENTIFIER_TYPE type) {
    Iterator<Identifier> iterator = identifiers.iterator();
    Identifier item = null;
    while (iterator.hasNext()) {
      Identifier next = iterator.next();
      
      if (next.getIdentifierType().compareTo(type) == 0) {
        item = next;
        break;
      }
    }
    if (item != null) {
      if (identifier != null) {
        if (!identifier.equals(item.getValue())) {
          item.setValue(identifier);
        }
      } else {
        identifiers.remove(item);
      }
    } else {
      if (identifier != null) {
        item = Identifier.factoryIdentifier(identifier, type);
        identifiers.add(item);
      }
    }
  }

  /**
   * Transform dataresource to metadata schema record.
   *
   * @param applicationProperties Configuration of repository.
   * @param dataResource dataresource to transform
   * @param provideETag Calculate ETag or not.
   * @return dataresource as metadata schema record.
   */
  public static MetadataSchemaRecord migrateToMetadataSchemaRecord(RepoBaseConfiguration applicationProperties,
          DataResource dataResource,
          boolean provideETag) {
    MetadataSchemaRecord metadataSchemaRecord = new MetadataSchemaRecord();
    long nano = 0, nano1 = 0, nano2 = 0, nano3 = 0, nano4 = 0, nano5 = 0, nano6 = 0;
    if (dataResource != null) {
      nano1 = System.nanoTime() / 1000000;
      if (provideETag) {
        metadataSchemaRecord.setETag(dataResource.getEtag());
      }
      metadataSchemaRecord.setSchemaId(dataResource.getId());
      nano2 = System.nanoTime() / 1000000;
      MetadataSchemaRecord.SCHEMA_TYPE schemaType = MetadataSchemaRecord.SCHEMA_TYPE.valueOf(dataResource.getFormats().iterator().next());
      metadataSchemaRecord.setType(schemaType);
      nano3 = System.nanoTime() / 1000000;
      metadataSchemaRecord.setMimeType(dataResource.getTitles().iterator().next().getValue());
      nano4 = System.nanoTime() / 1000000;
      metadataSchemaRecord.setAcl(dataResource.getAcls());
      nano5 = System.nanoTime() / 1000000;
      for (edu.kit.datamanager.repo.domain.Date d : dataResource.getDates()) {
        if (edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())) {
          LOG.trace("Creation date entry found.");
          metadataSchemaRecord.setCreatedAt(d.getValue());
          break;
        }
      }
      nano6 = System.nanoTime() / 1000000;
      if (dataResource.getLastUpdate() != null) {
        metadataSchemaRecord.setLastUpdate(dataResource.getLastUpdate());
      }
      Iterator<Identifier> iterator = dataResource.getAlternateIdentifiers().iterator();
      while (iterator.hasNext()) {
        Identifier identifier = iterator.next();
        if (identifier.getIdentifierType() != Identifier.IDENTIFIER_TYPE.URL) {
          if (identifier.getIdentifierType() != Identifier.IDENTIFIER_TYPE.INTERNAL) {
            ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryResourceIdentifier(identifier.getValue(), ResourceIdentifier.IdentifierType.valueOf(identifier.getIdentifierType().name()));
            LOG.trace("Set PID to '{}' of type '{}'", resourceIdentifier.getIdentifier(), resourceIdentifier.getIdentifierType());
            metadataSchemaRecord.setPid(resourceIdentifier);
            break;
          } else {
            LOG.debug("'INTERNAL' identifier shouldn't be used! Migrate them to 'URL' if possible.");
          }
        }
      }
      
      Long schemaVersion = 1l;
      if (dataResource.getVersion() != null) {
        schemaVersion = Long.parseLong(dataResource.getVersion());
      }
      metadataSchemaRecord.setSchemaVersion(schemaVersion);
      
      SchemaRecord schemaRecord = null;
      try {
        LOG.debug("findByIDAndVersion {},{}", dataResource.getId(), metadataSchemaRecord.getSchemaVersion());
        schemaRecord = schemaRecordDao.findBySchemaIdAndVersion(dataResource.getId(), metadataSchemaRecord.getSchemaVersion());
        metadataSchemaRecord.setSchemaDocumentUri(schemaRecord.getSchemaDocumentUri());
        metadataSchemaRecord.setSchemaHash(schemaRecord.getDocumentHash());
      } catch (NullPointerException npe) {
        LOG.debug("No schema record found! -> Create new schema record.");
        ContentInformation info;
        info = getContentInformationOfResource(applicationProperties, dataResource);
        if (info != null) {
          metadataSchemaRecord.setSchemaDocumentUri(info.getContentUri());
          metadataSchemaRecord.setSchemaHash(info.getHash());
          saveNewSchemaRecord(metadataSchemaRecord);
        }
      }
    }
    long nano7 = System.nanoTime() / 1000000;
    // label -> description of type (OTHER)
    // description -> description of type (TECHNICAL_INFO)
    // comment -> description of type (ABSTRACT)
    Iterator<Description> iterator = dataResource.getDescriptions().iterator();
    while (iterator.hasNext()) {
      Description nextDescription = iterator.next();
      switch (nextDescription.getType()) {
        case ABSTRACT:
          metadataSchemaRecord.setComment(nextDescription.getDescription());
          break;
        case TECHNICAL_INFO:
          metadataSchemaRecord.setDefinition(nextDescription.getDescription());
          break;
        case OTHER:
          metadataSchemaRecord.setLabel(nextDescription.getDescription());
          break;
        default:
          LOG.trace("Unknown description type: '{}' -> skipped", nextDescription.getType());
      }
    }
    LOG.info("Migrate to schema record, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano4 - nano1, nano6 - nano1, nano6 - nano1, nano7 - nano1, provideETag);
    return metadataSchemaRecord;
  }
  
  private static ContentInformation getContentInformationOfResource(RepoBaseConfiguration applicationProperties,
          DataResource dataResource) {
    ContentInformation returnValue = null;
    long nano1 = System.nanoTime() / 1000000;
    IContentInformationService contentInformationService = applicationProperties.getContentInformationService();
    ContentInformation info = new ContentInformation();
    info.setParentResource(dataResource);
    List<ContentInformation> listOfFiles = contentInformationService.findAll(info, PageRequest.of(0, 100)).getContent();
    long nano2 = System.nanoTime() / 1000000;
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
    long nano3 = System.nanoTime() / 1000000;
    LOG.info("Content information of resource, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1);
    return returnValue;
  }

  /**
   * Validate metadata document with given schema. In case of an error a runtime
   * exception is thrown.
   *
   * @param metastoreProperties
   * @param document document to validate.
   * @param identifier identifier of schema.
   * @param version Version of the document.
   */
  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          ResourceIdentifier identifier,
          Long version) {
    SchemaRecord schemaRecord = getSchemaRecord(identifier, version);
    try {
      validateMetadataDocument(metastoreProperties, document, schemaRecord);
    } catch (Throwable tw) {
      throw tw;
    } finally {
      cleanUp(schemaRecord);
    }
  }
  
  private static void mergeSchemaRecord(SchemaRecord oldRecord, MetadataSchemaRecord newSettings) {
    LOG.trace("Merge Schema record...");
    Objects.requireNonNull(oldRecord);
    Objects.requireNonNull(newSettings);
    oldRecord.setDocumentHash(newSettings.getSchemaHash());
    oldRecord.setSchemaDocumentUri(newSettings.getSchemaDocumentUri());
    oldRecord.setSchemaId(newSettings.getSchemaId());
    oldRecord.setVersion(newSettings.getSchemaVersion());
    oldRecord.setType(newSettings.getType());
  }

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
      case INTERNAL:
        String schemaId = identifier.getIdentifier();
        if (schemaId == null) {
          String message = "Missing schemaID. Returning HTTP BAD_REQUEST.";
          LOG.error(message);
          throw new BadArgumentException(message);
        }
        if (version != null) {
          schemaRecord = schemaRecordDao.findBySchemaIdAndVersion(schemaId, version);
        } else {
          schemaRecord = schemaRecordDao.findFirstBySchemaIdOrderByVersionDesc(schemaId);
        }
        break;
      case URL:
        String url = identifier.getIdentifier();
        Path pathToFile;
        SCHEMA_TYPE type = null;
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
        schemaRecord = new SchemaRecord();
        schemaRecord.setSchemaDocumentUri(pathToFile.toUri().toString());
        schemaRecord.setType(type);
        break;
      default:
        throw new BadArgumentException("For schema document identifier type '" + identifier.getIdentifierType() + "' is not allowed!");
    }
    if (schemaRecord != null) {
      LOG.trace("getSchemaRecord {},{}", schemaRecord.getSchemaDocumentUri(), schemaRecord.getVersion());
    } else {
      LOG.trace("No matching schema record found!");
    }
    return schemaRecord;
  }
  
  public static void cleanUp(SchemaRecord schemaRecord) {
    LOG.trace("Clean up {}", schemaRecord);
    if (schemaRecord == null || schemaRecord.getSchemaDocumentUri() == null) {
      String message = "Missing resource locator for schema.";
      LOG.error(message);
    } else {
      String pathToSchemaDocument = fixRelativeURI(schemaRecord.getSchemaDocumentUri());
      List<Url2Path> findByUrl = url2PathDao.findByPath(pathToSchemaDocument);
      if (findByUrl.isEmpty()) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("-----------------------------------------");
          url2PathDao.findAll().forEach((item) -> {
            LOG.trace("- {}", item);
          });
          LOG.trace("-----------------------------------------");
        }
        // Remove downloaded file
        String uri = schemaRecord.getSchemaDocumentUri();
        Path pathToFile = Paths.get(URI.create(uri));
        DownloadUtil.removeFile(pathToFile);
      }
    }
  }

  /**
   * Validate metadata document with given schema. In case of an error a runtime
   * exception is thrown.
   *
   * @param metastoreProperties
   * @param document document to validate.
   * @param schemaId schemaId of schema.
   * @param version Version of the document.
   */
  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          String schemaId,
          Long version) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, schemaId, document);
    if (schemaId == null) {
      String message = "Missing schemaID. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    
    long nano1 = System.nanoTime() / 1000000;
    ResourceIdentifier resourceIdentifier = ResourceIdentifier.factoryInternalResourceIdentifier(schemaId);
    SchemaRecord schemaRecord = getSchemaRecord(resourceIdentifier, version);
    long nano2 = System.nanoTime() / 1000000;
    validateMetadataDocument(metastoreProperties, document, schemaRecord);
    long nano3 = System.nanoTime() / 1000000;
    LOG.info("Validate document(schemaId,version), {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1);
  }

  /**
   * Validate metadata document with given schema. In case of an error a runtime
   * exception is thrown.
   *
   * @param metastoreProperties
   * @param document document to validate.
   * @param schemaRecord record of the schema.
   */
  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          SchemaRecord schemaRecord) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, schemaRecord, document);
    
    long nano1 = System.nanoTime() / 1000000;
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      try ( InputStream inputStream = document.getInputStream()) {
        validateMetadataDocument(metastoreProperties, inputStream, schemaRecord);
      }
    } catch (IOException ex) {
      String message = "Failed to read metadata document from input stream.";
      LOG.error(message, ex);
      throw new UnprocessableEntityException(message);
    }
  }

  /**
   * Validate metadata document with given schema. In case of an error a runtime
   * exception is thrown.
   *
   * @param metastoreProperties
   * @param inputStream document to validate.
   * @param schemaRecord record of the schema.
   */
  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          InputStream inputStream,
          SchemaRecord schemaRecord) throws IOException {
    LOG.trace("validateMetadataInputStream {},{}, {}", metastoreProperties, schemaRecord, inputStream);
    
    long nano1 = System.nanoTime() / 1000000;
    if (schemaRecord == null || schemaRecord.getSchemaDocumentUri() == null || schemaRecord.getSchemaDocumentUri().trim().isEmpty()) {
      String message = "Missing or invalid schema record. Returning HTTP BAD_REQUEST.";
      LOG.error(message + " -> '{}'", schemaRecord);
      throw new BadArgumentException(message);
    }
    long nano2 = System.nanoTime() / 1000000;
    LOG.trace("Checking local schema file.");
    Path schemaDocumentPath = Paths.get(URI.create(schemaRecord.getSchemaDocumentUri()));
    
    if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
      LOG.error("Schema document with schemaId '{}'at path {} either does not exist or is no file or is not readable.", schemaRecord.getSchemaId(), schemaDocumentPath);
      throw new CustomInternalServerError("Schema document on server either does not exist or is no file or is not readable.");
    }
    LOG.trace("obtain validator for type");
    IValidator applicableValidator;
    if (schemaRecord.getType() == null) {
      byte[] schemaDocument = FileUtils.readFileToByteArray(schemaDocumentPath.toFile());
      applicableValidator = getValidatorForRecord(metastoreProperties, schemaRecord, schemaDocument);
    } else {
      applicableValidator = getValidatorForRecord(metastoreProperties, schemaRecord, null);
    }
    long nano3 = System.nanoTime() / 1000000;
    
    if (applicableValidator == null) {
      String message = "No validator found for schema type " + schemaRecord.getType();
      LOG.error(message);
      throw new UnprocessableEntityException(message);
    } else {
      LOG.trace("Validator found.");
      
      LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", schemaRecord.getSchemaId(), schemaRecord.getVersion(), applicableValidator);
      long nano4 = System.nanoTime() / 1000000;
      if (!applicableValidator.validateMetadataDocument(schemaDocumentPath.toFile(), inputStream)) {
        LOG.warn("Metadata document validation failed. -> " + applicableValidator.getErrorMessage());
        throw new UnprocessableEntityException(applicableValidator.getErrorMessage());
      }
      long nano5 = System.nanoTime() / 1000000;
      LOG.info("Validate document(schemaRecord), {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano4 - nano1);
    }
    LOG.trace("Metadata document validation succeeded.");
  }
  
  public static MetadataSchemaRecord getRecordById(MetastoreConfiguration metastoreProperties,
          String recordId) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(metastoreProperties, recordId, null, false);
  }
  
  public static MetadataSchemaRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(metastoreProperties, recordId, version, false);
  }
  
  public static MetadataSchemaRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version, boolean supportEtag) throws ResourceNotFoundException {
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    long nano = System.nanoTime() / 1000000;
    MetadataSchemaRecord result = null;
    Page<DataResource> dataResource = metastoreProperties.getDataResourceService().findAllVersions(recordId, null);
//    DataResource dataResource = metastoreProperties.getDataResourceService().findByAnyIdentifier(recordId, version);
    long nano2 = System.nanoTime() / 1000000;
    Stream<DataResource> stream = dataResource.get();
    if (version != null) {
      stream = stream.filter(resource -> Long.parseLong(resource.getVersion()) == version);
    }
    Optional<DataResource> findFirst = stream.findFirst();
    if (findFirst.isPresent()) {
      result = migrateToMetadataSchemaRecord(metastoreProperties, findFirst.get(), supportEtag);
    } else {
      String message = String.format("ID '%s' or version '%d' doesn't exist!", recordId, version.longValue());
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    long nano3 = System.nanoTime() / 1000000;
    LOG.info("getRecordByIdAndVersion {}, {}, {}", nano, (nano2 - nano), (nano3 - nano));
    return result;
  }
  
  public static MetadataSchemaRecord mergeRecords(MetadataSchemaRecord managed, MetadataSchemaRecord provided) {
    if (provided != null) {
      // update pid
      if (!Objects.isNull(provided.getPid())) {
        if (!provided.getPid().equals(managed.getPid())) {
          LOG.trace("Updating pid from {} to {}.", managed.getPid(), provided.getPid());
          managed.setPid(provided.getPid());
        }
      }
      //update acl
      if (!provided.getAcl().isEmpty()) {
        if (!provided.getAcl().equals(managed.getAcl())) {
          // check for special access rights 
          // - only administrators are allowed to change ACL
          // - at least principal has to remain as ADMIN 
          if (MetadataRecordUtil.checkAccessRights(provided.getAcl())) {
            LOG.trace("Updating record acl from {} to {}.", managed.getAcl(), provided.getAcl());
            managed.setAcl(provided.getAcl());
          }
        }
      }
      //update mimetype
      if (provided.getMimeType() != null) {
        if (!provided.getMimeType().equals(managed.getMimeType())) {
          LOG.trace("Updating record mimetype from {} to {}.", managed.getMimeType(), provided.getMimeType());
          managed.setMimeType(provided.getMimeType());
        }
      }
      //update type
      if (provided.getType() != null) {
        if (!provided.getType().equals(managed.getType())) {
          LOG.trace("Updating record type from {} to {}.", managed.getType(), provided.getType());
          managed.setType(provided.getType());
        }
      }
      //update label
      if (provided.getLabel() != null) {
        if (!provided.getLabel().equals(managed.getLabel())) {
          LOG.trace("Updating record label from {} to {}.", managed.getLabel(), provided.getLabel());
          managed.setLabel(checkForEmptyString(provided.getLabel()));
        }
      }
      //update definition
      if (provided.getDefinition() != null) {
        if (!provided.getDefinition().equals(managed.getDefinition())) {
          LOG.trace("Updating record definition from {} to {}.", managed.getDefinition(), provided.getDefinition());
          managed.setDefinition(checkForEmptyString(provided.getDefinition()));
        }
      }
      //update comment
      if (provided.getComment() != null) {
        if (!provided.getComment().equals(managed.getComment())) {
          LOG.trace("Updating record comment from {} to {}.", managed.getComment(), provided.getComment());
          managed.setComment(checkForEmptyString(provided.getComment()));
        }
      }
      //update doNotSync
      if (!provided.getDoNotSync().equals(managed.getDoNotSync())) {
        LOG.trace("Updating record comment from {} to {}.", managed.getDoNotSync(), provided.getDoNotSync());
        managed.setDoNotSync(provided.getDoNotSync());
      }
      //update schemaId
      if (provided.getSchemaId() != null) {
        if (!provided.getSchemaId().equals(managed.getSchemaId())) {
          LOG.trace("Updating record comment from {} to {}.", managed.getSchemaId(), provided.getSchemaId());
          managed.setSchemaId(provided.getSchemaId());
        }
      }
      //update schemaVersion
      if (provided.getSchemaVersion() != null) {
        if (!provided.getSchemaVersion().equals(managed.getSchemaVersion())) {
          LOG.trace("Updating record comment from {} to {}.", managed.getSchemaVersion(), provided.getSchemaVersion());
          managed.setSchemaVersion(provided.getSchemaVersion());
        }
      }
    }
//    LOG.trace("Setting lastUpdate to now().");
//    managed.setLastUpdate(Instant.now());
    return managed;
  }

  /**
   * Check for empty String. If String is empty return 'NULL'.
   *
   * @param string String to check.
   * @return String or 'NULL'
   */
  private static String checkForEmptyString(String string) {
    String returnValue = null;
    if (!string.isEmpty()) {
      returnValue = string;
    }
    return returnValue;
  }
  
  private static void validateMetadataSchemaDocument(MetastoreConfiguration metastoreProperties, SchemaRecord schemaRecord, MultipartFile document) {
    LOG.debug("Validate metadata schema document...");
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata schema document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      validateMetadataSchemaDocument(metastoreProperties, schemaRecord, document.getBytes());
    } catch (IOException ex) {
      String message = "Failed to read metadata document from input stream.";
      LOG.error(message, ex);
      throw new UnprocessableEntityException(message);
    }
  }
  
  private static void validateMetadataSchemaDocument(MetastoreConfiguration metastoreProperties, SchemaRecord schemaRecord, byte[] document) {
    LOG.debug("Validate metadata schema document...");
    if (document == null || document.length == 0) {
      String message = "Missing metadata schema document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    
    IValidator applicableValidator = null;
    try {
      applicableValidator = getValidatorForRecord(metastoreProperties, schemaRecord, document);
      
      if (applicableValidator == null) {
        String message = "No validator found for schema type " + schemaRecord.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.";
        LOG.error(message);
        throw new UnprocessableEntityException(message);
      } else {
        LOG.trace("Validator found. Checking provided schema file.");
        LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", schemaRecord.getSchemaId(), schemaRecord.getVersion(), applicableValidator);
        try ( InputStream inputStream = new ByteArrayInputStream(document)) {
          if (!applicableValidator.isSchemaValid(inputStream)) {
            String message = "Metadata schema document validation failed. Returning HTTP UNPROCESSABLE_ENTITY.";
            LOG.warn(message);
            if (LOG.isTraceEnabled()) {
              LOG.trace("Schema: " + document);
            }
            throw new UnprocessableEntityException(message);
          }
        }
      }
    } catch (IOException ex) {
      String message = "Failed to read metadata document from input stream.";
      LOG.error(message, ex);
      throw new UnprocessableEntityException(message);
    }
    
    LOG.trace("Schema document is valid!");
  }
  
  private static IValidator getValidatorForRecord(MetastoreConfiguration metastoreProperties, SchemaRecord schemaRecord, byte[] schemaDocument) {
    IValidator applicableValidator = null;
    //obtain/guess record type
    if (schemaRecord.getType() == null) {
      schemaRecord.setType(SchemaUtils.guessType(schemaDocument));
      if (schemaRecord.getType() == null) {
        String message = "Unable to detect schema type automatically. Please provide a valid type";
        LOG.error(message);
        throw new UnprocessableEntityException(message);
      } else {
        LOG.debug("Automatically detected schema type {}.", schemaRecord.getType());
      }
    }
    for (IValidator validator : metastoreProperties.getValidators()) {
      if (validator.supportsSchemaType(schemaRecord.getType())) {
        applicableValidator = validator.getInstance();
        LOG.trace("Found validator for schema: '{}'", schemaRecord.getType().name());
        break;
      }
    }
    return applicableValidator;
  }

  /**
   * @param aSchemaRecordDao the schemaRecordDao to set
   */
  public static void setSchemaRecordDao(ISchemaRecordDao aSchemaRecordDao) {
    schemaRecordDao = aSchemaRecordDao;
  }

  /**
   * @param aSchemaRecordDao the schemaRecordDao to set
   */
  public static void setMetadataFormatDao(IMetadataFormatDao aMetadataFormatDao) {
    metadataFormatDao = aMetadataFormatDao;
  }
  
  private static void saveNewSchemaRecord(MetadataSchemaRecord result) {
    SchemaRecord schemaRecord = null;

    // Create shortcut for access.
    LOG.trace("Save new schema record!");
    schemaRecord = transformToSchemaRecord(result);
    
    saveNewSchemaRecord(schemaRecord);
  }
  
  private static SchemaRecord transformToSchemaRecord(MetadataSchemaRecord result) {
    SchemaRecord schemaRecord = null;
    LOG.trace("Transform to schema record! ({})", result);
    if (result != null) {
      schemaRecord = new SchemaRecord();
      schemaRecord.setSchemaId(result.getSchemaId());
      schemaRecord.setVersion(result.getSchemaVersion());
      schemaRecord.setSchemaDocumentUri(result.getSchemaDocumentUri());
      schemaRecord.setType(result.getType());
      schemaRecord.setDocumentHash(result.getSchemaHash());
    }
    return schemaRecord;
  }
  
  private static void saveNewSchemaRecord(SchemaRecord schemaRecord) {
    if (schemaRecordDao != null) {
      try {
        schemaRecordDao.save(schemaRecord);
      } catch (Exception npe) {
        LOG.error("Can't save schema record: " + schemaRecord, npe);
      }
      LOG.trace("Schema record saved: {}", schemaRecord);
    }
  }

  /**
   * Transform schema identifier to global available identifier (if neccessary).
   *
   * @param applicationProperties configuration of schema registry.
   * @param metadataRecord Metadata record hold schema identifier.
   * @return ResourceIdentifier with a global accessible identifier.
   */
  public static ResourceIdentifier getSchemaIdentifier(MetastoreConfiguration applicationProperties,
          MetadataRecord metadataRecord) {
    LOG.trace("Get schema identifier for '{}'.", metadataRecord.getSchema());
    ResourceIdentifier returnValue = metadataRecord.getSchema();
    if (metadataRecord.getSchema().getIdentifierType() == IdentifierType.INTERNAL) {
      MetadataSchemaRecord msr = MetadataSchemaRecordUtil.getRecordByIdAndVersion(applicationProperties, metadataRecord.getSchema().getIdentifier(), metadataRecord.getSchemaVersion());
      returnValue = getSchemaIdentifier(applicationProperties, msr);
    }
    return returnValue;
  }
  
  public static ResourceIdentifier getSchemaIdentifier(MetastoreConfiguration applicationProperties,
          MetadataSchemaRecord metadataSchemaRecord) {
    LOG.trace("Get schema identifier for '{}'.", metadataSchemaRecord);
    ResourceIdentifier returnValue;
    if (LOG.isTraceEnabled()) {
      LOG.trace("Looking for path '{}'", metadataSchemaRecord.getSchemaDocumentUri());
      LOG.trace("-----------------------------------------");
      url2PathDao.findAll().forEach((item) -> {
        LOG.trace("- {}", item);
      });
      LOG.trace("-----------------------------------------");
    }
    List<Url2Path> findByPath = url2PathDao.findByPath(metadataSchemaRecord.getSchemaDocumentUri());
    if (findByPath.isEmpty()) {
      throw new CustomInternalServerError("Unknown schemaID '" + metadataSchemaRecord.getSchemaId() + "'!");
    }
    returnValue = ResourceIdentifier.factoryUrlResourceIdentifier(findByPath.get(0).getUrl());
    
    LOG.trace("Return: '{}'", returnValue);
    return returnValue;
  }
  
  public static void updateMetadataFormat(MetadataSchemaRecord record) {
    Optional<MetadataFormat> metadataFormat = metadataFormatDao.findById(record.getSchemaId());
    if (metadataFormat.isPresent()) {
      MetadataFormat mf = metadataFormat.get();
      mf.setSchema(record.getSchemaDocumentUri());
      metadataFormatDao.save(mf);
    }
    
  }

  /**
   * Returns schema record with the current version.
   *
   * @param metastoreProperties Configuration for accessing services
   * @param schema Identifier of the schema.
   * @return MetadataSchemaRecord ResponseEntity in case of an error.
   * @throws IOException Error reading document.
   */
  public static MetadataSchemaRecord getCurrentSchemaRecord(MetastoreConfiguration metastoreProperties,
          ResourceIdentifier schema) {
    MetadataSchemaRecord msr;
    if (schema.getIdentifierType() == IdentifierType.INTERNAL) {
      msr = MetadataRecordUtil.getCurrentInternalSchemaRecord(metastoreProperties, schema.getIdentifier());
    } else {
      msr = new MetadataSchemaRecord();
      Optional<Url2Path> url2path = url2PathDao.findByUrl(schema.getIdentifier());
      Long version = 1l;
      if (url2path.isPresent()) {
        version = url2path.get().getVersion();
      }
      msr.setSchemaVersion(version);
    }
    
    return msr;
  }

  /**
   * @param aUrl2PathDao the url2PathDao to set
   */
  public static void setUrl2PathDao(IUrl2PathDao aUrl2PathDao) {
    url2PathDao = aUrl2PathDao;
  }

  /**
   * Fix relative URI.
   *
   * @param uri (relative) URI
   * @return absolute URL
   */
  public static String fixRelativeURI(String uri) {
    String returnValue = null;
    URI urig = URI.create(uri);
    try {
      if (urig.isAbsolute()) {
        returnValue = Paths.get(new URI(uri)).toAbsolutePath().toUri().toURL().toString();
      } else {
        returnValue = Paths.get(uri).toFile().toURI().toURL().toString();
      }
    } catch (URISyntaxException | MalformedURLException ex) {
      LOG.error("Error fixing URI '" + uri + "'", ex);
    }
    LOG.trace("Fix URI '{}' -> '{}'", uri, returnValue);
    return returnValue;
  }

}
