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

import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import org.json.simple.JSONObject;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author hartmann-v
 */
public class EditorRequestMetadataTest {
  
  public EditorRequestMetadataTest() {
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
   * Test of builder method, of class EditorRequestMetadata.
   */
  @Test
  public void testBuilder() {
    System.out.println("builder");
    EditorRequestMetadata.EditorRequestMetadataBuilder expResult = null;
    EditorRequestMetadata.EditorRequestMetadataBuilder result = EditorRequestMetadata.builder();
    assertNotEquals(expResult, result);
  }

  /**
   * Test of getDataModel method, of class EditorRequestMetadata.
   */
  @Test
  public void testGetDataModel() {
    System.out.println("getDataModel");
    JSONObject expResult = new JSONObject();
    expResult.put("key", "value");
    EditorRequestMetadata instance =  EditorRequestMetadata.builder().dataModel(expResult).build();
    JSONObject result = instance.getDataModel();
    assertEquals(expResult, instance.getDataModel());
    assertNull(instance.getItems());
    assertNull(instance.getMetadataRecords());
    assertNull(instance.getUiForm());
  }

  /**
   * Test of getUiForm method, of class EditorRequestMetadata.
   */
  @Test
  public void testGetUiForm() {
    System.out.println("getUiForm");
    JSONObject expResult = new JSONObject();
    expResult.put("key", "value");
    EditorRequestMetadata instance =  EditorRequestMetadata.builder().uiForm(expResult).build();
    JSONObject result = instance.getUiForm();
    assertEquals(expResult, result);
    assertNull(instance.getDataModel());
    assertNull(instance.getItems());
    assertNull(instance.getMetadataRecords());
  }

  /**
   * Test of getMetadataRecords method, of class EditorRequestMetadata.
   */
  @Test
  public void testGetMetadataRecords() {
    System.out.println("getMetadataRecords");
    List<MetadataRecord> expResult = new ArrayList<>();
    EditorRequestMetadata instance =  EditorRequestMetadata.builder().metadataRecords(expResult).build();
    List<MetadataRecord> result = instance.getMetadataRecords();
    assertEquals(expResult, result);
    assertNull(instance.getDataModel());
    assertNull(instance.getItems());
    assertNull(instance.getUiForm());
  }

  /**
   * Test of getItems method, of class EditorRequestMetadata.
   */
  @Test
  public void testGetItems() {
    System.out.println("getItems");
    TabulatorItems[] expResult;
    expResult = new TabulatorItems[1];
    EditorRequestMetadata instance =  EditorRequestMetadata.builder().items(expResult).build();
    TabulatorItems[] result = instance.getItems();
    assertArrayEquals(expResult, result);
    assertNull(instance.getDataModel());
    assertNull(instance.getMetadataRecords());
    assertNull(instance.getUiForm());
  }
  
}
