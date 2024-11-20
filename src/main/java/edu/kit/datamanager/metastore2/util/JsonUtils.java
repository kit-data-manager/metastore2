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
import com.networknt.schema.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.metastore2.exception.JsonValidationException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
  public static final String UNKNOWN_JSON_SCHEMA = "Error: Unknown or not supported JSON schema version:";
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
  private static final ObjectMapper mapper = new ObjectMapper();
  
  JsonUtils() {
    //Utility class
  }

  /**
   * Validate JSON schema document based on detected JSON schema or version
   * 2020-12 if no schema is defined.
   *
   * @see https://json-schema.org/draft/2020-12/json-schema-core.html
   * @param jsonSchemaStream schema document as string
   * @return true if schema is valid.
   */
  public static boolean validateJsonSchemaDocument(InputStream jsonSchemaStream) throws JsonValidationException {
    String jsonSchema = transformStreamToString(jsonSchemaStream);

    return validateJsonSchemaDocument(jsonSchema);
  }

  /**
   * Validate JSON schema document based on detected JSON schema or version
   * 2020-12 if no schema is defined.
   *
   * @see https://json-schema.org/draft/2020-12/json-schema-core.html
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
   * @see https://json-schema.org/draft/2020-12/json-schema-core.html
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
   * @see https://json-schema.org/draft/2020-12/json-schema-core.html
   * @see VersionFlag
   * @param jsonSchema schema document as string
   * @param version use specific version
   * @return true if valid (throws JsonValidationException if not)
   */
  public static boolean validateJsonSchemaDocument(String jsonSchema, VersionFlag version) throws JsonValidationException {
    boolean valid = true;
    try {
      // validate schema with meta schema.
      validateJson(jsonSchema, getSchema(version));
      getJsonSchemaFromString(jsonSchema, version);
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
    VersionFlag version = VersionFlag.V202012;
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
    JsonSchema schema = factory.getSchema(schemaContent);
    checkSchema(schema);
    Optional<VersionFlag> optionalVersionFound = SpecVersionDetector.detectOptionalVersion(schema.getSchemaNode(), false);
    VersionFlag versionFound = version;
    if (!optionalVersionFound.isEmpty()) {
      versionFound = optionalVersionFound.get();
    }
    if (!Objects.equals(version, versionFound)) {
      LOG.error("Unknown MetaSchema or not matching: Expected: '{}' <-> Found: '{}'", version, versionFound);
      throw new JsonSchemaException("Unknown MetaSchema or not matching: Expected: '" + version + "' <-> Found: '" + versionFound + "'");
    }
    return schema;
  }

  /**
   * Read json document from string
   *
   * @param content json doucment
   * @return JsonNode represented by String
   * @throws Exception Not a valid json document.
   */
  protected static synchronized JsonNode getJsonNodeFromString(String content) throws Exception {
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
   * Test whether the schema has at least one validator.
   *
   * @param schema schema to check
   */
  protected static void checkSchema(JsonSchema schema) {
    if (schema.getSchemaNode().isEmpty()) {
      throw new JsonValidationException(EMPTY_SCHEMA_DETECTED);
    }
  }

  /**
   * Downloads the meta schema of the given version.
   *
   * @param version the given version
   * @return Content of the meta schema as String.
   */
  protected static String getSchema(VersionFlag version) {
    String content = null;
    URI resourceUrl = null;

    try {
      switch (version) {
        case V4:
        case V6:
        case V7:
        case V201909:
        case V202012:
          resourceUrl = new URI(version.getId());
          break;
        default:
          String message = String.format(UNKNOWN_JSON_SCHEMA + " '%s'", version);
          LOG.error(message);
          throw new JsonValidationException(message);
      }
      content = SimpleServiceClient
              .create(resourceUrl.toString())
              .accept(MediaType.TEXT_PLAIN)
              .getResource(String.class);
    } catch (Throwable tw) {
      LOG.error("Error reading URI '" + resourceUrl + "'", tw);
      throw new JsonValidationException("Error downloading resource from '" + resourceUrl + "'! -> " + tw.getMessage(), tw);
    }

    return content;
  }

}
