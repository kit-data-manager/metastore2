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
public class AboutTypeTest {
    
    public AboutTypeTest() {
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
     * Test of getAny method, of class AboutType.
     */
    @Test
    public void testSetAndGetAny() {
        System.out.println("getAny");
        AboutType instance = new AboutType();
        Object value = null;
        Object expResult = value;
        Object result = instance.getAny();
        assertEquals(expResult, result);
        value = "about";
        expResult = value;
        instance.setAny(value);
        result = instance.getAny();
        assertEquals(expResult, result);
    }
}
