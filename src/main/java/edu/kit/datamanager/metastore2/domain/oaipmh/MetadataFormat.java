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

import edu.kit.datamanager.metastore2.domain.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.kit.datamanager.entities.EtagSupport;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import edu.kit.datamanager.util.json.CustomInstantDeserializer;
import edu.kit.datamanager.util.json.CustomInstantSerializer;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.MediaType;

/**
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
