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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
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
    try {
      schemaRecordDao.save(schemaRecord);
      LOG.error("Schema record saved: " + schemaRecord);
    } catch (Exception npe) {
      LOG.error("Can't save schema record: " + schemaRecord, npe);
    }
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
    MetadataSchemaRecord existingRecord;
    DataResource newResource;

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
        LOG.error(message);
        throw new BadArgumentException(message);
      }
    }

    LOG.trace("Obtaining most recent metadata record with id {}.", resourceId);
    DataResource dataResource = applicationProperties.getDataResourceService().findById(resourceId);
    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(eTag, dataResource);
    if (record != null) {
      existingRecord = migrateToMetadataSchemaRecord(applicationProperties, dataResource, false);
      existingRecord = mergeRecords(existingRecord, record);
      dataResource = migrateToDataResource(applicationProperties, existingRecord);
    } else {
      dataResource = DataResourceUtils.copyDataResource(dataResource);
    }
    String version = dataResource.getVersion();
    if (version != null) {
      dataResource.setVersion(Long.toString(Long.parseLong(version) + 1l));
    }
    dataResource = DataResourceUtils.updateResource(applicationProperties, resourceId, dataResource, eTag, supplier);

    record = migrateToMetadataSchemaRecord(applicationProperties, dataResource, false);
    if (schemaDocument != null) {
      // Create schema record
      SchemaRecord schemaRecord = schemaRecordDao.findFirstBySchemaIdOrderByVersionDesc(record.getSchemaId());
      validateMetadataSchemaDocument(applicationProperties, schemaRecord, schemaDocument);
      LOG.trace("Updating metadata document.");
      ContentInformation info;
      info = getContentInformationOfResource(applicationProperties, dataResource);

      ContentInformation addFile = ContentDataUtils.addFile(applicationProperties, dataResource, schemaDocument, info.getRelativePath(), null, true, supplier);
      schemaRecord.setSchemaDocumentUri(addFile.getContentUri());
      LOG.trace("Updated to: {}", schemaRecord);
      try {
        schemaRecordDao.save(schemaRecord);
        LOG.error("Schema record saved: " + schemaRecord);
      } catch (Exception npe) {
        LOG.error("Can't save schema record: " + schemaRecord, npe);
      }
    }
    return migrateToMetadataSchemaRecord(applicationProperties, dataResource, true);
  }

  public static void deleteMetadataSchemaRecord(MetastoreConfiguration applicationProperties,
          String id,
          String eTag,
          Function<String, String> supplier) {
    DataResourceUtils.deleteResource(applicationProperties, id, eTag, supplier);
    List<SchemaRecord> listOfSchemaIds = schemaRecordDao.findBySchemaIdOrderByVersionDesc(id);
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
      if (metadataSchemaRecord.getPid() != null) {
        dataResource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier(metadataSchemaRecord.getPid()));
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
    return dataResource;
  }

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

      if (dataResource.getIdentifier() != null) {
        PrimaryIdentifier identifier = dataResource.getIdentifier();
        if (identifier.hasDoi()) {
          metadataSchemaRecord.setPid(identifier.getValue());
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
      } catch (NullPointerException npe) {
        LOG.debug("No schema record found! -> Create new schema record.");
        ContentInformation info;
        info = getContentInformationOfResource(applicationProperties, dataResource);
        if (info != null) {
          metadataSchemaRecord.setSchemaDocumentUri(info.getContentUri());
          saveNewSchemaRecord(metadataSchemaRecord);
        }
      }
    }
    long nano7 = System.nanoTime() / 1000000;
    LOG.error("Migrate to schema record, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano4 - nano1, nano6 - nano1, nano6 - nano1, nano7 - nano1);
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
    LOG.error("Content information of resource, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1);
    return returnValue;
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param metastoreProperties
   * @param document document to validate.
   * @param schemaId schemaId of schema.
   * @param version Version of the document.
   * @throws Exception Error validating document.
   */
  public static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MultipartFile document,
          String schemaId,
          Long version) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, schemaId, document);

    long nano1 = System.nanoTime() / 1000000;
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    SchemaRecord schemaRecord;
    if (version != null) {
      schemaRecord = schemaRecordDao.findBySchemaIdAndVersion(schemaId, version);
    } else {
      schemaRecord = schemaRecordDao.findFirstBySchemaIdOrderByVersionDesc(schemaId);
    }
    if (schemaRecord == null) {
      String message = String.format("No schema record found for '%s' and version '%d'!", schemaId, version);
      LOG.error(message);
      throw new ResourceNotFoundException(message);
    }
    long nano2 = System.nanoTime() / 1000000;
    try {
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
        schemaRecordDao.save(schemaRecord);
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
        if (!applicableValidator.validateMetadataDocument(schemaDocumentPath.toFile(), document.getInputStream())) {
          LOG.warn("Metadata document validation failed. -> " + applicableValidator.getErrorMessage());
          throw new UnprocessableEntityException(applicableValidator.getErrorMessage());
        }
        long nano5 = System.nanoTime() / 1000000;
        LOG.error("Validate document, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano4 - nano1);
      }
    } catch (IOException ex) {
      String message = "Failed to read metadata document from input stream.";
      LOG.error(message, ex);
      throw new UnprocessableEntityException(message);
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
    long nanoTime = System.nanoTime() / 1000000;
    DataResource dataResource = metastoreProperties.getDataResourceService().findByAnyIdentifier(recordId, version);
    long nanoTime2 = System.nanoTime() / 1000000;
    MetadataSchemaRecord result = migrateToMetadataSchemaRecord(metastoreProperties, dataResource, supportEtag);
    long nanoTime3 = System.nanoTime() / 1000000;
    LOG.error("getRecordByIDAndVersion," + nanoTime + ", " + (nanoTime2 - nanoTime) + ", " + (nanoTime3 - nanoTime));
    return result;
  }

  public static MetadataSchemaRecord mergeRecords(MetadataSchemaRecord managed, MetadataSchemaRecord provided) {
    if (provided != null) {
      if (!Objects.isNull(provided.getPid())) {
        LOG.trace("Updating pid from {} to {}.", managed.getPid(), provided.getPid());
        managed.setPid(provided.getPid());
      }

      //update acl
      if (provided.getAcl() != null) {
        LOG.trace("Updating record acl from {} to {}.", managed.getAcl(), provided.getAcl());
        managed.setAcl(provided.getAcl());
      }
      //update mimetype
      if (provided.getMimeType() != null) {
        LOG.trace("Updating record mimetype from {} to {}.", managed.getMimeType(), provided.getMimeType());
        managed.setMimeType(provided.getMimeType());
      }
      //update mimetype
      if (provided.getType() != null) {
        LOG.trace("Updating record type from {} to {}.", managed.getType(), provided.getType());
        managed.setType(provided.getType());
      }
    }
//    LOG.trace("Setting lastUpdate to now().");
//    managed.setLastUpdate(Instant.now());
    return managed;
  }

  private static void validateMetadataSchemaDocument(MetastoreConfiguration metastoreProperties, SchemaRecord schemaRecord, MultipartFile document) {
    LOG.debug("Validate metadata schema document...");
    if (document == null || document.isEmpty()) {
      String message = "Missing metadata schema document in body. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }

    IValidator applicableValidator = null;
    try {
      applicableValidator = getValidatorForRecord(metastoreProperties, schemaRecord, document.getBytes());

      if (applicableValidator == null) {
        String message = "No validator found for schema type " + schemaRecord.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.";
        LOG.error(message);
        throw new UnprocessableEntityException(message);
      } else {
        LOG.trace("Validator found. Checking provided schema file.");
        LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", schemaRecord.getSchemaId(), schemaRecord.getVersion(), applicableValidator);
        if (!applicableValidator.isSchemaValid(document.getInputStream())) {
          String message = "Metadata document validation failed. Returning HTTP UNPROCESSABLE_ENTITY.";
          LOG.warn(message);
          throw new UnprocessableEntityException(message);
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
        applicableValidator = validator;
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
    if (schemaRecordDao != null) {
      // Create shortcut for access.
      LOG.trace("Found new schema record!");
      SchemaRecord schemaRecord = new SchemaRecord();
      schemaRecord.setSchemaId(result.getSchemaId());
      schemaRecord.setVersion(result.getSchemaVersion());
      schemaRecord.setSchemaDocumentUri(result.getSchemaDocumentUri());
      schemaRecord.setType(result.getType());
      try {
        schemaRecordDao.save(schemaRecord);
        LOG.trace("Schema record saved: " + schemaRecord);
      } catch (Exception npe) {
        LOG.error("Can't save schema record: " + schemaRecord, npe);
      }
      LOG.trace("Schema record saved: {}", schemaRecord);
    }
  }

  public static void updateMetadataFormat(MetadataSchemaRecord record) {
    Optional<MetadataFormat> metadataFormat = metadataFormatDao.findById(record.getSchemaId());
    if (metadataFormat.isPresent()) {
      MetadataFormat mf = metadataFormat.get();
      mf.setSchema(record.getSchemaDocumentUri());
      metadataFormatDao.save(mf);
    }

  }
}
