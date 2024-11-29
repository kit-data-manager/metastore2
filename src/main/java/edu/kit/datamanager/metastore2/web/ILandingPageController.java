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

import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

/**
 * Interface for metadata documents controller.
 */
@ApiResponses(value = {
  @ApiResponse(responseCode = "401", description = "Unauthorized is returned if authorization is required but was not provided."),
  @ApiResponse(responseCode = "403", description = "Forbidden is returned if the caller has no sufficient privileges.")})
public interface ILandingPageController {

  @Operation(summary = "Get landing page of schema by schema id (and version).", description = "Show landing page by its schema id. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. "
          + "Furthermore, a specific version of the schema can be returned by providing a version number as request parameter. If no version is specified, all versions will be returned.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the landingpage is returned if the id exists and the user has sufficient permission.", content = @Content(schema = @Schema(implementation = MetadataSchemaRecord.class))),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id and version was found.")})
  @RequestMapping(value = {"/schema-landing-page"}, method = {RequestMethod.GET}, produces = {"text/html"})
  String getLandingPageOfSchemaWithId(
          @Parameter(description = "The record identifier or schema identifier.", required = true) @RequestParam(value = "schemaId") String id,
          @Parameter(description = "The version of the record.", required = false) @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr,
          Model model);

  @Operation(summary = "Get a landing page by id.", description = "Obtain a single record by its resource identifier. "
          + "Depending on a user's role, accessing a specific record may be allowed or forbidden. Furthermore, a specific version of the record can be returned "
          + "by providing a version number as request parameter.",
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the record is returned if the record exists and the user has sufficient permission.", content = @Content(schema = @Schema(implementation = MetadataRecord.class))),
            @ApiResponse(responseCode = "404", description = "Not found is returned, if no record for the provided id or version was found.")})

  @RequestMapping(value = {"/metadata-landing-page"}, method = {RequestMethod.GET}, produces = {"text/html"})
  String getLandingPageOfMetadataDocumentWithId(
          @Parameter(description = "The identifier of the metadata document.", required = true) @RequestParam(value = "id") String id,
          @Parameter(description = "The version of the digital object. This parameter only has an effect if versioning  is enabled.", required = false) @RequestParam(value = "version") Long version,
          WebRequest wr,
          HttpServletResponse hsr,
          Model model);
}
