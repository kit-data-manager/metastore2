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
public class TabulatorItemsTest {
  
  public TabulatorItemsTest() {
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
   * Test of getTitle method, of class TabulatorItems.
   */
  @Test
  public void testGetTitle() {
    System.out.println("getTitle");
    TabulatorItems instance = new TabulatorItems();
    String expResult = null;
    String result = instance.getTitle();
    assertEquals(expResult, result);
  }

  /**
   * Test of getField method, of class TabulatorItems.
   */
  @Test
  public void testGetField() {
    System.out.println("getField");
    TabulatorItems instance = new TabulatorItems();
    String expResult = null;
    String result = instance.getField();
    assertEquals(expResult, result);
  }

  /**
   * Test of getEditor method, of class TabulatorItems.
   */
  @Test
  public void testGetEditor() {
    System.out.println("getEditor");
    TabulatorItems instance = new TabulatorItems();
    String expResult = null;
    String result = instance.getEditor();
    assertEquals(expResult, result);
  }

  /**
   * Test of getFormatter method, of class TabulatorItems.
   */
  @Test
  public void testGetFormatter() {
    System.out.println("getFormatter");
    TabulatorItems instance = new TabulatorItems();
    String expResult = null;
    String result = instance.getFormatter();
    assertEquals(expResult, result);
  }

  /**
   * Test of getFormatterParams method, of class TabulatorItems.
   */
  @Test
  public void testGetFormatterParams() {
    System.out.println("getFormatterParams");
    TabulatorItems instance = new TabulatorItems();
    TabulatorFormatterParam expResult = null;
    TabulatorFormatterParam result = instance.getFormatterParams();
    assertEquals(expResult, result);
  }
  
}
