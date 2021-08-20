/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.entities.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.kit.datamanager.exceptions.MessageValidationException;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class MetadataResourceMessageTest {
  
  public MetadataResourceMessageTest() {
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
  
  
  /*
      MetadataRecord mdr = new MetadataRecord();
    mdr.setId("myId");
    mdr.setMetadataDocumentUri("https://www.example.org/api/v1/metadata/anyId");
    mdr.setSchemaId("my_dc");
    MetadataResourceMessage msg = MetadataResourceMessage.factoryCreateMetadataMessage(mdr, "me", "you");
    System.out.println(msg.toJson());
    System.out.println(msg.getRoutingKey());

  */

  /**
   * Test of factoryCreateMetadataMessage method, of class MetadataResourceMessage.
   */
  @Test
  public void testConstructor() {
    MetadataResourceMessage mdrm = new MetadataResourceMessage();
    assertTrue(true);
  }

  /**
   * Test of factoryCreateMetadataMessage method, of class MetadataResourceMessage.
   */
  @Test
  public void testFactoryCreateMetadataMessage() throws JsonProcessingException {
    System.out.println("factoryCreateMetadataMessage");
    MetadataRecord metadataRecord = null;
    String caller = "anyCaller";
    String sender = "anySender";
    String[] ids = {"id1", "id2", "id3", "id4"};
    String[] uris= {null, null, "uri1", "uri2"};
    String[] types = {null, "type1", null, "type2"};
    String action = DataResourceMessage.ACTION.CREATE.getValue();
    for (int index = 0; index < ids.length; index++) {
    String id = ids[index];
    String uri = uris[index];
    String type = types[index];
    metadataRecord = buildMetadataRecord(id, uri, type);
    MetadataResourceMessage result = MetadataResourceMessage.factoryCreateMetadataMessage(metadataRecord, caller, sender);
    checkJsonString(result, sender, caller, action, id, uri, type);
    }
  }

  /**
   * Test of factoryCreateMetadataMessage method, of class MetadataResourceMessage.
   */
  @Test
  public void testActionIsNull() throws JsonProcessingException {
    System.out.println("factoryCreateMetadataMessage");
    MetadataRecord metadataRecord = null;
    String caller = "anyCaller";
    String sender = "anySender";
    String[] ids = {"id1"};
    String[] uris= {"uri2"};
    String[] types = {"type2"};
    String action = null;
    for (int index = 0; index < ids.length; index++) {
    String id = ids[index];
    String uri = uris[index];
    String type = types[index];
    metadataRecord = buildMetadataRecord(id, uri, type);
    MetadataResourceMessage result = MetadataResourceMessage.createMessage(metadataRecord, null, DataResourceMessage.SUB_CATEGORY.DATA, uri, sender);
    try {
      checkJsonString(result, sender, caller, action, id, uri, type);
      assertTrue(false);
    } catch (MessageValidationException mve) {
      assertTrue(mve.getMessage().contains("must not be null"));
    }
    }
  }

  @Test
  public void testIdIsNull() throws JsonProcessingException {
    System.out.println("testIdIsNull");
    MetadataRecord metadataRecord = null;
    String caller = "anyCaller";
    String sender = "anySender";
    String[] ids = {null, null, null, null};
    String[] uris= {null, null, "uri1", "uri2"};
    String[] types = {null, "type1", null, "type2"};
    String action = DataResourceMessage.ACTION.CREATE.getValue();
    for (int index = 0; index < ids.length; index++) {
    String id = ids[index];
    String uri = uris[index];
    String type = types[index];
    metadataRecord = buildMetadataRecord(id, uri, type);
    MetadataResourceMessage result = MetadataResourceMessage.factoryCreateMetadataMessage(metadataRecord, caller, sender);
    try {
      checkJsonString(result, sender, caller, action, id, uri, type);
      assertTrue(false);
    } catch (MessageValidationException mve) {
      assertTrue(mve.getMessage().contains("must not be null"));
    }
    }
  }
  @Test
  public void testMetadataRecordIsNull() throws JsonProcessingException {
    System.out.println("testIdIsNull");
    MetadataRecord metadataRecord = null;
    String caller = "anyCaller";
    String sender = "anySender";
    String action = DataResourceMessage.ACTION.CREATE.getValue();
    String id = null;
    String uri = null;
    String type = null;
    metadataRecord = buildMetadataRecord(id, uri, type);
    MetadataResourceMessage result = MetadataResourceMessage.factoryCreateMetadataMessage(metadataRecord, caller, sender);
    try {
      checkJsonString(result, sender, caller, action, id, uri, type);
      assertTrue(false);
    } catch (MessageValidationException mve) {
      assertTrue(mve.getMessage().contains("must not be null"));
    }
  }

  /**
   * Test of factoryUpdateMetadataMessage method, of class MetadataResourceMessage.
   */
  @Test
  public void testFactoryUpdateMetadataMessage() throws JsonProcessingException {
    System.out.println("factoryUpdateMetadataMessage");
    MetadataRecord metadataRecord = null;
    String caller = "anyCaller";
    String sender = "anySender";
    String[] ids = {"id1", "id2", "id3", "id4"};
    String[] uris= {null, null, "uri1", "uri2"};
    String[] types = {null, "type1", null, "type2"};
    String action = DataResourceMessage.ACTION.UPDATE.getValue();
    for (int index = 0; index < ids.length; index++) {
    String id = ids[index];
    String uri = uris[index];
    String type = types[index];
    metadataRecord = buildMetadataRecord(id, uri, type);
    MetadataResourceMessage result = MetadataResourceMessage.factoryUpdateMetadataMessage(metadataRecord, caller, sender);
    checkJsonString(result, sender, caller, action, id, uri, type);
    }
  }

  /**
   * Test of factoryDeleteMetadataMessage method, of class MetadataResourceMessage.
   */
  @Test
  public void testFactoryDeleteMetadataMessage() throws JsonProcessingException {
    System.out.println("factoryDeleteMetadataMessage");
    MetadataRecord metadataRecord = null;
    String caller = "anyCaller";
    String sender = "anySender";
    String[] ids = {"id1", "id2", "id3", "id4"};
    String[] uris= {null, null, "uri1", "uri2"};
    String[] types = {null, "type1", null, "type2"};
    String action = DataResourceMessage.ACTION.DELETE.getValue();
    for (int index = 0; index < ids.length; index++) {
    String id = ids[index];
    String uri = uris[index];
    String type = types[index];
    metadataRecord = buildMetadataRecord(id, uri, type);
    MetadataResourceMessage result = MetadataResourceMessage.factoryDeleteMetadataMessage(metadataRecord, caller, sender);
    checkJsonString(result, sender, caller, action, id, uri, type);
    }
  }

  /**
   * Test of createMessage method, of class MetadataResourceMessage.
   */
  @Test
  public void testCreateMessage() {
    System.out.println("createMessage");
    MetadataRecord metadataRecord = null;
    DataResourceMessage.ACTION action = null;
    DataResourceMessage.SUB_CATEGORY subCategory = null;
    String principal = "";
    String sender = "";
    MetadataResourceMessage expResult = new MetadataResourceMessage();
    MetadataResourceMessage result = MetadataResourceMessage.createMessage(metadataRecord, action, subCategory, principal, sender);
    assertEquals(expResult, result);
  }

  /**
   * Test of equals method, of class MetadataResourceMessage.
   */
  @Test
  public void testEquals() {
    System.out.println("equals");
    Object o = null;
    MetadataResourceMessage instance = new MetadataResourceMessage();
    boolean expResult = false;
    boolean result = instance.equals(o);
    assertEquals(expResult, result);
    expResult = true;
    o = new MetadataResourceMessage();
     result = instance.equals(o);
    assertEquals(expResult, result);
  }

  /**
   * Test of canEqual method, of class MetadataResourceMessage.
   */
  @Test
  public void testCanEqual() {
    System.out.println("canEqual");
    Object other = null;
    MetadataResourceMessage instance = new MetadataResourceMessage();
    boolean expResult = false;
    boolean result = instance.canEqual(other);
    assertEquals(expResult, result);
  }

  /**
   * Test of toString method, of class MetadataResourceMessage.
   */
  @Test
  public void testToString() throws JsonProcessingException {
    System.out.println("toString");
    MetadataRecord metadataRecord = null;
    DataResourceMessage.ACTION action = DataResourceMessage.ACTION.FIX;
    DataResourceMessage.SUB_CATEGORY subCategory = null;
    String principal = "principal";
    String sender = "sender";
     String id = "id";
    String uri = "uri";
    String type = "type";
    metadataRecord = buildMetadataRecord(id, uri, type);
   MetadataResourceMessage instance = MetadataResourceMessage.createMessage(metadataRecord, action, subCategory, principal, sender);
    String expResult = new MetadataResourceMessage().toString();
    String result = instance.toString();
    assertEquals(expResult, result);
  }
  
  private MetadataRecord buildMetadataRecord(String id, String uri, String type) {
    MetadataRecord mr = new MetadataRecord();
    mr.setId(id);
    mr.setMetadataDocumentUri(uri);
    mr.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(type));
    return mr;
  }
  
  private void checkJsonString(MetadataResourceMessage mdrm, String sender, String caller, String action, String id, String uri, String type) throws JsonProcessingException {
    String jsonString = mdrm.toJson();
    System.out.println(jsonString);
    if (sender != null) {
      assertTrue(jsonString.contains("\"sender\":\"" + sender + "\""));
    } else {
      assertFalse(jsonString.contains("\"sender\":\""));
    }
    if (caller != null) {
      assertTrue(jsonString.contains("\"principal\":\"" + caller + "\""));
    } else {
      assertFalse(jsonString.contains("\"principal\":\""));
    }
    if (action != null) {
      assertTrue(jsonString.contains("\"action\":\"" + action + "\""));
    } else {
      assertFalse(jsonString.contains("\"action\":\""));
    }
    if (id != null) {
      assertTrue(jsonString.contains("\"entityId\":\"" + id + "\""));
    } else {
      assertFalse(jsonString.contains("\"entityId\":\""));
    }
    if (uri != null) {
      assertTrue(jsonString.contains("\"" + MetadataResourceMessage.RESOLVING_URL_PROPERTY + "\":\"" + uri + "\""));
    } else {
      assertFalse(jsonString.contains("\"" + MetadataResourceMessage.RESOLVING_URL_PROPERTY + "\":\""));
    }
    if (type != null) {
      assertTrue(jsonString.contains("\"" + MetadataResourceMessage.DOCUMENT_TYPE_PROPERTY + "\":\"" + type + "\""));
    } else {
      assertFalse(jsonString.contains("\"" + MetadataResourceMessage.DOCUMENT_TYPE_PROPERTY + "\":\""));
    }
    
    assertTrue(mdrm.getEntityName().equals("metadata"));
    assertTrue(mdrm.getRoutingKey().startsWith("metadata"));
    
  }
  
}
