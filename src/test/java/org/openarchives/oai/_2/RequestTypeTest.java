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
public class RequestTypeTest {
    
    public RequestTypeTest() {
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
     * Test of getValue method, of class RequestType.
     */
    @Test
    public void testSetAndGetValue() {
        System.out.println("getValue");
        RequestType instance = new RequestType();
        String value = null;
        String expResult = value;
        String result = instance.getValue();
        assertEquals(expResult, result);
        value = "newValue";
        expResult = value;
        instance.setValue(value);
        result = instance.getValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of getVerb method, of class RequestType.
     */
    @Test
    public void testSetAndGetVerb() {
        System.out.println("testSetAndGetVerb");
        RequestType instance = new RequestType();
         VerbType value = null;
        VerbType expResult = value;
        VerbType result = instance.getVerb();
        assertEquals(expResult, result);
        value = VerbType.GET_RECORD;
        expResult = value;
        instance.setVerb(value);
        result = instance.getVerb();
        assertEquals(expResult, result);
   }

    /**
     * Test of setVerb method, of class RequestType.
     */
    @Test
    public void testSetVerb() {
        System.out.println("setVerb");
        VerbType value = null;
        RequestType instance = new RequestType();
        instance.setVerb(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getIdentifier method, of class RequestType.
     */
    @Test
    public void testGetIdentifier() {
        System.out.println("getIdentifier");
        RequestType instance = new RequestType();
        String expResult = "";
        String result = instance.getIdentifier();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setIdentifier method, of class RequestType.
     */
    @Test
    public void testSetIdentifier() {
        System.out.println("setIdentifier");
        String value = "";
        RequestType instance = new RequestType();
        instance.setIdentifier(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMetadataPrefix method, of class RequestType.
     */
    @Test
    public void testGetMetadataPrefix() {
        System.out.println("getMetadataPrefix");
        RequestType instance = new RequestType();
        String expResult = "";
        String result = instance.getMetadataPrefix();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMetadataPrefix method, of class RequestType.
     */
    @Test
    public void testSetMetadataPrefix() {
        System.out.println("setMetadataPrefix");
        String value = "";
        RequestType instance = new RequestType();
        instance.setMetadataPrefix(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFrom method, of class RequestType.
     */
    @Test
    public void testGetFrom() {
        System.out.println("getFrom");
        RequestType instance = new RequestType();
        String expResult = "";
        String result = instance.getFrom();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFrom method, of class RequestType.
     */
    @Test
    public void testSetFrom() {
        System.out.println("setFrom");
        String value = "";
        RequestType instance = new RequestType();
        instance.setFrom(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getUntil method, of class RequestType.
     */
    @Test
    public void testGetUntil() {
        System.out.println("getUntil");
        RequestType instance = new RequestType();
        String expResult = "";
        String result = instance.getUntil();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setUntil method, of class RequestType.
     */
    @Test
    public void testSetUntil() {
        System.out.println("setUntil");
        String value = "";
        RequestType instance = new RequestType();
        instance.setUntil(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSet method, of class RequestType.
     */
    @Test
    public void testGetSet() {
        System.out.println("getSet");
        RequestType instance = new RequestType();
        String expResult = "";
        String result = instance.getSet();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSet method, of class RequestType.
     */
    @Test
    public void testSetSet() {
        System.out.println("setSet");
        String value = "";
        RequestType instance = new RequestType();
        instance.setSet(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getResumptionToken method, of class RequestType.
     */
    @Test
    public void testGetResumptionToken() {
        System.out.println("getResumptionToken");
        RequestType instance = new RequestType();
        String expResult = "";
        String result = instance.getResumptionToken();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setResumptionToken method, of class RequestType.
     */
    @Test
    public void testSetResumptionToken() {
        System.out.println("setResumptionToken");
        String value = "";
        RequestType instance = new RequestType();
        instance.setResumptionToken(value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
