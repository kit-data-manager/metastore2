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

import com.fasterxml.jackson.core.JsonParseException;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.entities.messaging.MetadataResourceMessage;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.metastore2.util.ActuatorUtil;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.web.IMetadataControllerV2;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.PermissionSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.StateSpecification;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.service.impl.LogfileMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

import static edu.kit.datamanager.entities.Identifier.IDENTIFIER_TYPE.INTERNAL;
import edu.kit.datamanager.metastore2.domain.ElasticWrapper;

/**
 * Controller for metadata documents.
 */
@Controller
@RequestMapping(value = "/api/v2/metadata")
@Tag(name = "Metadata Repository")
@Schema(description = "Metadata Resource Management")
public class MetadataControllerImplV2 implements IMetadataControllerV2 {

  public static final String POST_FILTER = "post_filter";
  /**
   * Placeholder string for id of resource. (landingpage)
   */
  public static final String PLACEHOLDER_ID = "$(id)";
  /**
   * Placeholder string for version of resource. (landingpage)
   */
  public static final String PLACEHOLDER_VERSION = "$(version)";

  private static final Logger LOG = LoggerFactory.getLogger(MetadataControllerImplV2.class);

  private final ApplicationProperties applicationProperties;

  private final MetastoreConfiguration metadataConfig;

  private final ISchemaRecordDao schemaRecordDao;

  /**
   * Optional messagingService bean may or may not be available, depending on a
   * service's configuration. If messaging capabilities are disabled, this bean
   * should be not available. In that case, messages are only logged.
   */
  @Autowired
  private Optional<IMessagingService> messagingService;

  private final String guestToken;

  /**
   * Constructor for metadata documents controller.
   *
   * @param applicationProperties Configuration for controller.
   * @param metadataConfig Configuration for metadata documents repository.
   * @param metadataRecordDao DAO for metadata records.
   * @param schemaRecordDao  DAO for schema records.
   */
  public MetadataControllerImplV2(ApplicationProperties applicationProperties,
          MetastoreConfiguration metadataConfig,
          ILinkedMetadataRecordDao metadataRecordDao,
          ISchemaRecordDao schemaRecordDao) {
    this.applicationProperties = applicationProperties;
    this.metadataConfig = metadataConfig;
    this.schemaRecordDao = schemaRecordDao;
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", this.metadataConfig);
    LOG.info("------------------------------------------------------");
    LOG.trace("Create guest token");
    guestToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("guest", RepoUserRole.GUEST).
            addSimpleClaim("email", "metastore@localhost").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());
    MetadataRecordUtil.setToken(guestToken);
  }

  @Override
  public ResponseEntity createRecord(
          @RequestPart(name = "record") final MultipartFile recordDocument,
          @RequestPart(name = "document") final MultipartFile document,
          HttpServletRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder) throws URISyntaxException {

    LOG.trace("Performing createRecord({},...).", recordDocument);
    DataResource metadataRecord;
    if (recordDocument == null || recordDocument.isEmpty()) {
      String message = "No data resource record provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      metadataRecord = Json.mapper().readValue(recordDocument.getInputStream(), DataResource.class);
    } catch (IOException ex) {
      String message = "No valid data resource record provided. Returning HTTP BAD_REQUEST.";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error("Error parsing json: ", ex);
      throw new BadArgumentException(message);
    }

    DataResourceRecordUtil.validateRelatedResources4MetadataDocuments(metadataRecord);

    LOG.debug("Test for existing metadata record for given schema and resource");
    RelatedIdentifier schemaIdentifier;
    schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(metadataRecord);
    switch (schemaIdentifier.getIdentifierType()) {
      case INTERNAL:
        // nothing to do
        break;
      case URL:
        SchemaRecord schemaRecord = schemaRecordDao.findByAlternateId(schemaIdentifier.getValue());
        if (schemaRecord == null) {
          String message = "External URLs are not supported yet!\n"
                  + "But '" + schemaIdentifier.getValue() + "' seems not to be an internal one!\n"
                  + "Hint: Maybe version number is missing (e.g.: [...]?version=1";
          LOG.error(message);
          throw new ResourceNotFoundException(message);
        }
        schemaIdentifier.setValue(schemaRecord.getSchemaId());
        schemaIdentifier.setIdentifierType(INTERNAL);
        break;
      default:
        throw new UnprocessableEntityException("Schema referenced by '" + schemaIdentifier.getIdentifierType().toString() + "' is not supported yet!");
    }

    DataResource result = DataResourceRecordUtil.createDataResourceRecord4Metadata(metadataConfig, recordDocument, document);
    LOG.trace("Get dataresource: '{}'", result);
    String eTag = result.getEtag();
    LOG.trace("Get ETag: ' {}'", eTag);
    // Successfully created metadata record.
    LOG.trace("Metadata record successfully persisted. Returning result.");
    DataResourceRecordUtil.fixSchemaUrl(result);

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(result.getId(), Long.valueOf(result.getVersion()), null, null)).toUri();

    LOG.trace("Sending CREATE event.");
    messagingService.orElse(new LogfileMessagingService()).
            send(MetadataResourceMessage.factoryCreateMetadataMessage(result, AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));

    return ResponseEntity.created(locationUri).eTag("\"" + eTag + "\"").body(result);
  }

  @Override
  public ResponseEntity<DataResource> getRecordById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getRecordById({}, {}).", id, version);

    LOG.trace("Obtaining metadata record with id {} and version {}.", id, version);
    DataResource metadataRecord = DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version);
    LOG.trace("Metadata record found. Prepare response.");
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    LOG.trace("Get ETag of DataResource.");
    LOG.trace("Get dataresource: '{}'", metadataRecord);
    String etag = metadataRecord.getEtag();
    LOG.trace("Get ETag: ' {}'", etag);
    DataResourceRecordUtil.fixSchemaUrl(metadataRecord);
    URI locationUri;
    locationUri = DataResourceRecordUtil.getMetadataDocumentUri(metadataRecord.getId(), metadataRecord.getVersion());

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(metadataRecord);
  }

  @Override
  public ResponseEntity<ContentInformation> getContentInformationById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getContentInformationById({}, {}).", id, version);

    LOG.trace("Obtaining metadata record with id {} and version {}.", id, version);
    ContentInformation contentInformation = DataResourceRecordUtil.getContentInformationByIdAndVersion(metadataConfig, id, version);
    LOG.trace("ContentInformation record found. Prepare response...");
    DataResource minimalDataResource = DataResource.factoryNewDataResource(contentInformation.getParentResource().getId());
    URI locationUri;
    locationUri = DataResourceRecordUtil.getMetadataDocumentUri(id, contentInformation.getVersion().toString());
    contentInformation.setParentResource(minimalDataResource);
    contentInformation.setContentUri(locationUri.toString());
    contentInformation.setRelativePath(null);
    contentInformation.setVersioningService(null);

    return ResponseEntity.ok().body(contentInformation);
  }

  @Override
  public ResponseEntity<ElasticWrapper> getAclById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.info("Performing getAclById({}, {}).", id, version);
    if (!AuthenticationHelper.isAuthenticatedAsService()) {
      throw new AccessForbiddenException("Only for services!");
    }

    DataResource metadataRecord = DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version);
    DataResourceRecordUtil.fixSchemaUrl(metadataRecord);
    ElasticWrapper aclRecord = new ElasticWrapper(metadataRecord);

    return ResponseEntity.ok().body(aclRecord);
  }

  @Override
  public ResponseEntity getMetadataDocumentById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getMetadataDocumentById({}, {}).", id, version);

    Path metadataDocumentPath = DataResourceRecordUtil.getMetadataDocumentByIdAndVersion(metadataConfig, id, version);

    return ResponseEntity.
            ok().
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadataDocumentPath.toFile().length())).
            body(new FileSystemResource(metadataDocumentPath.toFile()));
  }

  @Override
  public ModelAndView getLandingpageById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing Landing page for metadata document with ({}, {}).", id, version);
    String redirectUrl = applicationProperties.getMetadataLandingPage();
    redirectUrl = redirectUrl.replace(PLACEHOLDER_ID, id);
    String versionString = "";
    if (version != null) {
      versionString = version.toString();
    }
    redirectUrl = "redirect:" + redirectUrl.replace(PLACEHOLDER_VERSION, versionString);

    LOG.trace("Redirect to '{}'", redirectUrl);

    return new ModelAndView(redirectUrl);
  }

  public ResponseEntity<List<DataResource>> getAllVersions(
          @PathVariable(value = "id") String id,
          Pageable pgbl
  ) {
    LOG.trace("Performing getAllVersions({}).", id);
    // Search for resource type of MetadataSchemaRecord

    //if security is enabled, include principal in query
    LOG.debug("Performing query for records.");
    DataResource recordByIdAndVersion = DataResourceRecordUtil.getRecordById(metadataConfig, id);
    List<DataResource> recordList = new ArrayList<>();
    long totalNoOfElements = Long.parseLong(recordByIdAndVersion.getVersion());
    for (long version = totalNoOfElements - pgbl.getOffset(), size = 0; version > 0 && size < pgbl.getPageSize(); version--, size++) {
      recordList.add(DataResourceRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version));
    }

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), totalNoOfElements);

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(recordList);
  }

  @Override
  public ResponseEntity<List<DataResource>> getRecords(
          @RequestParam(value = "id", required = false) String id,
          @RequestParam(value = "resourceId", required = false) List<String> relatedIds,
          @RequestParam(value = "schemaId", required = false) List<String> schemaIds,
          @RequestParam(name = "from", required = false) Instant updateFrom,
          @RequestParam(name = "until", required = false) Instant updateUntil,
          Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb
  ) {
    LOG.trace("Performing getRecords({}, {}, {}, {}, {}).", id, relatedIds, schemaIds, updateFrom, updateUntil);
    if (id != null) {
      return getAllVersions(id, pgbl);
    }
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(DataResourceRecordUtil.METADATA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    // Add authentication if enabled
    spec = DataResourceRecordUtil.findByAccessRights(spec);
    spec = DataResourceRecordUtil.findBySchemaId(spec, schemaIds);
    spec = DataResourceRecordUtil.findByRelatedId(spec, relatedIds);

    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }

    // Hide revoked and gone data resources. 
    DataResource.State[] states = {DataResource.State.FIXED, DataResource.State.VOLATILE};
    List<DataResource.State> stateList = Arrays.asList(states);
    spec = spec.and(StateSpecification.toSpecification(stateList));

    Page<DataResource> records = DataResourceRecordUtil.queryDataResources(spec, pgbl);

    LOG.trace("Transforming Dataresource to DataResource");
    List<DataResource> recordList = records.getContent();
    for (DataResource item : recordList) {
      DataResourceRecordUtil.fixSchemaUrl(item);
    }

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(recordList);
  }

  @Override
  public ResponseEntity updateRecord(
          @PathVariable("id") String id,
          @RequestPart(name = "record", required = false) MultipartFile metadataRecord,
          @RequestPart(name = "document", required = false) final MultipartFile document,
          WebRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder
  ) {
    LOG.trace("Performing updateRecord({}, {}, {}).", id, metadataRecord, "#document");
    UnaryOperator<String> getById;
    getById = t -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, response)).toString();
    String eTag = ControllerUtils.getEtagFromHeader(request);
    DataResource updateDataResource = DataResourceRecordUtil.updateDataResource4MetadataDocument(metadataConfig, id, eTag, metadataRecord, document, getById);

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    String etag = updateDataResource.getEtag();
    DataResourceRecordUtil.fixSchemaUrl(updateDataResource);

    URI locationUri;
    locationUri = DataResourceRecordUtil.getMetadataDocumentUri(updateDataResource.getId(), updateDataResource.getVersion());

    LOG.trace("Sending UPDATE event.");
    messagingService.orElse(new LogfileMessagingService()).
            send(MetadataResourceMessage.factoryUpdateMetadataMessage(updateDataResource, AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updateDataResource);
  }

  @Override
  public ResponseEntity deleteRecord(
          @PathVariable(value = "id") String id,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing deleteRecord({}).", id);
    UnaryOperator<String> getById;
    getById = t -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, wr, hsr)).toString();

    String eTag = ControllerUtils.getEtagFromHeader(wr);
    DataResourceRecordUtil.deleteDataResourceRecord(metadataConfig, id, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public void contribute(Info.Builder builder) {
    LOG.trace("Check for MetadataRepo actuator information...");

    URL basePath = metadataConfig.getBasepath();
    Map<String, String> details = ActuatorUtil.testDirectory(basePath);

    if (!details.isEmpty()) {
      details.put("No of metadata documents", Long.toString(DataResourceRecordUtil.getNoOfMetadataDocuments()));
      builder.withDetail("metadataRepo", details);
    }
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
