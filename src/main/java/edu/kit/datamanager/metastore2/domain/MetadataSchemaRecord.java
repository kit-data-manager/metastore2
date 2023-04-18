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
package edu.kit.datamanager.metastore2.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.kit.datamanager.entities.EtagSupport;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.json.CustomInstantDeserializer;
import edu.kit.datamanager.util.json.CustomInstantSerializer;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.MediaType;

/**
 * Record for a metadata document.
 *
 * @author jejkal
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MetadataSchemaRecord implements EtagSupport, Serializable {

  public final static String RESOURCE_TYPE = "application/vnd.datamanager.schema-record+json";

  public final static MediaType METADATA_SCHEMA_RECORD_MEDIA_TYPE = MediaType.valueOf(RESOURCE_TYPE);

  public enum SCHEMA_TYPE {
    JSON,
    XML
    //,PROPERTIES
  }
  @Id
  @NotBlank(message = "The unqiue identifier of the schema used in the metadata repository for identifying the schema.")
  private String schemaId;
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "identifier_id", referencedColumnName = "id")
  @NotBlank(message = "A globally unique identifier pointing to this record, e.g. DOI, Handle, PURL.")
  private ResourceIdentifier pid;
  @NotBlank(message = "The schema version. The version is set by the schema registry and cannot be provided manually. Typically, a new schema version is only for metadata changes via PUT. In a few cases, \"\n"
          + "          + \"e.g. schema synchronization, a new version can be also created by overwriting an existing schema received from a remote, authoritative source.")
  private Long schemaVersion;

  @NotBlank(message = "A (human readable) label for the schema, e.g. a label used in user interfaces.")
  private String label;
  @NotBlank(message = "A (human readable) definition for the schema, e.g. the purpose or specific details.")
  private String definition;
  @NotBlank(message = "A (human readable) comment for the schema, e.g. examples, exceptions etc.")
  private String comment;

  @NotBlank(message = "The mime type this schema is associated with. It's value can be used for querying for a schema supporting a given mime type, in case no schema identifier is known.\"\n"
          + "          + \"The validation itself is done for a concrete schema identifier.")
  private String mimeType;
  @Enumerated(EnumType.STRING)
  @NotBlank(message = "The schema type used for quick decision making, e.g. to select a proper validator.")
  private SCHEMA_TYPE type;
  @NotNull(message = "The date the record has been initially created.")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @JsonSerialize(using = CustomInstantSerializer.class)
  private Instant createdAt;
  @NotNull(message = "The date the record had been updated the last time.")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @JsonSerialize(using = CustomInstantSerializer.class)
  private Instant lastUpdate;
  @Transient
  private final Set<AclEntry> acl = new HashSet<>();
  @NotBlank(message = "The schema document uri, e.g. pointing to a local file.")
  private String schemaDocumentUri;
  @NotBlank(message = "The SHA-1 hash of the associated schema file. The hash is used for comparison while synchonization.")
  private String schemaHash;
  @NotBlank(message = "The schema can be synchronized from a central registry. If 'true', synchronization will be skipped.")
  private Boolean doNotSync = true;
  @JsonIgnore
  private String eTag;

  /**
   * Set new access control list.
   *
   * @param newAclList new list with acls.
   */
  public void setAcl(Set<AclEntry> newAclList) {
    acl.clear();
    if (newAclList != null) {
      acl.addAll(newAclList);
    }
  }

  /**
   * Set creation date (truncated to milliseconds).
   *
   * @param instant creation date
   */
  public void setCreatedAt(Instant instant) {
    if (instant != null) {
      createdAt = instant.truncatedTo(ChronoUnit.MILLIS);
    } else {
      createdAt = null;
    }
  }

  /**
   * Set update date (truncated to milliseconds).
   *
   * @param instant update date
   */
  public void setLastUpdate(Instant instant) {
    if (instant != null) {
      lastUpdate = instant.truncatedTo(ChronoUnit.MILLIS);
    } else {
      lastUpdate = null;
    }
  }

  @Override
  @JsonIgnore
  public String getEtag() {
    return eTag;
  }
}
