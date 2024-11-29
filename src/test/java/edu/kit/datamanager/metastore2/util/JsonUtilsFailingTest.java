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
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class JsonUtilsFailingTest {

  private final String jsonSchemaWithversiondraft201909 = "{"
          + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\", "
          + "  \"properties\": {"
          + "    \"id\": {"
          + "      \"type\": \"number\""
          + "    }"
          + "  }"
          + "}";
  private final String invalidJsonSchemaDocumentWithversiondraft201909 = "{\n"
          + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
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
  private final static String ENCODING = "UTF-8";

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

  @Test
  public void testValidateInvalidJsonSchemaDocumentWithNotExistingVersion() {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909ButWrongVersion");

    String schemaDocument = invalidJsonSchemaDocumentWithversiondraft201909;

    try ( MockedStatic<SimpleServiceClient> utilities = Mockito.mockStatic(SimpleServiceClient.class)) {
      utilities.when(() -> SimpleServiceClient.create(any(String.class)))
              .thenThrow(new NullPointerException());
      JsonUtils.validateJsonSchemaDocument(schemaDocument, VersionFlag.V201909);
      fail();
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
  public void testValidateJsonSchemaDocumentWithSchemaDraft201909AsStreamButWrongVersion() throws IOException {
    System.out.println("testValidateJsonSchemaDocumentWithSchemaDraft201909AsStreamButWrongVersion");
    InputStream schemaDocument = IOUtils.toInputStream(jsonSchemaWithversiondraft201909, ENCODING);
    try {
      // unfortunately there is no suitable way to test 'default' branch inside switch with enum
      VersionFlag version = mock(VersionFlag.class);
      when(version.ordinal()).thenReturn(Integer.valueOf(VersionFlag.values().length));
      JsonUtils.validateJsonSchemaDocument(schemaDocument, version);
      fail();
    } catch (JsonValidationException jvex) {
      assertTrue(true);
    }
  }
}
