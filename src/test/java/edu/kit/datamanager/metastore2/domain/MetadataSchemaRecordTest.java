/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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
public class MetadataSchemaRecordTest {
  
  String metadataSchemaRecordAsJson = "{\"schemaId\":\"dc\",\"mimeType\":\"application/xml\",\"type\":\"XML\",\"createdAt\":\"2020-04-15T05:34:01.5465Z\",\"lastUpdate\":\"2020-04-15T05:34:01.546502Z\",\"acl\":[{\"id\":11,\"sid\":\"SELF\",\"permission\":\"WRITE\"}],\"schemaDocumentUri\":\"http://localhost/api/v1/schemas/dc\",\"locked\":false}";
  
  public MetadataSchemaRecordTest() {
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
   * Test of setAcl method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetAcl() {
    System.out.println("setAcl");
    Set<AclEntry> newAclList = new HashSet<>();
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    instance.setAcl(newAclList);
    assertEquals(newAclList, instance.getAcl());
  }

   /**
   * Test of getSchemaId method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetSchemaId() {
    System.out.println("getSchemaId");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "schema";
    instance.setSchemaId(expResult);
    String result = instance.getSchemaId();
    assertEquals(expResult, result);
  }

  /**
   * Test of getSchemaVersion method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetSchemaVersion() {
    System.out.println("getSchemaVersion");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    Long expResult = 3l;
    instance.setSchemaVersion(expResult);
    Long result = instance.getSchemaVersion();
    assertEquals(expResult, result);
  }

  /**
   * Test of getLabel method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetLabel() {
    System.out.println("getLabel");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "label";
    instance.setLabel(expResult);
    String result = instance.getLabel();
    assertEquals(expResult, result);
  }

  /**
   * Test of getDefinition method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetDefinition() {
    System.out.println("getDefinition");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "definition";
    instance.setDefinition(expResult);
    String result = instance.getDefinition();
    assertEquals(expResult, result);
  }

  /**
   * Test of getComment method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetComment() {
    System.out.println("getComment");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "comment";
    instance.setComment(expResult);
    String result = instance.getComment();
    assertEquals(expResult, result);
  }

  /**
   * Test of getMimeType method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetMimeType() {
    System.out.println("getMimeType");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "mimetype";
    instance.setMimeType(expResult);
    String result = instance.getMimeType();
    assertEquals(expResult, result);
  }

  /**
   * Test of getType method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetType() {
    System.out.println("getType");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    MetadataSchemaRecord.SCHEMA_TYPE expResult = MetadataSchemaRecord.SCHEMA_TYPE.JSON;
    instance.setType(expResult);
    MetadataSchemaRecord.SCHEMA_TYPE result = instance.getType();
    assertEquals(expResult, result);
  }

  /**
   * Test of getCreatedAt method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetCreatedAt() {
    System.out.println("getCreatedAt");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    Instant expResult = Instant.now();
    instance.setCreatedAt(expResult);
    Instant result = instance.getCreatedAt();
    assertEquals(expResult, result);
  }

  /**
   * Test of getLastUpdate method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetLastUpdate() {
    System.out.println("getLastUpdate");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    Instant expResult = Instant.now();
    instance.setLastUpdate(expResult);
    Instant result = instance.getLastUpdate();
    assertEquals(expResult, result);
  }
  /**
   * Test of getSchemaDocumentUri method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetSchemaDocumentUri() {
    System.out.println("getSchemaDocumentUri");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "anyUri";
    instance.setSchemaDocumentUri(expResult);
    String result = instance.getSchemaDocumentUri();
    assertEquals(expResult, result);
  }

  /**
   * Test of getSchemaHash method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetSchemaHash() {
    System.out.println("getSchemaHash");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "hash";
    instance.setSchemaHash(expResult);
    String result = instance.getSchemaHash();
    assertEquals(expResult, result);
  }

  /**
   * Test of getLocked method, of class MetadataSchemaRecord.
   */
  @Test
  public void testSetAndGetLocked() {
    System.out.println("getLocked");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    Boolean expResult = true;
    instance.setLocked(expResult);
    Boolean result = instance.getLocked();
    assertEquals(expResult, result);
    expResult = false;
    instance.setLocked(expResult);
    result = instance.getLocked();
    assertEquals(expResult, result);
  }

  /**
   * Test of equals method, of class MetadataSchemaRecord.
   */
  @Test
  public void testEquals() {
    System.out.println("equals");
    Object o = null;
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    boolean expResult = false;
    boolean result = instance.equals(o);
    assertEquals(expResult, result);
    MetadataSchemaRecord instance2 = new MetadataSchemaRecord();
     expResult = true;
     result = instance.equals(instance2);
    assertEquals(expResult, result);
     result = instance2.equals(instance);
    assertEquals(expResult, result);
  }

  /**
   * Test of canEqual method, of class MetadataSchemaRecord.
   */
  @Test
  public void testCanEqual() {
    System.out.println("canEqual");
    Object other = null;
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    boolean expResult = false;
    boolean result = instance.canEqual(other);
    assertEquals(expResult, result);
  }
  
  @Test
  public void testJsonMapping() throws JsonProcessingException {
    System.out.println("testJsonMapping");
        ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(metadataSchemaRecordAsJson, MetadataSchemaRecord.class);
    assertEquals("dc", record.getSchemaId());
    assertEquals("application/xml", record.getMimeType());
    assertEquals(Instant.parse("2020-04-15T05:34:01.5465Z"),record.getCreatedAt());
    assertEquals(Instant.parse("2020-04-15T05:34:01.546502Z"),record.getLastUpdate());
    assertEquals("http://localhost/api/v1/schemas/dc",record.getSchemaDocumentUri());
    assertEquals(Boolean.FALSE,record.getLocked());
    //,\"acl\":[{\"id\":11,\"sid\":\"SELF\",\"permission\":\"WRITE\"}],\"schemaDocumentUri\":\"http://localhost/api/v1/schemas/dc\",\"locked\":false}";

  }

  /**
   * Test of hashCode method, of class MetadataSchemaRecord.
   */
 // @Test
  public void testHashCode() {
    System.out.println("hashCode");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    int expResult = 0;
    int result = instance.hashCode();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of toString method, of class MetadataSchemaRecord.
   */
//  @Test
  public void testToString() {
    System.out.println("toString");
    MetadataSchemaRecord instance = new MetadataSchemaRecord();
    String expResult = "";
    String result = instance.toString();
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }
  
}
