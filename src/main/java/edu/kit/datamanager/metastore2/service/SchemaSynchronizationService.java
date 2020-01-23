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
package edu.kit.datamanager.metastore2.service;

import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.SynchronizationSource;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.dao.ISchemaSynchronizationEventDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.SchemaSynchronizationEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Component
public class SchemaSynchronizationService{

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaSynchronizationService.class);

  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private ISchemaSynchronizationEventDao schemaSynchronizationEventDao;
  @Autowired
  Environment environment;

  @Scheduled(cron = "${repo.schema.synchronization.cron.value:-}")
  public void performSynchronization(){

    if(!applicationProperties.isSynchronizationEnabled()){
      LOGGER.trace("Schema synchonization is disabled.");
      return;
    }

    if(applicationProperties.getSchemaSources() == null || applicationProperties.getSchemaSources().isEmpty()){
      LOGGER.warn("Schema synchonization is enabled but schema sources are empty. Skipping schema synchronization.");
      return;
    }

    LOGGER.trace("Determining local base URL.");
    String localPort = environment.getProperty("local.server.port");
    boolean isSecure = (environment.getProperty("server.ssl.key-store") != null);

    String localBaseUrl = (isSecure) ? "https://localhost:" : "http://localhost:";
    localBaseUrl += localPort;
    localBaseUrl += "/api/v1/schemas/";
    LOGGER.trace("Using {} as local base URL.", localBaseUrl);
    LOGGER.trace("Synchronizing schema(s) from {} source(s).", applicationProperties.getSchemaSources().size());
    RestTemplate restTemplate = new RestTemplate();

    for(SynchronizationSource source : applicationProperties.getSchemaSources()){
      LOGGER.trace("Synchronizing schema(s) from source '{}'.", source.getId());
      Optional<SchemaSynchronizationEvent> optEvent = schemaSynchronizationEventDao.findBySourceName(source.getId());
      Instant lastSynchronization = null;
      SchemaSynchronizationEvent event;
      if(optEvent.isPresent()){
        lastSynchronization = optEvent.get().getLastSynchronization();
        event = optEvent.get();
      } else{
        event = new SchemaSynchronizationEvent();
        event.setSourceName(source.getId());
      }

      if(event.getErrorCount() != null && event.getErrorCount() > 3){
        LOGGER.warn("Synchronization with schema source with id {} at {} failed 3 times, skipping entry for now. Please check the synchronization source registry.", source.getId(), source.getBaseUrl());
        continue;
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/vnd.datamanager.schema-record+json")));

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(source.getBaseUrl());

      if(lastSynchronization != null){
        uriBuilder = uriBuilder.queryParam("from", lastSynchronization);
      }

      LOGGER.trace("Obtaining schemas updated after {} via URL {}.", lastSynchronization, uriBuilder.toUriString());

      //@TODO switch to service identity, add bearer token optionally
      ResponseEntity<MetadataSchemaRecord[]> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, requestEntity, MetadataSchemaRecord[].class);
      if(response.getStatusCodeValue() == 200){
        MetadataSchemaRecord[] receivedRecords = response.getBody();
        if(receivedRecords != null && receivedRecords.length > 0){
          LOGGER.trace("Performing update for {} schema(s).", receivedRecords.length);
          for(MetadataSchemaRecord record : receivedRecords){
            LOGGER.trace("Checking record with schema with id {} in local schema registry.", record.getSchemaId());
            //obtain local schema
            Optional<MetadataSchemaRecord> optRecord = metadataSchemaDao.findById(record.getSchemaId());
            if(!optRecord.isPresent()){
              LOGGER.trace("New schema with id {} detected. Downloading remote schema document.", record.getSchemaId());
              createOrUpdateSchema(source.getBaseUrl(), localBaseUrl, record, event);
            } else{
              LOGGER.trace("Existing schema with id {} detected. Downloading remote schema document.", record.getSchemaId());
              MetadataSchemaRecord localRecord = optRecord.get();
              if(localRecord.getLocked() == null || !localRecord.getLocked()){
                LOGGER.trace("Synchronization of local record enabled. Comparing schema document hashes.");
                if(!localRecord.getSchemaHash().equals(record.getSchemaHash())){
                  //download remote schema
                  LOGGER.trace("Schema document hashes are NOT equal. Downloading schema document with id {} from {}.", record.getSchemaId(), source.getBaseUrl());
                  createOrUpdateSchema(source.getBaseUrl(), localBaseUrl, localRecord, event);
                } else{
                  LOGGER.trace("Schema document hashes are equal. Skipping update.");
                  updateSynchronizationEvent(event, true);
                  LOGGER.trace("Schema synchronization finished.");
                }
              } else{
                //skip
                LOGGER.trace("Synchronization for local record is disabled. Skipping synchronization of schema {}.", localRecord.getSchemaId());
                updateSynchronizationEvent(event, true);
                LOGGER.trace("Schema synchronization finished.");
              }
            }
          }
        } else{
          LOGGER.trace("No new or updated schemas received.");
        }

      } else{
        LOGGER.error("Failed to obtain updates schemas from URL {}. Service returned status {}.", uriBuilder.toUriString(), response.getStatusCodeValue());
      }
    }
  }

  private void createOrUpdateSchema(String baseUrl, String localBaseUrl, MetadataSchemaRecord record, SchemaSynchronizationEvent event){
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    int status = SimpleServiceClient.create(baseUrl).accept(MediaType.APPLICATION_OCTET_STREAM).withResourcePath(record.getSchemaId()).getResource(stream);
    if(status == 200){
      try{
        LOGGER.trace("Remote schema document successfully downloaded. Updating schema at local registry.");
        HttpStatus createStatus = SimpleServiceClient.create(localBaseUrl).withFormParam("record", record).withFormParam("schema", new ByteArrayInputStream(stream.toByteArray())).postForm();
        if(HttpStatus.CREATED.equals(createStatus)){
          LOGGER.trace("New schema with id {} successfully updated. Updating lastSynchronization timestamp.", record.getSchemaId());
          updateSynchronizationEvent(event, true);
          LOGGER.trace("Schema synchronization finished.");
        } else{
          LOGGER.error("Failed to update schema at local registry, service returned {}. Updating errorCount.", createStatus);
          updateSynchronizationEvent(event, false);
          LOGGER.trace("Schema synchronization finished.");
        }
      } catch(IOException ex){
        LOGGER.error("Failed to update schema record locally.", ex);
        updateSynchronizationEvent(event, false);
        LOGGER.trace("Schema synchronization finished.");
      }
    }
  }

  private void updateSynchronizationEvent(SchemaSynchronizationEvent event, boolean success){
    event.setLastSynchronization(Instant.now());
    if(success){
      event.setErrorCount((short) 0);
    } else{
      event.setErrorCount((short) (event.getErrorCount() + 1));
    }
    LOGGER.trace("Writing updated synchronization event.");
    schemaSynchronizationEventDao.save(event);
  }
}
