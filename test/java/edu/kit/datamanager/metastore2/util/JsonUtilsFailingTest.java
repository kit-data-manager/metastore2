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

  private final String jsonSchemaWithversiondraft201909 = "{"
          + "  \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\", "
          + "  \"properties\": {"
          + "    \"id\": {"
          + "      \"type\": \"number\""
          + "    }"
          + "  }"
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
