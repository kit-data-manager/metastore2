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

import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.entities.messaging.MetadataResourceMessage;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.spec.RelatedIdSpecification;
import edu.kit.datamanager.metastore2.domain.LinkedMetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.util.MetadataRecordUtil;
import edu.kit.datamanager.metastore2.web.IMetadataController;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.LastUpdateSpecification;
import edu.kit.datamanager.repo.dao.spec.dataresource.RelatedIdentifierSpec;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.service.IDataResourceService;
import edu.kit.datamanager.repo.service.IRepoStorageService;
import edu.kit.datamanager.repo.service.IRepoVersioningService;
import edu.kit.datamanager.repo.service.impl.ContentInformationAuditService;
import edu.kit.datamanager.repo.service.impl.DataResourceAuditService;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.service.IMessagingService;
import edu.kit.datamanager.util.AuthenticationHelper;
import edu.kit.datamanager.util.ControllerUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
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
@Schema(description = "Metadata Resource Management")
public class MetadataControllerImpl implements IMetadataController {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataControllerImpl.class);
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

  private final IAuditService<DataResource> auditServiceDataResource;
  private final IAuditService<ContentInformation> contentAuditService;
  @Autowired
  private ILinkedMetadataRecordDao metadataRecordDao;
  @Autowired
  private IAuditService<MetadataRecord> auditService;
  @Autowired
  private IMessagingService messagingService;
 
  private final MetastoreConfiguration metastoreProperties;
  @Autowired
  private final IDataResourceDao dataResourceDao;
 
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
  public MetadataControllerImpl(ApplicationProperties applicationProperties,
          Javers javers,
          IDataResourceService dataResourceService,
          IDataResourceDao dataResourceDao,
          IContentInformationService contentInformationService,
          IRepoVersioningService[] versioningServices,
          IRepoStorageService[] storageServices,
          ApplicationEventPublisher eventPublisher
  ) {
    this.applicationProperties = applicationProperties;
    this.javers = javers;
    this.dataResourceDao = dataResourceDao;
    this.dataResourceService = dataResourceService;
    this.contentInformationService = contentInformationService;
    this.versioningServices = versioningServices;
    this.storageServices = storageServices;
    MetastoreConfiguration rbc = new MetastoreConfiguration();
    rbc.setBasepath(applicationProperties.getMetadataFolder());
    rbc.setReadOnly(false);
    rbc.setDataResourceService(this.dataResourceService);
    rbc.setContentInformationService(this.contentInformationService);
    rbc.setEventPublisher(eventPublisher);
    for (IRepoVersioningService versioningService : versioningServices) {
      if ("simple".equals(versioningService.getServiceName())) {
        LOG.info("Set versioning service: {}", versioningService.getServiceName());
        rbc.setVersioningService(versioningService);
        break;
      }
    }
    for (IRepoStorageService storageService : storageServices) {
      if ("dateBased".equals(storageService.getServiceName())) {
        LOG.info("Set storage service: {}", storageService.getServiceName());
        rbc.setStorageService(storageService);
        break;
      }
    }
    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
//    dataResourceService = new DataResourceService();
    dataResourceService.configure(rbc);
//    contentInformationService = new ContentInformationService();
    contentInformationService.configure(rbc);
//    rbc.setContentInformationAuditService(contentInformationAuditService);
    rbc.setAuditService(auditServiceDataResource);
    metastoreProperties = rbc;
    metastoreProperties.setSchemaRegistries(applicationProperties.getSchemaRegistries());
    System.out.println("kkkk" + metastoreProperties);
//    ContentInformationAuditService cias = new ContentInformationAuditService(javers, metastoreProperties);
//    metastoreProperties.setContentInformationAuditService(cias);
//    metastoreProperties.setContentInformationService(contentInformationService);
//     metastoreProperties.setReadOnly(false);
//    metastoreProperties.setStorageService(new DateBasedStorageService());
//    metastoreProperties.setVersioningService(new SimpleDataVersioningService());
//   RepoBaseConfiguration rbc = metastoreProperties;
//    rbc.setEventPublisher(eventPublisher);
//    auditServiceDataResource = new DataResourceAuditService(this.javers, rbc);
//   metastoreProperties.setAuditService(auditServiceDataResource);
//    contentAuditService = new ContentInformationAuditService(this.javers, rbc);
//    dataResourceService = new DataResourceService();
//    dataResourceService.configure(rbc);
//    contentInformationService = new ContentInformationService();
//    contentInformationService.configure(rbc);
  }

  @Override
  public ResponseEntity createRecord(
          @RequestPart(name = "record") final MultipartFile recordDocument,
          @RequestPart(name = "document") final MultipartFile document,
          HttpServletRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder) throws URISyntaxException {

    LOG.trace("Performing createRecord({},...).", recordDocument);
    MetadataRecord record;
    try {
      if (recordDocument == null || recordDocument.isEmpty()) {
        throw new IOException();
      }
      record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
    } catch (IOException ex) {
      LOG.error("No metadata record provided. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No metadata record provided.");
    }

    if (record.getRelatedResource() == null || record.getSchemaId() == null) {
      LOG.error("Mandatory attributes relatedResource and/or schemaId not found in record. Returning HTTP BAD_REQUEST.");
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mandatory attributes relatedResource and/or schemaId not found in record.");
    }

    if (record.getId() != null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not expecting record id to be assigned by user.");
    }

    LOG.debug("Test for existing metadata record for given schema and resource");
    boolean recordAlreadyExists = metadataRecordDao.existsMetadataRecordByRelatedResourceAndSchemaId(record.getRelatedResource(), record.getSchemaId());

    if (recordAlreadyExists) {
      LOG.error("Conflict with existing metadata record!");
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Metadata record already exists! Please update existing record instead!");
    }
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(metastoreProperties, recordDocument, document);
    // Successfully created metadata record.
    metadataRecordDao.save(new LinkedMetadataRecord(result));
    
    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(result.getId(), result.getRecordVersion(), null, null)).toUri();

    LOG.trace("Schema record successfully persisted. Returning result.");
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
    MetadataRecord record = MetadataRecordUtil.getRecordByIdAndVersion(metastoreProperties, id, version);
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    LOG.trace("Get ETag of MetadataRecord.");
    String etag = record.getEtag();

    fixMetadataDocumentUri(record);
    LOG.trace("Document URI successfully updated. Returning result.");
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

    LOG.trace("Obtaining metadata record with id {} and version {}.", id, version);
    MetadataRecord record = MetadataRecordUtil.getRecordByIdAndVersion(metastoreProperties, id, version);

    URI metadataDocumentUri = URI.create(record.getMetadataDocumentUri());

    Path metadataDocumentPath = Paths.get(metadataDocumentUri);
    if (!Files.exists(metadataDocumentPath) || !Files.isRegularFile(metadataDocumentPath) || !Files.isReadable(metadataDocumentPath)) {
      LOG.trace("Metadata document at path {} either does not exist or is no file or is not readable. Returning HTTP NOT_FOUND.", metadataDocumentPath);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Metadata document on server either does not exist or is no file or is not readable.");
    }

    return ResponseEntity.
            ok().
            header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadataDocumentPath.toFile().length())).
            body(new FileSystemResource(metadataDocumentPath.toFile()));
  }

  @Override
  public ResponseEntity<List<MetadataRecord>> getRecords(
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
        Specification<DataResource> spec = Specification.where(null);
    List<String> allRelatedIdentifiers = new ArrayList<>();
    if (schemaIds != null) {
      allRelatedIdentifiers.addAll(schemaIds);
    }
    if (relatedIds != null) {
      allRelatedIdentifiers.addAll(relatedIds);
    }
    if (!allRelatedIdentifiers.isEmpty()) {
       spec = RelatedIdentifierSpec.toSpecification(allRelatedIdentifiers.toArray(new String[allRelatedIdentifiers.size()]));
    }
    if ((updateFrom != null) || (updateUntil != null)) {
      spec = spec.and(LastUpdateSpecification.toSpecification(updateFrom, updateUntil));
    }

    //if security is enabled, include principal in query
    LOG.debug("Performing query for records.");
    Page<DataResource> records = dataResourceDao.findAll(spec, pgbl);

    LOG.trace("Transforming Dataresource to MetadataRecord");
    List<DataResource> recordList = records.getContent();
    List<MetadataRecord> metadataList = new ArrayList<>();
    System.out.println("oooooodataResourceDao size: " + dataResourceDao.count());
     System.out.println("Get Records -> found: " + recordList.size());
    recordList.forEach((record) -> {
      metadataList.add(MetadataRecordUtil.migrateToMetadataRecord(metastoreProperties, record));
      System.out.println("id: " + record.getId());
    });

    String contentRange = ControllerUtils.getContentRangeHeader(pgbl.getPageNumber(), pgbl.getPageSize(), records.getTotalElements());

    return ResponseEntity.status(HttpStatus.OK).header("Content-Range", contentRange).body(metadataList);
  }

  @Override
  public ResponseEntity updateRecord(
          @PathVariable("id") String id,
          @RequestPart(name = "record", required = false) MultipartFile record,
          @RequestPart(name = "document", required = false)
          final MultipartFile document,
          WebRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder
  ) {
    LOG.trace("Performing updateRecord({}, {}, {}).", id, record, "#document");

    String eTag = ControllerUtils.getEtagFromHeader(request);
     MetadataRecordUtil.updateMetadataRecord(metastoreProperties, eTag, record, document);


    LOG.trace("Metadata record successfully persisted. Updating document URI and returning result.");
//    fixMetadataDocumentUri(record);
    String etag = record.getEtag();

    URI locationUri;
    locationUri = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(record.getId(), record.getRecordVersion(), null, null)).toUri();

    return ResponseEntity.ok().location(locationUri).eTag("\"" + etag + "\"").body(record);
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
      return WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getRecordById(t, 1l, wr, hsr)).toString();
    };
    String eTag = ControllerUtils.getEtagFromHeader(wr);
    MetadataRecordUtil.deleteMetadataRecord(metastoreProperties, id, eTag, getById);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  public MetadataRecord mergeRecords(MetadataRecord managed, MetadataRecord provided) {
    if (provided != null) {
      if (!Objects.isNull(provided.getPid())) {
        LOG.trace("Updating pid from {} to {}.", managed.getPid(), provided.getPid());
        managed.setPid(provided.getPid());
      }

      if (!Objects.isNull(provided.getRelatedResource())) {
        LOG.trace("Updating related resource from {} to {}.", managed.getRelatedResource(), provided.getRelatedResource());
        managed.setRelatedResource(provided.getRelatedResource());
      }

      if (!Objects.isNull(provided.getSchemaId())) {
        LOG.trace("Updating schemaId from {} to {}.", managed.getSchemaId(), provided.getSchemaId());
        managed.setSchemaId(provided.getSchemaId());
      }

      //update acl
      if (provided.getAcl() != null) {
        LOG.trace("Updating record acl from {} to {}.", managed.getAcl(), provided.getAcl());
        managed.setAcl(provided.getAcl());
      }
    }
    LOG.trace("Setting lastUpdate to now().");
    managed.setLastUpdate(Instant.now());
    return managed;
  }

  private void fixMetadataDocumentUri(MetadataRecord record) {
    record.setMetadataDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass()).getMetadataDocumentById(record.getId(), record.getRecordVersion(), null, null)).toUri().toString());
  }

  private String getUniqueRecordHash(MetadataRecord record) {
    String hash = null;
    try {
      LOG.trace("Creating metadata record hash.");
      MessageDigest md = MessageDigest.getInstance("SHA1");
      md.update(record.getId().getBytes(), 0, record.getId().length());
      md.update(record.getRelatedResource().getBytes(), 0, record.getRelatedResource().length());
      md.update(record.getSchemaId().getBytes(), 0, record.getSchemaId().length());
      md.update(Long.toString(record.getRecordVersion()).getBytes(), 0, Long.toString(record.getRecordVersion()).length());
      hash = Hex.encodeHexString(md.digest());
    } catch (NoSuchAlgorithmException ex) {
      LOG.error("Failed to initialize SHA1 MessageDigest.", ex);
      throw new CustomInternalServerError("Failed to create metadata record hash.");
    }
    return hash;
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param record metadata of the document.
   * @param document document
   * @return ResponseEntity in case of an error.
   * @throws IOException Error reading document.
   */
  private ResponseEntity<String> validateMetadataDocument(MetadataRecord record, byte[] document) {
    ResponseEntity<String> responseEntity = null;
    boolean validationSuccess = false;
    StringBuilder errorMessage = new StringBuilder();
    for (String schemaRegistry : metastoreProperties.getSchemaRegistries()) {
      URI schemaRegistryUri = URI.create(schemaRegistry);
      UriComponentsBuilder builder = UriComponentsBuilder.newInstance().scheme(schemaRegistryUri.getScheme()).host(schemaRegistryUri.getHost()).port(schemaRegistryUri.getPort()).pathSegment(schemaRegistryUri.getPath(), "schemas", record.getSchemaId(), "validate");

      URI finalUri = builder.build().toUri();

      try {
        HttpStatus status = SimpleServiceClient.create(finalUri.toString()).accept(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).withFormParam("document", new ByteArrayInputStream(document)).postForm(MediaType.MULTIPART_FORM_DATA);

        if (Objects.equals(HttpStatus.NO_CONTENT, status)) {
          LOG.trace("Successfully validated document against schema {} in registry {}.", record.getSchemaId(), schemaRegistry);
          validationSuccess = true;
          break;
        }
      } catch (HttpClientErrorException ce) {
        //not valid 
        String message = new String("Failed to validate metadata document against schema " + record.getSchemaId() + " at '" + schemaRegistry + "' with status " + ce.getStatusCode() + ".");
        LOG.error(message, ce);
        errorMessage.append(message).append("\n");
        responseEntity = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorMessage.toString());
      } catch (IOException | RestClientException ex) {
        String message = new String("Failed to access schema registry at '" + schemaRegistry + "'. Proceeding with next registry.");
        LOG.error(message, ex);
        errorMessage.append(message).append("\n");
        responseEntity = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorMessage.toString());
      }
    }
    if (!validationSuccess) {
      return responseEntity;
    }

    return null;
  }
}
