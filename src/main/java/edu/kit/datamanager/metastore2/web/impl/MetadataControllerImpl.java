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
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.domain.LinkedMetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.IMetadataController;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.PermissionSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.RelatedIdentifierSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/metadata")
@Tag(name = "Metadata Repository")
@Schema(description = "Metadata Resource Management")
public class MetadataControllerImpl implements IMetadataController {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataControllerImpl.class);
  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private ILinkedMetadataRecordDao metadataRecordDao;

  private final MetastoreConfiguration metadataConfig;
  @Autowired
  private final IDataResourceDao dataResourceDao;

  private final String guestToken;

  /**
   *
   * @param applicationProperties
   * @param metadataConfig
   * @param metadataRecordDao
   * @param dataResourceDao
   */
  public MetadataControllerImpl(ApplicationProperties applicationProperties,
          MetastoreConfiguration metadataConfig,
          ILinkedMetadataRecordDao metadataRecordDao,
          IDataResourceDao dataResourceDao) {
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
    MetadataRecord record;
    if (recordDocument == null || recordDocument.isEmpty()) {
      String message = "No metadata record provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    try {
      record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
    } catch (IOException ex) {
      String message = "No valid metadata record provided. Returning HTTP BAD_REQUEST.";
      if (ex instanceof JsonParseException) {
        message = message + " Reason: " + ex.getMessage();
      }
      LOG.error("Error parsing json: ", ex);
      throw new BadArgumentException(message);
    }
    long nano2 = System.nanoTime() / 1000000;

    if (record.getRelatedResource() == null || record.getRelatedResource().getIdentifier() == null || record.getSchema() == null || record.getSchema().getIdentifier() == null) {
      LOG.error("Mandatory attributes relatedResource and/or schemaId not found in record. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes relatedResource and/or schemaId not found in record.");
    }

    LOG.debug("Test for existing metadata record for given schema and resource");
    ResourceIdentifier schemaIdentifier;
    try {
      schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(metadataConfig, record);
    } catch (ResourceNotFoundException rnfe) {
      LOG.debug("Error checking for existing relations.", rnfe);
      throw new UnprocessableEntityException("Schema ID seems to be invalid");
    }
    boolean recordAlreadyExists = metadataRecordDao.existsMetadataRecordByRelatedResourceAndSchemaId(record.getRelatedResource().getIdentifier(), schemaIdentifier.getIdentifier());
    long nano3 = System.nanoTime() / 1000000;

    if (recordAlreadyExists) {
      LOG.error("Conflict with existing metadata record!");
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Metadata record already exists! Please update existing record instead!");
    }
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(metadataConfig, recordDocument, document);
    // Successfully created metadata record.
    long nano4 = System.nanoTime() / 1000000;
    LOG.trace("Metadata record successfully persisted. Returning result.");
    fixMetadataDocumentUri(result);
    long nano5 = System.nanoTime() / 1000000;
    metadataRecordDao.save(new LinkedMetadataRecord(result));
    long nano6 = System.nanoTime() / 1000000;

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(result.getId(), result.getRecordVersion(), null, null)).toUri();
    long nano7 = System.nanoTime() / 1000000;
    LOG.info("Create Record Service, {}, {}, {}, {}, {}, {}, {}", nano1, nano2 - nano1, nano3 - nano1, nano4 - nano1, nano5 - nano1, nano6 - nano1, nano7 - nano1);

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
    MetadataRecord record = MetadataRecordUtil.getRecordByIdAndVersion(metadataConfig, id, version, true);
    LOG.trace("Metadata record found. Prepare response.");
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    LOG.trace("Get ETag of MetadataRecord.");
    String etag = record.getEtag();
    fixMetadataDocumentUri(record);

    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(record);
  }

  @Override
  public ResponseEntity getMetadataDocumentById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing getMetadataDocumentById({}, {}).", id, version);

    Path metadataDocumentPath = MetadataRecordUtil.getMetadataDocumentByIdAndVersion(metadataConfig, id, version);

    return ResponseEntity.
            ok().
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadataDocumentPath.toFile().length())).
            body(new FileSystemResource(metadataDocumentPath.toFile()));
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
    recordList.forEach((record) -> {
      fixMetadataDocumentUri(record);
      metadataList.add(record);
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
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataRecord.RESOURCE_TYPE));
    // Add authentication if enabled
    if (metadataConfig.isAuthEnabled()) {
      boolean isAdmin;
      isAdmin = AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString());
      // Add authorization for non administrators
      if (!isAdmin) {
        List<String> authorizationIdentities = AuthenticationHelper.getAuthorizationIdentities();
        if (authorizationIdentities != null) {
          LOG.trace("Creating (READ) permission specification.");
          authorizationIdentities.add(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL);
          Specification<DataResource> permissionSpec = PermissionSpecification.toSpecification(authorizationIdentities, PERMISSION.READ);
          spec = spec.and(permissionSpec);
        } else {
          LOG.trace("No permission information provided. Skip creating permission specification.");
        }
      }
    }
    List<String> allRelatedIdentifiers = new ArrayList<>();
//    File file = new File(new URIoa)
    if (schemaIds != null) {
      for (String schemaId : schemaIds) {
        MetadataSchemaRecord currentSchemaRecord;
        try {
          currentSchemaRecord = MetadataRecordUtil.getCurrentInternalSchemaRecord(metadataConfig, schemaId);
          // Test for internal URI -> Transform to global URI.
          if (currentSchemaRecord.getSchemaDocumentUri().startsWith("file:")) {
            ResourceIdentifier schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(metadataConfig, currentSchemaRecord);
            currentSchemaRecord.setSchemaDocumentUri(schemaIdentifier.getIdentifier());
          }
          allRelatedIdentifiers.add(currentSchemaRecord.getSchemaDocumentUri());
        } catch (ResourceNotFoundException rnfe) {
          //  schemaID not found set version to 1
          currentSchemaRecord = new MetadataSchemaRecord();
          currentSchemaRecord.setSchemaVersion(1l);
          allRelatedIdentifiers.add("UNKNOWN_SCHEMA_ID");
        }
        for (long versionNumber = 1; versionNumber < currentSchemaRecord.getSchemaVersion(); versionNumber++) {
          MetadataSchemaRecord schemaRecord = MetadataRecordUtil.getInternalSchemaRecord(metadataConfig, schemaId, versionNumber);
          // Test for internal URI -> Transform to global URI.
          if (schemaRecord.getSchemaDocumentUri().startsWith("file:")) {
            ResourceIdentifier schemaIdentifier = MetadataSchemaRecordUtil.getSchemaIdentifier(metadataConfig, schemaRecord);
            schemaRecord.setSchemaDocumentUri(schemaIdentifier.getIdentifier());
          }
          allRelatedIdentifiers.add(schemaRecord.getSchemaDocumentUri());
        }
      }
    }
    if (relatedIds != null) {
      allRelatedIdentifiers.addAll(relatedIds);
    }
    if (!allRelatedIdentifiers.isEmpty()) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("---------------------------------------------------------");
        for (String relatedId : allRelatedIdentifiers) {
          LOG.trace("Look for related Identifier: '{}'", relatedId);
        }
        LOG.trace("---------------------------------------------------------");
      }
      Specification<DataResource> toSpecification = RelatedIdentifierSpec.toSpecification(allRelatedIdentifiers.toArray(new String[allRelatedIdentifiers.size()]));
      spec = spec.and(toSpecification);
    }
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }

    //if security is enabled, include principal in query
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
    recordList.forEach((record) -> {
      MetadataRecord item = MetadataRecordUtil.migrateToMetadataRecord(metadataConfig, record, false);
      fixMetadataDocumentUri(item);
      metadataList.add(item);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(metadataList);
  }

  @Override
  public ResponseEntity updateRecord(
          @PathVariable("id") String id,
          @RequestPart(name = "record", required = false) MultipartFile record,
          @RequestPart(name = "document", required = false) final MultipartFile document,
          WebRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder
  ) {
    LOG.trace("Performing updateRecord({}, {}, {}).", id, record, "#document");
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, response)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(request);
    MetadataRecord updateMetadataRecord = MetadataRecordUtil.updateMetadataRecord(metadataConfig, id, eTag, record, document, getById);

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    String etag = updateMetadataRecord.getEtag();
    fixMetadataDocumentUri(updateMetadataRecord);

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(updateMetadataRecord.getId(), updateMetadataRecord.getRecordVersion(), null, null)).toUri();

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updateMetadataRecord);
  }

  @Override
  public ResponseEntity deleteRecord(
          @PathVariable(value = "id") String id,
          WebRequest wr,
          HttpServletResponse hsr
  ) {
    LOG.trace("Performing deleteRecord({}).", id);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, wr, hsr)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(wr);
    MetadataRecordUtil.deleteMetadataRecord(metadataConfig, id, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  private void fixMetadataDocumentUri(MetadataRecord record) {
    String metadataDocumentUri = record.getMetadataDocumentUri();
    record.setMetadataDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getMetadataDocumentById(record.getId(), record.getRecordVersion(), null, null)).toUri().toString());
    LOG.trace("Fix metadata document Uri '{}' -> '{}'", metadataDocumentUri, record.getMetadataDocumentUri());
  }
}
