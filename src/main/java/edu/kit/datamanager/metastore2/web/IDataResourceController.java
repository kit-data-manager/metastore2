/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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

import com.github.fge.jsonpatch.JsonPatch;
import edu.kit.datamanager.controller.IControllerAuditSupport;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.controller.IGenericResourceController;
import edu.kit.datamanager.repo.domain.ContentInformation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springdoc.core.converters.models.PageableAsQueryParam;
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
 * Data resource controller interface definition. In addition to the common
 * controller endpoints defined in IGenericResourceController, the data resource
 * controller also provides additional endpoints for content access. Access to
 * data resources and content is separated by /data/ within the endpoint URLs.
 *
 * @author jejkal
 */
public interface IDataResourceController extends IGenericResourceController<DataResource>, IControllerAuditSupport{

  @Operation(summary = "Upload data for a data resource.", description = "This endpoint allows to upload or assign data and content metadata related to the uploaded file to a resource identified by its id. "
          + "Uploaded data will be stored at the configured backend, typically the local hard disk. Furthermore, it is possible to register data stored elsewhere by providing only a content URI within the content metadata."
          + "In any other case, providing content metadata is optional. Parts of the content metadata, e.g. content type or checksum, may be generated or overwritten after a file upload if they not already exist or if "
          + "the configuration does not allow the user to provide particular content metadata entries, e.g. because a certain checksum digest is mandatory."
          + "All uploaded data can be virtually structured by providing the relative path where they should be accessible within the request URL. If a file at a given path already exists, there will be typically returned HTTP CONFLICT. "
          + "If desired, overwriting of existing content can be enforced by setting the request parameter 'force' to  true. In that case, the existing file will be marked for deletion and is deleted after the upload operation "
          + "has successfully finished. If the overwritten element only contains a reference URI, the entry is just replaced by the user provided entry.", security = {
            @SecurityRequirement(name = "bearer-jwt")})
  @RequestMapping(path = "/{id}/data/**", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity createContent(@Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
          @Parameter(description = "The file to upload. If no file is uploaded, a metadata document must be provided containing a reference URI to the externally hosted data.", required = false) @RequestPart(name = "file", required = false) final MultipartFile file,
          @Parameter(description = "Json representation of a content information metadata document. Providing this metadata document is optional unless no file is uploaded.", required = false) @RequestPart(name = "metadata", required = false) final ContentInformation contentInformation,
          @Parameter(description = "Flag to indicate, that existing content at the same location should be overwritten.", required = false) @RequestParam(name = "force", defaultValue = "false") final boolean force,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

  @Operation(summary = "Access content information for single or multiple data elements.",
          description = "List metadata of one or more content elements associated with a data resource in a paginated and/or sorted form. This endpoint is addressed if the caller provides content type "
          + "'application/vnd.datamanager.content-information+json' within the 'Accept' header. If this content type is not present, the content element is downloaded instead."
          + "The content path, defining whether one or more content element(s) is/are returned, is provided within the request URL. Everything after 'data/' is expected to be either a virtual folder or single content element. "
          + "If the provided content path ends with a slash, it is expected to represent a virtual collection which should be listed. If the content path does not end with a slash, it is expected to refer to a single element. "
          + "If not element with the exact content path exists, HTTP NOT_FOUND is returned. The user may provide custom sort criteria for ordering the returned elements. If no sort criteria is provided, the default sorting is "
          + "applied which returning all matching elements in ascending order by hierarchy depth and alphabetically by their relative path.", security = {
            @SecurityRequirement(name = "bearer-jwt")})
  @RequestMapping(path = "/{id}/data/**", method = RequestMethod.GET, produces = "application/vnd.datamanager.content-information+json")
  @ResponseBody
  @PageableAsQueryParam
  public ResponseEntity getContentMetadata(
          @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
          @Parameter(description = "A single tag assigned to certain content elements. Tags allow easy structuring and filtering of content associated to a resource.", required = false) @RequestParam(name = "tag", required = false) final String tag,
          @Parameter(description = "The resource version to access.", required = false) @RequestParam(value = "The version number of the content information.", name = "version", required = false) final Long version,
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

  @Operation(summary = "List content information by example.", description = "List all content information in a paginated and/or sorted form by example using an example document provided in the request body. "
          + "The example is a normal instance of the resource. However, search-relevant top level primitives are marked as 'Searchable' within the implementation. "
          + "For string values, '%' can be used as wildcard character. If the example document is omitted, the response is identical to listing all resources with the same pagination parameters. "
          + "As well as listing of all resources, the number of total results might be affected by the caller's role.", security = {
            @SecurityRequirement(name = "bearer-jwt")})
  @RequestMapping(value = {"/search/data"}, method = {RequestMethod.POST})
  @ResponseBody
  @PageableAsQueryParam
  public ResponseEntity<List<ContentInformation>> findContentMetadataByExample(
          @Parameter(description = "Json representation of the resource serving as example for the search operation. Typically, only first level primitive attributes are evaluated while building queries from examples.", required = true) @RequestBody final ContentInformation c,
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb);

  @Operation(summary = "Patch a single content information element.",
          description = "This endpoint allows to patch single content information elements associated with a data resource. As most of the content information attributes are typically automatically generated their modification is restricted "
          + "to privileged users, e.g. user with role ADMINISTRATOR or permission ADMINISTRATE. Users having WRITE permissions to the associated resource are only allowed to modify contained metadata elements or tags assigned to the content element.", security = {
            @SecurityRequirement(name = "bearer-jwt")})
  @RequestMapping(path = "/{id}/data/**", method = RequestMethod.PATCH, consumes = "application/json-patch+json")
  @ResponseBody
  public ResponseEntity patchContentMetadata(
          @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
          @Parameter(description = "Json representation of a json patch document. The document must comply with RFC 6902 specified by the IETF.", required = true) @RequestBody final JsonPatch patch,
          final WebRequest request,
          final HttpServletResponse response);

  @Operation(summary = "Download data located at the provided content path.",
          description = "This endpoint allows to download the data associated with a data resource and located at a particular virtual part. The virtual path starts after 'data/' and should end with a filename. "
          + "Depending on the content located at the provided path, different response scenarios can occur. If the content is a locally stored, accessible file, the bitstream of the file is retured. If the file is (temporarily) not available, "
          + "HTTP 404 is returned. If the content referes to an externally stored resource accessible via http(s), the service will try if the resource is accessible. If this is the case, the service will return HTTP 303 (SEE_OTHER) together "
          + "with the resource URI in the 'Location' header. Depending on the client, the request is then redirected and the bitstream is returned. If the resource is not accessible or if the protocol is not http(s), the service "
          + "will either return the status received by accessing the resource URI, SERVICE_UNAVAILABLE if the request has failed or NO_CONTENT if not other status applies. In addition, the resource URI is returned in the 'Content-Location' header "
          + "in case the client wants to try to access the resource URI.", security = {
            @SecurityRequirement(name = "bearer-jwt")})
  @RequestMapping(path = "/{id}/data/**", method = RequestMethod.GET)
  @ResponseBody
  public void getContent(
          @Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
          @Parameter(description = "The resource version to access.", required = false) @RequestParam(value = "The version number of the content information.", name = "version", required = false) final Long version,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

  @Operation(summary = "Remove a single content information element.",
          description = "This endpoint allows to remove single content information elements associated with a data resource. Removing content information elements including their content is restricted "
          + "to privileged users, e.g. user with role ADMINISTRATOR or permission ADMINISTRATE.")
  @RequestMapping(path = "/{id}/data/**", method = RequestMethod.DELETE)
  @ResponseBody
  public ResponseEntity deleteContent(@Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
          final WebRequest request,
          final HttpServletResponse response);

  @Operation(summary = "Access audit information for a single content information resource.",
          description = "List audit information for a content information resource in a paginated form. Sorting can be supported but is optional. If no sorting is supported it is recommended to return audit "
          + "information sorted by version number in descending order. This endpoint is addressed if the caller provides content type "
          + "'application/vnd.datamanager.audit+json' within the 'Accept' header. If no audit support is enabled or no audit information are available for a certain resource, "
          + "an empty result should be returned.", security = {
            @SecurityRequirement(name = "bearer-jwt")})
  @RequestMapping(path = "/{id}/data/**", method = RequestMethod.GET, produces = "application/vnd.datamanager.audit+json")
  @ResponseBody
  @PageableAsQueryParam
  public ResponseEntity getContentAuditInformation(@Parameter(description = "The resource identifier.", required = true) @PathVariable(value = "id") final String id,
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder);

}
