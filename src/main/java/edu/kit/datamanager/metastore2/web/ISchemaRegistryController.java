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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@ApiResponses(value = {
  @ApiResponse(responseCode = "401", description = "Unauthorized is returned if authorization in required but was not provided."),
  @ApiResponse(responseCode = "403", description = "Forbidden is returned if the caller has no sufficient privileges.")})
public interface ISchemaRegistryController {

  @Operation(summary = "Register/replace a schema record.", description = "This endpoint allows to create or replace a metadata schema record. If no metadata record for the schema identifier provided in the record argument "
          + "is found, a new schema record is created. Otherwise, the existing record is updated to a new version. The later use case is meant to be used mainly for schema synchronization from an external authoritive source, "
          + " as updating a schema document for an existing schema may break the validation of previously assigned and validated metadata documents. That's why schema updates are only possible from the local machine. ",
          responses = {
            @ApiResponse(responseCode = "201", description = "Created is returned only if the record has been validated, persisted and the document was successfully validated and stored.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request is returned if the provided metadata record is invalid or if the validation using the provided schema failed."),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no schema for the provided schema id was found."),
            @ApiResponse(responseCode = "409", description = "A Conflict is returned, if there is already a record for the related resource id and the provided schema id.")})
  @RequestMapping(path = "/", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public ResponseEntity createRecord(
          @Parameter(description = "Json representation of the schema record.", required = true, content = {
    @Content(encoding = @Encoding(name = "record", contentType = "application/json"))}) @RequestPart(name = "record", required = true) final MetadataSchemaRecord record,
          @Parameter(description = "The metadata schema document associated with the record.", required = true) @RequestPart(name = "schema", required = true) final MultipartFile document,
          final HttpServletRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

  @Operation(summary = "Get a schema record by schema id.", description = "Obtain is single schema record by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden."
          + "Furthermore, a specific version of the record can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the record is returned if the record exists and the user has sufficient permission.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/{id}"}, method = {RequestMethod.GET}, produces = {"application/vnd.datamanager.schema-record+json"})
  @ResponseBody
  public ResponseEntity<MetadataSchemaRecord> getRecordById(@Parameter(description = "The record identifier or schema identifier.", required = true) @PathVariable(value = "id") String id,
          @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr);

  @Operation(summary = "Validate a metadata document.", description = "Validate the provided metadata document using the addressed schema. If all parameters"
          + " are provided, the schema is identified uniquely by schemaId and version. If the version is omitted, the most recent version of the "
          + "schema is used. This endpoint returns HTTP NO_CONTENT if it succeeds. Otherwise, an error response is returned, e.g. HTTP UNPROCESSABLE_ENTITY (422) if validation fails.",
          responses = {
            @ApiResponse(responseCode = "204", description = "No Content if validate succeeded."),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found."),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity if validation fails.")
          })
  @RequestMapping(value = {"/{id}/validate"}, method = {RequestMethod.POST}, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})

  @ResponseBody
  public ResponseEntity<MetadataSchemaRecord> validate(@Parameter(description = "The record identifier or schema identifier.", required = true) @PathVariable(value = "id") String id,
          @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          @Parameter(description = "The metadata file to validate against the addressed schema.", required = true) @RequestPart(name = "document", required = true) final MultipartFile document,
          WebRequest wr,
          HttpServletResponse hsr);

  @Operation(summary = "Get a schema document by schema id.", description = "Obtain is single schema document identified by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the schema document can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the schema document is returned if the record exists and the user has sufficient permission."),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/{id}"}, method = {RequestMethod.GET})

  @ResponseBody
  public ResponseEntity getSchemaDocumentById(@Parameter(description = "The schema id.", required = true) @PathVariable(value = "id") String id,
          @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr);

  @Operation(summary = "Get all schema records.", description = "List all schema records in a paginated and/or sorted form. The result can be refined by providing a list of one or more mimetypes returned schema(s) are linked to  and/or one or more schema identifier valid records must match. "
          + "If both parameters are provided, a record matches if its associated mime type AND the schema id are matching. "
          + "Furthermore, the UTC time of the last update can be provided in three different fashions: 1) Providing only updateFrom returns all records updated at or after the provided date, 2) Providing only updateUntil returns all records updated before or "
          + "at the provided date, 3) Providing both returns all records updated within the provided date range."
          + "If no parameters are provided, all accessible records are listed. With regard to schema versions, only the most recent version of each schema is listed.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and a list of records or an empty list of no record matches.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MetadataSchemaRecord.class))))})
  @RequestMapping(value = {"/"}, method = {RequestMethod.GET})
  @ResponseBody
  @PageableAsQueryParam
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(
          @Parameter(description = "A list of schema ids of returned schemas.", required = false) @RequestParam(value = "schemaId", required = false) List<String> schemaIds,
          @Parameter(description = "A list of mime types returned schemas are associated with.", required = false) @RequestParam(value = "mimeType", required = false) List<String> mimeTypes,
          @Parameter(description = "The UTC time of the earliest update of a returned record.", required = false) @RequestParam(name = "from", required = false) Instant updateFrom,
          @Parameter(description = "The UTC time of the latest update of a returned record.", required = false) @RequestParam(name = "until", required = false) Instant updateUntil,
          Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb);

  @Operation(summary = "Update a schema record.", description = "Apply an update to the schema record with the provided schema id. "
          + "The update capabilities for a schema record are quite limited. An update is always related to the most recent version. "
          + "Only the associated mimeType and acl can be changed.  All other fields are updated automatically or are read-only. Updating a record does not affect the version number. "
          + "A new version is only created while POSTing a record including a schema document.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK is returned in case of a successful update."
                    + "The updated record is returned in the response.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request is returned if the provided schema record is invalid."),
            @ApiResponse(responseCode = "404", description = "Not Found is returned if no record for the provided id was found.")})
  @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = {"application/json"})
  @Parameters({
    @Parameter(name = "If-Match", description = "ETag of the object. Please use quotation marks!", required = true, in = ParameterIn.HEADER)
  })
  ResponseEntity<MetadataSchemaRecord> updateRecord(
          @Parameter(description = "The schema id.", required = true) @PathVariable("id") final String schemaId,
          @Parameter(description = "Json representation of the schema record.", required = false) @RequestBody final MetadataSchemaRecord record,
          final WebRequest request,
          final HttpServletResponse response
  );

  @Operation(summary = "Delete a schema record.", description = "Delete a single schema record. Deleting a record typically requires the caller to have special permissions. "
          + "In some cases, deleting a record can also be available for the owner or other privileged users or can be forbidden at all. Deleting a record only removes the current version from the database. "
          + "Older versions as well as schema documents are still available and can be accessed. If a new schema with the same schemaId is created later, the version counter will continue with the most recent "
          + "version before deleting the schema.",
          responses = {
            @ApiResponse(responseCode = "204", description = "No Content is returned as long as no error occurs while deleting a record. Multiple delete operations to the same record will also return HTTP 204 even if the deletion succeeded in the first call.")})
  @RequestMapping(value = {"/{id}"}, method = {RequestMethod.DELETE})
  @Parameters({
    @Parameter(name = "If-Match", description = "ETag of the object. Please use quotation marks!", required = true, in = ParameterIn.HEADER)
  })
  @ResponseBody
  public ResponseEntity deleteRecord(@Parameter(description = "The schema id.", required = true) @PathVariable(value = "id") String id, @Header(name = "ETag", required = true) WebRequest wr, HttpServletResponse hsr);
}
