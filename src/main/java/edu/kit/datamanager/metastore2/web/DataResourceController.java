/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.IRepoStorageService;
import edu.kit.datamanager.repo.service.IRepoVersioningService;
import edu.kit.datamanager.repo.service.impl.ContentInformationAuditService;
import edu.kit.datamanager.repo.service.impl.DataResourceAuditService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.http.client.utils.URIBuilder;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/dataresources")
@Schema(description = "Data Resource Management")
public class DataResourceController implements IDataResourceController {

  public static final String VERSION_HEADER = "Resource-Version";
  public static final String CONTENT_RANGE_HEADER = "Content-Range";
  // private final JsonResult json = JsonResult.instance();
  @Autowired
  private Logger LOGGER = LoggerFactory.getLogger(DataResourceController.class);
  ;
  @Autowired
  private final Javers javers;
  @Autowired
  private final IDataResourceService dataResourceService;
  @Autowired
  private final IContentInformationService contentInformationService;
  @Autowired
  private ApplicationEventPublisher eventPublisher;
  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private IRepoVersioningService[] versioningServices;
  @Autowired
  private IRepoStorageService[] storageServices;

  private final IAuditService<DataResource> auditService;
  private final IAuditService<ContentInformation> contentAuditService;
  private final RepoBaseConfiguration repositoryProperties;

  /**
   *
   * @param applicationProperties
   * @param javers
   * @param dataResourceService
   * @param contentInformationService
   * @param versioningServices
   * @param storageServices
   * @param eventPublisher
   */
  public DataResourceController(ApplicationProperties applicationProperties,
          Javers javers,
          IDataResourceService dataResourceService,
          IContentInformationService contentInformationService,
          IRepoVersioningService[] versioningServices,
          IRepoStorageService[] storageServices,
          ApplicationEventPublisher eventPublisher
  ) {
    this.applicationProperties = applicationProperties;
    this.javers = javers;
    this.dataResourceService = dataResourceService;
    this.contentInformationService = contentInformationService;
    this.versioningServices = versioningServices;
    this.storageServices = storageServices;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(applicationProperties.getMetadataFolder());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(this.dataResourceService);
    rbc.setContentInformationService(contentInformationService);
    rbc.setEventPublisher(eventPublisher);
    for (IRepoVersioningService versioningService : versioningServices) {
      if ("simple".equals(versioningService.getServiceName())) {
        LOGGER.info("Set versioning service: {}", versioningService.getServiceName());
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    for (IRepoStorageService storageService : storageServices) {
      if ("dateBased".equals(storageService.getServiceName())) {
        LOGGER.info("Set storage service: {}", storageService.getServiceName());
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditService = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
//    dataResourceService = new DataResourceService();
    dataResourceService.configure(rbc);
//    contentInformationService = new ContentInformationService();
    contentInformationService.configure(rbc);
    repositoryProperties = rbc;
  }

  @Override
  public ResponseEntity<DataResource> create(@RequestBody final DataResource resource,
          final WebRequest request,
          final HttpServletResponse response) {

    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, 1l, request, response)).toString();
    };
    DataResource result = DataResourceUtils.createResource(repositoryProperties, resource);
    try {
      LOGGER.trace("Creating controller link for resource identifier {}.", result.getId());
      //do some hacking in order to properly escape the resource identifier
      //if escaping in beforehand, WebMvcLinkBuilder will escape again, which invalidated the link
      String uriLink = getById.apply("WorkaroundPlaceholder");
      //replace placeholder with escaped identifier in order to ensure single-escaping
      uriLink = uriLink.replaceFirst("WorkaroundPlaceholder", URLEncoder.encode(result.getId(), "UTF-8"));
      uriLink = uriLink.substring(0, uriLink.lastIndexOf("?"));

      LOGGER.trace("Created resource link is: {}", uriLink);
      return ResponseEntity.created(URI.create(uriLink)).eTag("\"" + result.getEtag() + "\"").header(VERSION_HEADER, Long.toString(1l)).body(result);
    } catch (UnsupportedEncodingException ex) {
      LOGGER.error("Failed to encode resource identifier " + result.getId() + ".", ex);
      throw new CustomInternalServerError("Failed to decode resource identifier " + result.getId() + ", but resource has been created.");
    }
  }

  @Override
  public ResponseEntity<DataResource> getById(@PathVariable("id") final String identifier,
          @RequestParam(name = "version", required = false) final Long version,
          final WebRequest request,
          final HttpServletResponse response) {
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, version, request, response)).toString();
    };
    return DataResourceUtils.readResource(repositoryProperties, identifier, version, getById);
  }

  @Override
  public ResponseEntity<List<DataResource>> findAll(@RequestParam(name = "from", required = false) final Instant lastUpdateFrom,
          @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
          final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {
    return findByExample(null, lastUpdateFrom, lastUpdateUntil, pgbl, request, response, uriBuilder);
  }

  @Override
  public ResponseEntity<List<DataResource>> findByExample(@RequestBody DataResource example,
          @RequestParam(name = "from", required = false) final Instant lastUpdateFrom,
          @RequestParam(name = "until", required = false) final Instant lastUpdateUntil,
          final Pageable pgbl,
          final WebRequest req,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {
    Page<DataResource> page = DataResourceUtils.readAllResourcesFilteredByExample(repositoryProperties, example, lastUpdateFrom, lastUpdateUntil, pgbl, response, uriBuilder);
    //set content-range header for react-admin (index_start-index_end/total
    PageRequest request = ControllerUtils.checkPaginationInformation(pgbl);
    response.addHeader(CONTENT_RANGE_HEADER, ControllerUtils.getContentRangeHeader(page.getNumber(), request.getPageSize(), page.getTotalElements()));
    return ResponseEntity.ok().body(DataResourceUtils.filterResources(page.getContent()));

  }

  @Override
  public ResponseEntity patch(@PathVariable("id") final String identifier,
          @RequestBody final JsonPatch patch,
          final WebRequest request,
          final HttpServletResponse response) {
    Function<String, String> patchDataResource = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).patch(t, patch, request, response)).toString();
    };
    String path = ContentDataUtils.getContentPathFromRequest(request);
    String eTag = ControllerUtils.getEtagFromHeader(request);
    DataResourceUtils.patchResource(repositoryProperties, identifier, patch, eTag, patchDataResource);

    long currentVersion = auditService.getCurrentVersion(identifier);
    if (currentVersion > 0) {
      return ResponseEntity.noContent().header(VERSION_HEADER, Long.toString(currentVersion)).build();
    } else {
      return ResponseEntity.noContent().build();

    }
  }

  @Override
  public ResponseEntity put(@PathVariable("id") final String identifier,
          @RequestBody final DataResource newResource,
          final WebRequest request,
          final HttpServletResponse response) {
    Function<String, String> putWithId;
    putWithId = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).put(t, newResource, request, response)).toString();
    };
    DataResource result = DataResourceUtils.updateResource(repositoryProperties, identifier, newResource, request, putWithId);
    long currentVersion = repositoryProperties.getAuditService().getCurrentVersion(result.getId());

    if (currentVersion > 0) {
      //trigger response creation and set etag...the response body is set automatically
      return ResponseEntity.ok().eTag("\"" + result.getEtag() + "\"").header(VERSION_HEADER, Long.toString(currentVersion)).body(DataResourceUtils.filterResource(result));
    } else {
      return ResponseEntity.ok().eTag("\"" + result.getEtag() + "\"").body(DataResourceUtils.filterResource(result));
    }

  }

  @Override
  public ResponseEntity delete(@PathVariable("id") final String identifier,
          final WebRequest request,
          final HttpServletResponse response) {
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, 1l, request, response)).toString();
    };
    DataResourceUtils.deleteResource(repositoryProperties, identifier, request, getById);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity createContent(@PathVariable(value = "id") final String identifier,
          @RequestPart(name = "file", required = false) MultipartFile file,
          @RequestPart(name = "metadata", required = false) final ContentInformation contentInformation,
          @RequestParam(name = "force", defaultValue = "false") boolean force,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {
    Function<String, String> createContent = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).createContent(t, file, contentInformation, force, request, response, uriBuilder)).toString();
    };
    DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, identifier, null, createContent);
    String path = ContentDataUtils.getContentPathFromRequest(request);
    ContentInformation result = ContentDataUtils.addFile(repositoryProperties, resource, file, path, contentInformation, force, createContent);

    URI link = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(resource.getId(), null, 1l, null, request, response, uriBuilder)).toUri();

    URIBuilder builder = new URIBuilder(link);
    builder.setPath(builder.getPath().replace("**", path));
    URI resourceUri = null;

    try {
      resourceUri = builder.build();
    } catch (URISyntaxException ex) {
      LOGGER.error("Failed to create location URI for path " + path + ". However, resource should be created.", ex);
      throw new CustomInternalServerError("Resource creation successful, but unable to create resource linkfor path " + path + ".");
    }

    long currentVersion = contentAuditService.getCurrentVersion(Long.toString(result.getId()));
    if (currentVersion > 0) {
      return ResponseEntity.created(resourceUri).header(VERSION_HEADER, Long.toString(currentVersion)).build();
    } else {
      return ResponseEntity.created(resourceUri).build();
    }
  }

  @Override
  public ResponseEntity getContentMetadata(@PathVariable(value = "id") final String identifier,
          @RequestParam(name = "tag", required = false) final String tag,
          @RequestParam(name = "version", required = false) final Long version,
          final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {
    Function<String, String> getContentMetadata = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, tag, version, pgbl, request, response, uriBuilder)).toString();
    };
    //check resource and permission
    DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, identifier, null, getContentMetadata);
    String path = ContentDataUtils.getContentPathFromRequest(request);

    List<ContentInformation> result = ContentDataUtils.readFiles(repositoryProperties, resource, path, tag, version, pgbl, getContentMetadata);

    if (path.endsWith("/") || path.length() == 0) {
      LOGGER.trace("Obtained {} content information result(s).", result.size());
      return ResponseEntity.ok().body(ContentDataUtils.filterContentInformation(result));
    } else {
      LOGGER.trace("Obtained single content information result.");
      ContentInformation contentInformation = result.get(0);
      long currentVersion = contentAuditService.getCurrentVersion(Long.toString(contentInformation.getId()));
      if (currentVersion > 0) {
        return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").header(VERSION_HEADER, Long.toString(currentVersion)).body(ContentDataUtils.filterContentInformation(contentInformation));
      } else {
        return ResponseEntity.ok().eTag("\"" + resource.getEtag() + "\"").body(ContentDataUtils.filterContentInformation(contentInformation));
      }
    }
  }

  @Override
  public ResponseEntity<List<ContentInformation>> findContentMetadataByExample(@RequestBody final ContentInformation example,
          final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {

    PageRequest request = ControllerUtils.checkPaginationInformation(pgbl);
    Page<ContentInformation> page = contentInformationService.findByExample(example, AuthenticationHelper.getAuthorizationIdentities(),
            AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString()), pgbl);

    response.addHeader(CONTENT_RANGE_HEADER, ControllerUtils.getContentRangeHeader(page.getNumber(), request.getPageSize(), page.getTotalElements()));
    return ResponseEntity.ok().body(ContentDataUtils.filterContentInformation(page.getContent()));
  }

  @Override
  public ResponseEntity patchContentMetadata(@PathVariable(value = "id") final String identifier,
          final @RequestBody JsonPatch patch,
          final WebRequest request,
          final HttpServletResponse response) {
    Function<String, String> patchContentMetadata = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).patchContentMetadata(t, patch, request, response)).toString();
    };
    String path = ContentDataUtils.getContentPathFromRequest(request);
    String eTag = ControllerUtils.getEtagFromHeader(request);
    ContentInformation toUpdate = ContentDataUtils.patchContentInformation(repositoryProperties, identifier, path, patch, eTag, patchContentMetadata);

    long currentVersion = contentAuditService.getCurrentVersion(Long.toString(toUpdate.getId()));
    if (currentVersion > 0) {
      return ResponseEntity.noContent().header(VERSION_HEADER, Long.toString(currentVersion)).build();
    } else {
      return ResponseEntity.noContent().build();
    }
  }

  @Override
  public void getContent(@PathVariable(value = "id") final String identifier,
          @RequestParam(value = "version", required = false) Long version,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {
    String path = ContentDataUtils.getContentPathFromRequest(request);
    String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
    DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, identifier, null, (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, null, 1l, null, request, response, uriBuilder)).toString();
    });
    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);
    LOGGER.debug("Access to resource with identifier {} granted. Continue with content access.", resource.getId());
    contentInformationService.read(resource, path, version, acceptHeader, response);
  }

  @Override
  public ResponseEntity deleteContent(@PathVariable(value = "id")
          final String identifier,
          final WebRequest request,
          final HttpServletResponse response) {
    String path = ContentDataUtils.getContentPathFromRequest(request);
    String eTag = ControllerUtils.getEtagFromHeader(request);
    Function<String, String> deleteContent = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).deleteContent(t, request, response)).toString();
    };
    ContentDataUtils.deleteFile(repositoryProperties, identifier, path, eTag, deleteContent);

    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity getAuditInformation(@PathVariable("id") final String resourceIdentifier,
          final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder ucb) {
    LOGGER.trace("Performing getAuditInformation({}, {}).", resourceIdentifier, pgbl);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getById(t, null, request, response)).toString();
    };
    Optional<String> auditInformation = DataResourceUtils.getAuditInformation(repositoryProperties, resourceIdentifier, pgbl, getById);

    if (!auditInformation.isPresent()) {
      LOGGER.trace("No audit information found for resource {}. Returning empty JSON array.", resourceIdentifier);
      return ResponseEntity.ok().body("[]");
    }

    long currentVersion = auditService.getCurrentVersion(resourceIdentifier);

    LOGGER.trace("Audit information found, returning result.");
    return ResponseEntity.ok().header(VERSION_HEADER, Long.toString(currentVersion)).body(auditInformation.get());
  }

  @Override
  public ResponseEntity getContentAuditInformation(@PathVariable("id") final String resourceIdentifier,
          final Pageable pgbl,
          final WebRequest request,
          final HttpServletResponse response,
          final UriComponentsBuilder uriBuilder) {
    LOGGER.trace("Performing getContentAuditInformation({}, {}).", resourceIdentifier, pgbl);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getContentMetadata(t, null, null, pgbl, request, response, uriBuilder)).toString();
    };
    String path = ContentDataUtils.getContentPathFromRequest(request);
    //check resource and permission
    DataResource resource = DataResourceUtils.getResourceByIdentifierOrRedirect(repositoryProperties, resourceIdentifier, null, getById);

    DataResourceUtils.performPermissionCheck(resource, PERMISSION.READ);

    LOGGER.trace("Checking provided path {}.", path);
    if (path.startsWith("/")) {
      LOGGER.debug("Removing leading slash from path {}.", path);
      //remove leading slash if present, which should actually never happen
      path = path.substring(1);
    }

    //switch between collection and element listing
    if (path.endsWith("/") || path.length() == 0) {
      LOGGER.error("Path ends with slash or is empty. Obtaining audit information for collection elements is not supported.");
      throw new BadArgumentException("Provided path is invalid for obtaining audit information. Path must not be empty and must not end with a slash.");
    }
    LOGGER.trace("Path does not end with slash and/or is not empty. Assuming single element access.");
    ContentInformation contentInformation = contentInformationService.getContentInformation(resource.getId(), path, null);

    Optional<String> auditInformation = contentInformationService.getAuditInformationAsJson(Long.toString(contentInformation.getId()), pgbl);

    if (!auditInformation.isPresent()) {
      LOGGER.trace("No audit information found for resource {} and path {}. Returning empty JSON array.", resourceIdentifier, path);
      return ResponseEntity.ok().body("[]");
    }

    LOGGER.trace("Audit information found, returning result.");
    long currentVersion = contentAuditService.getCurrentVersion(Long.toString(contentInformation.getId()));

    return ResponseEntity.ok().header(VERSION_HEADER, Long.toString(currentVersion)).body(auditInformation.get());
  }
}
