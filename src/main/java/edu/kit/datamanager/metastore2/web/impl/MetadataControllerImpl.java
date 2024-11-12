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
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.entities.messaging.MetadataResourceMessage;
import edu.kit.datamanager.exceptions.AccessForbiddenException;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.domain.*;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.IMetadataController;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.*;
import edu.kit.datamanager.repo.domain.DataResource;
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
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Controller for metadata documents.
 * @deprecated Should be replaced by API v2 (api/v2/metadata/...)
 */
@Controller
@RequestMapping(value = "/api/v1/metadata")
@Tag(name = "Metadata Repository")
@Schema(description = "Metadata Resource Management")
@Deprecated(since = "2.0.0", forRemoval = true)
public class MetadataControllerImpl implements IMetadataController {

  public static final String POST_FILTER = "post_filter";
  /**
   * Placeholder string for id of resource. (landingpage)
   */
  public static final String PLACEHOLDER_ID = "$(id)";
  /**
   * Placeholder string for version of resource. (landingpage)
   */
  public static final String PLACEHOLDER_VERSION = "$(version)";

  private static final Logger LOG = LoggerFactory.getLogger(MetadataControllerImpl.class);

  private final ApplicationProperties applicationProperties;

  private final ILinkedMetadataRecordDao metadataRecordDao;

  private final MetastoreConfiguration metadataConfig;

  private final IDataResourceDao dataResourceDao;

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
   * @param dataResourceDao DAO for data resources.
   */
  public MetadataControllerImpl(ApplicationProperties applicationProperties,
          MetastoreConfiguration metadataConfig,
          ILinkedMetadataRecordDao metadataRecordDao,
          IDataResourceDao dataResourceDao) {
    this.applicationProperties = applicationProperties;
    this.metadataConfig = metadataConfig;
    this.metadataRecordDao = metadataRecordDao;
    this.dataResourceDao = dataResourceDao;
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

    long nano1 = System.nanoTime() / 1000000;
    LOG.trace("Performing createRecord({},...).", recordDocument);
    MetadataRecord metadataRecord;
    if (recordDocument == null || recordDocument.isEmpty()) {
      String message = "No metadata record provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      metadataRecord = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
    } catch (IOException ex) {
      String message = "No valid metadata record provided. Returning HTTP BAD_REQUEST.";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error("Error parsing json: ", ex);
      throw new BadArgumentException(message);
    }
    long nano2 = System.nanoTime() / 1000000;

    if (metadataRecord.getRelatedResource() == null || metadataRecord.getRelatedResource().getIdentifier() == null || metadataRecord.getSchema() == null || metadataRecord.getSchema().getIdentifier() == null) {
      LOG.error("Mandatory attributes relatedResource and/or schemaId not found in record. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes relatedResource and/or schemaId not found in record.");
    }

    LOG.debug("Test for existing metadata record for given schema and resource");
    ResourceIdentifier schemaIdentifier;
    try {
      schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(metadataConfig, metadataRecord);
    } catch (ResourceNotFoundException rnfe) {
      LOG.debug("Error checking for existing relations.", rnfe);
      throw new UnprocessableEntityException("Schema ID seems to be invalid");
    }
    boolean recordAlreadyExists = metadataRecordDao.existsMetadataRecordByRelatedResourceAndSchemaId(metadataRecord.getRelatedResource().getIdentifier(), schemaIdentifier.getIdentifier());
    long nano3 = System.nanoTime() / 1000000;

    if (recordAlreadyExists) {
      String message = String.format("Conflict! There is already a metadata document with "
              + "the same schema ('%s') and the same related resource ('%s')",
              metadataRecord.getSchemaId(),
              metadataRecord.getRelatedResource().getIdentifier());
      LOG.error(message);
      return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(metadataConfig, recordDocument, document);
    // Successfully created metadata record.
    long nano4 = System.nanoTime() / 1000000;
    LOG.trace("Metadata record successfully persisted. Returning result.");
    MetadataRecordUtil.fixMetadataDocumentUri(result);
    long nano5 = System.nanoTime() / 1000000;
    metadataRecordDao.save(new LinkedMetadataRecord(result));
    long nano6 = System.nanoTime() / 1000000;

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(MetadataControllerImplV2.class).getRecordById(result.getId(), result.getRecordVersion(), null, null)).toUri();
    long nano7 = System.nanoTime() / 1000000;
    LOG.info("Create Record Service, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1, nano6 - nano1, nano7 - nano1);

    LOG.trace("Sending CREATE event.");
    messagingService.orElse(new LogfileMessagingService()).
            send(MetadataResourceMessage.factoryCreateMetadataMessage(result, AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));

    return ResponseEntity.created(locationUri).eTag("\"" + result.getEtag() + "\"").body(result);
  }

  @Override
  public ResponseEntity<MetadataRecord> getRecordById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getRecordById({}, {}).", id, version);

    LOG.trace("Obtaining metadata record with id {} and version {}.", id, version);
    MetadataRecord metadataRecord = MetadataRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version, true);
    LOG.trace("Metadata record found. Prepare response.");
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    LOG.trace("Get ETag of MetadataRecord.");
    String etag = metadataRecord.getEtag();
    MetadataRecordUtil.fixMetadataDocumentUri(metadataRecord);

    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(metadataRecord);
  }


  @Override
  public ResponseEntity<ElasticWrapper> getAclById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getAclById({}, {}).", id, version);
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

  public ResponseEntity<List<MetadataRecord>> getAllVersions(
          @PathVariable(value = "id") String id,
          Pageable pgbl
  ) {
    LOG.trace("Performing getAllVersions({}).", id);
    // Search for resource type of MetadataSchemaRecord

    //if security is enabled, include principal in query
    LOG.debug("Performing query for records.");
    MetadataRecord recordByIdAndVersion = MetadataRecordUtil.getRecordByIdAndVersion(metadataConfig, id);
    List<MetadataRecord> recordList = new ArrayList<>();
    long totalNoOfElements = recordByIdAndVersion.getRecordVersion();
    for (long version = totalNoOfElements - pgbl.getOffset(), size = 0; version > 0 && size < pgbl.getPageSize(); version--, size++) {
      recordList.add(MetadataRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version));
    }

    LOG.trace("Transforming Dataresource to MetadataRecord");
    List<MetadataRecord> metadataList = new ArrayList<>();
    recordList.forEach(metadataRecord -> {
      MetadataRecordUtil.fixMetadataDocumentUri(metadataRecord);
      metadataList.add(metadataRecord);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), totalNoOfElements);

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(metadataList);
  }

  @Override
  public ResponseEntity<List<MetadataRecord>> getRecords(
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
    LOG.trace("Performing getRecords({}, {}, {}, {}).", relatedIds, schemaIds, updateFrom, updateUntil);
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

    if (LOG.isTraceEnabled()) {
      Page<DataResource> records = dataResourceDao.findAll(pgbl);
      LOG.trace("List all data resources...");
      LOG.trace("-----------------------------------------------");
      for (DataResource item : records.getContent()) {
        LOG.trace("- '{}'", item);
      }
      LOG.trace("-----------------------------------------------");
      LOG.trace("Specification: '{}'", spec);
    }
    LOG.debug("Performing query for records.");
    Page<DataResource> records = dataResourceDao.findAll(spec, pgbl);

    LOG.trace("Transforming Dataresource to MetadataRecord");
    List<DataResource> recordList = records.getContent();
    List<MetadataRecord> metadataList = new ArrayList<>();
    recordList.forEach(metadataRecord -> {
      MetadataRecord item = MetadataRecordUtil.migrateToMetadataRecord(metadataConfig, metadataRecord, false);
      MetadataRecordUtil.fixMetadataDocumentUri(item);
      metadataList.add(item);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(metadataList);
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
    MetadataRecord updateMetadataRecord = MetadataRecordUtil.updateMetadataRecord(metadataConfig, id, eTag, metadataRecord, document, getById);

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    String etag = updateMetadataRecord.getEtag();
    DataResourceRecordUtil.fixMetadataDocumentUri(updateMetadataRecord);

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(updateMetadataRecord.getId(), updateMetadataRecord.getRecordVersion(), null, null)).toUri();

    LOG.trace("Sending UPDATE event.");
    messagingService.orElse(new LogfileMessagingService()).
            send(MetadataResourceMessage.factoryUpdateMetadataMessage(updateMetadataRecord, AuthenticationHelper.getPrincipal(), ControllerUtils.getLocalHostname()));

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updateMetadataRecord);
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
    MetadataRecordUtil.deleteMetadataRecord(metadataConfig, id, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public void contribute(Info.Builder builder) {
    LOG.trace("Check for MetadataRepo actuator information (v1)...");
    LOG.trace("Check for MetadataRepo actuator information (v1) disabled!");
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
