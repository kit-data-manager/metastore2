/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

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
public class MetadataFormatTypeTest {
    
    public MetadataFormatTypeTest() {
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
     * Test of getMetadataPrefix method, of class MetadataFormatType.
     */
    @Test
    public void testSetAndGetMetadataPrefix() {
        System.out.println("getMetadataPrefix");
        MetadataFormatType instance = new MetadataFormatType();
        String value = null;
        String expResult = value;
        String result = instance.getMetadataPrefix();
        assertEquals(expResult, result);
        value = "metadataprefix";
        expResult = value;
        instance.setMetadataPrefix(value);
        result = instance.getMetadataPrefix();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSchema method, of class MetadataFormatType.
     */
    @Test
    public void testSetAndGetSchema() {
        System.out.println("getSchema");
        MetadataFormatType instance = new MetadataFormatType();
        String value = null;
        String expResult = value;
        String result = instance.getSchema();
        assertEquals(expResult, result);
        value = "schema";
        expResult = value;
        instance.setSchema(value);
        result = instance.getSchema();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMetadataNamespace method, of class MetadataFormatType.
     */
    @Test
    public void testSetAndGetMetadataNamespace() {
        System.out.println("getMetadataNamespace");
        MetadataFormatType instance = new MetadataFormatType();
        String value = null;
        String expResult = value;
        String result = instance.getMetadataNamespace();
        assertEquals(expResult, result);
        value = "metadatanamespace";
        expResult = value;
        instance.setMetadataNamespace(value);
        result = instance.getMetadataNamespace();
        assertEquals(expResult, result);
    }
    
}
