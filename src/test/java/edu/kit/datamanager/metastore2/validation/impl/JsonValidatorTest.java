/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.validation.impl;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class JsonValidatorTest {
  private final String jsonSchemaFile = "/tmp/JsonValidatorSchema.json";
  
  private final String jsonSchemaWithversiondraft201909 = "{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\", \"properties\": { \"id\": {\"type\": \"number\"}}}";

  private final String validDocument = "{\"id\": 1}";

  public JsonValidatorTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() throws IOException {
    File schemaFile = new File(jsonSchemaFile);
    if (!schemaFile.exists()) {
      try ( FileOutputStream fout = new FileOutputStream(schemaFile)) {
        fout.write(jsonSchemaWithversiondraft201909.getBytes());
        fout.flush();
      }
    }
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of supportsSchemaType method, of class JsonValidator.
   */
  @Test
  public void testConstructor() {
    System.out.println("testConstructor");
    assertNotNull(new JsonValidator());
  }

  /**
   * Test of supportsSchemaType method, of class JsonValidator.
   */
  @Test
  public void testSupportsSchemaTypeNull() {
    System.out.println("testSupportsSchemaTypeNull");
    MetadataSchemaRecord.SCHEMA_TYPE type = null;
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.supportsSchemaType(type);
    assertEquals(expResult, result);
  }

  /**
   * Test of supportsSchemaType method, of class JsonValidator.
   */
  @Test
  public void testSupportsSchemaType() {
    System.out.println("supportsSchemaType");
    MetadataSchemaRecord.SCHEMA_TYPE type = MetadataSchemaRecord.SCHEMA_TYPE.JSON;
    JsonValidator instance = new JsonValidator();
    boolean expResult = true;
    boolean result = instance.supportsSchemaType(type);
    assertEquals(expResult, result);
  }

  /**
   * Test of isSchemaValid method, of class JsonValidator.
   */
  @Test
  public void testIsSchemaValid() {
    System.out.println("isSchemaValid");
    InputStream schemaStream = null;
    schemaStream = new ByteArrayInputStream(jsonSchemaWithversiondraft201909.getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = true;
    boolean result = instance.isSchemaValid(schemaStream);
    assertEquals(expResult, result);
  }

  /**
   * Test of isSchemaValid method, of class JsonValidator.
   */
  @Test
  public void testIsSchemaValidWithEmptySchema() {
    System.out.println("testIsSchemaValidWithEmptySchema");
    InputStream schemaStream = null;
    schemaStream = new ByteArrayInputStream("{}".getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.isSchemaValid(schemaStream);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateMetadataDocument method, of class JsonValidator.
   */
  @Test
  public void testValidateMetadataDocumentWithNull() {
    System.out.println("testValidateMetadataDocumentWithNull");
    File schemaFile = null;
    InputStream metadataDocumentStream = null;
    metadataDocumentStream = new ByteArrayInputStream("{}".getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
    assertEquals(expResult, result);
    assertNotNull(instance.getErrorMessage());
  }

  /**
   * Test of validateMetadataDocument method, of class JsonValidator.
   */
  @Test
  public void testValidateMetadataDocumentWithNotExistingFile() {
    System.out.println("testValidateMetadataDocumentWithNotExistingFile");
    File schemaFile = new File("/not/existing/file.txt");
    InputStream metadataDocumentStream = null;
    metadataDocumentStream = new ByteArrayInputStream("{}".getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
    assertEquals(expResult, result);
  }

  /**
   * Test of validateMetadataDocument method, of class JsonValidator.
   */
  @Test
  public void testValidateMetadataDocumentWithDirectoryinsteadOfFile() {
    System.out.println("testValidateMetadataDocumentWithDirectoryinsteadOfFile");
    File schemaFile = new File("/tmp");
    InputStream metadataDocumentStream = null;
    metadataDocumentStream = new ByteArrayInputStream("{}".getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
    assertEquals(expResult, result);
  }

  @Test
  public void testValidateMetadataDocumentWithNullArguments() {
    System.out.println("testValidateMetadataDocumentWithNullArguments");
    File schemaFile = null;
    InputStream schemaStream = null;
    schemaStream = null;
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.validateMetadataDocument(schemaFile, schemaStream);
    assertEquals(expResult, result);
    assertNotNull(instance.getErrorMessage());
  }
  @Test
  public void testValidateMetadataDocumentWithNullDocument() throws IOException {
    System.out.println("testValidateMetadataDocumentWithEmptyDocument");
    File schemaFile = new File(jsonSchemaFile);
    InputStream metadataDocumentStream = null;
    JsonValidator instance = new JsonValidator();
    boolean expResult = false;
    boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
    assertEquals(expResult, result);
  }
  @Test
  public void testValidateMetadataDocumentWithEmptyDocument() throws IOException {
    System.out.println("testValidateMetadataDocumentWithEmptyDocument");
    File schemaFile = new File(jsonSchemaFile);
    InputStream metadataDocumentStream = null;
    metadataDocumentStream = new ByteArrayInputStream("{}".getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = true;
    boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
    assertEquals(expResult, result);
  }

  @Test
  public void testValidateMetadataDocumentWithValidDocument() throws IOException {
    System.out.println("testValidateMetadataDocumentWithEmptyDocument");
    File schemaFile = new File(jsonSchemaFile);
     InputStream metadataDocumentStream = null;
    metadataDocumentStream = new ByteArrayInputStream(validDocument.getBytes());
    JsonValidator instance = new JsonValidator();
    boolean expResult = true;
    boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
    assertEquals(expResult, result);
    assertNull(instance.getErrorMessage());
  }

  /**
   * Test of getErrorMessage method, of class JsonValidator.
   */
  @Test
  public void testGetErrorMessage() {
    System.out.println("getErrorMessage");
    JsonValidator instance = new JsonValidator();
    String expResult = null;
    String result = instance.getErrorMessage();
    assertEquals(expResult, result);
  }

}
