/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.configuration;

import edu.kit.datamanager.metastore2.validation.IValidator;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;

import java.util.List;

/**
 * Holds all properties needed to manage data resources.
 */
public class MetastoreConfiguration extends RepoBaseConfiguration {

  private List<String> schemaRegistries;

  private List<IValidator>validators;

  /**
   * Get schema registries.
   *
   * @return the schemaRegistries
   */
  public List<String> getSchemaRegistries() {
    return schemaRegistries;
  }

  /**
   * Set schema registries.
   *
   * @param schemaRegistries the schemaRegistries to set
   */
  public void setSchemaRegistries(List<String> schemaRegistries) {
    this.schemaRegistries = schemaRegistries;
  }

  /**
   * Get validators for schemas.
   *
   * @return the validators
   */
  public List<IValidator> getValidators() {
    return validators;
  }

  /**
   * Set validators for schemas.
   *
   * @param validators the validators to set
   */
  public void setValidators(List<IValidator> validators) {
    this.validators = validators;
  }

}
