/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.validation;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import java.io.File;
import java.io.InputStream;

/**
 *
 * @author Torridity
 */
public interface IValidator {

  /**
   * Supports the given schema type.
   *
   * @see MetadataSchemaRecord#type
   *
   * @param type Type of the schema.
   *
   * @return supports schema type or not.
   */
  boolean supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE type);

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
