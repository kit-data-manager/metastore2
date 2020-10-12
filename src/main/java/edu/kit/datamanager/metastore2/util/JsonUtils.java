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
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling json documents
 */
public class JsonUtils {

  /**
   * Logger for messages.
   */
  private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);
  /**
   * Mapper for parsing json.
   */
  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Validate JSON schema document based on detected JSON schema or version
   * 2019-09 if no schema is defined.
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @param schemaDocument schema document as string
   * @return true if schema is valid.
   */
  public static boolean validateJsonSchemaDocument(String schemaDocument) throws JsonValidationException {
    VersionFlag version = determineSchemaVersion(schemaDocument);
    
    return validateJsonSchemaDocument(schemaDocument, version);
  }

  /**
   * Extract schema version from json schema.
   *
   * @see VersionFlag
   * @param schemaDocument schema document as string
   * @return version of the schema.
   */
  private static VersionFlag determineSchemaVersion(String schemaDocument) throws JsonValidationException {
    VersionFlag version = VersionFlag.V201909;
    try {
      JsonNode jsonNode = getJsonNodeFromStringContent(schemaDocument);
      version = SpecVersionDetector.detect(jsonNode);
    } catch (JsonSchemaException jvex) {
      // no version detected use newest version (v201909)
      LOG.warn("Error pasing json schema using newest version.", jvex);
    } catch (Exception ex) {
      LOG.error("Unknown error", ex);
      throw new JsonValidationException("Error determining JSON schema version: " + ex.getMessage());
    }
    return version;
  }

  /**
   * Validate JSON schema document based on JSON Schema.
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @see VersionFlag
   * @param schemaDocument schema document as string
   * @param version use specific version
   * @return
   */
  public static boolean validateJsonSchemaDocument(String schemaDocument, VersionFlag version) throws JsonValidationException {
    boolean valid = false;
    try {
      JsonSchema schema = getJsonSchemaFromStringContent(schemaDocument, version);
      if (schema != null) {
        if (!schema.getValidators().isEmpty()) {
          valid = true;
        }
      }
    } catch (Exception ex) {
      LOG.error("Unknown error", ex);
      String errorMessage = ex.getMessage();
      if (version == null) {
        errorMessage = new String("No version defined!");
      }
      throw new JsonValidationException("Error validating JSON schema: " + errorMessage);
    }
    return valid;
  }
  /** 
   * Validate json document by given json schema.
   * @param jsonDocument json document.
   * @param jsonSchema  json schema.
   * @return  true (throws exception if not valid)
   */
  public static boolean validateJson(String jsonDocument, String jsonSchema) {
    VersionFlag version = determineSchemaVersion(jsonSchema);

    return validateJson(jsonDocument, jsonSchema, version);
  }

  public static boolean validateJson(String jsonDocument, String jsonSchema, VersionFlag version) {
    boolean returnValue = false;
    StringBuffer errorMessage = new StringBuffer("Error validating json!\n");
    try {
      JsonSchema jsonSchemaFromString = getJsonSchemaFromStringContent(jsonSchema, version);
      JsonNode jsonNode = getJsonNodeFromStringContent(jsonDocument);
      Set<ValidationMessage> validate = jsonSchemaFromString.validate(jsonNode);
      for (ValidationMessage message : validate) {
        LOG.debug(message.getMessage());
        errorMessage.append(message.getMessage()).append("\n");
      }
      returnValue = validate.isEmpty();
    } catch (Exception ex) {
      LOG.error("Error validating json! ", ex);
      errorMessage.append(ex.getMessage());
    }
    if (!returnValue) {
      throw new JsonValidationException(errorMessage.toString());
    }

    return returnValue;
  }

  protected static JsonSchema getJsonSchemaFromStringContent(String schemaContent, VersionFlag version) throws Exception {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(version);
    return factory.getSchema(schemaContent);
  }

  protected static JsonNode getJsonNodeFromStringContent(String content) throws Exception {
    return mapper.readTree(content);
  }
}
