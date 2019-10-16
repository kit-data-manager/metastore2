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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import edu.kit.datamanager.util.json.CustomInstantDeserializer;
import edu.kit.datamanager.util.json.CustomInstantSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "Metadata schema record")
@Data
@IdClass(MetadataSchemaId.class)
public class MetadataSchemaRecord implements Serializable{

  public enum SCHEMA_TYPE{
    JSON,
    XML
    //,PROPERTIES
  }
  @Id
  @ApiModelProperty(value = "The unqiue identifier of the schema used in the metadata repository for identifying the schema.", dataType = "String", example = "dc")
  private String schemaId;
  @Id
  @ApiModelProperty(value = "The schema version.", dataType = "Integer")
  private Integer schemaVersion;

  @ApiModelProperty(value = "The mime type this schema is associated with. The mime type should be as specific as possible in order to allow an easy mapping of documents matching the schema.", dataType = "String", example = "application/vnd.datamanager.data-resource+json")
  private String mimeType;
  @Enumerated(EnumType.STRING)
  @ApiModelProperty(value = "The schema type used for quick decision making, e.g. to select a proper validator.", example = "JSON")
  private SCHEMA_TYPE type;
  @ApiModelProperty(value = "The date the record has been initially created.", example = "2017-05-10T10:41:00Z", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @JsonSerialize(using = CustomInstantSerializer.class)
  private Instant createdAt;
  @ApiModelProperty(value = "The date the record had been updated the last time.", example = "2017-05-10T10:41:00Z", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @JsonSerialize(using = CustomInstantSerializer.class)
  private Instant lastUpdate;
  @ApiModelProperty(value = "A list of access control entries for resticting access.")
  @OneToMany(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  //@JoinColumn(name = "resource_id")
  private Set<AclEntry> acl = new HashSet<>();
  @ApiModelProperty(value = "The schema document uri, e.g. pointing to a local file.")
  private String schemaDocumentUri;

}
