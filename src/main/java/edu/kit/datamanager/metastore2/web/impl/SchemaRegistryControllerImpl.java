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
import edu.kit.datamanager.metastore2.dao.spec.LastUpdateSpecification;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.InternalIdentifierSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.TitleSpec;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.IRepoStorageService;
import edu.kit.datamanager.repo.service.IRepoVersioningService;
import edu.kit.datamanager.repo.service.impl.ContentInformationAuditService;
import edu.kit.datamanager.repo.service.impl.DataResourceAuditService;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/schemas")
@Schema(description = "Schema Registry")
public class SchemaRegistryControllerImpl implements ISchemaRegistryController {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImpl.class);
  @Autowired
  private final Javers javers;
  @Autowired
  private final IDataResourceService schemaResourceService;
  @Autowired
  private final IContentInformationService schemaInformationService;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private IRepoVersioningService[] versioningServices;
  @Autowired
  private IRepoStorageService[] storageServices;

  private final IAuditService<DataResource> auditServiceDataResource;
  private final IAuditService<ContentInformation> contentAuditService;

  private final MetastoreConfiguration metastoreProperties;
  @Autowired
  private final IDataResourceDao dataResourceDao;

  @Autowired
  private IValidator[] validators;

  /**
   *
   * @param applicationProperties
   * @param javers
   * @param schemaResourceService
   * @param dataResourceDao
   * @param schemaInformationService
   * @param versioningServices
   * @param storageServices
   * @param eventPublisher
   */
  public SchemaRegistryControllerImpl(ApplicationProperties applicationProperties,
          Javers javers,
          IDataResourceService schemaResourceService,
          IDataResourceDao dataResourceDao,
          IContentInformationService schemaInformationService,
          IRepoVersioningService[] versioningServices,
          IRepoStorageService[] storageServices,
          ApplicationEventPublisher eventPublisher,
          IValidator[] validators
  ) {
    this.applicationProperties = applicationProperties;
    this.javers = javers;
    this.dataResourceDao = dataResourceDao;
    this.schemaResourceService = schemaResourceService;
    this.schemaInformationService = schemaInformationService;
    this.versioningServices = versioningServices;
    this.storageServices = storageServices;
    this.eventPublisher = eventPublisher;
    this.validators = validators;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(this.applicationProperties.getSchemaFolder());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(this.schemaResourceService);
    rbc.setContentInformationService(this.schemaInformationService);
    rbc.setEventPublisher(this.eventPublisher);
    for (IRepoVersioningService versioningService : this.versioningServices) {
      if ("simple".equals(versioningService.getServiceName())) {
        LOG.info("Set versioning service: {}", versioningService.getServiceName());
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    for (IRepoStorageService storageService : this.storageServices) {
      if ("simple".equals(storageService.getServiceName())) {
        LOG.info("Set storage service: {}", storageService.getServiceName());
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
//    dataResourceService = new DataResourceService();
    this.schemaResourceService.configure(rbc);
//    contentInformationService = new ContentInformationService();
    this.schemaInformationService.configure(rbc);
//    rbc.setContentInformationAuditService(contentInformationAuditService);
    rbc.setAuditService(auditServiceDataResource);
    metastoreProperties = rbc;
    metastoreProperties.setSchemaRegistries(applicationProperties.getSchemaRegistries());
    metastoreProperties.setValidators(validators);
  }

  @Override
  public ResponseEntity createRecord(
          @RequestPart(name = "record") final MultipartFile recordDocument,
          @RequestPart(name = "schema") MultipartFile document,
          HttpServletRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder) {
    LOG.trace("Performing createRecord({},....", recordDocument);
    MetadataSchemaRecord record = MetadataSchemaRecordUtil.createMetadataSchemaRecord(metastoreProperties, recordDocument, document);
    LOG.trace("Schema record successfully persisted. Returning result.");
    String etag = record.getEtag();

    LOG.trace("Schema record successfully persisted. Updating document URI.");
    fixSchemaDocumentUri(record);
    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri();
    LOG.warn("uri              " + locationUri);
    return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(record);
//    if (document == null || document.isEmpty()) {
//      LOG.error("No schema document provided. Returning HTTP BAD_REQUEST.");
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No schema document provided.");
//    }
//    MetadataSchemaRecord record;
//    try {
//      if (recordDocument == null || recordDocument.isEmpty()) {
//        throw new IOException();
//      }
//      record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataSchemaRecord.class);
//    } catch (IOException ex) {
//      LOG.error("No metadata schema record provided. Returning HTTP BAD_REQUEST.");
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No metadata schema record provided.");
//    }
//
//    if (record.getSchemaId() == null) {
//      LOG.error("Mandatory attributes schemaId not found in record. Returning HTTP BAD_REQUEST.");
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes schemaId and/or mimeType not found in record.");
//    }
//
//    Optional<MetadataSchemaRecord> optRecord = metadataSchemaDao.findById(record.getSchemaId());
//    String existingSchemaHash = null;
//
//    if (!optRecord.isPresent()) {
//      //we have either a new record or the record has been deleted before
//      record.setCreatedAt(Instant.now());
//      record.setLastUpdate(record.getCreatedAt());
//    } else {
//      LOG.trace("Found existing schema record. Merging records.");
//      record = mergeRecords(optRecord.get(), record);
//      existingSchemaHash = record.getSchemaHash();
//    }
//
//    long version = auditService.getCurrentVersion(record.getSchemaId());
//    LOG.trace("Actual version: {}, Host: {}, HostAddr: ", version, request.getRemoteHost(), request.getRemoteAddr());
//    if (version > 0) {
//      try {
//        InetAddress addr = InetAddress.getByName(request.getRemoteHost());
//        if (!addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
//          LOG.error("Schema updates are only allowed from localhost.");
//          return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Schema updates are only allowed from localhost.");
//        }
//      } catch (UnknownHostException ex) {
//        LOG.error("Unable to check remove host address " + request.getRemoteAddr() + ".", ex);
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unable to check remote host address.");
//      }
//
//      LOG.trace("Schema with id {} found. Setting new schema version to {}.", record.getSchemaId(), (version + 1));
//      record.setSchemaVersion(version + 1);
//    } else if (version < 0) {
//      LOG.warn("Schema record with id {} has been deleted and cannot be re-activated.", record.getSchemaId());
//      return ResponseEntity.status(HttpStatus.GONE).body("The record with id " + record.getSchemaId() + "has been deleted and cannot be re-activated.");
//    } else {
//      version = 1l;
//      LOG.trace("Setting initial schema version to {}.", version);
//      record.setSchemaVersion(version);
//    }
//
//    try {
//      byte[] schemaBytes = document.getBytes();
//
//      //obtain/guess record type
//      if (record.getType() == null) {
//        record.setType(SchemaUtils.guessType(schemaBytes));
//        if (record.getType() == null) {
//          LOG.error("Unable to detect schema type automatically. Returning HTTP UNPROCESSABLE_ENTITY.");
//          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Unable to detect schema type automatically.");
//        } else {
//          LOG.debug("Automatically detected schema type {}.", record.getType());
//        }
//      }
//
//      //obtain validator for type
//      IValidator applicableValidator = null;
//      for (IValidator validator : validators) {
//        if (validator.supportsSchemaType(record.getType())) {
//          applicableValidator = validator;
//          break;
//        }
//      }
//
//      if (applicableValidator == null) {
//        LOG.error("No validator found for schema type " + record.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.");
//        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("No validator found for schema type " + record.getType() + ".");
//      } else {
//        //validate schema
//        if (!applicableValidator.isSchemaValid(new ByteArrayInputStream(schemaBytes))) {
//          LOG.error("Failed to validate provided schema document. Returning HTTP UNPROCESSABLE_ENTITY.");
//          LOG.trace(new String(schemaBytes));
//          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to validate provided schema document.");
//        }
//
//        //hash schema document and compare with existing hash
//        boolean writeSchemaFile = true;
//        try {
//          LOG.trace("Creating schema document hash and updating record.");
//          MessageDigest md = MessageDigest.getInstance("SHA1");
//          md.update(schemaBytes, 0, schemaBytes.length);
//          record.setSchemaHash("sha1:" + Hex.encodeHexString(md.digest()));
//
//          if (Objects.equals(record.getSchemaHash(), existingSchemaHash)) {
//            LOG.trace("Schema file hashes are equal. Skip writing new schema file.");
//            writeSchemaFile = false;
//          }
//        } catch (NoSuchAlgorithmException ex) {
//          LOG.error("Failed to initialize SHA1 MessageDigest.", ex);
//          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initialize SHA1 MessageDigest.");
//        }
//
//        if (writeSchemaFile) {
//          LOG.trace("Writing user-provided schema file to repository.");
//          URL schemaFolderUrl = metastoreProperties.getSchemaFolder();
//          try {
//            Path schemaDir = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId());
//            if (!Files.exists(schemaDir)) {
//              LOG.trace("Initially creating schema directory at {}.", schemaDir);
//              Files.createDirectories(schemaDir);
//            } else {
//              if (!Files.isDirectory(schemaDir)) {
//                LOG.error("Schema directory {} exists but is no folder. Aborting operation.", schemaDir);
//                throw new CustomInternalServerError("Illegal schema registry state detected.");
//              }
//            }
//
//            Path p = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId(), record.getSchemaId() + "_" + record.getSchemaVersion() + ((MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType())) ? ".xsd" : ".schema"));
//            if (Files.exists(p)) {
//              LOG.error("Schema conflict. A schema file at path {} already exists.", p);
//              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal schema filename conflict.");
//            }
//
//            LOG.trace("Persisting valid schema document at {}.", p);
//            Files.write(p, schemaBytes);
//            LOG.trace("Schema document successfully persisted. Updating record.");
//            record.setSchemaDocumentUri(p.toUri().toString());
//
//            LOG.trace("Schema record completed.");
//          } catch (URISyntaxException ex) {
//            LOG.error("Failed to determine schema storage location.", ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal misconfiguration of schema location.");
//          }
//        } else {
//          LOG.trace("Skip writing user-provided schema file to repository. Using unchanged, existing schema at {}.", record.getSchemaDocumentUri());
//        }
//
//        //schema file is written/skipped, continue with checking and writing metadata
//        String callerPrincipal = (String) AuthenticationHelper.getAuthentication().getPrincipal();
//        LOG.trace("Checking resource for caller acl entry. [sid = '{}']", callerPrincipal);
//        //check ACLs for caller
//        AclEntry callerEntry = null;
//        for (AclEntry entry : record.getAcl()) {
//          if (callerPrincipal.equals(entry.getSid())) {
//            LOG.trace("Acl entry for caller {} found: {}", callerPrincipal, entry);
//            callerEntry = entry;
//            break;
//          }
//        }
//
//        if (callerEntry == null) {
//          LOG.debug("Adding caller entry with ADMINISTRATE permissions.");
//          callerEntry = new AclEntry(callerPrincipal, PERMISSION.ADMINISTRATE);
//          record.getAcl().add(callerEntry);
//        } else {
//          LOG.debug("Ensuring ADMINISTRATE permissions for acl entry {}.", callerEntry);
//          //make sure at least the caller has administrate permissions
//          callerEntry.setPermission(PERMISSION.ADMINISTRATE);
//        }
//
//        LOG.trace("Persisting schema record.");
//        record = metadataSchemaDao.save(record);
//
//        LOG.trace("Capturing metadata schema audit information.");
//        auditService.captureAuditInformation(record, AuthenticationHelper.getPrincipal());
//
//        LOG.trace("Schema record successfully persisted. Returning result.");
//        String etag = record.getEtag();
//
//        LOG.trace("Schema record successfully persisted. Updating document URI.");
//        fixSchemaDocumentUri(record);
//        URI locationUri;
//        locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri();
//        LOG.warn("uri              " + locationUri);
//        return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(record);
//      }
//    } catch (IOException ex) {
//      LOG.error("Failed to read schema data from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
//      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read schema data from input stream.");
//    }
  }

  @Override
  public ResponseEntity getRecordById(String schemaId, Long version, WebRequest wr, HttpServletResponse hsr) {
    LOG.trace("Performing getRecordById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = MetadataSchemaRecordUtil.getRecordByIdAndVersion(metastoreProperties, schemaId, version);
    String etag = record.getEtag();

    fixSchemaDocumentUri(record);
    LOG.trace("Document URI successfully updated. Returning result.");
    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(record);
  }

  @Override
  public ResponseEntity getSchemaDocumentById(
          @PathVariable(value = "id") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getSchemaDocumentById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = MetadataSchemaRecordUtil.getRecordByIdAndVersion(metastoreProperties, schemaId, version);
    URI schemaDocumentUri = URI.create(record.getSchemaDocumentUri());

    MediaType contentType = MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType()) ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
    Path schemaDocumentPath = Paths.get(schemaDocumentUri);
    if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
      LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Schema document on server either does not exist or is no file or is not readable.");
    }

    return ResponseEntity.
            ok().
            contentType(contentType).
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(schemaDocumentPath.toFile().length())).
            body(new FileSystemResource(schemaDocumentPath.toFile()));
  }

  @Override
  public ResponseEntity validate(String schemaId, Long version, MultipartFile document, WebRequest wr, HttpServletResponse hsr) {
    LOG.trace("Performing validate({}, {}, {}).", schemaId, version, "#document");
    MetadataSchemaRecordUtil.validateMetadataDocument(metastoreProperties, document, schemaId, version);
    LOG.trace("Metadata document validation succeeded. Returning HTTP NOT_CONTENT.");
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(List<String> schemaIds, List<String> mimeTypes, Instant updateFrom, Instant updateUntil, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb) {
    LOG.trace("Performing getRecords({}, {}, {}, {}).", schemaIds, mimeTypes, updateFrom, updateUntil);
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE));
    //one of given ids.
    if ((schemaIds != null) && !schemaIds.isEmpty()) {
      spec = spec.and(InternalIdentifierSpec.toSpecification(schemaIds.toArray(new String[schemaIds.size()])));
    }
    if ((mimeTypes != null) && !mimeTypes.isEmpty()) {
      spec = spec.and(TitleSpec.toSpecification(mimeTypes.toArray(new String[mimeTypes.size()])));
    }
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }

    LOG.debug("Performing query for records.");
    Page<DataResource> records = null;
    try {
      records = dataResourceDao.findAll(spec, pgbl);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
    List<DataResource> recordList = records.getContent();
    LOG.trace("Cleaning up schemaDocumentUri of query result.");
    List<MetadataSchemaRecord> schemaList = new ArrayList<>();
    recordList.forEach((record) -> {
      MetadataSchemaRecord item = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(metastoreProperties, record);
      fixSchemaDocumentUri(item);
      schemaList.add(item);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(schemaList);
  }

  @Override
  public ResponseEntity updateRecord(
          @PathVariable("id") final String schemaId,
          @RequestPart(name = "record", required = false) MultipartFile record,
          @RequestPart(name = "schema", required = false) final MultipartFile document,
          final WebRequest request, final HttpServletResponse response) {
    LOG.trace("Performing updateMetadataSchemaRecord({}, {}).", schemaId, record);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, response)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(request);
    MetadataSchemaRecord updatedSchemaRecord = MetadataSchemaRecordUtil.updateMetadataSchemaRecord(metastoreProperties, schemaId, eTag, record, document, getById);

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    String etag = updatedSchemaRecord.getEtag();
    fixSchemaDocumentUri(updatedSchemaRecord);

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(updatedSchemaRecord.getSchemaId(), updatedSchemaRecord.getSchemaVersion(), null, null)).toUri();
    LOG.trace("Set locationUri to '{}'", locationUri.toString());
    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updatedSchemaRecord);
  }

  @Override
  public ResponseEntity deleteRecord(String schemaId, WebRequest request, HttpServletResponse hsr) {
    LOG.trace("Performing deleteRecord({}).", schemaId);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, hsr)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(request);
    MetadataSchemaRecordUtil.deleteMetadataSchemaRecord(metastoreProperties, schemaId, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private void fixSchemaDocumentUri(MetadataSchemaRecord record) {
    LOG.trace("Fix schema document Uri '{}'",record.getSchemaDocumentUri());
    record.setSchemaDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri().toString());
  }

}
