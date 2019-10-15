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

import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import io.swagger.annotations.Api;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
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
public class SchemaRegistryControllerImpl implements ISchemaRegistryController {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImpl.class);

    @Autowired
    private IMetadataSchemaDao metadataSchemaDao;
    @Autowired
    private IValidator[] validators;

    @Override
    public ResponseEntity createRecord(MetadataSchemaRecord record, MultipartFile document, WebRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder) {
        if (record == null || document == null) {
            LOG.error("No metadata schema record and/or schema document provided. Returning HTTP BAD_REQUEST.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No metadata schema record and/or schema document provided.");
        }
        if (record.getSchemaId() == null || record.getMimeType() == null) {
            LOG.error("Mandatory attributes schemaId and/or mimeType not found in record. Returning HTTP BAD_REQUEST.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes schemaId and/or mimeType not found in record.");
        }

        int version = (record.getSchemaVersion() != null) ? record.getSchemaVersion() : 1;
        LOG.trace("Searching for existing schema record with id {} and version {}.", record.getSchemaId(), version);
        Optional<MetadataSchemaRecord> optRecord = metadataSchemaDao.findBySchemaIdAndSchemaVersion(record.getSchemaId(), version);

        if (optRecord.isPresent()) {
            LOG.error("Existing schema record with id {} and version {} found. Returning HTTP CONFLICT.", record.getSchemaId(), version);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Existing schema record with id " + record.getSchemaId() + " and version " + version + " found.");
        }

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
            try {
                byte[] schemaBytes = document.getBytes();
                if (!applicableValidator.isSchemaValid(new ByteArrayInputStream(schemaBytes))) {
                    LOG.error("Failed to validate provided schema document. Returning HTTP BAD_REQUEST.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to validate provided schema document.");
                }
            } catch (IOException ex) {
                LOG.error("Failed to read schema data from input stream. Returning HTTP UNPROCESSABLE_ENTITY.");
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Failed to read schema data from input stream.");
            }
        }

        //check record type -> not found: guess according to document
        //try to read document according to type in record -> if fails HTTP BAD_REQUEST
        //persist document
        //update schemaDocumentUri in record 
        //set createdAt and lastUpdate to Instant.now()
        //set schemaVersion to 1
        //persist record
        //return HTTP 201 with record
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<MetadataSchemaRecord> getRecordById(String id, Integer version, WebRequest wr, HttpServletResponse hsr) {
        //search for record with provided schemaId and version -> if not found HTTP NOT_FOUND
        //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
        //set schemaDocumentUri to path of getSchemaDocumentById(id, version)
        //return HTTP 200 with record
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity getSchemaDocumentById(String id, Integer version, WebRequest wr, HttpServletResponse hsr) {
        //search for record with provided schemaId and version -> if not found HTTP NOT_FOUND
        //if security is enabled, check permissions -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
        //obtain schemaDocumentUri from record -> if file not found return HTTP NOT_FOUND
        //return HTTP 200 with streamed file
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<MetadataSchemaRecord> validate(String id, Integer version, MultipartFile document, WebRequest wr, HttpServletResponse hsr) {
        //check document -> if not provided return HTTP BAD_REQUEST
        //search for record with provided schemaId and version -> if not found HTTP NOT_FOUND
        //if security is enabled, check permissions -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
        //find validator -> if not found, return HTTP UNPROCESSABLE ENTITY
        //try to validate -> if fails, return HTTP UNPROCESSABLE ENTITY
        //return HTTP NO_CONTENT
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<List<MetadataSchemaRecord>> getRecords(List<String> schemaIds, List<String> mimeTypes, Instant updateFrom, Instant updateUntil, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb) {
        //search for records with provided schemaIds AND mimeTypes including updateFrom and updateUntil for lastUpdate field with the biggest version number
        //if security is enabled, include principal in query
        //set all schemaDocumentUri elemets to path of getSchemaDocumentById(schemaId, version)
        //return HTTP 200 and result list
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<MetadataSchemaRecord> updateRecord(String id, MetadataSchemaRecord record, MultipartFile document, WebRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder) {
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
    public ResponseEntity deleteRecord(String id, WebRequest wr, HttpServletResponse hsr) {
        //search for all versions of record with provided schemaId
        //ETag check -> HTTP CONFLICT if fails
        //Delete documents and records
        //return HTTP 204
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
