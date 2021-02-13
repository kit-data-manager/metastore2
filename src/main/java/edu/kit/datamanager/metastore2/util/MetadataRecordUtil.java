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
package edu.kit.datamanager.metastore2.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UnprocessableEntityException;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.PrimaryIdentifier;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.ContentDataUtils;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import io.swagger.v3.core.util.Json;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for handling json documents
 */
public class MetadataRecordUtil {

  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MetadataRecordUtil.class);
  /**
   * Encoding for strings/inputstreams.
   */
  private static final String ENCODING = "UTF-8";
  /**
   * Mapper for parsing json.
   */
  private static ObjectMapper mapper = new ObjectMapper();

  public static MetadataRecord createMetadataRecord(MetastoreConfiguration applicationProperties,
          MultipartFile recordDocument, MultipartFile document) {
    MetadataRecord result = null;
    MetadataRecord record;

    // Do some checks first.
    try {
      if (recordDocument == null || recordDocument.isEmpty() || document == null || document.isEmpty()) {
        throw new IOException();
      }
      record = Json.mapper().readValue(recordDocument.getInputStream(), MetadataRecord.class);
    } catch (IOException ex) {
      String message = "No metadata record provided. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
      
  if (record.getRelatedResource() == null || record.getSchemaId() == null) {
      String message = "Mandatory attributes relatedResource and/or schemaId not found in record. Returning HTTP BAD_REQUEST.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }

    if (record.getId() != null) {
      String message = "Not expecting record id to be assigned by user.";
      LOG.error(message);
      throw new BadArgumentException(message);
    }
    // validate document
    try {
      validateMetadataDocument(applicationProperties, record, document.getBytes());
    } catch (IOException ex) {
      LOG.error(null, ex);
      throw new CustomInternalServerError(ex.getMessage());
    }
    // create record.
    DataResource dataResource = migrateToDataResource(applicationProperties, record);
    DataResource createResource = DataResourceUtils.createResource(applicationProperties, dataResource);
    // store document
    ContentInformation contentInformation = ContentDataUtils.addFile(applicationProperties, createResource, document, document.getOriginalFilename(), null, true, (t) -> {
      return "somethingStupid";
    });

    return migrateToMetadataRecord(applicationProperties, createResource);
  }

  public static void deleteMetadataRecord(MetastoreConfiguration applicationProperties,
           String id, 
           String eTag,
           Function<String, String> supplier) {
    DataResourceUtils.deleteResource(applicationProperties, id, eTag, supplier);
  }

  public static DataResource migrateToDataResource(RepoBaseConfiguration applicationProperties,
          MetadataRecord metadataRecord) {
    DataResource dataResource;
    if (metadataRecord.getId() != null) {
      try {
        dataResource = applicationProperties.getDataResourceService().findById(metadataRecord.getId(), metadataRecord.getRecordVersion());
      } catch (ResourceNotFoundException rnfe) {
        dataResource = DataResource.factoryNewDataResource(metadataRecord.getId());
      }
    } else {
      dataResource = new DataResource();
    }
    dataResource.setAcls(metadataRecord.getAcl());
    if (metadataRecord.getCreatedAt() != null) {
      boolean createDateExists = false;
      Set<Date> dates = dataResource.getDates();
      for (edu.kit.datamanager.repo.domain.Date d : dates) {
        if (edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())) {
          LOG.trace("Creation date entry found.");
          createDateExists = true;
          break;
        }
      }
      if (!createDateExists) {
        dataResource.getDates().add(Date.factoryDate(metadataRecord.getCreatedAt(), Date.DATE_TYPE.CREATED));
      }
    }
    if (metadataRecord.getPid() != null) {
      dataResource.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier(metadataRecord.getPid()));
    }
    boolean relationFound = false;
    boolean schemaIdFound = false;
    for (RelatedIdentifier relatedIds : dataResource.getRelatedIdentifiers()) {
      if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR) {
        LOG.trace("Set relation to '{}'", metadataRecord.getRelatedResource());
        relatedIds.setValue(metadataRecord.getRelatedResource());
      }
      if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM) {
        LOG.trace("Set schemaId to '{}'", metadataRecord.getSchemaId());
        relatedIds.setValue(metadataRecord.getSchemaId());
      }
    }
    if (!relationFound) {
      RelatedIdentifier relatedResource = RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR, metadataRecord.getRelatedResource(), null, null);
      dataResource.getRelatedIdentifiers().add(relatedResource);
    }
    if (!schemaIdFound) {
      RelatedIdentifier schemaId = RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM, metadataRecord.getSchemaId(), null, null);
      dataResource.getRelatedIdentifiers().add(schemaId);
    }
    
    dataResource.getTitles().add(Title.factoryTitle("Metadata 4 metastore", Title.TYPE.OTHER));
    dataResource.setResourceType(ResourceType.createResourceType("metadata"));

    return dataResource;
  }

  private static MetadataRecord migrateToMetadataRecord(RepoBaseConfiguration applicationProperties,
          DataResource dataResource) {
    MetadataRecord metadataRecord = new MetadataRecord();
    if (dataResource != null) {
      metadataRecord.setId(dataResource.getId());
      metadataRecord.setAcl(dataResource.getAcls());

      for (edu.kit.datamanager.repo.domain.Date d : dataResource.getDates()) {
        if (edu.kit.datamanager.repo.domain.Date.DATE_TYPE.CREATED.equals(d.getType())) {
          LOG.trace("Creation date entry found.");
          metadataRecord.setCreatedAt(d.getValue());
          break;
        }
      }
      if (dataResource.getLastUpdate() != null) {
        metadataRecord.setLastUpdate(dataResource.getLastUpdate());
      }

    }
    if (dataResource.getIdentifier() != null) {
      PrimaryIdentifier identifier = dataResource.getIdentifier();
      if (identifier.hasDoi()) {
        metadataRecord.setPid(identifier.getValue());
      }
    }
    metadataRecord.setRecordVersion(applicationProperties.getAuditService().getCurrentVersion(dataResource.getId()));
    for (RelatedIdentifier relatedIds : dataResource.getRelatedIdentifiers()) {
      if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_METADATA_FOR) {
        LOG.trace("Set relation to '{}'", relatedIds.getValue());
        metadataRecord.setRelatedResource(relatedIds.getValue());
      }
      if (relatedIds.getRelationType() == RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM) {
        LOG.trace("Set schemaId to '{}'", relatedIds.getValue());
        metadataRecord.setSchemaId(relatedIds.getValue());
      }
    }
    IContentInformationService contentInformationService = applicationProperties.getContentInformationService();
    ContentInformation info = new ContentInformation();
    info.setParentResource(dataResource);
    List<ContentInformation> listOfFiles = contentInformationService.findAll(info, PageRequest.of(0, 1)).getContent();
    if (!listOfFiles.isEmpty()) {
      info = listOfFiles.get(0);
      metadataRecord.setDocumentHash(info.getHash());
      metadataRecord.setMetadataDocumentUri(info.getContentUri());
    }
    return metadataRecord;
  }

  /**
   * Validate metadata document with given schema.
   *
   * @param record metadata of the document.
   * @param document document
   * @return ResponseEntity in case of an error.
   * @throws IOException Error reading document.
   */
  private static void validateMetadataDocument(MetastoreConfiguration metastoreProperties,
          MetadataRecord record,
          byte[] document) {
    LOG.trace("validateMetadataDocument {},{}, {}", metastoreProperties, record, document);
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
      } catch (IOException | RestClientException ex) {
        String message = new String("Failed to access schema registry at '" + schemaRegistry + "'. Proceeding with next registry.");
        LOG.error(message, ex);
        errorMessage.append(message).append("\n");
      }
    }
    if (!validationSuccess) {
      throw new UnprocessableEntityException(errorMessage.toString());
    }

    return;
  }

  public static MetadataRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId) throws ResourceNotFoundException {
    return getRecordByIdAndVersion(metastoreProperties, recordId, null);
  }

  public static MetadataRecord getRecordByIdAndVersion(MetastoreConfiguration metastoreProperties,
          String recordId, Long version) throws ResourceNotFoundException {
    //if security enabled, check permission -> if not matching, return HTTP UNAUTHORIZED or FORBIDDEN
    DataResource dataResource = metastoreProperties.getDataResourceService().findByAnyIdentifier(recordId, version);

    return migrateToMetadataRecord(metastoreProperties, dataResource);
  }

}
