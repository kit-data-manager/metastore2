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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.util.ActuatorUtil;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryControllerV2;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.StateSpecification;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

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
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * Controller for schema documents.
 */
@Controller
@RequestMapping(value = "/api/v2/schemas")
@Tag(name = "Schema Registry")
@Schema(description = "Schema Registry")
public class SchemaRegistryControllerImplV2 implements ISchemaRegistryControllerV2 {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImplV2.class);

  private final ApplicationProperties applicationProperties;

  private final MetastoreConfiguration schemaConfig;

  private final IDataResourceDao dataResourceDao;

  /**
   * Constructor for schema documents controller.
   *
   * @param applicationProperties Configuration for controller.
   * @param schemaConfig Configuration for metadata documents repository.
   * @param dataResourceDao DAO for data resources.
   */
  public SchemaRegistryControllerImplV2(ApplicationProperties applicationProperties,
          MetastoreConfiguration schemaConfig,
          IDataResourceDao dataResourceDao) {
    this.applicationProperties = applicationProperties;
    this.schemaConfig = schemaConfig;
    this.dataResourceDao = dataResourceDao;
    DataResourceRecordUtil.setDataResourceDao(this.dataResourceDao);
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", schemaConfig);
    LOG.info("------------------------------------------------------");
  }

  @Override
  public ResponseEntity<DataResource> createRecord(
          @RequestPart(name = "record") final MultipartFile recordDocument,
          @RequestPart(name = "schema") MultipartFile document,
          HttpServletRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder) {
    LOG.trace("Performing createRecord({},....", recordDocument);
    DataResource dataResourceRecord = DataResourceRecordUtil.createDataResourceRecord4Schema(schemaConfig, recordDocument, document);
    LOG.trace("Schema record successfully persisted. Returning result.");
    String etag = dataResourceRecord.getEtag();
    if (LOG.isTraceEnabled()) {
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      String json;
      try {
        json = ow.writeValueAsString(dataResourceRecord);
        LOG.trace(json);
      } catch (JsonProcessingException ex) {
        java.util.logging.Logger.getLogger(SchemaRegistryControllerImplV2.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    LOG.trace("Schema record successfully persisted.");
    URI locationUri;
    locationUri = SchemaRegistryControllerImplV2.getSchemaDocumentUri(dataResourceRecord);
    LOG.trace("Set locationUri to '{}'", locationUri);
    return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(dataResourceRecord);
  }

  @Override
  public ResponseEntity<DataResource> getRecordById(
          @PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getRecordById({}, {}).", schemaId, version);

    DataResource schemaRecord = DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, schemaId, version);
    String etag = schemaRecord.getEtag();

    LOG.trace("Returning result.");
    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(schemaRecord);
  }

  @Override
  public ResponseEntity<ContentInformation> getContentInformationById(
          @PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getContentInformationById({}, {}).", schemaId, version);

    ContentInformation contentInformation = DataResourceRecordUtil.getContentInformationByIdAndVersion(schemaConfig, schemaId, version);
    DataResource minimalDataResource = DataResource.factoryNewDataResource(contentInformation.getParentResource().getId());
    URI locationUri;
    locationUri = DataResourceRecordUtil.getMetadataDocumentUri(schemaId, contentInformation.getVersion().toString());
    contentInformation.setParentResource(minimalDataResource);
    contentInformation.setContentUri(locationUri.toString());
    contentInformation.setRelativePath(null);
    contentInformation.setVersioningService(null);
    return ResponseEntity.ok().body(contentInformation);
  }

  @Override
  public ModelAndView getLandingPageById(@PathVariable(value = "schemaId") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing Landing page for schema document with ({}, {}).", id, version);
    String redirectUrl = applicationProperties.getSchemaLandingPage();
    redirectUrl = redirectUrl.replace(MetadataControllerImpl.PLACEHOLDER_ID, id);
    String versionString = "";
    if (version != null) {
      versionString = version.toString();
    }
    redirectUrl = "redirect:" + redirectUrl.replace(MetadataControllerImpl.PLACEHOLDER_VERSION, versionString);

    LOG.trace("Redirect to '{}'", redirectUrl);

    return new ModelAndView(redirectUrl);
  }

  @Override
  public ResponseEntity getSchemaDocumentById(
          @PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getSchemaDocumentById({}, {}).", schemaId, version);

    DataResource schemaRecord = DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, schemaId, version);
    ContentInformation contentInfo = DataResourceRecordUtil.getContentInformationByIdAndVersion(schemaConfig, schemaRecord.getId(), Long.valueOf(schemaRecord.getVersion()));
    MediaType contentType = MediaType.valueOf(contentInfo.getMediaType());
    URI pathToFile = URI.create(contentInfo.getContentUri());
    Path schemaDocumentPath = Paths.get(pathToFile);
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

  public ResponseEntity<List<DataResource>> getAllVersions(
          String id,
          Pageable pgbl
  ) {
    LOG.trace("Performing getAllVersions({}).", id);
    // Search for resource type of MetadataSchemaRecord

    //if security is enabled, include principal in query
    LOG.debug("Performing query for records.");
    DataResource recordByIdAndVersion;
    List<DataResource> recordList = new ArrayList<>();
    long totalNoOfElements = 5;
    try {
      recordByIdAndVersion = DataResourceRecordUtil.getRecordById(schemaConfig, id);
      totalNoOfElements = Long.parseLong(recordByIdAndVersion.getVersion());
      for (long version = totalNoOfElements - pgbl.getOffset(), size = 0; version > 0 && size < pgbl.getPageSize(); version--, size++) {
        recordList.add(DataResourceRecordUtil.getRecordByIdAndVersion(schemaConfig, id, version));
      }
    } catch (ResourceNotFoundException rnfe) {
      LOG.info("Schema ID '{}' is unkown. Return empty list...", id);
    }

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), totalNoOfElements);

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(recordList);
  }

  @Override
  public ResponseEntity validate(@PathVariable(value = "schemaId") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          MultipartFile document,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing validate({}, {}, {}).", schemaId, version, "#document");
    DataResourceRecordUtil.validateMetadataDocument(schemaConfig, document, schemaId, version);
    LOG.trace("Metadata document validation succeeded. Returning HTTP NOT_CONTENT.");
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<List<DataResource>> getRecords(@RequestParam(value = "schemaId", required = false) String schemaId,
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
    Specification<DataResource> spec = DataResourceRecordUtil.findByMimetypes(mimeTypes);

    // Add authentication if enabled
    spec = DataResourceRecordUtil.findByAccessRights(spec);
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }
    // Hide revoked and gone data resources. 
    DataResource.State[] states = {DataResource.State.FIXED, DataResource.State.VOLATILE};
    List<DataResource.State> stateList = Arrays.asList(states);
    spec = spec.and(StateSpecification.toSpecification(stateList));

    LOG.debug("Performing query for records.");
    Page<DataResource> records = DataResourceRecordUtil.queryDataResources(spec, pgbl);
    List<DataResource> recordList = records.getContent();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Cleaning up schemaDocumentUri of query result.");
      for (DataResource item : recordList) {
        LOG.trace("---> " + item.toString());
      }
    }

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(recordList);
  }

  @Override
  public ResponseEntity<DataResource> updateRecord(@PathVariable("schemaId") final String schemaId,
          @RequestPart(name = "record", required = false) MultipartFile schemaRecord,
          @RequestPart(name = "schema", required = false) final MultipartFile document,
          final WebRequest request, final HttpServletResponse response) {
    LOG.trace("Performing updateRecord({}, {}).", schemaId, schemaRecord);
    UnaryOperator<String> getById;
    getById = t -> WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, response)).toString();
    String eTag = ControllerUtils.getEtagFromHeader(request);
    DataResource updatedSchemaRecord = DataResourceRecordUtil.updateDataResource4SchemaDocument(schemaConfig, schemaId, eTag, schemaRecord, document, getById);

    LOG.trace("DataResource record successfully persisted. Updating document URI and returning result.");
    String etag = updatedSchemaRecord.getEtag();

    URI locationUri;
    locationUri = SchemaRegistryControllerImplV2.getSchemaDocumentUri(updatedSchemaRecord);
    LOG.trace("Set locationUri to '{}'", locationUri);
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
      details.put("No of schema documents", Long.toString(DataResourceRecordUtil.getNoOfSchemaDocuments()));
      builder.withDetail("schemaRepo", details);
    }
  }

  /**
   * Get URI for accessing schema document via schemaId and version.
   *
   * @param dataResourceRecord Record holding schemaId and version.
   * @return URI for accessing schema document.
   */
  public static final URI getSchemaDocumentUri(DataResource dataResourceRecord) {
    return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SchemaRegistryControllerImplV2.class).getSchemaDocumentById(dataResourceRecord.getId(), Long.parseLong(dataResourceRecord.getVersion()), null, null)).toUri();
  }
}
