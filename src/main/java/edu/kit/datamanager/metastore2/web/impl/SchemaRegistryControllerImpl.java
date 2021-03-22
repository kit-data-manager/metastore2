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

import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryController;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.InternalIdentifierSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.dao.spec.dataresource.TitleSpec;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/schemas")
@Schema(description = "Schema Registry")
public class SchemaRegistryControllerImpl implements ISchemaRegistryController {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaRegistryControllerImpl.class);

  @Autowired
  private final MetastoreConfiguration schemaConfig;
  @Autowired
  private final IDataResourceDao dataResourceDao;
  @Autowired
  private final IContentInformationDao contentInformationDao;

  /**
   * 
   * @param schemaConfig
   * @param dataResourceDao
   * @param contentInformationDao 
   */
  public SchemaRegistryControllerImpl(MetastoreConfiguration schemaConfig,
          IDataResourceDao dataResourceDao,
          IContentInformationDao contentInformationDao) {
    this.schemaConfig = schemaConfig;
    this.dataResourceDao = dataResourceDao;
    this.contentInformationDao = contentInformationDao;
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
    getSchemaDocumentById = (schema, version) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(schema, version, null, null)).toString();
    };
    MetadataSchemaRecord record = MetadataSchemaRecordUtil.createMetadataSchemaRecord(schemaConfig, recordDocument, document, getSchemaDocumentById);
    LOG.trace("Schema record successfully persisted. Returning result.");
    String etag = record.getEtag();

    LOG.trace("Schema record successfully persisted. Updating document URI.");
    fixSchemaDocumentUri(record);
    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri();
    LOG.warn("location uri              " + locationUri);
    return ResponseEntity.created(locationUri).eTag("\"" + etag + "\"").body(record);
  }

  @Override
  public ResponseEntity getRecordById(String schemaId, Long version, WebRequest wr, HttpServletResponse hsr) {
    LOG.trace("Performing getRecordById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = MetadataSchemaRecordUtil.getRecordByIdAndVersion(schemaConfig, schemaId, version, true);
    String etag = record.getEtag();

    fixSchemaDocumentUri(record);
    LOG.trace("Document URI successfully updated. Returning result.");
    return ResponseEntity.ok().eTag("\"" + etag + "\"").body(record);
  }

  @Override
  public ResponseEntity getSchemaDocumentById(
          @PathVariable(value = "id") String schemaId,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr) {
    LOG.trace("Performing getSchemaDocumentById({}, {}).", schemaId, version);

    LOG.trace("Obtaining schema record with id {} and version {}.", schemaId, version);
    MetadataSchemaRecord record = MetadataSchemaRecordUtil.getRecordByIdAndVersion(schemaConfig, schemaId, version);
    URI schemaDocumentUri = URI.create(record.getSchemaDocumentUri());

    MediaType contentType = MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(record.getType()) ? MediaType.APPLICATION_XML : MediaType.APPLICATION_JSON;
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

  @Override
  public ResponseEntity validate(String schemaId, Long version, MultipartFile document, WebRequest wr, HttpServletResponse hsr) {
    LOG.trace("Performing validate({}, {}, {}).", schemaId, version, "#document");
    MetadataSchemaRecordUtil.validateMetadataDocument(schemaConfig, document, schemaId, version);
    LOG.trace("Metadata document validation succeeded. Returning HTTP NOT_CONTENT.");
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<List<MetadataSchemaRecord>> getRecords(List<String> schemaIds, List<String> mimeTypes, Instant updateFrom, Instant updateUntil, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb) {
    LOG.trace("Performing getRecords({}, {}, {}, {}).", schemaIds, mimeTypes, updateFrom, updateUntil);
    // Search for resource type of MetadataSchemaRecord
    Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE));
    //one of given ids.
    if ((schemaIds != null) && !schemaIds.isEmpty()) {
      spec = spec.and(InternalIdentifierSpec.toSpecification(schemaIds.toArray(new String[schemaIds.size()])));
    }
    if ((mimeTypes != null) && !mimeTypes.isEmpty()) {
      spec = spec.and(TitleSpec.toSpecification(mimeTypes.toArray(new String[mimeTypes.size()])));
    }
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }

    LOG.debug("Performing query for records.");
    Page<DataResource> records = null;
    try {
      records = dataResourceDao.findAll(spec, pgbl);
    } catch (Exception ex) {
      LOG.error("Error find metadata records by specification!", ex);
      throw ex;
    }
    List<DataResource> recordList = records.getContent();
    LOG.trace("Cleaning up schemaDocumentUri of query result.");
    List<MetadataSchemaRecord> schemaList = new ArrayList<>();
    recordList.forEach((record) -> {
      MetadataSchemaRecord item = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(schemaConfig, record, false);
      fixSchemaDocumentUri(item);
      schemaList.add(item);
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(schemaList);
  }

  @Override
  public ResponseEntity updateRecord(
          @PathVariable("id") final String schemaId,
          @RequestPart(name = "record", required = false) MultipartFile record,
          @RequestPart(name = "schema", required = false) final MultipartFile document,
          final WebRequest request, final HttpServletResponse response) {
    LOG.trace("Performing updateMetadataSchemaRecord({}, {}).", schemaId, record);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, response)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(request);
    MetadataSchemaRecord updatedSchemaRecord = MetadataSchemaRecordUtil.updateMetadataSchemaRecord(schemaConfig, schemaId, eTag, record, document, getById);

    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
    String etag = updatedSchemaRecord.getEtag();
    fixSchemaDocumentUri(updatedSchemaRecord);
    // Fix Url for OAI PMH entry
    MetadataSchemaRecordUtil.updateMetadataFormat(updatedSchemaRecord);
    
    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(updatedSchemaRecord.getSchemaId(), updatedSchemaRecord.getSchemaVersion(), null, null)).toUri();
    LOG.trace("Set locationUri to '{}'", locationUri.toString());
    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(updatedSchemaRecord);
  }

  @Override
  public ResponseEntity deleteRecord(String schemaId, WebRequest request, HttpServletResponse hsr) {
    LOG.trace("Performing deleteRecord({}).", schemaId);
    Function<String, String> getById;
    getById = (t) -> {
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, null, request, hsr)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(request);
    MetadataSchemaRecordUtil.deleteMetadataSchemaRecord(schemaConfig, schemaId, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private void fixSchemaDocumentUri(MetadataSchemaRecord record) {
    String schemaDocumentUri = record.getSchemaDocumentUri();
    record.setSchemaDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri().toString());
     LOG.trace("Fix schema document Uri '{}' -> '{}'",schemaDocumentUri, record.getSchemaDocumentUri());
 }
}
