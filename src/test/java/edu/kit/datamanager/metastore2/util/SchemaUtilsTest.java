/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
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

  private final static String DC_SCHEMA = "<schema targetNamespace=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
          + "        xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
          + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
          + "        xmlns=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "        elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "\n"
          + "<import namespace=\"http://purl.org/dc/elements/1.1/\" schemaLocation=\"https://www.dublincore.org/schemas/xmls/qdc/2008/02/11/dc.xsd\"/>\n"
          + "\n"
          + "<element name=\"dc\" type=\"oai_dc:oai_dcType\"/>\n"
          + "\n"
          + "<complexType name=\"oai_dcType\">\n"
          + "  <choice minOccurs=\"0\" maxOccurs=\"unbounded\">\n"
          + "    <element ref=\"dc:title\"/>\n"
          + "    <element ref=\"dc:creator\"/>\n"
          + "    <element ref=\"dc:subject\"/>\n"
          + "    <element ref=\"dc:description\"/>\n"
          + "    <element ref=\"dc:publisher\"/>\n"
          + "    <element ref=\"dc:contributor\"/>\n"
          + "    <element ref=\"dc:date\"/>\n"
          + "    <element ref=\"dc:type\"/>\n"
          + "    <element ref=\"dc:format\"/>\n"
          + "    <element ref=\"dc:identifier\"/>\n"
          + "    <element ref=\"dc:source\"/>\n"
          + "    <element ref=\"dc:language\"/>\n"
          + "    <element ref=\"dc:relation\"/>\n"
          + "    <element ref=\"dc:coverage\"/>\n"
          + "    <element ref=\"dc:rights\"/>\n"
          + "  </choice>\n"
          + "</complexType>\n"
          + "\n"
          + "</schema>";

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
    MetadataSchemaRecord.SCHEMA_TYPE result = SchemaUtils.guessType(DC_SCHEMA.getBytes());
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
