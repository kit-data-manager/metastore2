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

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.Url2Path;
import edu.kit.datamanager.metastore2.util.ActuatorUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.PermissionSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.StateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.TitleSpec;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * Controller for schema documents.
 */
@Controller
@RequestMapping(value = "/api/v1/schemas")
@Tag(name = "Schema Registry")
@Schema(description = "Schema Registry")
public class SchemaRegistryControllerImpl implements ISchemaRegistryController {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImpl.class);

  @Autowired
  private final MetastoreConfiguration schemaConfig;
  @Autowired
  private final IDataResourceDao dataResourceDao;
  @Autowired
  private final IUrl2PathDao url2PathDao;

    /**
   * Constructor for schema documents controller.
   *
   * @param schemaConfig Configuration for metadata documents repository.
   * @param dataResourceDao DAO for data resources.
   * @param url2PathDao DAO for storing url and linked path 
   */
  public SchemaRegistryControllerImpl(MetastoreConfiguration schemaConfig,
          IDataResourceDao dataResourceDao,
          IUrl2PathDao url2PathDao) {
    this.schemaConfig = schemaConfig;
    this.dataResourceDao = dataResourceDao;
    this.url2PathDao = url2PathDao;
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", schemaConfig);
    LOG.info("------------------------------------------------------");
  }

  @Override
  public ResponseEntity createRecord(
          @RequestPart(name = "record") final MultipartFile recordDocument,
          @RequestPart(name = "schema") MultipartFile document,
          HttpServletRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder) {
    LOG.trace("Performing createRecord({},....", recordDocument);
    BiFunction<String, Long, String> getSchemaDocumentById;
    getSchemaDocumentById = (schema, version) -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(schema, version, null, null)).toString();

    MetadataSchemaRecord schemaRecord = MetadataSchemaRecordUtil.createMetadataSchemaRecord(schemaConfig, recordDocument, document, getSchemaDocumentById);
    LOG.trace("Schema record successfully persisted. Returning result.");
    String etag = schemaRecord.getEtag();

    LOG.trace("Schema record successfully persisted. Updating document URI.");
    fixSchemaDocumentUri(schemaRecord, true);
    URI locationUri;
    locationUri = getSchemaDocumentUri(schemaRecord);
    LOG.warn("location uri              " + locationUri);
    return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(schemaRecord);
  }

  @Override
  public ResponseEntity getRecordById(
          @PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getRecordById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord schemaRecord = MetadataSchemaRecordUtil.getRecordByIdAndVersion(schemaConfig, schemaId, version, true);
    String etag = schemaRecord.getEtag();

    fixSchemaDocumentUri(schemaRecord);
    LOG.trace("Document URI successfully updated. Returning result.");
    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(schemaRecord);
  }

  @Override
  public ResponseEntity getSchemaDocumentById(
          @PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getSchemaDocumentById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord schemaRecord = MetadataSchemaRecordUtil.getRecordByIdAndVersion(schemaConfig, schemaId, version);
    URI schemaDocumentUri = URI.create(schemaRecord.getSchemaDocumentUri());

    MediaType contentType = MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(schemaRecord.getType()) ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
    Path schemaDocumentPath = Paths.get(schemaDocumentUri);
    if (!Files.exists(schemaDocumentPath) || !Files.isRegularFile(schemaDocumentPath) || !Files.isReadable(schemaDocumentPath)) {
      LOG.trace("Schema document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", schemaDocumentPath);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Schema document on server either does not exist or is no file or is not readable.");
    }

    return ResponseEntity.
            ok().
            contentType(contentType).
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(schemaDocumentPath.toFile().length())).
            body(new FileSystemResource(schemaDocumentPath.toFile()));
  }

  public ResponseEntity<List<MetadataSchemaRecord>> getAllVersions(
          String id,
          Pageable pgbl
  ) {
    LOG.trace("Performing getAllVersions({}).", id);
    // Search for resource type of MetadataSchemaRecord

    //if security is enabled, include principal in query
    LOG.debug("Performing query for records.");
  MetadataSchemaRecord recordByIdAndVersion = MetadataSchemaRecordUtil.getRecordById(schemaConfig, id);
    List<MetadataSchemaRecord> recordList = new ArrayList<>();
    long totalNoOfElements = recordByIdAndVersion.getSchemaVersion();
    for (long version = totalNoOfElements - pgbl.getOffset(), size = 0; version > 0 && size < pgbl.getPageSize(); version--, size++) {
      recordList.add(MetadataSchemaRecordUtil.getRecordByIdAndVersion(schemaConfig, id, version));
    }

    LOG.trace("Transforming Dataresource to MetadataRecord");
    List<MetadataSchemaRecord> metadataList = new ArrayList<>();
    recordList.forEach(schemaRecord -> {
      fixSchemaDocumentUri(schemaRecord);
      metadataList.add(schemaRecord);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), totalNoOfElements);

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(metadataList);
  }

  @Override
  public ResponseEntity validate(@PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version, 
          MultipartFile document, 
          WebRequest wr, 
          HttpServletResponse hsr) {
    LOG.trace("Performing validate({}, {}, {}).", schemaId, version, "#document");
    MetadataSchemaRecordUtil.validateMetadataDocument(schemaConfig, document, schemaId, version);
    LOG.trace("Metadata document validation succeeded. Returning HTTP NOT_CONTENT.");
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(@RequestParam(value = "schemaId", required = false)  String schemaId,
          @RequestParam(value = "mimeType", required = false) List<String> mimeTypes,
          @RequestParam(name = "from", required = false) Instant updateFrom,
          @RequestParam(name = "until", required = false) Instant updateUntil,
          Pageable pgbl, 
          WebRequest wr, 
          HttpServletResponse hsr, 
          UriComponentsBuilder ucb) {
    LOG.trace("Performing getRecords({}, {}, {}, {}).", schemaId, mimeTypes, updateFrom, updateUntil);
    // if schemaId is given return all versions 
    if (schemaId != null) {
      return getAllVersions(schemaId, pgbl);
    }
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE));
    // Add authentication if enabled
    if (schemaConfig.isAuthEnabled()) {
      boolean isAdmin;
      isAdmin = AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString());
      // Add authorization for non administrators
      if (!isAdmin) {
        List<String> authorizationIdentities = AuthenticationHelper.getAuthorizationIdentities();
        if (authorizationIdentities != null) {
        LOG.trace("Creating (READ) permission specification.");
          Specification<DataResource> permissionSpec = PermissionSpecification.toSpecification(authorizationIdentities, PERMISSION.READ);
          spec = spec.and(permissionSpec);
        } else {
          LOG.trace("No permission information provided. Skip creating permission specification.");
        }
      }
    }
    //one of given mimetypes.
    if ((mimeTypes != null) && !mimeTypes.isEmpty()) {
      spec = spec.and(TitleSpec.toSpecification(mimeTypes.toArray(new String[mimeTypes.size()])));
    }
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }
    // Hide revoked and gone data resources. 
    DataResource.State[] states = {DataResource.State.FIXED, DataResource.State.VOLATILE};
    List<DataResource.State> stateList = Arrays.asList(states);
    spec = spec.and(StateSpecification.toSpecification(stateList));

    LOG.debug("Performing query for records.");
    Page<DataResource> records = null;
    try {
      records = dataResourceDao.findAll(spec, pgbl);
    } catch (Exception ex) {
      LOG.error("Error find metadata records by specification!", ex);
      throw ex;
    }
    List<DataResource> recordList = records.getContent();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Cleaning up schemaDocumentUri of query result.");
      for (DataResource item : recordList) {
        LOG.trace("---> " + item.toString());
      }
    }
    List<MetadataSchemaRecord> schemaList = new ArrayList<>();
    recordList.forEach(schemaRecord -> {
      MetadataSchemaRecord item = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(schemaConfig, schemaRecord, false);
      fixSchemaDocumentUri(item);
      schemaList.add(item);
      if (LOG.isTraceEnabled()) {
        LOG.trace("===> " + item.toString());
      }
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(schemaList);
  }

  @Override
  public ResponseEntity updateRecord(@PathVariable("schemaId") final String schemaId,
          @RequestPart(name = "record", required = false) MultipartFile schemaRecord,
          @RequestPart(name = "schema", required = false) final MultipartFile document,
          final WebRequest request, final HttpServletResponse response) {
    LOG.trace("Performing updateRecord({}, {}).", schemaId, schemaRecord);
    UnaryOperator<String> getById;
    getById = t -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, response)).toString();
    String eTag = ControllerUtils.getEtagFromHeader(request);
    MetadataSchemaRecord updatedSchemaRecord = MetadataSchemaRecordUtil.updateMetadataSchemaRecord(schemaConfig, schemaId, eTag, schemaRecord, document, getById);

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    String etag = updatedSchemaRecord.getEtag();
    fixSchemaDocumentUri(updatedSchemaRecord, true);
    // Fix Url for OAI PMH entry
    MetadataSchemaRecordUtil.updateMetadataFormat(updatedSchemaRecord);

    URI locationUri;
    locationUri = getSchemaDocumentUri(updatedSchemaRecord);
    LOG.trace("Set locationUri to '{}'", locationUri.toString());
    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updatedSchemaRecord);
  }

  @Override
  public ResponseEntity deleteRecord(@PathVariable("schemaId") final String schemaId,
          WebRequest request, 
          HttpServletResponse hsr) {
    LOG.trace("Performing deleteRecord({}).", schemaId);
    UnaryOperator<String> getById;
    getById = t -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, hsr)).toString();
    String eTag = ControllerUtils.getEtagFromHeader(request);

    MetadataSchemaRecordUtil.deleteMetadataSchemaRecord(schemaConfig, schemaId, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public void contribute(Info.Builder builder) {
    LOG.trace("Check for SchemaRepo actuator information...");

    URL basePath = schemaConfig.getBasepath();
    Map<String, String> details = ActuatorUtil.testDirectory(basePath);

    if (!details.isEmpty()) {
      details.put("No of schema documents", Long.toString(MetadataSchemaRecordUtil.getNoOfSchemas()));
      builder.withDetail("schemaRepo", details);
    }
  }

  /**
   * Fix local document URI to URL.
   *
   * @param schemaRecord record holding schemaId and version of local document.
   */
  private void fixSchemaDocumentUri(MetadataSchemaRecord schemaRecord) {
    fixSchemaDocumentUri(schemaRecord, false);
  }

  /**
   * Fix local document URI to URL.
   *
   * @param schemaRecord record holding schemaId and version of local document.
   * @param saveUrl save path to file for URL.
   */
  private void fixSchemaDocumentUri(MetadataSchemaRecord schemaRecord, boolean saveUrl) {
    String schemaDocumentUri = schemaRecord.getSchemaDocumentUri();
    schemaRecord.setSchemaDocumentUri(getSchemaDocumentUri(schemaRecord).toString());
    LOG.trace("Fix schema document Uri '{}' -> '{}'", schemaDocumentUri, schemaRecord.getSchemaDocumentUri());
    if (saveUrl) {
      LOG.trace("Store path for URI!");
      Url2Path url2Path = new Url2Path();
      url2Path.setPath(schemaDocumentUri);
      url2Path.setUrl(schemaRecord.getSchemaDocumentUri());
      url2Path.setType(schemaRecord.getType());
      url2Path.setVersion(schemaRecord.getSchemaVersion());
      url2PathDao.save(url2Path);
    }
  }

  /**
   * Get URI for accessing schema document via schemaId and version.
   *
   * @param schemaRecord Record holding schemaId and version.
   * @return URI for accessing schema document.
   */
  public URI getSchemaDocumentUri(MetadataSchemaRecord schemaRecord) {
    return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(schemaRecord.getSchemaId(), schemaRecord.getSchemaVersion(), null, null)).toUri();
  }
}
