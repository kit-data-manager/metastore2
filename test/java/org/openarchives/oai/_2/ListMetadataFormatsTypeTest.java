/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

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
public class ListMetadataFormatsTypeTest {
    
    public ListMetadataFormatsTypeTest() {
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
     * Test of getMetadataFormat method, of class ListMetadataFormatsType.
     */
    @Test
    public void testGetMetadataFormat() {
        System.out.println("getMetadataFormat");
        ListMetadataFormatsType instance = new ListMetadataFormatsType();
        List<MetadataFormatType> value = null;
        List<MetadataFormatType> expResult = value;
        List<MetadataFormatType> result = instance.getMetadataFormat();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add(new MetadataFormatType());
        result = instance.getMetadataFormat();
        assertEquals(expResult, result);
    }
    
}
