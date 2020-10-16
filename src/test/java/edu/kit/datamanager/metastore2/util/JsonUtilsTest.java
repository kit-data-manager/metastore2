/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.SpecVersion;
import edu.kit.datamanager.metastore2.exception.JsonValidationException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hartmann-v
 */
public class JsonUtilsTest {

  private final String emptySchema = "{}";
  private final String jsonSchemaWithversiondraft04 = "{\"$schema\": \"http://json-schema.org/draft-04/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private final String jsonSchemaWithversiondraft06 = "{\"$schema\": \"http://json-schema.org/draft-06/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private final String jsonSchemaWithversiondraft07 = "{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private final String jsonSchemaWithversiondraft201909 = "{\"$schema\": \"http://json-schema.org/draft/2019-09/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private final String moreComplexExample = "{\n"
          + "    \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n"
          + "    \"$id\": \"http://www.example.org/schema/json\",\n"
          + "    \"type\": \"object\",\n"
          + "    \"title\": \"Json schema for tests\",\n"
          + "    \"default\": {},\n"
          + "    \"required\": [\n"
          + "        \"string\",\n"
          + "        \"number\"\n"
          + "    ],\n"
          + "    \"properties\": {\n"
          + "        \"string\": {\n"
          + "            \"$id\": \"#/properties/string\",\n"
          + "            \"type\": \"string\",\n"
          + "            \"title\": \"The string schema\",\n"
          + "            \"description\": \"An explanation about the purpose of this instance.\",\n"
          + "            \"default\": \"no default\"\n"
          + "        },\n"
          + "        \"number\": {\n"
          + "            \"$id\": \"#/properties/number\",\n"
          + "            \"type\": \"integer\",\n"
          + "            \"title\": \"The number schema\",\n"
          + "            \"description\": \"An explanation about the purpose of this instance.\",\n"
          + "            \"default\": 0\n"
          + "        }\n"
          + "    },\n"
          + "    \"additionalProperties\": false\n"
          + "}";
  private final static String dateExample =  "{\n"
          + "    \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n"
          + "    \"$id\": \"http://www.example.org/schema/json\",\n"
          + "    \"type\": \"object\",\n"
          + "    \"title\": \"Json schema for tests\",\n"
          + "    \"default\": {},\n"
          + "    \"required\": [\n"
          + "        \"title\",\n"
          + "        \"date\"\n"
          + "    ],\n"
          + "    \"properties\": {\n"
          + "        \"title\": {\n"
          + "            \"$id\": \"#/properties/string\",\n"
          + "            \"type\": \"string\",\n"
          + "            \"title\": \"Title\",\n"
          + "            \"description\": \"Title of object.\"\n"
          + "        },\n"
          + "        \"date\": {\n"
          + "            \"$id\": \"#/properties/string\",\n"
          + "            \"type\": \"string\",\n"
          + "            \"format\": \"date\",\n"
          + "            \"title\": \"Date\",\n"
          + "            \"description\": \"Date of object\"\n"
          + "        }\n"
          + "    },\n"
          + "    \"additionalProperties\": false\n"
          + "}";

  private final String validJsonDocument = "{\"string\":\"any string\",\"number\":3}";
  private final String invalidJsonDocument1 = "{\"string\":\"any string\",\"number\":3,}";
  private final String invalidJsonDocument2 = "{\"string\":2,\"number\":3}";
  private final String invalidJsonDocument3 = "{\"string\":\"2\",\"number\":\"3\"}";
  private final String invalidJsonDocument4 = "{\"tring\":\"any string\",\"number\":3}";
  private final String invalidJsonDocument5 = "{\"string\":\"any string\",\"umber\":3}";
  private final String invalidJsonDocument6 = "{\"number\":3}";
  private final String invalidJsonDocument7 = "{\"string\":\"any string\"}";
  private final String invalidJsonDocument8 = "{\"string\":\"any string\",\"number\":3,\"additional\":1}";
  private final static String ENCODING = "UTF-8";
  
  private final static String validDateDocument = "{\"title\":\"any string\",\"date\": \"2020-10-16\"}";
  private final static String invalidDateDocument = "{\"title\":\"any string\",\"date\":\"2020-10-16T10:13:24\"}";

  public JsonUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testConstructor() {
    assertNotNull(new JsonUtils());
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithNull() {
    System.out.println("testValidateJsonSchemaDocumentWithNull");
    String schemaDocument = null;
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("argument \"content\" is null"));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentStreamWithNull() {
    System.out.println("testValidateJsonSchemaDocumentStreamWithNull");
    InputStream schemaDocument = null;
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.ERROR_READING_INPUT_STREAM));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithEmptyString() {
    System.out.println("testValidateJsonSchemaDocumentWithEmptyString");
    String schemaDocument = "";
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.EMPTY_SCHEMA_DETECTED));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonSchemaDocumentWithEmptyStream() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithEmptyStream");
    InputStream schemaDocument = IOUtils.toInputStream("", ENCODING);
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.EMPTY_SCHEMA_DETECTED));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithEmptyJson() {
    System.out.println("testValidateJsonSchemaDocumentWithEmptyJson");
    String schemaDocument = "{}";
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.EMPTY_SCHEMA_DETECTED));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonSchemaDocumentWithEmptyJsonStream() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithEmptyJsonStream");
    InputStream schemaDocument = IOUtils.toInputStream("{}", ENCODING);
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.EMPTY_SCHEMA_DETECTED));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft04() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft04");
    String schemaDocument = jsonSchemaWithversiondraft04;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft06() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft06");
    String schemaDocument = jsonSchemaWithversiondraft06;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft07() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft07");
    String schemaDocument = jsonSchemaWithversiondraft07;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft201909() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909");
    String schemaDocument = jsonSchemaWithversiondraft201909;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonSchemaDocumentAsStreamWithSchemaDraft201909() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentAsStreamWithSchemaDraft201909");
    InputStream schemaDocument = IOUtils.toInputStream(jsonSchemaWithversiondraft201909, ENCODING);
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft201909ButWrongVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909ButWrongVersion");
    String schemaDocument = jsonSchemaWithversiondraft201909;
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument, SpecVersion.VersionFlag.V4);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("Unknown MetaSchema"));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft201909AsStreamButWrongVersion() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909AsStreamButWrongVersion");
    InputStream schemaDocument = IOUtils.toInputStream(jsonSchemaWithversiondraft201909, ENCODING);
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument, SpecVersion.VersionFlag.V4);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("Unknown MetaSchema"));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft04ButWrongVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909ButWrongVersion");
    String schemaDocument = jsonSchemaWithversiondraft04;
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument, SpecVersion.VersionFlag.V201909);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("Unknown MetaSchema"));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaDraft04AsStreamButWrongVersion() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft04AsStreamButWrongVersion");
    InputStream schemaDocument = IOUtils.toInputStream(jsonSchemaWithversiondraft04, ENCODING);
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument, SpecVersion.VersionFlag.V201909);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("Unknown MetaSchema"));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaButNullVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaButNullVersion");
    try {
      String schemaDocument = jsonSchemaWithversiondraft04;
      SpecVersion.VersionFlag version = null;
      JsonUtils.validateJsonSchemaDocument(schemaDocument, version);
      assertTrue(false); //should not executed.
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.MISSING_SCHEMA_VERSION));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonSchemaDocumentWithSchemaAsStreamButNullVersion() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaAsStreamButNullVersion");
    try {
      InputStream schemaDocument = IOUtils.toInputStream(jsonSchemaWithversiondraft04, ENCODING);
      SpecVersion.VersionFlag version = null;
      JsonUtils.validateJsonSchemaDocument(schemaDocument, version);
      assertTrue(false); //should not executed.
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.MISSING_SCHEMA_VERSION));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithNullSchemaButVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaButNullVersion");
    try {
      String schemaDocument = null;
      SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
      JsonUtils.validateJsonSchemaDocument(schemaDocument, version);
      assertTrue(false); //should not executed.
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("argument \"content\" is null"));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithNullSchemaAsStreamButVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaAsStreamButNullVersion");
    try {
      InputStream schemaDocument = null;
      SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
      JsonUtils.validateJsonSchemaDocument(schemaDocument, version);
      assertTrue(false); //should not executed.
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.ERROR_READING_INPUT_STREAM));
    }
  }

  /**
   * Test of getJsonSchemaFromStringContent method, of class JsonUtils.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testValidateJsonSchemaDocument() throws Exception {
    System.out.println("getJsonSchemaFromStringContent");
    String schemaContent = jsonSchemaWithversiondraft201909;
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaContent, version);
    assertEquals(expResult, result);
  }

  /**
   * Test of getJsonSchemaFromStringContent method, of class JsonUtils.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testValidateJsonSchemaDocumentAsStream() throws Exception {
    System.out.println("testValidateJsonSchemaDocumentAsStream");
    InputStream schemaContent = IOUtils.toInputStream(jsonSchemaWithversiondraft201909, ENCODING);
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaContent, version);
    assertEquals(expResult, result);
  }

  /**
   * Test of getJsonSchemaFromStringContent method, of class JsonUtils.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetJsonSchemaFromStringContent() throws Exception {
    System.out.println("getJsonSchemaFromStringContent");
    String schemaContent = jsonSchemaWithversiondraft201909;
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    JsonSchema result = JsonUtils.getJsonSchemaFromString(schemaContent, version);
    assertNotNull(result);
  }

  /**
   * Test of getJsonNodeFromStringContent method, of class JsonUtils.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void testGetJsonNodeFromStringContent() throws Exception {
    System.out.println("getJsonNodeFromStringContent");
    String content = jsonSchemaWithversiondraft04;
    JsonNode result = JsonUtils.getJsonNodeFromString(content);
    assertNotNull(result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithComplexSchemaDraft201909() {
    System.out.println("testValidateJsonSchemaDocumentWithComplexSchemaDraft201909");
    String schemaDocument = moreComplexExample;
    boolean expResult = true;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateJson() {
    System.out.println("validateJson");
    String jsonDocument = validJsonDocument;
    String jsonSchema = moreComplexExample;
    boolean expResult = true;
    boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonAsStream() throws IOException {
    System.out.println("testValidateJsonAsStream");
    InputStream jsonDocument = IOUtils.toInputStream(validJsonDocument, ENCODING);
    InputStream jsonSchema = IOUtils.toInputStream(moreComplexExample, ENCODING);
    boolean expResult = true;
    boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonWithVersion() {
    System.out.println("testValidateJsonWithVersion");
    String jsonDocument = validJsonDocument;
    String jsonSchema = moreComplexExample;
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    boolean expResult = true;
    boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema, version);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonAsStreamWithVersion() throws IOException {
    System.out.println("testValidateJsonAsStreamWithVersion");
    InputStream jsonDocument = IOUtils.toInputStream(validJsonDocument, ENCODING);
    InputStream jsonSchema = IOUtils.toInputStream(moreComplexExample, ENCODING);
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    boolean expResult = true;
    boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema, version);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonWithEmptySchema() {
    System.out.println("testValidateJsonWithEmptySchema");
    String jsonDocument = validJsonDocument;
    String jsonSchema = emptySchema;
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    try {
      JsonUtils.validateJson(jsonDocument, jsonSchema, version);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.EMPTY_SCHEMA_DETECTED));
    }
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonAsStreamWithEmptySchema() throws IOException {
    System.out.println("testValidateJsonAsStreamWithEmptySchema");
    InputStream jsonDocument = IOUtils.toInputStream(validJsonDocument, ENCODING);
    InputStream jsonSchema = IOUtils.toInputStream(emptySchema, ENCODING);
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    try {
      JsonUtils.validateJson(jsonDocument, jsonSchema, version);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.EMPTY_SCHEMA_DETECTED));
    }
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonWithWrongVersion() {
    System.out.println("testValidateJsonWithWrongVersion");
    String jsonDocument = validJsonDocument;
    String jsonSchema = moreComplexExample;
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V7;
    try {
      JsonUtils.validateJson(jsonDocument, jsonSchema, version);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("Unknown MetaSchema"));
    }
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonAsStreamWithWrongVersion() throws IOException {
    System.out.println("testValidateJsonAsStreamWithWrongVersion");
    InputStream jsonDocument = IOUtils.toInputStream(validJsonDocument, ENCODING);
    InputStream jsonSchema = IOUtils.toInputStream(moreComplexExample, ENCODING);
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V7;
    try {
      JsonUtils.validateJson(jsonDocument, jsonSchema, version);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains("Unknown MetaSchema"));
    }
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonWithInvalidDocuments() {
    System.out.println("testValidateJsonWithInvalidDocuments");
    String[] jsonDocuments = {invalidJsonDocument1, invalidJsonDocument2, invalidJsonDocument3, invalidJsonDocument4, invalidJsonDocument5, invalidJsonDocument6, invalidJsonDocument7, invalidJsonDocument8};
    String jsonSchema = moreComplexExample;
    boolean expResult = false;
//  String jsonDocument = invalidJsonDocument2;
    for (String jsonDocument : jsonDocuments) {
      try {
        boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema);
        System.out.println(jsonDocument);
        assertEquals(expResult, result);
      } catch (JsonValidationException jvex) {
        assertTrue(true);
        assertTrue(jvex.getMessage().contains(JsonUtils.ERROR_VALIDATING_JSON_DOCUMENT));
      }
    }
  }

  /**
   * Test of validateJson method, of class JsonUtils.
   * @throws java.io.IOException
   */
  @Test
  public void testValidateJsonWithInvalidDocumentsAsStream() throws IOException {
    System.out.println("testValidateJsonWithInvalidDocumentsAsStream");
    String[] jsonDocuments = {invalidJsonDocument1, invalidJsonDocument2, invalidJsonDocument3, invalidJsonDocument4, invalidJsonDocument5, invalidJsonDocument6, invalidJsonDocument7, invalidJsonDocument8};
    boolean expResult = false;
//  String jsonDocument = invalidJsonDocument2;
    for (String jsonDocument : jsonDocuments) {
      try {
        InputStream inputStream = IOUtils.toInputStream(jsonDocument, ENCODING);
        InputStream jsonSchema = IOUtils.toInputStream(moreComplexExample, ENCODING);
        boolean result = JsonUtils.validateJson(inputStream, jsonSchema);
        assertEquals(expResult, result);
      } catch (JsonValidationException jvex) {
        assertTrue(true);
        assertTrue(jvex.getMessage().contains(JsonUtils.ERROR_VALIDATING_JSON_DOCUMENT));
      }
    }
  }


  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateDateJson() {
    System.out.println("validateDateJson");
    String jsonDocument = validDateDocument;
    String jsonSchema = dateExample;
    boolean expResult = true;
    boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema);
    assertEquals(expResult, result);
  }
  /**
   * Test of validateJson method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonWithInvalidDateDocument() {
    System.out.println("testValidateJsonWithInvalidDateDocument");
    String[] jsonDocuments = {invalidDateDocument};
    String jsonSchema = dateExample;
    boolean expResult = false;
//  String jsonDocument = invalidJsonDocument2;
    for (String jsonDocument : jsonDocuments) {
      try {
        boolean result = JsonUtils.validateJson(jsonDocument, jsonSchema);
        System.out.println(jsonDocument);
        assertEquals(expResult, result);
      } catch (JsonValidationException jvex) {
        assertTrue(true);
        assertTrue(jvex.getMessage().contains(JsonUtils.ERROR_VALIDATING_JSON_DOCUMENT));
      }
    }
  }
}
