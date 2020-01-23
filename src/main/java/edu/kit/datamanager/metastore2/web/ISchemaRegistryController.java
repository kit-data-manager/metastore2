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
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
  @ApiResponse(code = 401, message = "Unauthorized is returned if authorization in required but was not provided."),
  @ApiResponse(code = 403, message = "Forbidden is returned if the caller has no sufficient privileges.")})
public interface ISchemaRegistryController{

  @ApiOperation(value = "Register/replace a schema record.", notes = "This endpoint allows to create or replace a metadata schema record. If no metadata record for the schema identifier provided in the record argument "
          + "is found, a new schema record is created. Otherwise, the existing record is updated to a new version. The later use case is meant to be used mainly for schema synchronization from an external authoritive source, "
          + " as updating a schema document for an existing schema may break the validation of previously assigned and validated metadata documents. That's why schema updates are only possible from the local machine. ", response = MetadataSchemaRecord.class)
  @RequestMapping(path = "/", method = RequestMethod.POST)
  @ApiResponses(value = {
    @ApiResponse(code = 201, message = "Created is returned only if the record has been validated, persisted and the document was successfully validated and stored.", response = MetadataSchemaRecord.class),
    @ApiResponse(code = 400, message = "Bad Request is returned if the provided metadata record is invalid or if the validation using the provided schema failed."),
    @ApiResponse(code = 404, message = "Not found is returned, if no schema for the provided schema id was found."),
    @ApiResponse(code = 409, message = "A Conflict is returned, if there is already a record for the related resource id and the provided schema id.")})
  @ResponseBody
  public ResponseEntity createRecord(
          @ApiParam(value = "Json representation of the schema record.", required = true) @RequestPart(name = "record", required = true) final MetadataSchemaRecord record,
          @ApiParam(value = "The metadata schema document associated with the record.", required = true) @RequestPart(name = "schema", required = true) final MultipartFile document,
          final HttpServletRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

  @ApiOperation(value = "Get a schema record by schema id.", notes = "Obtain is single schema record by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden."
          + "Furthermore, a specific version of the record can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.")
  @RequestMapping(value = {"/{id}"}, method = {RequestMethod.GET}, produces = {"application/vnd.datamanager.schema-record+json"})
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "OK and the record is returned if the record exists and the user has sufficient permission.", response = MetadataSchemaRecord.class),
    @ApiResponse(code = 404, message = "Not found is returned, if no record for the provided id and version was found.")})
  @ResponseBody
  public ResponseEntity<MetadataSchemaRecord> getRecordById(@ApiParam(value = "The record identifier or schema identifier.", required = true) @PathVariable(value = "id") String id,
          @ApiParam(value = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr);

  @ApiOperation(value = "Validate a metadata document.", notes = "Validate the provided metadata document using the addressed schema. If all parameters"
          + " are provided, the schema is identified uniquely by schemaId and version. If the version is omitted, the most recent version of the "
          + "schema is used. This endpoint returns HTTP NO_CONTENT if it succeeds. Otherwise, an error response is returned, e.g. HTTP UNPROCESSABLE_ENTITY (422) if validation fails.")
  @RequestMapping(value = {"/{id}/validate"}, method = {RequestMethod.POST})
  @ApiResponses(value = {
    @ApiResponse(code = 204, message = "No Content if validate succeeded."),
    @ApiResponse(code = 404, message = "Not found is returned, if no record for the provided id and version was found."),
    @ApiResponse(code = 422, message = "Unprocessable Entity if validation fails.")
  })
  @ResponseBody
  public ResponseEntity<MetadataSchemaRecord> validate(@ApiParam(value = "The record identifier or schema identifier.", required = true) @PathVariable(value = "id") String id,
          @ApiParam(value = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          @ApiParam(value = "The metadata file to validate against the addressed schema.", required = true) @RequestPart(name = "document", required = true) final MultipartFile document,
          WebRequest wr,
          HttpServletResponse hsr);

  @ApiOperation(value = "Get a schema document by schema id.", notes = "Obtain is single schema document identified by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the schema document can be returned by providing a version number as request parameter. If no version is specified, the most recent version is returned.")
  @RequestMapping(value = {"/{id}"}, method = {RequestMethod.GET})
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "OK and the schema document is returned if the record exists and the user has sufficient permission."),
    @ApiResponse(code = 404, message = "Not found is returned, if no record for the provided id and version was found.")})
  @ResponseBody
  public ResponseEntity getSchemaDocumentById(@ApiParam(value = "The schema id.", required = true) @PathVariable(value = "id") String id,
          @ApiParam(value = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr);

  @ApiOperation(value = "Get all schema records.", notes = "List all schema records in a paginated and/or sorted form. The result can be refined by providing a list of one or more mimetypes returned schema(s) are linked to  and/or one or more schema identifier valid records must match. "
          + "If both parameters are provided, a record matches if its associated mime type AND the schema id are matching. "
          + "Furthermore, the UTC time of the last update can be provided in three different fashions: 1) Providing only updateFrom returns all records updated at or after the provided date, 2) Providing only updateUntil returns all records updated before or "
          + "at the provided date, 3) Providing both returns all records updated within the provided date range."
          + "If no parameters are provided, all accessible records are listed. With regard to schema versions, only the most recent version of each schema is listed.")
  @ApiImplicitParams(value = {
    @ApiImplicitParam(name = "page", dataType = "integer", paramType = "query", value = "Results page you want to retrieve (0..N)"),
    @ApiImplicitParam(name = "size", dataType = "integer", paramType = "query", value = "Number of records per page."),
    @ApiImplicitParam(name = "sort", allowMultiple = true, dataType = "string", paramType = "query", value = "Sorting criteria in the format: property(,asc|desc). Default sort order is ascending. Multiple sort criteria are supported.")})
  @RequestMapping(value = {"/"}, method = {RequestMethod.GET})
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "OK and a list of records or an empty list of no record matches.")})
  @ResponseBody
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(
          @ApiParam(value = "A list of schema ids of returned schemas.", required = false) @RequestParam(value = "schemaId", required = false) List<String> schemaIds,
          @ApiParam(value = "A list of mime types returned schemas are associated with.", required = false) @RequestParam(value = "mimeType", required = false) List<String> mimeTypes,
          @ApiParam(value = "The UTC time of the earliest update of a returned record.", required = false) @RequestParam(name = "from", required = false) Instant updateFrom,
          @ApiParam(value = "The UTC time of the latest update of a returned record.", required = false) @RequestParam(name = "until", required = false) Instant updateUntil,
          Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb);

  @ApiOperation(value = "Update a schema record.", notes = "Apply an update to the schema record with the provided schema id. "
          + "The update capabilities for a schema record are quite limited. An update is always related to the most recent version. "
          + "Only the associated mimeType and acl can be changed.  All other fields are updated automatically or are read-only. Updating a record does not affect the version number. "
          + "A new version is only created while POSTing a record including a schema document.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "OK is returned in case of a successful update."
            + "The updated record is returned in the response."),
    @ApiResponse(code = 400, message = "Bad Request is returned if the provided schema record is invalid."),
    @ApiResponse(code = 404, message = "Not Found is returned if no record for the provided id was found.")})
  @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = {"application/json"})
  ResponseEntity<MetadataSchemaRecord> updateRecord(
          @ApiParam(value = "The schema id.", required = true) @PathVariable("id") String id,
          @ApiParam(value = "JSON representation of the schema record.", required = false) @RequestBody final MetadataSchemaRecord record,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder
  );

  @ApiOperation(value = "Delete a schema record.", notes = "Delete a single schema record. Deleting a record typically requires the caller to have special permissions. "
          + "In some cases, deleting a record can also be available for the owner or other privileged users or can be forbidden at all. Deleting a record only removes the current version from the database. "
          + "Older versions as well as schema documents are still available and can be accessed. If a new schema with the same schemaId is created later, the version counter will continue with the most recent "
          + "version before deleting the schema.")
  @RequestMapping(value = {"/{id}"}, method = {RequestMethod.DELETE})
  @ApiResponses(value = {
    @ApiResponse(code = 204, message = "No Content is returned as long as no error occurs while deleting a record. Multiple delete operations to the same record will also return HTTP 204 even if the deletion succeeded in the first call.")})
  @ResponseBody
  public ResponseEntity deleteRecord(@ApiParam(value = "The schema id.", required = true) @PathVariable(value = "id") String id, WebRequest wr, HttpServletResponse hsr);
}
