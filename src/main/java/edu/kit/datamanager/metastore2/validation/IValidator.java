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
package edu.kit.datamanager.metastore2.validation;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;

import java.io.File;
import java.io.InputStream;

/**
 *
 * Interface for validating metadata (schema) documents.
 */
public interface IValidator {

  /**
   * Get an instance of the validator.
   *
   * @return instance of validator.
   */
  default IValidator getInstance() {
    return this;
  }

  /**
   * Supports the given schema type.
   *
   * @see MetadataSchemaRecord#type
   * @param type Type of the schema.
   * 
   * @return supports schema type or not.
   * @deprecated Should be replaced by 'supportsMimeType'.
   */
  @Deprecated
  boolean supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE type);

  /**
   * Supports the given MIME type.
   *
   * @see https://www.iana.org/assignments/media-types/media-types.xhtml
   * @param mimetype Type of the schema.
   * 
   * @return supports schema type or not.
   */
  boolean supportsMimetype(String mimetype);

  /**
   * Is given schema valid.
   *
   * @param schemaStream Stream containing schema.
   *
   * @return Schema is valid or not.
   */
  boolean isSchemaValid(InputStream schemaStream);

  /**
   * Validate metadata document with metadata schema. In case of invalid schema
   * an explanation may be available via getErrorMessage.
   *
   * @param schemaFile File containing schema.
   * @param metadataDocumentStream Stream containing metadata document.
   * 
   * @return valid or not.
   */
  boolean validateMetadataDocument(File schemaFile, InputStream metadataDocumentStream);

  /**
   * Get the error message if available.
   *
   * @return error message or NULL if validation was successful.
   */
  String getErrorMessage();
}
