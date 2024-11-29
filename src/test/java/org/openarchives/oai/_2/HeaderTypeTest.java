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
public class HeaderTypeTest {
    
    public HeaderTypeTest() {
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
     * Test of getIdentifier method, of class HeaderType.
     */
    @Test
    public void testSetAndGetIdentifier() {
        System.out.println("getIdentifier");
        HeaderType instance = new HeaderType();
        String value = null;
        String expResult = value;
        String result = instance.getIdentifier();
        assertEquals(expResult, result);
        value = "identifier";
        expResult = value;
        instance.setIdentifier(value);
        result = instance.getIdentifier();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDatestamp method, of class HeaderType.
     */
    @Test
    public void testSetAndGetDatestamp() {
        System.out.println("getDatestamp");
        HeaderType instance = new HeaderType();
        String value = null;
        String expResult = value;
        String result = instance.getDatestamp();
        assertEquals(expResult, result);
        value = "identifier";
        expResult = value;
        instance.setDatestamp(value);
        result = instance.getDatestamp();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSetSpec method, of class HeaderType.
     */
    @Test
    public void testGetSetSpec() {
        System.out.println("getSetSpec");
        HeaderType instance = new HeaderType();
        List<String> value = null;
        List<String> expResult = value;
        List<String> result = instance.getSetSpec();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add("spec");
        result = instance.getSetSpec();
        assertEquals(expResult, result);
    }

    /**
     * Test of getStatus method, of class HeaderType.
     */
    @Test
    public void testSetAndGetStatus() {
        System.out.println("getStatus");
        HeaderType instance = new HeaderType();
        StatusType value = null;
        StatusType expResult = value;
        StatusType result = instance.getStatus();
        assertEquals(expResult, result);
        value = StatusType.DELETED;
        expResult = value;
        instance.setStatus(value);
        result = instance.getStatus();
    }
}
