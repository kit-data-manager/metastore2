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
public class DeletedRecordTypeTest {
    
    public DeletedRecordTypeTest() {
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
     * Test of values method, of class DeletedRecordType.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        DeletedRecordType[] result = DeletedRecordType.values();
        assertEquals(result.length, 3);
    }

    /**
     * Test of valueOf method, of class DeletedRecordType.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String[] values = {"NO", "PERSISTENT", "TRANSIENT"};
        DeletedRecordType[] expResults = {DeletedRecordType.NO, DeletedRecordType.PERSISTENT, DeletedRecordType.TRANSIENT};
        for (int index1 = 0; index1 < values.length; index1++) {
            DeletedRecordType result = DeletedRecordType.valueOf(values[index1].toUpperCase());
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of value method, of class DeletedRecordType.
     */
    @Test
    public void testValue() {
        System.out.println("value");
        String[] expResults = {"no", "persistent", "transient"};
        DeletedRecordType[] verbTypes = {DeletedRecordType.NO, DeletedRecordType.PERSISTENT, DeletedRecordType.TRANSIENT};
        for (int index1 = 0; index1 < verbTypes.length; index1++) {
            String result = verbTypes[index1].value();
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class DeletedRecordType.
     */
    @Test
    public void testFromValue() {
        System.out.println("fromValue");
        String[] values = {"no", "persistent", "transient"};
        DeletedRecordType[] expResults = {DeletedRecordType.NO, DeletedRecordType.PERSISTENT, DeletedRecordType.TRANSIENT};
        for (int index1 = 0; index1 < values.length; index1++) {
            DeletedRecordType result = DeletedRecordType.fromValue(values[index1]);
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class DeletedRecordType.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValueWithException() {
        System.out.println("fromValue");
        String value = "No";
        DeletedRecordType expResult = DeletedRecordType.NO;
        DeletedRecordType result = DeletedRecordType.fromValue(value);
        assertEquals(expResult, result);
    }
    
}
