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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Simplified record for linked resources for metadata document.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Table(uniqueConstraints = {
  @UniqueConstraint(columnNames = {"relatedResource", "schemaId"})})
public class LinkedMetadataRecord implements Serializable {

  public LinkedMetadataRecord() {
  }

  public LinkedMetadataRecord(MetadataRecord metadataRecord) {
    schemaId = metadataRecord.getSchema().getIdentifier();
    relatedResource = metadataRecord.getRelatedResource().getIdentifier();
  }
  @Id
  @GeneratedValue
  private Long id;

  @NotBlank(message = "The unqiue identifier of the schema used in the metadata repository for identifying the schema.")
  private String schemaId;
  @NotBlank(message = "The unqiue identifier of the related source.")
  private String relatedResource;

}
