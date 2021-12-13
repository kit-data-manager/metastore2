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

import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Data
public class DataRecord implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;
  @NotBlank(message = "The unqiue identifier of the metadata.")
  private String metadataId;
  @NotBlank(message = "The version number of the metadata document.")
  private Long version;
  @NotBlank(message = "The unqiue identifier of the schema used in the metadata repository for identifying the schema.")
  private String schemaId;
  @NotBlank(message = "The version of the schema.")
  private Long schemaVersion;
  @NotBlank(message = "The timestamp of the last update on this resource.")
  private Instant lastUpdate;
  @NotBlank(message = "The SHA-1 hash of the associated metadata file. The hash is used for comparison while updating.")
  private String documentHash;
  @NotBlank(message = "The schema document uri, e.g. pointing to a local file.")
  private String metadataDocumentUri;

}
