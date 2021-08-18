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
 */
public class StatusTypeTest {
    
    public StatusTypeTest() {
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
     * Test of values method, of class StatusType.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        StatusType[] result = StatusType.values();
        assertEquals(result.length, 1);
    }

    /**
     * Test of valueOf method, of class StatusType.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String[] values = {"DELETED"};
        StatusType[] expResults = {StatusType. DELETED};
        for (int index1 = 0; index1 < values.length; index1++) {
            StatusType result = StatusType.valueOf(values[index1].toUpperCase());
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of value method, of class StatusType.
     */
    @Test
    public void testValue() {
        System.out.println("value");
        String[] expResults = {"deleted"};
        StatusType[] verbTypes = {StatusType.DELETED};
        for (int index1 = 0; index1 < verbTypes.length; index1++) {
            String result = verbTypes[index1].value();
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class StatusType.
     */
    @Test
    public void testFromValue() {
        System.out.println("fromValue");
        String[] values = {"deleted"};
        StatusType[] expResults = {StatusType.DELETED};
        for (int index1 = 0; index1 < values.length; index1++) {
            StatusType result = StatusType.fromValue(values[index1]);
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class StatusType.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValueWithException() {
        System.out.println("fromValue");
        String value = "DELETED";
        StatusType expResult = StatusType.DELETED;
        StatusType result = StatusType.fromValue(value);
        assertEquals(expResult, result);
    }
}
