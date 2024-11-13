/*
 * Copyright 2023 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date.DATE_TYPE;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.Data;
import java.util.Set;
import java.util.HashSet;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.http.MediaType;

/**
 * Record holding metadata document + list of SIDs allowed to at least read the
 * document. Structure is similar to elastic content of base-repo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ElasticWrapper {

  /**
   * Resource type for elastic search indexing.
   */
  public static final String RESOURCE_TYPE = "application/vnd.datamanager.acl+json";
  /**
   * Media type for elastic search indexing.
   */
  public static final MediaType ACL_RECORD_MEDIA_TYPE = MediaType.valueOf(RESOURCE_TYPE);

  private String id;

  private String pid;

  @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
  private Date created;

  @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
  private Date lastUpdate;

  @NotNull(message = "A list of access control entries with at least access for READ.")
  @OneToMany(cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
  private final Set<String> read;

  @NotBlank(message = "The metadata record.")
  private DataResource metadataRecord;

  @NotBlank(message = "The metadata document.")
  private Object metadataDocument;


  public ElasticWrapper(DataResource resource) {
    id = resource.getId();
    pid = (resource.getIdentifier() != null) ? resource.getIdentifier().getValue() : null;
    metadataRecord = resource;
    read = new HashSet<>();
    resource.getAcls().forEach(entry -> {
      String sid = entry.getSid();
      if (entry.getPermission().atLeast(PERMISSION.READ)) {
        read.add(sid);
      }
    });

    resource.getDates().stream().filter(d -> DATE_TYPE.CREATED.equals(d.getType())).
            forEachOrdered((edu.kit.datamanager.repo.domain.Date d) -> created = Date.from(d.getValue()));
 
    lastUpdate = Date.from(resource.getLastUpdate());
  }

}
