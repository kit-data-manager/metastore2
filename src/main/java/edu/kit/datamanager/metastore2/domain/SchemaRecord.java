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

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Simplified record for a schema document.
 *
 * @author jejkal
 */
@Entity
@Data
@Table(uniqueConstraints = {
  @UniqueConstraint(columnNames = {"version", "schemaId"})})
public class SchemaRecord implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;
  @NotBlank(message = "The unqiue (internal) identifier of the schema used in the metadata repository for identifying the schema.")
  private String schemaId;
  @NotNull(message = "The schema version. The version is set by the schema registry and cannot be provided manually. Typically, a new schema version is only for metadata changes via PUT. In a few cases, \"\n"
          + "          + \"e.g. schema synchronization, a new version can be also created by overwriting an existing schema received from a remote, authoritative source.")
  private Long version;
  @Enumerated(EnumType.STRING)
  @NotNull(message = "The schema type used for quick decision making, e.g. to select a proper validator.")
  private MetadataSchemaRecord.SCHEMA_TYPE type;
  @NotBlank(message = "The schema document uri, e.g. pointing to a local file.")
  private String schemaDocumentUri;
  @NotBlank(message = "The SHA-1 hash of the associated metadata file. The hash is used for comparison while updating.")
  private String documentHash;
//  @NotBlank(message = "Alternate id of schema document.")
  private String alternateId;
  
  public String getSchemaIdWithoutVersion() {
    String pureSchemaId = null;
    if (schemaId != null) {
      pureSchemaId = schemaId.split("/", -1)[0];
    } 
    return pureSchemaId;
  }
}
