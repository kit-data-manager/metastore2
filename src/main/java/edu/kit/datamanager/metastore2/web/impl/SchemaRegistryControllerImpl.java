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
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.util.JPAQueryHelper;
import edu.kit.datamanager.metastore2.util.SchemaUtils;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import io.swagger.annotations.Api;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
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
@Api(value = "Metadata Schema Registry")
public class SchemaRegistryControllerImpl implements ISchemaRegistryController{

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImpl.class);

  @Autowired
  private ApplicationProperties metastoreProperties;
  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private IValidator[] validators;

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

    int version = (record.getSchemaVersion() != null) ? record.getSchemaVersion() : 1;
    LOG.trace("Searching for existing schema record with id {} and version {}.", record.getSchemaId(), version);
    Optional<MetadataSchemaRecord> optRecord = metadataSchemaDao.findBySchemaIdAndSchemaVersion(record.getSchemaId(), version);

    if(optRecord.isPresent()){
      LOG.error("Existing schema record with id {} and version {} found. Returning HTTP CONFLICT.", record.getSchemaId(), version);
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Existing schema record with id " + record.getSchemaId() + " and version " + version + " found.");
    }
    try{
      byte[] schemaBytes = document.getBytes();

      IValidator applicableValidator = null;
      if(record.getType() == null){
        record.setType(SchemaUtils.guessType(schemaBytes));
        if(record.getType() == null){
          LOG.error("Unable to detect schema type automatically. Returning HTTP BAD_REQUEST.");
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to detect schema type automatically.");
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
          LOG.error("Failed to validate provided schema document. Returning HTTP BAD_REQUEST.");
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to validate provided schema document.");
        }
        LOG.trace("Setting initial schema version to 1.");
        record.setSchemaVersion(1);
        LOG.trace("Setting createdAt and lastUpdate to now().");
        record.setCreatedAt(Instant.now());
        record.setLastUpdate(record.getCreatedAt());

        URL schemaFolderUrl = metastoreProperties.getSchemaFolder();
        try{
          Path p = Paths.get(Paths.get(schemaFolderUrl.toURI()).toAbsolutePath().toString(), record.getSchemaId() + "_" + record.getSchemaVersion() + ((MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType())) ? ".xsd" : ".schema"));
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
        LOG.trace("Schema record successfully persisted. Updating document URI.");
        record.setSchemaDocumentUri(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ISchemaRegistryController.class).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), request, response)).toUri().toString());
        LOG.trace("Schema record successfully persisted. Returning result.");
        return new ResponseEntity<>(record, HttpStatus.CREATED);
      }
    } catch(IOException ex){
      LOG.error("Failed to read schema data from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read schema data from input stream.");
    }
  }

  @Override
  public ResponseEntity getRecordById(String schemaId, Integer version, WebRequest wr, HttpServletResponse hsr){
    LOG.trace("Performing getRecordById({}, {}).", schemaId, version);
    Optional<MetadataSchemaRecord> optRecord = new JPAQueryHelper(em).getSchemaRecordBySchemaIdAndVersion(schemaId, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    if(optRecord.isEmpty()){
      LOG.error("No metadata schema found for schemaId {} and version {}. Returning HTTP 404.", schemaId, version);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No metadata schema found for schemaId " + schemaId + " and version " + version + ".");
    }

    LOG.trace("Schema record successfully obtained. Updating document URI.");
    MetadataSchemaRecord record = optRecord.get();
    record.setSchemaDocumentUri(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(ISchemaRegistryController.class).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), wr, hsr)).toUri().toString());
    LOG.trace("Document URI successfully updated. Returning result.");
    return ResponseEntity.ok(record);
  }

  @Override
  public ResponseEntity getSchemaDocumentById(
          String schemaId,
          Integer version,
          WebRequest wr,
          HttpServletResponse hsr){
    LOG.trace("Performing getSchemaDocumentById({}, {}).", schemaId, version);
    Optional<MetadataSchemaRecord> optRecord = new JPAQueryHelper(em).getSchemaRecordBySchemaIdAndVersion(schemaId, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    if(optRecord.isEmpty()){
      LOG.error("No metadata schema found for schemaId {} and version {}. Returning HTTP 404.", schemaId, version);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No metadata schema found for schemaId " + schemaId + " and version " + version + ".");
    }

    LOG.trace("Schema record successfully obtained. Updating document URI.");
    MetadataSchemaRecord record = optRecord.get();
    URI schemaDocumentUri = URI.create(record.getSchemaDocumentUri());

    MediaType contentType = MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType()) ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
    Path schemaDocumentPath = Paths.get(schemaDocumentUri);
    if(!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)){
      LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Schema document on server either does not exist or is no file or is not readable.");
    }

    return ResponseEntity.
            ok().
            contentType(contentType).
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(schemaDocumentPath.toFile().length())).
            body(new FileSystemResource(schemaDocumentPath.toFile()));
  }

  @Override
  public ResponseEntity validate(String schemaId, Integer version, MultipartFile document, WebRequest wr, HttpServletResponse hsr){
    LOG.trace("Performing validate({}, {}, {}).", schemaId, version, "#document");

    if(document == null){
      LOG.error("Missing metadata document in body. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Metadata document is missing from request body.");
    }
    Optional<MetadataSchemaRecord> optRecord = new JPAQueryHelper(em).getSchemaRecordBySchemaIdAndVersion(schemaId, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    if(optRecord.isEmpty()){
      LOG.error("No metadata schema found for schemaId {} and version {}. Returning HTTP 404.", schemaId, version);
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No metadata schema found for schemaId " + schemaId + " and version " + version + ".");
    }
    MetadataSchemaRecord record = optRecord.get();
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
      System.out.println("EX " + Files.exists(schemaDocumentPath));
      System.out.println("REG " + Files.isRegularFile(schemaDocumentPath));
      System.out.println("RED " + Files.isReadable(schemaDocumentPath));

      if(!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)){
        LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Schema document on server either does not exist or is no file or is not readable.");
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
        LOG.error("Failed to read metadata document from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to metadata document from input stream.");
      }
    }
  }

  @Override
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(List<String> schemaIds, List<String> mimeTypes, Instant updateFrom, Instant updateUntil, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb){
    LOG.trace("Performing getRecords({}, {}, {}, {}).", schemaIds, mimeTypes, updateFrom, updateUntil);

    //search for records with provided schemaIds AND mimeTypes including updateFrom and updateUntil for lastUpdate field with the biggest version number
    //if security is enabled, include principal in query
    //set all schemaDocumentUri elemets to path of getSchemaDocumentById(schemaId, version)
    //return HTTP 200 and result list
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity<MetadataSchemaRecord> updateRecord(String schemaId, MetadataSchemaRecord record, MultipartFile document, WebRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder){
    LOG.trace("Performing updateRecord({}, {}, {}).", schemaId, record, "#document");

    //search for record with provided schemaId in the most recent version -> if not found, return HTTP NOT_FOUND
    //if authorization enabled, check principal -> return HTTP UNAUTHORIZED or FORBIDDEN if not matching
    //ETag check -> HTTP CONFLICT if fails
    //check for valid fields to update (mimeType and acl, if document is provided also recordType) -> return HTTP BAD_REQUEST if forbidden fields are included
    //if document is provided:
    //compare current document with provided and check for difference -> no difference, proceed at end
    //check record type -> guess if not provided
    //try to read document according to type -> HTTP BAD_REQUEST if fails
    //persist document
    //end if document provided
    //apply changes to record (mimeType, acl and schema type if document was provided)
    //update version (if document was provided)
    //update schemaDocumentUri (if document was provided)
    //update lastModifiedAt to Instant.now()
    //Persist record
    //set schemaDocumentUri to path of getSchemaDocumentById(id, version)
    //return HTTP 200 and record
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity deleteRecord(String schemaId, WebRequest wr, HttpServletResponse hsr){
    LOG.trace("Performing deleteRecord({}).", schemaId);
    //search for all versions of record with provided schemaId
    //ETag check -> HTTP CONFLICT if fails
    //Delete documents and records
    //return HTTP 204
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
