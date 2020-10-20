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
package edu.kit.datamanager.metastore2.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import edu.kit.datamanager.metastore2.exception.JsonValidationException;
import java.io.InputStream;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling json documents
 */
public class JsonUtils {
  public static final String ERROR_DETERMINE_SCHEMA_VERSION = "Error determining JSON schema version: ";
  public static final String EMPTY_SCHEMA_DETECTED = "Empty JSON schema is not allowed!";
  public static final String ERROR_VALIDATING_SCHEMA = "Error validating JSON schema: ";
  public static final String ERROR_READING_INPUT_STREAM = "Error reading from inputstream: ";
  public static final String ERROR_VALIDATING_JSON_DOCUMENT = "Error validating json!";
  public static final String MISSING_SCHEMA_VERSION = "No version defined!";
          /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);
  /**
   * Encoding for strings/inputstreams.
   */
  private static final String ENCODING = "UTF-8";
  /**
   * Mapper for parsing json.
   */
  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Validate JSON schema document based on detected JSON schema or version
   * 2019-09 if no schema is defined.
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @param jsonSchemaStream schema document as string
   * @return true if schema is valid.
   */
  public static boolean validateJsonSchemaDocument(InputStream jsonSchemaStream) throws JsonValidationException {
    String jsonSchema = transformStreamToString(jsonSchemaStream);

    return validateJsonSchemaDocument(jsonSchema);
  }

  /**
   * Validate JSON schema document based on detected JSON schema or version
   * 2019-09 if no schema is defined.
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @param jsonSchema schema document as string
   * @return true if schema is valid.
   */
  public static boolean validateJsonSchemaDocument(String jsonSchema) throws JsonValidationException {
    VersionFlag version = determineSchemaVersion(jsonSchema);

    return validateJsonSchemaDocument(jsonSchema, version);
  }

  /**
   * Validate JSON schema document based on JSON Schema.
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @see VersionFlag
   * @param jsonSchemaStream schema document as string
   * @param version use specific version
   * @return true if valid (throws JsonValidationException if not)
   */
  public static boolean validateJsonSchemaDocument(InputStream jsonSchemaStream, VersionFlag version) throws JsonValidationException {
    String jsonSchema = transformStreamToString(jsonSchemaStream);

    return validateJsonSchemaDocument(jsonSchema, version);
  }

  /**
   * Validate JSON schema document based on JSON Schema.
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @see VersionFlag
   * @param jsonSchema schema document as string
   * @param version use specific version
   * @return true if valid (throws JsonValidationException if not)
   */
  public static boolean validateJsonSchemaDocument(String jsonSchema, VersionFlag version) throws JsonValidationException {
    boolean valid = true;
    try {
      JsonSchema schema = getJsonSchemaFromString(jsonSchema, version);
      checkSchema(schema);
    } catch (Exception ex) {
      LOG.error("Unknown error", ex);
      String errorMessage = ex.getMessage();
      if (version == null) {
        errorMessage = MISSING_SCHEMA_VERSION;
      }
      throw new JsonValidationException(ERROR_VALIDATING_SCHEMA + errorMessage);
    }
    return valid;
  }

  /**
   * Validate json document by given json schema.
   *
   * @param jsonDocumentStream json document.
   * @param jsonSchemaStream json schema.
   * @return true if valid (throws JsonValidationException if not)
   */
  public static boolean validateJson(InputStream jsonDocumentStream, InputStream jsonSchemaStream) {
    String jsonDocument = transformStreamToString(jsonDocumentStream);
    String jsonSchema = transformStreamToString(jsonSchemaStream);

    return validateJson(jsonDocument, jsonSchema);
  }

  /**
   * Validate json document by given json schema.
   *
   * @param jsonDocument json document.
   * @param jsonSchema json schema.
   * @return true (throws exception if not valid)
   */
  public static boolean validateJson(String jsonDocument, String jsonSchema) {
    VersionFlag version = determineSchemaVersion(jsonSchema);

    return validateJson(jsonDocument, jsonSchema, version);
  }

  /**
   * Validate given json document by provided schema.
   *
   * @param jsonDocumentStream json document
   * @param jsonSchemaStream schema document
   * @param version version of schema document
   * @return true if valid (throws JsonValidationException if not)
   */
  public static boolean validateJson(InputStream jsonDocumentStream, InputStream jsonSchemaStream, VersionFlag version) {
    String jsonDocument = transformStreamToString(jsonDocumentStream);
    String jsonSchema = transformStreamToString(jsonSchemaStream);

    return validateJson(jsonDocument, jsonSchema, version);
  }

  /**
   * Validate given json document by provided schema.
   *
   * @param jsonDocument json document
   * @param jsonSchema schema document
   * @param version version of schema document
   * @return true if valid (throws JsonValidationException if not)
   */
  public static boolean validateJson(String jsonDocument, String jsonSchema, VersionFlag version) {
    boolean returnValue = false;
    StringBuilder errorMessage = new StringBuilder(ERROR_VALIDATING_JSON_DOCUMENT);
    try {
      JsonSchema jsonSchemaFromString = getJsonSchemaFromString(jsonSchema, version);
      checkSchema(jsonSchemaFromString);
      JsonNode jsonNode = getJsonNodeFromString(jsonDocument);
      Set<ValidationMessage> validate = jsonSchemaFromString.validate(jsonNode);
      for (ValidationMessage message : validate) {
        LOG.debug(message.getMessage());
        errorMessage.append("\n").append(message.getMessage());
      }
      returnValue = validate.isEmpty();
    } catch (Exception ex) {
      LOG.error("Error validating json! ", ex);
      errorMessage.append("\n").append(ex.getMessage());
    }
    if (!returnValue) {
      throw new JsonValidationException(errorMessage.toString());
    }

    return returnValue;
  }

  /**
   * Extract schema version from json schema.
   *
   * @see VersionFlag
   * @param jsonSchema schema document as string
   * @return version of the schema.
   * @throws JsonValidationException if schema is not in correct format.
   */
  private static VersionFlag determineSchemaVersion(String jsonSchema) throws JsonValidationException {
    VersionFlag version = VersionFlag.V201909;
    try {
      JsonNode jsonNode = getJsonNodeFromString(jsonSchema);
      version = SpecVersionDetector.detect(jsonNode);
    } catch (JsonSchemaException jvex) {
      // no version detected use newest version (v201909)
      LOG.warn("Error parsing json schema -> using newest version.", jvex);
    } catch (Exception ex) {
      LOG.error("Unknown error", ex);
      throw new JsonValidationException(ERROR_DETERMINE_SCHEMA_VERSION + ex.getMessage());
    }
    return version;
  }

  /**
   * Read json schema from string
   *
   * @param schemaContent schema document
   * @param version version of the schema
   * @return JsonSchema represented by string
   * @throws Exception Not a valid schema.
   */
  protected static JsonSchema getJsonSchemaFromString(String schemaContent, VersionFlag version) throws Exception {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(version);
    return factory.getSchema(schemaContent);
  }

  /**
   * Read json document from string
   *
   * @param content json doucment
   * @return JsonNode represented by String
   * @throws Exception Not a valid json document.
   */
  protected static JsonNode getJsonNodeFromString(String content) throws Exception {
    return mapper.readTree(content);
  }

  /**
   * Transform inputstream to string
   *
   * @param inputStream inputstream
   * @return string
   * @throws JsonValidationException Error reading from stream.
   */
  private static String transformStreamToString(InputStream inputStream) throws JsonValidationException {
    String string = null;
    try {
      string = IOUtils.toString(inputStream, ENCODING);
    } catch (Exception ex) {
      throw new JsonValidationException(ERROR_READING_INPUT_STREAM + ex.getMessage());
    }
    return string;
  }
  /**
   * Test wether the schema has at least one validator. 
   * 
   * @param schema schema to check
   */
  protected static void checkSchema(JsonSchema schema) {
      if (schema.getValidators().isEmpty()) {
        throw new JsonValidationException(EMPTY_SCHEMA_DETECTED);
      }
  }
}
