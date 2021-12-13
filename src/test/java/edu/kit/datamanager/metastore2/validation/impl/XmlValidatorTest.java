/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package edu.kit.datamanager.metastore2.validation.impl;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.validation.IValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.SAXException;

/**
 *
 * @author hartmann-v
 */
public class XmlValidatorTest {

    File schemaFile = new File("src/test/resources/examples/xml/example.xsd");
    File xmlFile = new File("src/test/resources/examples/xml/example.xml");
    File invalidXmlFile = new File("src/test/resources/examples/xml/invalidExample.xml");

    public XmlValidatorTest() {
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
     * Test of supportsSchemaType method, of class XmlValidator.
     */
    @Test
    public void testSupportsSchemaType() {
        System.out.println("supportsSchemaType");
        MetadataSchemaRecord.SCHEMA_TYPE type = MetadataSchemaRecord.SCHEMA_TYPE.JSON;
        XmlValidator instance = new XmlValidator();
        boolean expResult = false;
        boolean result = instance.supportsSchemaType(type);
        assertEquals(expResult, result);
        type = MetadataSchemaRecord.SCHEMA_TYPE.XML;
        expResult = true;
        result = instance.supportsSchemaType(type);
        assertEquals(expResult, result);
    }

    /**
     * Test of getInstance method, of class XmlValidator.
     */
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        XmlValidator instance = new XmlValidator();
        IValidator result = instance.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of isSchemaValid method, of class XmlValidator.
     */
    @Test
    public void testIsSchemaValid() throws FileNotFoundException {
        System.out.println("isSchemaValid");
        InputStream schemaStream = new FileInputStream(schemaFile);
        IValidator instance = new XmlValidator().getInstance();
        boolean expResult = true;
        boolean result = instance.isSchemaValid(schemaStream);
        assertEquals(expResult, result);
    }

    /**
     * Test of validateMetadataDocument method, of class XmlValidator.
     */
    @Test
    public void testValidateMetadataDocument() throws FileNotFoundException {
        System.out.println("validateMetadataDocument");
        assertTrue("Schema file is not available!", schemaFile.exists());
        assertTrue("Xml file is not available!", xmlFile.exists());

        InputStream metadataDocumentStream = new FileInputStream(xmlFile);
        IValidator instance = new XmlValidator().getInstance();
        boolean expResult = true;
        boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
        assertEquals(expResult, result);
    }

    /**
     * Test of validateMetadataDocument method, of class XmlValidator.
     */
    @Test
    public void testValidateMetadataDocumentInvalidDocument() throws FileNotFoundException {
        System.out.println("validateMetadataDocumentInvalidDocument");
        assertTrue("Schema file is not available!", schemaFile.exists());
        assertTrue("Xml file is not available!", invalidXmlFile.exists());
        InputStream metadataDocumentStream = new FileInputStream(invalidXmlFile);
        IValidator instance = new XmlValidator().getInstance();
        boolean expResult = false;
        boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
        assertEquals(expResult, result);
    }

    /**
     * Test of getErrorMessage method, of class XmlValidator.
     */
    @Test
    public void testGetErrorMessage() throws FileNotFoundException {
        System.out.println("getErrorMessage");
        assertTrue("Schema file is not available!", schemaFile.exists());
        assertTrue("Xml file is not available!", invalidXmlFile.exists());
        InputStream metadataDocumentStream = new FileInputStream(invalidXmlFile);
        IValidator instance = new XmlValidator().getInstance();
        boolean expResult = false;
        boolean result = instance.validateMetadataDocument(schemaFile, metadataDocumentStream);
        assertEquals(expResult, result);
        String errorMessage = instance.getErrorMessage();
        assertNotNull(errorMessage);
    }

}
