/*
 * Copyright 2023 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.dto;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import org.json.simple.JSONObject;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author hartmann-v
 */
public class EditorRequestSchemaTest {
  
  public EditorRequestSchemaTest() {
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

  /**
   * Test of builder method, of class EditorRequestSchema.
   */
  @Test
  public void testBuilder() {
    System.out.println("builder");
    EditorRequestSchema.EditorRequestSchemaBuilder expResult = null;
    EditorRequestSchema.EditorRequestSchemaBuilder result = EditorRequestSchema.builder();
    assertNotEquals(expResult, result);
  }

  /**
   * Test of getDataModel method, of class EditorRequestSchema.
   */
  @Test
  public void testGetDataModel() {
    System.out.println("getDataModel");
    JSONObject expResult = new JSONObject();
    expResult.put("key", "value");
    EditorRequestSchema instance =  EditorRequestSchema.builder().dataModel(expResult).build();
    JSONObject result = instance.getDataModel();
    assertEquals(expResult, instance.getDataModel());
    assertNull(instance.getItems());
    assertNull(instance.getSchemaRecords());
    assertNull(instance.getUiForm());
  }

  /**
   * Test of getUiForm method, of class EditorRequestSchema.
   */
  @Test
  public void testGetUiForm() {
    System.out.println("getUiForm");
    JSONObject expResult = new JSONObject();
    expResult.put("key", "value");
    EditorRequestSchema instance =  EditorRequestSchema.builder().uiForm(expResult).build();
    JSONObject result = instance.getUiForm();
    assertEquals(expResult, result);
    assertNull(instance.getDataModel());
    assertNull(instance.getItems());
    assertNull(instance.getSchemaRecords());
  }

  /**
   * Test of getSchemaRecords method, of class EditorRequestSchema.
   */
  @Test
  public void testGetSchemaRecords() {
    System.out.println("getSchemaRecords");
    List<MetadataSchemaRecord> expResult = new ArrayList<>();
    EditorRequestSchema instance =  EditorRequestSchema.builder().schemaRecords(expResult).build();
    List<MetadataSchemaRecord> result = instance.getSchemaRecords();
    assertEquals(expResult, result);
    assertNull(instance.getDataModel());
    assertNull(instance.getItems());
    assertNull(instance.getUiForm());
  }

  /**
   * Test of getItems method, of class EditorRequestSchema.
   */
  @Test
  public void testGetItems() {
    System.out.println("getItems");
    TabulatorItems[] expResult;
    expResult = new TabulatorItems[1];
    EditorRequestSchema instance =  EditorRequestSchema.builder().items(expResult).build();
    TabulatorItems[] result = instance.getItems();
    assertArrayEquals(expResult, result);
    assertNull(instance.getDataModel());
    assertNull(instance.getSchemaRecords());
    assertNull(instance.getUiForm());
  }
  
}
