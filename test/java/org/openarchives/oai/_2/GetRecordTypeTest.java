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
public class GetRecordTypeTest {
    
    public GetRecordTypeTest() {
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
     * Test of getRecord method, of class GetRecordType.
     */
    @Test
    public void testSetAndGetRecord() {
        System.out.println("getRecord");
        GetRecordType instance = new GetRecordType();
        RecordType value = null;
        RecordType expResult = value;
        RecordType result = instance.getRecord();
        assertEquals(expResult, result);
        value = new RecordType();
        expResult = value;
        instance.setRecord(value);
        result = instance.getRecord();
        assertEquals(expResult, result);
    }
    
}
