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
public class GranularityTypeTest {
    
    public GranularityTypeTest() {
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
     * Test of values method, of class GranularityType.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        GranularityType[] result = GranularityType.values();
        assertEquals(result.length, 2);
    }

    /**
     * Test of valueOf method, of class GranularityType.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String[] values = {"YYYY_MM_DD", "YYYY_MM_DD_THH_MM_SS_Z"};
        GranularityType[] expResults = {GranularityType.YYYY_MM_DD, GranularityType.YYYY_MM_DD_THH_MM_SS_Z};
        for (int index1 = 0; index1 < values.length; index1++) {
            GranularityType result = GranularityType.valueOf(values[index1].toUpperCase());
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of value method, of class GranularityType.
     */
    @Test
    public void testValue() {
        System.out.println("value");
        String[] expResults = {"YYYY-MM-DD", "YYYY-MM-DDThh:mm:ssZ"};
        GranularityType[] verbTypes = {GranularityType.YYYY_MM_DD, GranularityType.YYYY_MM_DD_THH_MM_SS_Z};
        for (int index1 = 0; index1 < verbTypes.length; index1++) {
            String result = verbTypes[index1].value();
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class GranularityType.
     */
    @Test
    public void testFromValue() {
        System.out.println("fromValue");
        String[] values = {"YYYY-MM-DD", "YYYY-MM-DDThh:mm:ssZ"};
        GranularityType[] expResults = {GranularityType.YYYY_MM_DD, GranularityType.YYYY_MM_DD_THH_MM_SS_Z};
        for (int index1 = 0; index1 < values.length; index1++) {
            GranularityType result = GranularityType.fromValue(values[index1]);
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class GranularityType.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValueWithException() {
        System.out.println("fromValue");
        String value = "YYYY-mm-DD";
        GranularityType expResult = GranularityType.YYYY_MM_DD;
        GranularityType result = GranularityType.fromValue(value);
        assertEquals(expResult, result);
    }
    
}
