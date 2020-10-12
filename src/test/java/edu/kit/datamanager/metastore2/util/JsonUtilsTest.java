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

  private String jsonSchemaWithversiondraft04 = "{\"$schema\": \"http://json-schema.org/draft-04/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private String jsonSchemaWithversiondraft06 = "{\"$schema\": \"http://json-schema.org/draft-06/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private String jsonSchemaWithversiondraft07 = "{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";
  private String jsonSchemaWithversiondraft201909 = "{\"$schema\": \"http://json-schema.org/draft/2019-09/schema#\", \"properties\": { \"id\": {\"type\": \"number\"}}}";

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
  public void testValidateJsonSchemaDocumentWithEmptyString() {
    System.out.println("testValidateJsonSchemaDocumentWithEmptyString");
    String schemaDocument = "";
    boolean expResult = false;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   */
  @Test
  public void testValidateJsonSchemaDocumentWithEmptyJson() {
    System.out.println("testValidateJsonSchemaDocumentWithEmptyJson");
    String schemaDocument = "{}";
    boolean expResult = false;
    boolean result = JsonUtils.validateJsonSchemaDocument(schemaDocument);
    assertEquals(expResult, result);
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
      assertTrue(jvex.getMessage().contains("No version defined"));
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
   * Test of getJsonSchemaFromStringContent method, of class JsonUtils.
   */
  @Test
  public void testGetJsonSchemaFromStringContent() throws Exception {
    System.out.println("getJsonSchemaFromStringContent");
    String schemaContent = jsonSchemaWithversiondraft201909;
    SpecVersion.VersionFlag version = SpecVersion.VersionFlag.V201909;
    JsonSchema result = JsonUtils.getJsonSchemaFromStringContent(schemaContent, version);
    assertNotNull(result);
  }

  /**
   * Test of getJsonNodeFromStringContent method, of class JsonUtils.
   */
  @Test
  public void testGetJsonNodeFromStringContent() throws Exception {
    System.out.println("getJsonNodeFromStringContent");
    String content = jsonSchemaWithversiondraft04;
    JsonNode result = JsonUtils.getJsonNodeFromStringContent(content);
     assertNotNull(result);
  }
}
