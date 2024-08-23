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
package edu.kit.datamanager.metastore2.domain.oaipmh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Record for OAI-PMH holding
 * <ul>
 * <li> prefix </li>
 * <li> schema </li>
 * <li> namespace </li> </ul>
 *
 * @author jejkal
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MetadataFormat implements Serializable {

  @Id
  @NotBlank(message = "The unqiue prefix of the schema used in the metadata repository for identifying the schema.")
  private String metadataPrefix;

  @NotBlank(message = "URI for accessing the schema.")
  private String schema;

  @NotBlank(message = "Namespace of the schema.")
  private String metadataNamespace;
}
