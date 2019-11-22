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

import edu.kit.datamanager.entities.repo.ContentInformation;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.dao.ISchemaSynchronizationEventDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.SchemaSynchronizationEvent;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

  private ApplicationProperties applicationProperties;
  private ISchemaSynchronizationEventDao schemaSynchronizationEventDao;

  @Autowired
  public SchemaSynchronizationService(ApplicationProperties applicationProperties, ISchemaSynchronizationEventDao schemaSynchronizationEventDao){
    this.applicationProperties = applicationProperties;
    this.schemaSynchronizationEventDao = schemaSynchronizationEventDao;

  }

  @Scheduled(cron = "${repo.schema.synchronization.cron.value}")
  public void receiveNextMessage(){
    if(!applicationProperties.isSynchronizationEnabled()){
      LOGGER.trace("Schema synchonization is disabled.");
      return;
    }

    Set<Entry<String, String>> sourceEntries = applicationProperties.getSchemaSources().entrySet();
    if(sourceEntries == null || sourceEntries.isEmpty()){
      LOGGER.warn("Schema synchronization is enabled, but no sources are provided.");
      return;
    }
    LOGGER.trace("Synchronizing schema(s) from {} source(s).", sourceEntries.size());
    RestTemplate restTemplate = new RestTemplate();

    for(Entry<String, String> entry : sourceEntries){
      LOGGER.trace("Synchronizing schema(s) from source '{}'.", entry.getKey());
      URI sourceUri = URI.create(entry.getValue());
      Optional<SchemaSynchronizationEvent> optEvent = schemaSynchronizationEventDao.findBySourceName(entry.getKey());
      Instant lastSynchronization = null;
      if(optEvent.isPresent()){
        lastSynchronization = optEvent.get().getLastSynchronization();
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setAccept(Arrays.asList(MediaType.valueOf(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)));

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(headers);

      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(entry.getValue()).queryParam("from", lastSynchronization);

      ResponseEntity<MetadataSchemaRecord[]> response = restTemplate.exchange(uriBuilder.toString(), HttpMethod.GET, requestEntity, MetadataSchemaRecord[].class);

    }

  }

}
