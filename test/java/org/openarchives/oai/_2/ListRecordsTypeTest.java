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
public class ListRecordsTypeTest {
    
    public ListRecordsTypeTest() {
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
     * Test of getSet method, of class ListRecordsType.
     */
    @Test
    public void testGetRecord() {
        System.out.println("getRecord");
        ListRecordsType instance = new ListRecordsType();
        List<RecordType> expResult = null;
        List<RecordType> result = instance.getRecord();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add(new RecordType());
        result = instance.getRecord();
        assertEquals(expResult, result);
    }

    /**
     * Test of getResumptionToken method, of class ListRecordsType.
     */
    @Test
    public void testSetAndGetResumptionToken() {
        System.out.println("getResumptionToken");
        ListRecordsType instance = new ListRecordsType();
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
