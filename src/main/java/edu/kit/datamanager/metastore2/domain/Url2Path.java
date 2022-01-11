/*
 * Copyright 2021 Karlsruhe Institute of Technology.
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

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Data
public class Url2Path implements Serializable {

  @Id
  @NotBlank(message = "The unique identifier of schema document.")
  private String url;
  @NotBlank(message = "Path of schema document linked to identifier.")
  private String path;
  @NotBlank(message = "Version of the schema document.")
  private Long version;
  @Enumerated(EnumType.STRING)
  @NotBlank(message = "The schema type used for quick decision making, e.g. to select a proper validator.")
  private SCHEMA_TYPE type;
}
