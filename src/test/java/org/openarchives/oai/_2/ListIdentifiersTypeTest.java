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
public class ListIdentifiersTypeTest {
    
    public ListIdentifiersTypeTest() {
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
     * Test of getHeader method, of class ListIdentifiersType.
     */
    @Test
    public void testGetHeader() {
        System.out.println("getHeader");
        ListIdentifiersType instance = new ListIdentifiersType();
        List<HeaderType> value = null;
        List<HeaderType> expResult = value;
        List<HeaderType> result = instance.getHeader();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add(new HeaderType());
        result = instance.getHeader();
        assertEquals(expResult, result);
    }

    /**
     * Test of getResumptionToken method, of class ListIdentifiersType.
     */
    @Test
    public void testSetAndGetResumptionToken() {
        System.out.println("getResumptionToken");
        ListIdentifiersType instance = new ListIdentifiersType();
        ResumptionTokenType value = null;
        ResumptionTokenType expResult = value;
        ResumptionTokenType result = instance.getResumptionToken();
        assertEquals(expResult, result);
        value = new ResumptionTokenType();
        expResult = value;
        instance.setResumptionToken(value);
        result = instance.getResumptionToken();
        assertEquals(expResult, result);
    }
    
}
