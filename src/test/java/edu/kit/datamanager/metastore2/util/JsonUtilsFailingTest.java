/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import com.networknt.schema.SpecVersion.VersionFlag;
import edu.kit.datamanager.clients.SimpleServiceClient;
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
import org.junit.runner.RunWith;
import static org.mockito.ArgumentMatchers.any;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 *
 */
@RunWith(PowerMockRunner.class)
public class JsonUtilsFailingTest {

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
  private final static String dateExample = "{\n"
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
  private final String invalidJsonSchemaDocumentWithversiondraft201909 = "{\n"
          + "  \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n"
          + "  \"$id\": \"http://localhost:8040/api/v1/schemas/Test\",\n"
          + "  \"title\": \"Test\",\n"
          + "  \"type\": \"object\",\n"
          + "  \"properties\": {\n"
          + "    \"asd\": {\n"
          + "      \"type\": \"lllll\",\n"
          + "      \"pattern\": 100,\n"
          + "      \"maxLength\": \"asda\"\n"
          + "    }\n"
          + "  },\n"
          + "  \"allOf\": \"nope\"\n"
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

  public JsonUtilsFailingTest() {
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

  @PrepareForTest(SimpleServiceClient.class)
  @Test
  public void testValidateInvalidJsonSchemaDocumentWithNotExistingVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909ButWrongVersion");

    PowerMockito.mockStatic(SimpleServiceClient.class);

    PowerMockito.when(SimpleServiceClient.create(any(String.class))).thenThrow(new NullPointerException());
    String schemaDocument = invalidJsonSchemaDocumentWithversiondraft201909;
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument, VersionFlag.V201909);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.ERROR_VALIDATING_SCHEMA));
    }
  }

  /**
   * Test of validateJsonSchemaDocument method, of class JsonUtils.
   *
   * @throws java.io.IOException
   */
  @Test
  @PrepareForTest(VersionFlag.class)
  public void testValidateJsonSchemaDocumentWithSchemaDraft201909AsStreamButWrongVersion() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909AsStreamButWrongVersion");
    VersionFlag C = PowerMockito.mock(VersionFlag.class);
    Whitebox.setInternalState(C, "name", "newVersion");
    Whitebox.setInternalState(C, "ordinal", 4);

    PowerMockito.mockStatic(VersionFlag.class);
    PowerMockito.when(VersionFlag.values()).thenReturn(new VersionFlag[]{VersionFlag.V4, VersionFlag.V6, VersionFlag.V7, VersionFlag.V201909, C});
    InputStream schemaDocument = IOUtils.toInputStream(jsonSchemaWithversiondraft201909, ENCODING);
    try {
      JsonUtils.validateJsonSchemaDocument(schemaDocument, C);
      assertTrue(false);
    } catch (JsonValidationException jvex) {
      assertTrue(true);
      assertTrue(jvex.getMessage().contains(JsonUtils.UNKNOWN_JSON_SCHEMA));
    }
  }
}
