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

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.dao.spec.LastUpdateSpecification;
import edu.kit.datamanager.metastore2.dao.spec.MimeTypeSpecification;
import edu.kit.datamanager.metastore2.dao.spec.SchemaIdSpecification;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import edu.kit.datamanager.metastore2.util.SchemaUtils;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/schemas")
public class SchemaRegistryControllerImpl implements ISchemaRegistryController {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImpl.class);

  @Autowired
  private ApplicationProperties metastoreProperties;
  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private IValidator[] validators;
  @Autowired
  private IAuditService<MetadataSchemaRecord> auditService;

  @PersistenceContext
  private EntityManager em;

  @Override
  public ResponseEntity createRecord(MetadataSchemaRecord record, MultipartFile document, HttpServletRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder) {
    LOG.trace("Performing createRecord({}, {}).", record, "#document");
    if (document == null || document.isEmpty()) {
      LOG.error("No schema document provided. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No schema document provided.");
    }
    if (record.getSchemaId() == null) {
      LOG.error("Mandatory attributes schemaId not found in record. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes schemaId and/or mimeType not found in record.");
    }

    Optional<MetadataSchemaRecord> optRecord = metadataSchemaDao.findById(record.getSchemaId());
    String existingSchemaHash = null;

    if (!optRecord.isPresent()) {
      //we have either a new record or the record has been deleted before
      record.setCreatedAt(Instant.now());
      record.setLastUpdate(record.getCreatedAt());
    } else {
      LOG.trace("Found existing schema record. Merging records.");
      record = mergeRecords(optRecord.get(), record);
      existingSchemaHash = record.getSchemaHash();
    }

    long version = auditService.getCurrentVersion(record.getSchemaId());
    LOG.trace("Actual version: {}, Host: {}, HostAddr: ", version, request.getRemoteHost(), request.getRemoteAddr());
    if (version > 0) {
      try {
        InetAddress addr = InetAddress.getByName(request.getRemoteHost());
        if (!addr.isAnyLocalAddress() && !addr.isLoopbackAddress()) {
          LOG.error("Schema updates are only allowed from localhost.");
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Schema updates are only allowed from localhost.");
        }
      } catch (UnknownHostException ex) {
        LOG.error("Unable to check remove host address " + request.getRemoteAddr() + ".", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unable to check remote host address.");
      }

      LOG.trace("Schema with id {} found. Setting new schema version to {}.", record.getSchemaId(), (version + 1));
      record.setSchemaVersion(version + 1);
    } else if (version < 0) {
      LOG.warn("Schema record with id {} has been deleted and cannot be re-activated.", record.getSchemaId());
      return ResponseEntity.status(HttpStatus.GONE).body("The record with id " + record.getSchemaId() + "has been deleted and cannot be re-activated.");
    } else {
      version = 1l;
      LOG.trace("Setting initial schema version to {}.", version);
      record.setSchemaVersion(version);
    }

    try {
      byte[] schemaBytes = document.getBytes();

      //obtain/guess record type
      if (record.getType() == null) {
        record.setType(SchemaUtils.guessType(schemaBytes));
        if (record.getType() == null) {
          LOG.error("Unable to detect schema type automatically. Returning HTTP UNPROCESSABLE_ENTITY.");
          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Unable to detect schema type automatically.");
        } else {
          LOG.debug("Automatically detected schema type {}.", record.getType());
        }
      }

      //obtain validator for type
      IValidator applicableValidator = null;
      for (IValidator validator : validators) {
        if (validator.supportsSchemaType(record.getType())) {
          applicableValidator = validator;
          break;
        }
      }

      if (applicableValidator == null) {
        LOG.error("No validator found for schema type " + record.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("No validator found for schema type " + record.getType() + ".");
      } else {
        //validate schema
        if (!applicableValidator.isSchemaValid(new ByteArrayInputStream(schemaBytes))) {
          LOG.error("Failed to validate provided schema document. Returning HTTP UNPROCESSABLE_ENTITY.");
          LOG.trace(new String(schemaBytes));
          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to validate provided schema document.");
        }

        //hash schema document and compare with existing hash
        boolean writeSchemaFile = true;
        try {
          LOG.trace("Creating schema document hash and updating record.");
          MessageDigest md = MessageDigest.getInstance("SHA1");
          md.update(schemaBytes, 0, schemaBytes.length);
          record.setSchemaHash("sha1:" + Hex.encodeHexString(md.digest()));

          if (Objects.equals(record.getSchemaHash(), existingSchemaHash)) {
            LOG.trace("Schema file hashes are equal. Skip writing new schema file.");
            writeSchemaFile = false;
          }
        } catch (NoSuchAlgorithmException ex) {
          LOG.error("Failed to initialize SHA1 MessageDigest.", ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initialize SHA1 MessageDigest.");
        }

        if (writeSchemaFile) {
          LOG.trace("Writing user-provided schema file to repository.");
          URL schemaFolderUrl = metastoreProperties.getSchemaFolder();
          try {
            Path schemaDir = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId());
            if (!Files.exists(schemaDir)) {
              LOG.trace("Initially creating schema directory at {}.", schemaDir);
              Files.createDirectories(schemaDir);
            } else {
              if (!Files.isDirectory(schemaDir)) {
                LOG.error("Schema directory {} exists but is no folder. Aborting operation.", schemaDir);
                throw new CustomInternalServerError("Illegal schema registry state detected.");
              }
            }

            Path p = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId(), record.getSchemaId() + "_" + record.getSchemaVersion() + ((MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType())) ? ".xsd" : ".schema"));
            if (Files.exists(p)) {
              LOG.error("Schema conflict. A schema file at path {} already exists.", p);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal schema filename conflict.");
            }

            LOG.trace("Persisting valid schema document at {}.", p);
            Files.write(p, schemaBytes);
            LOG.trace("Schema document successfully persisted. Updating record.");
            record.setSchemaDocumentUri(p.toUri().toString());

            LOG.trace("Schema record completed.");
          } catch (URISyntaxException ex) {
            LOG.error("Failed to determine schema storage location.", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal misconfiguration of schema location.");
          }
        } else {
          LOG.trace("Skip writing user-provided schema file to repository. Using unchanged, existing schema at {}.", record.getSchemaDocumentUri());
        }

        //schema file is written/skipped, continue with checking and writing metadata
        String callerPrincipal = (String) AuthenticationHelper.getAuthentication().getPrincipal();
        LOG.trace("Checking resource for caller acl entry. [sid = '{}']", callerPrincipal);
        //check ACLs for caller
        AclEntry callerEntry = null;
        for (AclEntry entry : record.getAcl()) {
          if (callerPrincipal.equals(entry.getSid())) {
            LOG.trace("Acl entry for caller {} found: {}", callerPrincipal, entry);
            callerEntry = entry;
            break;
          }
        }

        if (callerEntry == null) {
          LOG.debug("Adding caller entry with ADMINISTRATE permissions.");
          callerEntry = new AclEntry(callerPrincipal, PERMISSION.ADMINISTRATE);
          record.getAcl().add(callerEntry);
        } else {
          LOG.debug("Ensuring ADMINISTRATE permissions for acl entry {}.", callerEntry);
          //make sure at least the caller has administrate permissions
          callerEntry.setPermission(PERMISSION.ADMINISTRATE);
        }

        LOG.trace("Persisting schema record.");
        record = metadataSchemaDao.save(record);

        LOG.trace("Capturing metadata schema audit information.");
        auditService.captureAuditInformation(record, AuthenticationHelper.getPrincipal());

        LOG.trace("Schema record successfully persisted. Returning result.");
        String etag = record.getEtag();

        LOG.trace("Schema record successfully persisted. Updating document URI.");
         fixSchemaDocumentUri(record);
        URI locationUri;
        locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri();
        LOG.warn("uri              " + locationUri);
        return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(record);
      }
    } catch (IOException ex) {
      LOG.error("Failed to read schema data from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read schema data from input stream.");
    }
  }

  @Override
  public ResponseEntity getRecordById(String schemaId, Long version, WebRequest wr, HttpServletResponse hsr) {
    LOG.trace("Performing getRecordById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = getRecordByIdAndVersion(schemaId, version);
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
    MetadataSchemaRecord record = getRecordByIdAndVersion(schemaId, version);
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

    if (document == null || document.isEmpty()) {
      LOG.error("Missing metadata document in body. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Metadata document is missing from request body.");
    }

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = getRecordByIdAndVersion(schemaId, version);
    IValidator applicableValidator = null;
    LOG.trace("Checking for applicable validator.");
    for (IValidator validator : validators) {
      if (validator.supportsSchemaType(record.getType())) {
        applicableValidator = validator;
        break;
      }
    }

    if (applicableValidator == null) {
      LOG.error("No validator found for schema type " + record.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("No validator found for schema type " + record.getType() + ".");
    } else {
      LOG.trace("Validator found. Checking local schema file.");
      Path schemaDocumentPath = Paths.get(URI.create(record.getSchemaDocumentUri()));

      if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
        LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Schema document on server either does not exist or is no file or is not readable.");
      }

      try {
        LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", record.getSchemaId(), record.getSchemaVersion(), applicableValidator);
        if (applicableValidator.validateMetadataDocument(schemaDocumentPath.toFile(), document.getInputStream())) {
          LOG.trace("Metadata document validation succeeded. Returning HTTP NOT_CONTENT.");
          return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
          LOG.warn("Metadata document validation failed. Returning HTTP UNPROCESSABLE_ENTITY.");
          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(applicableValidator.getErrorMessage());
        }
      } catch (IOException ex) {
        LOG.error("Failed to read metadata document from input stream. Returning HTTP UNPROCESSABLE_ENTITY.", ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to metadata document from input stream.");
      }
    }
  }

  @Override
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(List<String> schemaIds, List<String> mimeTypes, Instant updateFrom, Instant updateUntil, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb) {
    LOG.trace("Performing getRecords({}, {}, {}, {}).", schemaIds, mimeTypes, updateFrom, updateUntil);
    Specification<MetadataSchemaRecord> spec = SchemaIdSpecification.toSpecification(schemaIds);
    spec = spec.and(MimeTypeSpecification.toSpecification(mimeTypes)).or(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    //if security is enabled, include principal in query

    LOG.debug("Performing query for records.");
    Page<MetadataSchemaRecord> records = metadataSchemaDao.findAll(spec, pgbl);

    LOG.trace("Cleaning up schemaDocumentUri of query result.");
    List<MetadataSchemaRecord> recordList = records.getContent();

    recordList.forEach((record) -> {
      fixSchemaDocumentUri(record);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(records.getContent());
  }

  @Override
  public ResponseEntity updateRecord(@PathVariable("id") final String schemaId, @RequestBody final MetadataSchemaRecord record, final WebRequest request, final HttpServletResponse response) {
    MetadataSchemaRecord updatedRecord;
    LOG.trace("Performing updateMetadataSchemaRecord({}, {}).", schemaId, record);
    if ((record == null) || (record.getSchemaId() == null)) {
      LOG.error("No metadata schema record provided. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No metadata schema record provided.");
    }

    LOG.trace("Obtaining most recent metadata schema record with id {}.", schemaId);
    MetadataSchemaRecord existingRecord = getRecordByIdAndVersion(schemaId);
    //if authorization enabled, check principal -> return HTTP UNAUTHORIZED or FORBIDDEN if not matching

    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(request, existingRecord);

    LOG.trace("Merging changes into existing schema record.");
    mergeRecords(existingRecord, record);

    //schema version is not changed for metadata updates
    LOG.trace("Persisting schema record.");
    updatedRecord = metadataSchemaDao.save(existingRecord);

    //audit information not captured here as version not changes via PUT
    LOG.trace("Schema record successfully persisted. Updating document URI and returning result.");
    fixSchemaDocumentUri(updatedRecord);

    String etag = updatedRecord.getEtag();
    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(updatedRecord.getSchemaId(), updatedRecord.getSchemaVersion(), null, null)).toUri();

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updatedRecord);
  }

  @Override
  public ResponseEntity deleteRecord(String schemaId, WebRequest request, HttpServletResponse hsr) {
    LOG.trace("Performing deleteRecord({}).", schemaId);

    try {
      LOG.trace("Obtaining most recent schema record with id {}.", schemaId);
      MetadataSchemaRecord existingRecord = getRecordByIdAndVersion(schemaId);
      LOG.trace("Checking provided ETag.");
      ControllerUtils.checkEtag(request, existingRecord);

      LOG.trace("Removing audit information of schema with id {}.", schemaId);
      auditService.deleteAuditInformation(AuthenticationHelper.getPrincipal(), existingRecord);

      LOG.trace("Removing schema from database.");
      metadataSchemaDao.delete(existingRecord);

      //Schema files are never removed, as creating a new schema record with the same identifier
      //will re-activate the record and its version history. Thus, it will be possible again
      //to access previous version of the record. Schema access for these "re-activated" records
      //would fail for older version, if the schema file is deleted.      
    } catch (ResourceNotFoundException ex) {
      //exception is hidden for DELETE
      LOG.debug("No metadata schema with id {} found. Skipping deletion.", schemaId);
    }

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private MetadataSchemaRecord mergeRecords(MetadataSchemaRecord managed, MetadataSchemaRecord provided) {
    //update mime type the schema can be applied to
    if (!Objects.isNull(provided.getMimeType())) {
      LOG.trace("Updating record mime type from {} to {}.", managed.getMimeType(), provided.getMimeType());
      managed.setMimeType(provided.getMimeType());
    }

    //update acl
    if (provided.getAcl() != null) {
      LOG.trace("Updating record acl from {} to {}.", managed.getAcl(), provided.getAcl());
      managed.setAcl(provided.getAcl());
    }
    LOG.trace("Setting lastUpdate to now().");
    managed.setLastUpdate(Instant.now());
    return managed;
  }

  private MetadataSchemaRecord getRecordByIdAndVersion(String schemaId) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(schemaId, null);
  }

  private MetadataSchemaRecord getRecordByIdAndVersion(String schemaId, Long version) throws ResourceNotFoundException {
    Long recordVersion = version;
    if (recordVersion == null) {
      LOG.trace("No record version provided. Reading schema record from database.");
      Optional<MetadataSchemaRecord> record = metadataSchemaDao.findById(schemaId);
      if (!record.isPresent()) {
        LOG.error("No metadata schema found for schemaId {}. Returning HTTP 404.", schemaId);
        throw new ResourceNotFoundException("No metadata schema found for schemaId " + schemaId + ".");
      }
      return record.get();
    }

    Optional<MetadataSchemaRecord> optRecord = auditService.getResourceByVersion(schemaId, recordVersion);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    if (!optRecord.isPresent()) {
      LOG.error("No metadata schema found for schemaId {} and version {}. Returning HTTP 404.", schemaId, version);
      throw new ResourceNotFoundException("No metadata schema found for schemaId " + schemaId + " and version " + version + ".");
    }

    return optRecord.get();
  }

  private void fixSchemaDocumentUri(MetadataSchemaRecord record) {
    record.setSchemaDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri().toString());
  }

}
