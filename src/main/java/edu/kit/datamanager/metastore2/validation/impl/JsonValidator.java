/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.validation.impl;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.exception.JsonValidationException;
import edu.kit.datamanager.metastore2.util.JsonUtils;
import edu.kit.datamanager.metastore2.validation.IValidator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Class for validating JSON files.
  */
@Component
public class JsonValidator implements IValidator {

  private static final Logger LOG = LoggerFactory.getLogger(JsonValidator.class);

  private String errorMessage;

  @Override
  public boolean supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE type) {
    return MetadataSchemaRecord.SCHEMA_TYPE.JSON.equals(type);
  }

  @Override
  public boolean isSchemaValid(InputStream schemaStream) {
    LOG.trace("Checking JSON schema for validity.");
    boolean result = false;
    try {
      result = JsonUtils.validateJsonSchemaDocument(schemaStream);
      LOG.trace("Is JSON schema valid? -> {}", result);
    } catch (JsonValidationException jvex) {
      LOG.error("Failed to validate JSON schema.", jvex);
      errorMessage = jvex.getMessage();
    }
    return result;
  }

  @Override
  public boolean validateMetadataDocument(File schemaFile, InputStream metadataDocumentStream) {
    LOG.trace("Checking metadata document using schema at {}.", schemaFile.getAbsolutePath());
    boolean valid = false;
    InputStream jsonSchemaDocumentStream;

    try {
      LOG.trace("Reading metadata document from stream.");
      jsonSchemaDocumentStream = FileUtils.openInputStream(schemaFile);

      LOG.trace("Validate JSON document");
      valid = JsonUtils.validateJson(metadataDocumentStream, jsonSchemaDocumentStream);
      LOG.trace("Is JSON document valid? -> {}", valid);
    } catch (IOException ex) {
      LOG.error("Error reading schema at '{}'", schemaFile.getAbsolutePath());
      errorMessage = ex.getMessage();
    } catch (JsonValidationException jvex) {
      LOG.error("Failed to validate JSON document.", jvex);
      errorMessage = jvex.getMessage();
    }

    return valid;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }
}
