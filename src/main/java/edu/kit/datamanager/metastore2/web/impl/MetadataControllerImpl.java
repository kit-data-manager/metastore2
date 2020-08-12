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

import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.dao.IMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.spec.LastUpdateSpecification;
import edu.kit.datamanager.metastore2.dao.spec.RelatedIdSpecification;
import edu.kit.datamanager.metastore2.dao.spec.SchemaIdSpecification;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import edu.kit.datamanager.metastore2.web.IMetadataController;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Example;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/metadata")
public class MetadataControllerImpl implements IMetadataController {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataControllerImpl.class);

  @Autowired
  private ApplicationProperties metastoreProperties;
  @Autowired
  private IMetadataRecordDao metadataRecordDao;
  @Autowired
  private IAuditService<MetadataRecord> auditService;

  @Override
  public ResponseEntity createRecord(
          @RequestPart(name = "record") final MetadataRecord record,
          @RequestPart(name = "document") final MultipartFile document,
          HttpServletRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder) throws URISyntaxException {

    LOG.trace("Performing createRecord({},...).", record);

    if (record.getRelatedResource() == null || record.getSchemaId() == null) {
      LOG.error("Mandatory attributes relatedResource and/or schemaId not found in record. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes relatedResource and/or schemaId not found in record.");
    }

    if (record.getId() != null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not expecting record id to be assigned by user.");
    }

    LOG.trace("Setting initial metadata version to {}.", 1l);
    record.setRecordVersion(1l);

    LOG.trace("Setting random UUID as record id.");
    record.setId(UUID.randomUUID().toString());
    LOG.debug("Test for existing metadata record for given schema and resource");
    MetadataRecord dummy = new MetadataRecord();
    dummy.setRelatedResource(record.getRelatedResource());
    dummy.setSchemaId(record.getSchemaId());
    Example<MetadataRecord> example = Example.of(dummy);
    Optional<MetadataRecord> findOne = metadataRecordDao.findOne(example);
    if (findOne.isPresent()) {
      LOG.error("Conflict with existing metadata record!");
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Metadata record already exists! Please update existing record instead!");
    }
    //check for re-use of old id
    long versionById = auditService.getCurrentVersion(record.getId());
    long reattempts = 0;
    while (versionById != 0) {
      reattempts++;
      LOG.warn("UUID collision detected. Assigning another random identifier.");
      record.setId(UUID.randomUUID().toString());

      if (reattempts == 10) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Unable to assign unique, random UUID. Please try again later.");
      }
      versionById = auditService.getCurrentVersion(record.getId());
    }

    LOG.trace("Trying to validate metadata document using one of {} schema registry/registries.", metastoreProperties.getSchemaRegistries().length);

    try {
      byte[] data = document.getBytes();
      if (metastoreProperties.getSchemaRegistries().length == 0) {
        LOG.error("Failed to validate metadata document at schema registry. No schema registry available!");
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("Failed to validate metadata document at schema registry. No schema registry available!");
      }

      ResponseEntity<String> responseEntity = validateMetadataDocument(record, data);

      if (responseEntity != null) {
        return responseEntity;
      }

      LOG.trace("Setting createdAt and lastUpdate to now().");
      record.setCreatedAt(Instant.now());
      record.setLastUpdate(record.getCreatedAt());

      try {
        LOG.trace("Creating metadata document hash and updating record.");
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(data, 0, data.length);
        record.setDocumentHash("sha1:" + Hex.encodeHexString(md.digest()));
      } catch (NoSuchAlgorithmException ex) {
        LOG.error("Failed to initialize SHA1 MessageDigest.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initialize SHA1 MessageDigest.");
      }

      //persist document
      LOG.trace("Writing user-provided metadata file to repository.");
      URL metadataFolderUrl = metastoreProperties.getMetadataFolder();
      try {
        Path metadataDir = Paths.get(Paths.get(metadataFolderUrl.toURI()).toAbsolutePath().toString(), record.getId());
        if (!Files.exists(metadataDir)) {
          LOG.trace("Creating metadata directory at {}.", metadataDir);
          Files.createDirectories(metadataDir);
        } else {
          if (!Files.isDirectory(metadataDir)) {
            LOG.error("Metadata directory {} exists but is no folder. Aborting operation.", metadataDir);
            throw new CustomInternalServerError("Illegal metadata registry state detected.");
          }
        }

        Path p = Paths.get(metadataDir.toAbsolutePath().toString(), getUniqueRecordHash(record));
        if (Files.exists(p)) {
          LOG.error("Metadata document conflict. A file at path {} already exists.", p);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal filename conflict.");
        }

        LOG.trace("Persisting valid metadata document at {}.", p);
        Files.write(p, data);
        LOG.trace("Metadata document successfully persisted. Updating record.");
        record.setMetadataDocumentUri(p.toUri().toString());
        LOG.trace("Metadata record completed.");
      } catch (URISyntaxException ex) {
        LOG.error("Failed to determine schema storage location.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal misconfiguration of schema location.");
      } catch (IOException ex) {
        LOG.error("Failed to write metadata to metadata folder. Returning HTTP INSUFFICIENT_STORAGE.", ex);
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("Failed to write medata to metadata folder.");
      }
    } catch (IOException ex) {
      LOG.error("Failed to read metadata from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read metadata from input stream.");
    }

    String callerPrincipal = (String) AuthenticationHelper.getAuthentication().getPrincipal();
    LOG.trace("Checking resource for caller acl entry.");
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

    LOG.trace("Persisting metadata record.");
    MetadataRecord result = metadataRecordDao.save(record);

    LOG.trace("Capturing metadata schema audit information.");
    auditService.captureAuditInformation(result, AuthenticationHelper.getPrincipal());

    LOG.trace("Get ETag of MetadataRecord.");
    String etag = result.getEtag();

    LOG.trace("Schema record successfully persisted. Updating document URI.");
    fixMetadataDocumentUri(result);

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(record.getId(), record.getRecordVersion(), null, null)).toUri();

    LOG.trace("Schema record successfully persisted. Returning result.");
    return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(result);
  }

  @Override
  public ResponseEntity<MetadataRecord> getRecordById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getRecordById({}, {}).", id, version);

    LOG.trace("Obtaining metadata record with id {} and version {}.", id, version);
    MetadataRecord record = getRecordByIdAndVersion(id, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    LOG.trace("Get ETag of MetadataRecord.");
    String etag = record.getEtag();

    fixMetadataDocumentUri(record);
    LOG.trace("Document URI successfully updated. Returning result.");
    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(record);
  }

  @Override
  public ResponseEntity getMetadataDocumentById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getMetadataDocumentById({}, {}).", id, version);

    LOG.trace("Obtaining metadata record with id {} and version {}.", id, version);
    MetadataRecord record = getRecordByIdAndVersion(id, version);

    URI metadataDocumentUri = URI.create(record.getMetadataDocumentUri());

    Path metadataDocumentPath = Paths.get(metadataDocumentUri);
    if (!Files.exists(metadataDocumentPath) || !Files.isRegularFile(metadataDocumentPath) || !Files.isReadable(metadataDocumentPath)) {
      LOG.trace("Metadata document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", metadataDocumentPath);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Metadata document on server either does not exist or is no file or is not readable.");
    }

    return ResponseEntity.
            ok().
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadataDocumentPath.toFile().length())).
            body(new FileSystemResource(metadataDocumentPath.toFile()));
  }

  @Override
  public ResponseEntity<List<MetadataRecord>> getRecords(
          @RequestParam(value = "resourceId", required = false) List<String> relatedIds,
          @RequestParam(value = "schemaId", required = false) List<String> schemaIds,
          @RequestParam(name = "from", required = false) Instant updateFrom,
          @RequestParam(name = "until", required = false) Instant updateUntil,
          Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb
  ) {
    LOG.trace("Performing getRecords({}, {}, {}, {}).", relatedIds, schemaIds, updateFrom, updateUntil);
    Specification<MetadataRecord> spec = SchemaIdSpecification.toSpecification(schemaIds);
    if (relatedIds != null) {
      spec = spec.and(RelatedIdSpecification.toSpecification(relatedIds));
    }
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }

    //if security is enabled, include principal in query
    LOG.debug("Performing query for records.");
    Page<MetadataRecord> records = metadataRecordDao.findAll(spec, pgbl);

    LOG.trace("Cleaning up schemaDocumentUri of query result.");
    List<MetadataRecord> recordList = records.getContent();

    recordList.forEach((record) -> {
      fixMetadataDocumentUri(record);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(records.getContent());
  }

  @Override
  public ResponseEntity updateRecord(
          @PathVariable("id") String id,
          @RequestPart(name = "record", required = false) MetadataRecord record,
          @RequestPart(name = "document", required = false)
          final MultipartFile document,
          WebRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder
  ) {
    LOG.trace("Performing updateRecord({}, {}, {}).", id, record, "#document");

    if (record == null && document == null) {
      LOG.error("No metadata schema record provided. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Neither metadata record nor metadata document provided.");
    }

    LOG.trace("Obtaining most recent metadata record with id {}.", id);
    MetadataRecord existingRecord = getRecordByIdAndVersion(id);
    //if authorization enabled, check principal -> return HTTP UNAUTHORIZED or FORBIDDEN if not matching

    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(request, existingRecord);
    mergeRecords(existingRecord, record);

    LOG.trace("Updating record version.");
    existingRecord.setRecordVersion(existingRecord.getRecordVersion() + 1);

    if (document != null) {
      LOG.trace("Updating metadata document.");
      try {
        byte[] data = document.getBytes();
        if (metastoreProperties.getSchemaRegistries().length == 0) {
          LOG.error("Failed to validate metadata document at schema registry. No schema registry available!");
          return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body("Failed to validate metadata document at schema registry. No schema registry available!");
        }
        ResponseEntity<String> responseEntity = validateMetadataDocument(existingRecord, data);

        if (responseEntity != null) {
          return responseEntity;
        }

        boolean writeMetadataFile = true;
        String existingDocumentHash = existingRecord.getDocumentHash();
        try {
          LOG.trace("Creating metadata document hash and updating record.");
          MessageDigest md = MessageDigest.getInstance("SHA1");
          md.update(data, 0, data.length);

          existingRecord.setDocumentHash("sha1:" + Hex.encodeHexString(md.digest()));

          if (Objects.equals(existingRecord.getDocumentHash(), existingDocumentHash)) {
            LOG.trace("Metadata file hashes are equal. Skip writing new metadata file.");
            writeMetadataFile = false;
          }
        } catch (NoSuchAlgorithmException ex) {
          LOG.error("Failed to initialize SHA1 MessageDigest.", ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initialize SHA1 MessageDigest.");
        }

        if (writeMetadataFile) {
          //persist document
          LOG.trace("Writing user-provided metadata file to repository.");
          URL metadataFolderUrl = metastoreProperties.getMetadataFolder();
          try {
            Path metadataDir = Paths.get(Paths.get(metadataFolderUrl.toURI()).toAbsolutePath().toString(), existingRecord.getId());
            if (!Files.exists(metadataDir)) {
              LOG.trace("Creating metadata directory at {}.", metadataDir);
              Files.createDirectories(metadataDir);
            } else {
              if (!Files.isDirectory(metadataDir)) {
                LOG.error("Metadata directory {} exists but is no folder. Aborting operation.", metadataDir);
                throw new CustomInternalServerError("Illegal metadata registry state detected.");
              }
            }

            Path p = Paths.get(metadataDir.toAbsolutePath().toString(), getUniqueRecordHash(existingRecord));
            if (Files.exists(p)) {
              LOG.error("Metadata document conflict. A file at path {} already exists.", p);
              return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal filename conflict.");
            }

            LOG.trace("Persisting valid metadata document at {}.", p);
            Files.write(p, data);
            LOG.trace("Metadata document successfully persisted. Updating record.");
            existingRecord.setMetadataDocumentUri(p.toUri().toString());

            LOG.trace("Metadata record completed.");
          } catch (URISyntaxException ex) {
            LOG.error("Failed to determine metadata storage location.", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal misconfiguration of metadata location.");
          }
        }
      } catch (IOException ex) {
        LOG.error("Failed to read medata from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read medata from input stream.");
      }
    }

    LOG.trace("Persisting metadata record.");
    record = metadataRecordDao.save(existingRecord);
    LOG.trace("Capturing metadata schema audit information.");
    auditService.captureAuditInformation(record, AuthenticationHelper.getPrincipal());

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    fixMetadataDocumentUri(record);
    String etag = record.getEtag();

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(record.getId(), record.getRecordVersion(), null, null)).toUri();

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(record);
  }

  @Override
  public ResponseEntity deleteRecord(
          @PathVariable(value = "id") String id,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing deleteRecord({}).", id);

    try {
      LOG.trace("Obtaining most recent schema record with id {}.", id);
      MetadataRecord existingRecord = getRecordByIdAndVersion(id);
      LOG.trace("Checking provided ETag.");
      ControllerUtils.checkEtag(wr, existingRecord);

      LOG.trace("Removing audit information of schema with id {}.", id);
      auditService.deleteAuditInformation(AuthenticationHelper.getPrincipal(), existingRecord);

      LOG.trace("Removing schema from database.");
      metadataRecordDao.delete(existingRecord);
      LOG.trace("Deleting all metadata documents from disk.");

      URL metadataFolderUrl = metastoreProperties.getMetadataFolder();
      try {
        Path p = Paths.get(Paths.get(metadataFolderUrl.toURI()).toAbsolutePath().toString(), existingRecord.getId());
        LOG.trace("Deleting schema file(s) from path.", p);
        FileUtils.deleteDirectory(p.toFile());

        LOG.trace("All metadata documents for record with id {} deleted.", id);
      } catch (URISyntaxException | IOException ex) {
        LOG.error("Failed to obtain schema document for schemaId {}. Please remove schema files manually. Skipping deletion.");
      }

    } catch (ResourceNotFoundException ex) {
      //exception is hidden for DELETE
      LOG.debug("No metadata schema with id {} found. Skipping deletion.", id);
    }

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  private MetadataRecord getRecordByIdAndVersion(String recordId) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(recordId, null);
  }

  private MetadataRecord getRecordByIdAndVersion(String recordId, Long version) throws ResourceNotFoundException {
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    Long recordVersion = version;
    if (recordVersion == null) {
      LOG.trace("No record version provided. Reading schema record from database.");
      Optional<MetadataRecord> record = metadataRecordDao.findById(recordId);
      if (!record.isPresent()) {
        LOG.error("No metadata record found for id {}. Returning HTTP 404.", recordId);
        throw new ResourceNotFoundException("No metadata record found for id " + recordId + ".");
      }
      return record.get();
    }

    Optional<MetadataRecord> optRecord = auditService.getResourceByVersion(recordId, recordVersion);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    if (!optRecord.isPresent()) {
      LOG.error("No metadata record found for id {} and version {}. Returning HTTP 404.", recordId, version);
      throw new ResourceNotFoundException("No metadata record found for id " + recordId + " and version " + version + ".");
    }

    return optRecord.get();
  }

  public MetadataRecord mergeRecords(MetadataRecord managed, MetadataRecord provided) {
    if (provided != null) {
      if (!Objects.isNull(provided.getPid())) {
        LOG.trace("Updating pid from {} to {}.", managed.getPid(), provided.getPid());
        managed.setPid(provided.getPid());
      }

      if (!Objects.isNull(provided.getRelatedResource())) {
        LOG.trace("Updating related resource from {} to {}.", managed.getRelatedResource(), provided.getRelatedResource());
        managed.setRelatedResource(provided.getRelatedResource());
      }

      if (!Objects.isNull(provided.getSchemaId())) {
        LOG.trace("Updating schemaId from {} to {}.", managed.getSchemaId(), provided.getSchemaId());
        managed.setSchemaId(provided.getSchemaId());
      }

      //update acl
      if (provided.getAcl() != null) {
        LOG.trace("Updating record acl from {} to {}.", managed.getAcl(), provided.getAcl());
        managed.setAcl(provided.getAcl());
      }
    }
    LOG.trace("Setting lastUpdate to now().");
    managed.setLastUpdate(Instant.now());
    return managed;
  }

  private void fixMetadataDocumentUri(MetadataRecord record) {
    record.setMetadataDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getMetadataDocumentById(record.getId(), record.getRecordVersion(), null, null)).toUri().toString());
  }

  private String getUniqueRecordHash(MetadataRecord record) {
    String hash = null;
    try {
      LOG.trace("Creating metadata record hash.");
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update(record.getId().getBytes(), 0, record.getId().length());
      md.update(record.getRelatedResource().getBytes(), 0, record.getRelatedResource().length());
      md.update(record.getSchemaId().getBytes(), 0, record.getSchemaId().length());
      md.update(Long.toString(record.getRecordVersion()).getBytes(), 0, Long.toString(record.getRecordVersion()).length());
      hash = Hex.encodeHexString(md.digest());
    } catch (NoSuchAlgorithmException ex) {
      LOG.error("Failed to initialize SHA1 MessageDigest.", ex);
      throw new CustomInternalServerError("Failed to create metadata record hash.");
    }
    return hash;
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param record metadata of the document.
   * @param document document
   * @return ResponseEntity in case of an error.
   * @throws IOException Error reading document.
   */
  private ResponseEntity<String> validateMetadataDocument(MetadataRecord record, byte[] document) {
    ResponseEntity<String> responseEntity = null;
    boolean validationSuccess = false;
    StringBuilder errorMessage = new StringBuilder();
    for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
      URI schemaRegistryUri = URI.create(schemaRegistry);
      UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), "schemas", record.getSchemaId(), "validate");

      URI finalUri = builder.build().toUri();

      try {
        HttpStatus status = SimpleServiceClient.create(finalUri.toString()).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).withFormParam("document", new ByteArrayInputStream(document)).postForm(MediaType.MULTIPART_FORM_DATA);

        if (Objects.equals(HttpStatus.NO_CONTENT, status)) {
          LOG.trace("Successfully validated document against schema {} in registry {}.", record.getSchemaId(), schemaRegistry);
          validationSuccess = true;
          break;
        }
      } catch (HttpClientErrorException ce) {
        //not valid 
        String message = new String("Failed to validate metadata document against schema " + record.getSchemaId() + " at '" + schemaRegistry + "' with status " + ce.getStatusCode() + ".");
        LOG.error(message, ce);
        errorMessage.append(message).append("\n");
        responseEntity = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorMessage.toString());
      } catch (IOException | RestClientException ex) {
        String message = new String("Failed to access schema registry at '" + schemaRegistry + "'. Proceeding with next registry.");
        LOG.error(message, ex);
        errorMessage.append(message).append("\n");
        responseEntity = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorMessage.toString());
      }
    }
    if (!validationSuccess) {
      return responseEntity;
    }

    return null;
  }
}
