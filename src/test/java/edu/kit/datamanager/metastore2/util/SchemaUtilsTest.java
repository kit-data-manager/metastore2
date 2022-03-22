/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.test.CreateSchemaUtil;
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
public class SchemaUtilsTest {

  private final static String KIT_SCHEMA = CreateSchemaUtil.KIT_SCHEMA;

  public SchemaUtilsTest() {
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
    assertNotNull(new SchemaUtils());
  }

  /**
   * Test of guessType method, of class SchemaUtils.
   */
  @Test
  public void testGuessTypeXMLLongSchema() {
    System.out.println("guessType for XML");
    byte[] schema = null;
    MetadataSchemaRecord.SCHEMA_TYPE expResult = MetadataSchemaRecord.SCHEMA_TYPE.XML;
    MetadataSchemaRecord.SCHEMA_TYPE result = SchemaUtils.guessType(KIT_SCHEMA.getBytes());
    assertEquals(expResult, result);

  }

  @Test
  public void testGuessTypeXML() {
    System.out.println("guessType for XML");
    byte[] schema = null;
    MetadataSchemaRecord.SCHEMA_TYPE expResult = MetadataSchemaRecord.SCHEMA_TYPE.XML;
    String[] patterns = {"<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n  <xs:schema ", "<xs:schema=", " <xs:schema=", " < xs:schema=", " <schema=", "< schema=", " < schema=", " < sch:schema = "};
    for (String beginning : patterns) {
      MetadataSchemaRecord.SCHEMA_TYPE result = SchemaUtils.guessType(beginning.getBytes());
      assertEquals(expResult, result);
    }
  }

  @Test
  public void testGuessTypeNullXML() {
    System.out.println("guessType is neither XML nor JSON");
    String[] patterns = {"<?xml version=\"1.0\">\n<myschema> \n<xs:schema ", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \n <xsschema ", " <sch:ema=", "< d:schema=", " < longprefix:schema=", " < schem=a = "};
    for (String beginning : patterns) {
      MetadataSchemaRecord.SCHEMA_TYPE result = SchemaUtils.guessType(beginning.getBytes());
      assertNull(result);
    }
  }

  /**
   * Test of guessType method, of class SchemaUtils.
   */
  @Test
  public void testGuessTypeJSON() {
    System.out.println("guessType for JSON");
    byte[] schema = null;
    MetadataSchemaRecord.SCHEMA_TYPE expResult = MetadataSchemaRecord.SCHEMA_TYPE.JSON;
    String[] patterns = {"{ \"$schema\" : \"https://...", "{\n \"$schema\": ", "{ \"$id\" : \"...", "{\n \"$id\" : \"...", "\n{ \"$schema\" : \"https://...", "{ \"$schema\" : \"https://..."};
    for (String beginning : patterns) {
      MetadataSchemaRecord.SCHEMA_TYPE result = SchemaUtils.guessType(beginning.getBytes());
      assertEquals(expResult, result);
    }
  }

  @Test
  public void testGuessTypeNullJSON() {
    System.out.println("guessType is neither XML nor JSON");
    String[] patterns = {"<?xml version=\"1.0\">\n{ \"$schema\" : \"https://...", "schema: { \"$schema\" : \"https://...", "{ \"schema\" : \"https://...", "{\n \"schema\": ", "{ \"id\" : \"...", "{\n \"id\" : \"...", "\n{[ \"$schema\" : \"https://...", "{{ \"$schema\" : \"https://..."};
    for (String beginning : patterns) {
      MetadataSchemaRecord.SCHEMA_TYPE result = SchemaUtils.guessType(beginning.getBytes());
      assertNull(result);
    }
  }

}
