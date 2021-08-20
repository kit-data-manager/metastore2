/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.domain;

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
public class LinkedMetadataRecordTest {

  public LinkedMetadataRecordTest() {
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
   * Test of getId method, of class LinkedMetadataRecord.
   */
  @Test
  public void testGetIdAndSchemaIdAndRelatedResource() {
    System.out.println("getId");
    LinkedMetadataRecord instance = new LinkedMetadataRecord();
    Long expResult = null;
    Long result = instance.getId();
    assertEquals(expResult, result);
    String expResultString = null;
    String resultString = instance.getSchemaId();
    assertEquals(expResultString, resultString);
    resultString = instance.getRelatedResource();
    assertEquals(expResultString, resultString);
  }


  /**
   * Test of equals method, of class LinkedMetadataRecord.
   */
  @Test
  public void testEquals() {
    System.out.println("equals");
    Object o = null;
    LinkedMetadataRecord instance = new LinkedMetadataRecord();
    boolean expResult = false;
    boolean result = instance.equals(o);
    assertEquals(expResult, result);
  }
  @Test
  public void testConstructor() {
    System.out.println("testConstructor");
    MetadataRecord mr = new MetadataRecord();
    String schemaId = "schemaId";
    ResourceIdentifier relatedResoure = ResourceIdentifier.factoryInternalResourceIdentifier("relatedResource");
    mr.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
    mr.setRelatedResource(relatedResoure);
    LinkedMetadataRecord instance = new LinkedMetadataRecord(mr);
    assertEquals(schemaId, instance.getSchemaId());
    assertEquals(relatedResoure.getIdentifier(), instance.getRelatedResource());
  }

  /**
   * Test of canEqual method, of class LinkedMetadataRecord.
   */
  @Test
  public void testCanEqual() {
    System.out.println("canEqual");
    Object other = null;
    LinkedMetadataRecord instance = new LinkedMetadataRecord();
    boolean expResult = false;
    boolean result = instance.canEqual(other);
    assertEquals(expResult, result);
  }

  /**
   * Test of hashCode method, of class LinkedMetadataRecord.
   */
  @Test
  public void testHashCode() {
    System.out.println("hashCode");
    LinkedMetadataRecord instance = new LinkedMetadataRecord();
    int expResult = 357642;
    int result = instance.hashCode();
    assertEquals(expResult, result);
  }

  /**
   * Test of toString method, of class LinkedMetadataRecord.
   */
  @Test
  public void testToString() {
    System.out.println("toString");
    LinkedMetadataRecord instance = new LinkedMetadataRecord();
    String expResult = "LinkedMetadataRecord(id=null, schemaId=null, relatedResource=null)";
    String result = instance.toString();
    assertEquals(expResult, result);
  }

}
