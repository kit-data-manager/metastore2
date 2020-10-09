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
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.SpecVersionDetector;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling json documents
 */
public class JsonUtils {

  private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Validate JSON schema document based on JSON Schema (version 2019-09).
   *
   * @see http://json-schema.org/draft/2019-09/json-schema-core.html
   * @param schemaDocument schema document as string
   * @return
   */
  public static boolean validateJsonSchemaDocument(String schemaDocument) {
    VersionFlag version = VersionFlag.V201909;
    try {
      JsonNode jsonNode = getJsonNodeFromStringContent(schemaDocument);
      version = SpecVersionDetector.detect(jsonNode);
    } catch (Exception ex) {
      // no version detected use newest version (v201909)
      LOG.error("Error pasing json schema.", ex);
    }
    return validateJsonSchemaDocument(schemaDocument, version);
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
  public static boolean validateJsonSchemaDocument(String schemaDocument, VersionFlag version) {
    boolean valid = false;
    JsonSchema schema = getJsonSchemaFromStringContent(schemaDocument, version);
    if (!schema.getValidators().isEmpty()) {
      valid = true;
    }
    return valid;
  }

  protected static JsonSchema getJsonSchemaFromStringContent(String schemaContent, VersionFlag version) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(version);
    return factory.getSchema(schemaContent);
  }

  protected static JsonNode getJsonNodeFromStringContent(String content) throws IOException {
    return mapper.readTree(content);
  }

  public static void main(String[] args) {
    System.out.println("valid? " + validateJsonSchemaDocument("{}"));

    System.out.println("valid? " + validateJsonSchemaDocument("{\"$schema\": \"http://json-schema.org/draft-06/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}"));
    System.out.println("Ende");
  }
}
