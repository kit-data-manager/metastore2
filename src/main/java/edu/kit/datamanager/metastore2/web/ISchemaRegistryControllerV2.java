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
package edu.kit.datamanager.metastore2.web;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Interface for schema document controller.
 *
 * @author jejkal
 */
@ApiResponses(value = {
  @ApiResponse(responseCode = "401", description = "Unauthorized is returned if authorization in required but was not provided."),
  @ApiResponse(responseCode = "403", description = "Forbidden is returned if the caller has no sufficient privileges.")})
public interface ISchemaRegistryControllerV2 extends InfoContributor {

  @Operation(operationId = "createSchema",
          summary = "Register a schema document and its record.", 
          description = "This endpoint allows to register a schema document and its (datacite) record. "
          + "The record must contain at least an unique identifier (schemaId) and the type of the schema (type).",
          responses = {
            @ApiResponse(responseCode = "201", description = "Created is returned only if the record has been validated, persisted and the document was successfully validated and stored.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request is returned if the provided metadata record is invalid or if the validation of the provided schema failed."),
            @ApiResponse(responseCode = "409", description = "A Conflict is returned, if there is already a record for the provided schema id.")})
  @RequestMapping(value = {"/"}, method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  ResponseEntity<DataResource> createRecord(
          @Parameter(description = "Json representation of the schema record.", required = true) @RequestPart(name = "record", required = true) final MultipartFile schemaRecord,
          @Parameter(description = "The metadata schema document associated with the record.", required = true) @RequestPart(name = "schema", required = true) final MultipartFile document,
          final HttpServletRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

  @Operation(operationId = "getDataCiteRecordOfSchema",
          summary = "Get schema record by schema id (and version).", 
          description = "Obtain is single schema record by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the record can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the record is returned if the record exists and the user has sufficient permission.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/{schemaId}"}, method = {RequestMethod.GET}, produces = {"application/vnd.datacite.org+json"})
  @ResponseBody
  ResponseEntity<DataResource> getRecordById(@Parameter(description = "The record identifier or schema identifier.", required = true) @PathVariable(value = "schemaId") String id,
                                             @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
                                             WebRequest wr,
                                             HttpServletResponse hsr);
  @Operation(operationId = "getContentInformationRecordOfSchema",
          summary = "Get content information record by schema id (and version).", 
          description = "Obtain is single schema record by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the record can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the record is returned if the record exists and the user has sufficient permission.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/{schemaId}"}, method = {RequestMethod.GET}, produces = {"application/vnd.datamanager.content-information+json"})
  @ResponseBody
  ResponseEntity<ContentInformation> getContentInformationById(@Parameter(description = "The record identifier or schema identifier.", required = true) @PathVariable(value = "schemaId") String id,
                                                               @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
                                                               WebRequest wr,
                                                               HttpServletResponse hsr);

  @Operation(summary = "Get landing page of schema by schema id (and version).", description = "Show landing page by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the schema can be returned by providing a version number as request parameter. If no version is specified, all versions will be returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the landingpage is returned if the id exists and the user has sufficient permission.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/{schemaId}"}, method = {RequestMethod.GET}, produces = {"text/html"})
  ModelAndView getLandingPageById(@Parameter(description = "The record identifier or schema identifier.", required = true) @PathVariable(value = "schemaId") String id,
                                  @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
                                  WebRequest wr,
                                  HttpServletResponse hsr);

  @Operation(summary = "Validate a metadata document.", description = "Validate the provided metadata document using the addressed schema. If all parameters"
          + " are provided, the schema is identified uniquely by schemaId and version. If the version is omitted, the most recent version of the "
          + "schema is used. This endpoint returns HTTP NO_CONTENT if it succeeds. Otherwise, an error response is returned, e.g. HTTP UNPROCESSABLE_ENTITY (422) if validation fails.",
          responses = {
            @ApiResponse(responseCode = "204", description = "No Content if validate succeeded."),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no schema for the provided schemaId and version was found."),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity if validation fails.")
          })
  @RequestMapping(value = {"/{schemaId}/validate"}, method = {RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @ResponseBody
  ResponseEntity validate(@Parameter(description = "The record identifier or schema identifier.", required = true) @PathVariable(value = "schemaId") String id,
                          @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
                          @Parameter(description = "The metadata file to validate against the addressed schema.", required = true) @RequestPart(name = "document", required = true) final MultipartFile document,
                          WebRequest wr,
                          HttpServletResponse hsr);

  @Operation(summary = "Get a schema document by schema id.", description = "Obtain a single schema document identified by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the schema document can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the schema document is returned if the record exists and the user has sufficient permission."),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/{schemaId}"}, method = {RequestMethod.GET}, produces = {"application/json", "application/xml"})
  @ResponseBody
  ResponseEntity getSchemaDocumentById(@Parameter(description = "The schema id.", required = true) @PathVariable(value = "schemaId") String id,
                                       @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
                                       WebRequest wr,
                                       HttpServletResponse hsr);

  @Operation(summary = "Get all schema records.", description = "List all schema records in a paginated and/or sorted form. The result can be refined by providing schemaId, a list of one or more mimetypes and/or a date range. Returned schema record(s) must match. "
          + "if 'schemaId' is provided all other parameters were skipped and all versions of the given schemaId record will be returned. "
          + "If 'mimetype' is provided, a record matches if its associated mime type matchs. "
          + "Furthermore, the UTC time of the last update can be provided in three different fashions: 1) Providing only updateFrom returns all records updated at or after the provided date, 2) Providing only updateUntil returns all records updated before or "
          + "at the provided date, 3) Providing both returns all records updated within the provided date range. "
          + "If no parameters are provided, all accessible records are listed. With regard to schema versions, only the most recent version of each schema is listed.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and a list of records or an empty list of no record matches.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MetadataSchemaRecord.class))))})
  @RequestMapping(value = {"/"}, method = {RequestMethod.GET})
  @ResponseBody
  @PageableAsQueryParam
  ResponseEntity<List<DataResource>> getRecords(
          @Parameter(description = "SchemaId", required = false) @RequestParam(value = "schemaId", required = false) String schemaId,
          @Parameter(description = "A list of mime types returned schemas are associated with.", required = false) @RequestParam(value = "mimeType", required = false) List<String> mimeTypes,
          @Parameter(description = "The UTC time of the earliest update of a returned record.", required = false) @RequestParam(name = "from", required = false) Instant updateFrom,
          @Parameter(description = "The UTC time of the latest update of a returned record.", required = false) @RequestParam(name = "until", required = false) Instant updateUntil,
          @Parameter(hidden = true) @PageableDefault(sort = {"id"}, direction = Sort.Direction.ASC) Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb);

  @Operation(summary = "Update a schema record.", description = "Apply an update to the schema record and/or schema document with the provided schema id. "
          + "The update capabilities for a schema record are quite limited. An update is always related to the most recent version. "
          + "Only the associated mimeType and acl can be changed.  All other fields are updated automatically or are read-only. Updating only the metadata record does not affect the version number. "
          + "A new version is only created while providing a (new) schema document.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK is returned in case of a successful update. "
                    + "The updated record is returned in the response.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request is returned if the provided schema record/schema document is invalid."),
            @ApiResponse(responseCode = "404", description = "Not Found is returned if no record for the provided id was found.")})
  @RequestMapping(value = "/{schemaId}", method = RequestMethod.PUT, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
  @Parameters({
    @Parameter(name = "If-Match", description = "ETag of the object. Please use quotation marks!", required = true, in = ParameterIn.HEADER)
  })
  ResponseEntity<DataResource> updateRecord(
          @Parameter(description = "The schema id.", required = true) @PathVariable("schemaId") final String schemaId,
          @Parameter(description = "Json representation of the schema record.", required = false) @RequestPart(name = "record", required = false) final MultipartFile schemaRecord,
          @Parameter(description = "The metadata schema document associated with the record.", required = false) @RequestPart(name = "schema", required = false) final MultipartFile document,
          final WebRequest request,
          final HttpServletResponse response
  );

  @Operation(summary = "Delete a schema record.", description = "Delete a single schema record. "
          + "Deleting a record typically requires the caller to have special permissions. "
          + "In some cases, deleting a record can also be available for the owner or other privileged users or can be forbidden at all. Deletion of a record affects all versions of the particular record.",
          responses = {
            @ApiResponse(responseCode = "204", description = "No Content is returned as long as no error occurs while deleting a record. Multiple delete operations to the same record will also return HTTP 204 even if the deletion succeeded in the first call.")})
  @RequestMapping(value = {"/{schemaId}"}, method = {RequestMethod.DELETE})
  @Parameters({
    @Parameter(name = "If-Match", description = "ETag of the object. Please use quotation marks!", required = true, in = ParameterIn.HEADER)
  })
  @ResponseBody
  ResponseEntity deleteRecord(@Parameter(description = "The schema id.", required = true) @PathVariable(value = "schemaId") String id, @Header(name = "ETag", required = true) WebRequest wr, HttpServletResponse hsr);
}
