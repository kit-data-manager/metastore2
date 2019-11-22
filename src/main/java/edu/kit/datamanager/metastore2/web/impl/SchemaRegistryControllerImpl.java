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

import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.dao.spec.LastUpdateSpecification;
import edu.kit.datamanager.metastore2.dao.spec.MimeTypeSpecification;
import edu.kit.datamanager.metastore2.dao.spec.SchemaIdSpecification;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.util.JPAQueryHelper;
import edu.kit.datamanager.metastore2.util.SchemaUtils;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.annotations.Api;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
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
@Api(value = "Metadata Schema Registry")
public class SchemaRegistryControllerImpl implements ISchemaRegistryController{

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
  public ResponseEntity createRecord(MetadataSchemaRecord record, MultipartFile document, WebRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder){
    LOG.trace("Performing createRecord({}, {}).", record, "#document");
    if(record == null || document == null){
      LOG.error("No metadata schema record and/or schema document provided. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No metadata schema record and/or schema document provided.");
    }
    if(record.getSchemaId() == null || record.getMimeType() == null){
      LOG.error("Mandatory attributes schemaId and/or mimeType not found in record. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes schemaId and/or mimeType not found in record.");
    }

    long version = auditService.getCurrentVersion(record.getSchemaId());

    if(version > 0){
      LOG.trace("Schema with id {} found. Setting new schema version to {}.", record.getSchemaId(), (version + 1));
      record.setSchemaVersion(version + 1);
    } else{
      LOG.trace("Setting initial schema version to {}.", 1);
      record.setSchemaVersion(1l);
    }

    try{
      byte[] schemaBytes = document.getBytes();

      IValidator applicableValidator = null;
      if(record.getType() == null){
        record.setType(SchemaUtils.guessType(schemaBytes));
        if(record.getType() == null){
          LOG.error("Unable to detect schema type automatically. Returning HTTP UNPROCESSABLE_ENTITY.");
          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Unable to detect schema type automatically.");
        } else{
          LOG.debug("Automatically detected schema type {}.", record.getType());
        }
      }

      for(IValidator validator : validators){
        if(validator.supportsSchemaType(record.getType())){
          applicableValidator = validator;
          break;
        }
      }

      if(applicableValidator == null){
        LOG.error("No validator found for schema type " + record.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("No validator found for schema type " + record.getType() + ".");
      } else{
        if(!applicableValidator.isSchemaValid(new ByteArrayInputStream(schemaBytes))){
          LOG.error("Failed to validate provided schema document. Returning HTTP UNPROCESSABLE_ENTITY.");
          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to validate provided schema document.");
        }

        LOG.trace("Setting createdAt and lastUpdate to now().");
        record.setCreatedAt(Instant.now());
        record.setLastUpdate(record.getCreatedAt());

        URL schemaFolderUrl = metastoreProperties.getSchemaFolder();
        try{
          Path schemaDir = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId());
          if(!Files.exists(schemaDir)){
            LOG.trace("Creating schema directory at {}.", schemaDir);
            Files.createDirectories(schemaDir);
          } else{
            if(!Files.isDirectory(schemaDir)){
              LOG.error("Schema directory {} exists but is no folder. Aborting operation.", schemaDir);
              throw new CustomInternalServerError("Illegal schema registry state detected.");
            }
          }

          Path p = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId(), record.getSchemaId() + "_" + record.getSchemaVersion() + ((MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType())) ? ".xsd" : ".schema"));
          if(Files.exists(p)){
            LOG.error("Schema conflict. A schema file at path {} already exists.", p);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal schema filename conflict.");
          }
          LOG.trace("Persisting valid schema document at {}.", p);
          Files.write(p, schemaBytes);
          LOG.trace("Schema document successfully persisted. Updating record.");
          record.setSchemaDocumentUri(p.toUri().toString());
          LOG.trace("Schema record completed.");
        } catch(URISyntaxException ex){
          LOG.error("Failed to determine schema storage location.", ex);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal misconfiguration of schema location.");
        }
        LOG.trace("Persisting schema record.");
        record = metadataSchemaDao.save(record);

        LOG.trace("Capturing metadata schema audit information.");
        auditService.captureAuditInformation(record, AuthenticationHelper.getPrincipal());

        LOG.trace("Schema record successfully persisted. Updating document URI.");
        fixSchemaDocumentUri(record);
        LOG.trace("Schema record successfully persisted. Returning result.");
        return new ResponseEntity<>(record, HttpStatus.CREATED);
      }
    } catch(IOException ex){
      LOG.error("Failed to read schema data from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read schema data from input stream.");
    }
  }

  @Override
  public ResponseEntity getRecordById(String schemaId, Long version, WebRequest wr, HttpServletResponse hsr){
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
          String schemaId,
          Long version,
          WebRequest wr,
          HttpServletResponse hsr){
    LOG.trace("Performing getSchemaDocumentById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = getRecordByIdAndVersion(schemaId, version);
    URI schemaDocumentUri = URI.create(record.getSchemaDocumentUri());

    MediaType contentType = MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType()) ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
    Path schemaDocumentPath = Paths.get(schemaDocumentUri);
    if(!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)){
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
  public ResponseEntity validate(String schemaId, Long version, MultipartFile document, WebRequest wr, HttpServletResponse hsr){
    LOG.trace("Performing validate({}, {}, {}).", schemaId, version, "#document");

    if(document == null){
      LOG.error("Missing metadata document in body. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Metadata document is missing from request body.");
    }

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = getRecordByIdAndVersion(schemaId, version);
    IValidator applicableValidator = null;
    LOG.trace("Checking for applicable validator.");
    for(IValidator validator : validators){
      if(validator.supportsSchemaType(record.getType())){
        applicableValidator = validator;
        break;
      }
    }

    if(applicableValidator == null){
      LOG.error("No validator found for schema type " + record.getType() + ". Returning HTTP UNPROCESSABLE_ENTITY.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("No validator found for schema type " + record.getType() + ".");
    } else{
      LOG.trace("Validator found. Checking local schema file.");
      Path schemaDocumentPath = Paths.get(URI.create(record.getSchemaDocumentUri()));

      if(!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)){
        LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Schema document on server either does not exist or is no file or is not readable.");
      }

      try{
        LOG.trace("Performing validation of metadata document using schema {}, version {} and validator {}.", record.getSchemaId(), record.getSchemaVersion(), applicableValidator);
        if(applicableValidator.validateMetadataDocument(schemaDocumentPath.toFile(), document.getInputStream())){
          LOG.trace("Metadata document validation succeeded. Returning HTTP NOT_CONTENT.");
          return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else{
          LOG.warn("Metadata document validation failed. Returning HTTP HTTP UNPROCESSABLE_ENTITY.");
          return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Metadata document is not valid according to the addressed schema.");
        }
      } catch(IOException ex){
        LOG.error("Failed to read metadata document from input stream. Returning HTTP UNPROCESSABLE_ENTITY.", ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to metadata document from input stream.");
      }
    }
  }

  @Override
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(List<String> schemaIds, List<String> mimeTypes, Instant updateFrom, Instant updateUntil, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb){
    LOG.trace("Performing getRecords({}, {}, {}, {}).", schemaIds, mimeTypes, updateFrom, updateUntil);
    Specification<MetadataSchemaRecord> spec = SchemaIdSpecification.toSpecification(schemaIds).and(MimeTypeSpecification.toSpecification(mimeTypes)).or(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
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
  public ResponseEntity updateRecord(String schemaId, MetadataSchemaRecord record, WebRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder){
    LOG.trace("Performing updateRecord({}, {}, {}).", schemaId, record, "#document");

    if(record == null){
      LOG.error("No metadata schema record provided. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No metadata schema record provided.");
    }

    LOG.trace("Obtaining most recent schema record with id {}.", schemaId);
    MetadataSchemaRecord existingRecord = getRecordByIdAndVersion(schemaId);
    //if authorization enabled, check principal -> return HTTP UNAUTHORIZED or FORBIDDEN if not matching

    LOG.trace("Checking provided ETag.");
    ControllerUtils.checkEtag(request, existingRecord);

    //update mime type the schema can be applied to
    if(!Objects.isNull(record.getMimeType())){
      LOG.trace("Updating record mime type from {} to {}.", existingRecord.getMimeType(), record.getMimeType());
      existingRecord.setMimeType(record.getMimeType());
    }

    //update acl
    if(record.getAcl() != null){
      LOG.trace("Updating record acl from {} to {}.", existingRecord.getAcl(), record.getAcl());
      existingRecord.setAcl(record.getAcl());
    }

    LOG.trace("Setting lastUpdate to now().");
    existingRecord.setLastUpdate(Instant.now());

    LOG.trace("Persisting schema record.");
    record = metadataSchemaDao.save(existingRecord);

    //audit information not captured here as version not changes via PUT
    LOG.trace("Schema record successfully persisted. Updating document URI.");
    fixSchemaDocumentUri(record);
    LOG.trace("Schema record successfully persisted. Returning result.");
    return new ResponseEntity<>(record, HttpStatus.OK);
  }

  @Override
  public ResponseEntity deleteRecord(String schemaId, WebRequest request, HttpServletResponse hsr){
    LOG.trace("Performing deleteRecord({}).", schemaId);

    try{
      LOG.trace("Obtaining most recent schema record with id {}.", schemaId);
      MetadataSchemaRecord existingRecord = getRecordByIdAndVersion(schemaId);
      LOG.trace("Checking provided ETag.");
      ControllerUtils.checkEtag(request, existingRecord);

//      URL schemaFolderUrl = metastoreProperties.getSchemaFolder();
//      try{
//        Path p = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), schemaId);
//        LOG.trace("Deleting schema file(s) from path.", p);
//
//        try(Stream<Path> walk = Files.walk(p)){
//          walk.sorted(Comparator.reverseOrder())
//                  .map(Path::toFile)
//                  .forEach(File::delete);
//        }
//
//        LOG.trace("All schema files for schema with id {} deleted.", schemaId);
//      } catch(URISyntaxException | IOException ex){
//        LOG.error("Failed to obtain schema document for schemaId {}. Please remove schema files manually. Skipping deletion.");
//      }
      LOG.trace("Removing audit information of schema with id {}.", schemaId);
      auditService.deleteAuditInformation(AuthenticationHelper.getPrincipal(), existingRecord);

      LOG.trace("Removing schema from database.");
      metadataSchemaDao.delete(existingRecord);
    } catch(ResourceNotFoundException ex){
      //exception is hidden for DELETE
      LOG.debug("No metadata schema with id {} found. Skipping deletion.", schemaId);
    }

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private MetadataSchemaRecord getRecordByIdAndVersion(String schemaId) throws ResourceNotFoundException{
    return getRecordByIdAndVersion(schemaId, null);
  }

  private MetadataSchemaRecord getRecordByIdAndVersion(String schemaId, Long version) throws ResourceNotFoundException{
    Long recordVersion = version;
    if(recordVersion == null){
      LOG.trace("No record version provided. Reading schema record from database.");
      Optional<MetadataSchemaRecord> record = metadataSchemaDao.findById(schemaId);
      if(record.isEmpty()){
        LOG.error("No metadata schema found for schemaId {}. Returning HTTP 404.", schemaId);
        throw new ResourceNotFoundException("No metadata schema found for schemaId " + schemaId + ".");
      }
      return record.get();
    }

    Optional<MetadataSchemaRecord> optRecord = auditService.getResourceByVersion(schemaId, recordVersion);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    if(optRecord.isEmpty()){
      LOG.error("No metadata schema found for schemaId {} and version {}. Returning HTTP 404.", schemaId, version);
      throw new ResourceNotFoundException("No metadata schema found for schemaId " + schemaId + " and version " + version + ".");
    }

    return optRecord.get();
  }

  private void fixSchemaDocumentUri(MetadataSchemaRecord record){
    record.setSchemaDocumentUri(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ISchemaRegistryController.class).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri().toString());
  }

}
