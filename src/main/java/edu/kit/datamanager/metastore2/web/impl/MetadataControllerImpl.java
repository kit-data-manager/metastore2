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

import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.web.IMetadataController;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
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
@RequestMapping(value = "/api/v1/metadata")
@Api(value = "Metadata Repository")
public class MetadataControllerImpl implements IMetadataController {

    @Override
    public ResponseEntity createRecord(
            @RequestPart(name = "record") final MetadataRecord record,
            @RequestPart(name = "document") final MultipartFile document,
            WebRequest request,
            HttpServletResponse response,
            UriComponentsBuilder uriBuilder) {
        //check required attributes (relatedResource, schemaId, [schemaVersion]) -> if not set return HTTP BAD_REQUEST
        //obtain schemaDocument for schemaid and version -> if not found HTTP NOT_FOUND
        //validate document using schema -> if fails, return HTTP BAD_REQUEST
        //persist document
        //update metadataDocumentUri in record 
        //set createdAt and lastUpdate to Instant.now()
        //check acl -> add at least caller to acl

        //persist record
        //update metadataDocumentUri to path of getMetadataDocumentById(id, version)
        //return HTTP 201 with record
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<MetadataRecord> getRecordById(
            @PathVariable(value = "id") String id,
            @RequestParam(value = "version", required = false) Long version,
            WebRequest wr,
            HttpServletResponse hsr) {
        //obtain record by id and version -> if not found, return HTTP NOT_FOUND
        //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
        //update metadataDocumentUri to path of getMetadataDocumentById(id, version) 
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity getMetadataDocumentById(
            String id,
            Long version,
            WebRequest wr,
            HttpServletResponse hsr) {
        //obtain record by id and version -> if not found, return HTTP NOT_FOUND
        //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
        //obtain metadataDocumentUri from record -> if file not found return HTTP NOT_FOUND
        //return HTTP 200 with streamed file
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<List<MetadataRecord>> getRecords(
            @RequestParam(value = "resoureId", required = false) List<String> relatedIds,
            @RequestParam(value = "schemaId", required = false) List<String> schemaIds,
            @RequestParam(name = "from", required = false) Instant updateFrom,
            @RequestParam(name = "until", required = false) Instant updateUntil,
            Pageable pgbl,
            WebRequest wr,
            HttpServletResponse hsr,
            UriComponentsBuilder ucb) {
        //search for records with provided relatedIds AND schemaIds including updateFrom and updateUntil for lastUpdate field with the biggest version number
        //if security is enabled, include principal in query
        //set all metadataDocumentUri elemets to path of getMetadataDocumentById(schemaId, version)
        //return HTTP 200 and result list
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity<MetadataRecord> updateRecord(
            @PathVariable("id") String id,
            @RequestPart(name = "record", required = false) final MetadataRecord record,
            @RequestPart(name = "document", required = false) final MultipartFile document,
            WebRequest request,
            HttpServletResponse response,
            UriComponentsBuilder uriBuilder) {
        //search for record with provided id in the most recent version -> if not found, return HTTP NOT_FOUND
        //if authorization enabled, check principal -> return HTTP UNAUTHORIZED or FORBIDDEN if not matching
        //ETag check -> HTTP CONFLICT if fails
        //check for valid fields to update relatedResource, schemaId, schemaVersion, acl) -> return HTTP BAD_REQUEST if forbidden fields are included
        //apply schemaId and schemaVersion update

        //if document is provided:
        //compare current document with provided and check for difference -> no difference, proceed at end
        //obtain schema for schemaId and version -> if fails return HTTP NOT_FOUND
        //validate document using schema -> if fails, return HTTP BAD_REQUEST
        //persist document
        //end if document provided
        //apply changes to record (relatedResource, acl)
        //update record version
        //update schemaDocumentUri (if document was provided)
        //update lastModifiedAt to Instant.now()
        //Persist record
        //set metadataDocumentUri to path of getMetadataDocumentById(id, version)
        //return HTTP 200 and record
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResponseEntity deleteRecord(
            @PathVariable(value = "id") String id,
            WebRequest wr,
            HttpServletResponse hsr) {
        //search for all versions of record with provided id
        //ETag check with most recent version -> HTTP CONFLICT if fails
        //Delete documents and records
        //return HTTP 204
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
