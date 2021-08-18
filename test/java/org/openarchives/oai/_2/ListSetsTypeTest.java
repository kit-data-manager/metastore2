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
public class ListSetsTypeTest {
    
    public ListSetsTypeTest() {
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
     * Test of getSet method, of class ListSetsType.
     */
    @Test
    public void testGetSet() {
        System.out.println("getSet");
        ListSetsType instance = new ListSetsType();
        List<SetType> expResult = null;
        List<SetType> result = instance.getSet();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add(new SetType());
        result = instance.getSet();
        assertEquals(expResult, result);
    }

    /**
     * Test of getResumptionToken method, of class ListSetsType.
     */
    @Test
    public void testSetAndGetResumptionToken() {
        System.out.println("getResumptionToken");
        ListSetsType instance = new ListSetsType();
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
