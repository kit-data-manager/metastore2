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
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.json.CustomInstantDeserializer;
import edu.kit.datamanager.util.json.CustomInstantSerializer;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.MediaType;

/**
 * Record for a metadata document.
 *
 * @author jejkal
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MetadataRecord implements EtagSupport, Serializable {

  public final static String RESOURCE_TYPE = "application/vnd.datamanager.metadata-record+json";

  public final static MediaType METADATA_RECORD_MEDIA_TYPE = MediaType.valueOf(RESOURCE_TYPE);

  @Id
  @NotBlank(message = "The unique identify of the record.")
  private String id;
  @NotBlank(message = "A globally unique identifier pointing to this record, e.g. DOI, Handle, PURL.")
  private ResourceIdentifier pid;
  @NotBlank(message = "The unqiue identifier of the resource the metadata record is related to. The value might be a URL, a PID or something else resolvable by an external tool/service.")
  private ResourceIdentifier relatedResource;
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
  @NotBlank(message = "The unqiue identifier of the schema used by this record. Possible types: INTERNAL, URL. Will be extended...")
  private ResourceIdentifier schema;
  @NotNull(message = "The version of the schema. If no version is provided the current schema is used.")
  private Long schemaVersion;
  @NotNull(message = "The record version. The version is set by the metadata registry and cannot be provided manually.")
  private Long recordVersion;

  @NotNull(message = "A list of access control entries for resticting access.")
  @OneToMany(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private final Set<AclEntry> acl = new HashSet<>();
  @NotBlank(message = "The metadata document uri, e.g. pointing to a local file.")
  private String metadataDocumentUri;
  @NotBlank(message = "The SHA-1 hash of the associated metadata file. The hash is used for comparison while updating.")
  private String documentHash;
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

  /**
   * Get (internal) schema identifier.
   *
   * @return schema identifier.
   */
  @JsonIgnore
  public String getSchemaId() {
    if (schema.getIdentifierType() == ResourceIdentifier.IdentifierType.INTERNAL) {
      return schema.getIdentifier();
    }
    throw new BadArgumentException("URL as schema identifier is not supported yet! (Coming soon)");
  }
}
