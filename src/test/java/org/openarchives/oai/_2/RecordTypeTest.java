/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

import java.util.ArrayList;
import java.util.List;
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
public class RecordTypeTest {
    
    public RecordTypeTest() {
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
     * Test of getHeader method, of class RecordType.
     */
    @Test
    public void testSetAndGetHeader() {
        System.out.println("getHeader");
        RecordType instance = new RecordType();
         HeaderType value = null;
        HeaderType expResult = value;
        HeaderType result = instance.getHeader();
        assertEquals(expResult, result);
        value = new HeaderType();
        expResult = value;
        instance.setHeader(value);
        result = instance.getHeader();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMetadata method, of class RecordType.
     */
    @Test
    public void testSetAndGetMetadata() {
        System.out.println("getMetadata");
        RecordType instance = new RecordType();
         MetadataType value = null;
        MetadataType expResult = value;
        MetadataType result = instance.getMetadata();
        assertEquals(expResult, result);
        value = new MetadataType();
        expResult = value;
        instance.setMetadata(value);
        result = instance.getMetadata();
        assertEquals(expResult, result);
    }

    /**
     * Test of getAbout method, of class RecordType.
     */
    @Test
    public void testGetAbout() {
        System.out.println("getAbout");
        RecordType instance = new RecordType();
        List<AboutType> expResult = new ArrayList<>();
        List<AboutType> result = instance.getAbout();
        assertEquals(expResult, result);
    }
    
}
