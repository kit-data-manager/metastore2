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
     * Test of getIdentifier method, of class RequestType.
     */
    @Test
    public void testSetAndGetIdentifier() {
        System.out.println("getIdentifier");
        RequestType instance = new RequestType();
         String value = null;
        String expResult = value;
        String result = instance.getIdentifier();
        assertEquals(expResult, result);
        value = "Identifier";
        expResult = value;
        instance.setIdentifier(value);
        result = instance.getIdentifier();
        assertEquals(expResult, result);
    }

    /**
     * Test of getMetadataPrefix method, of class RequestType.
     */
    @Test
    public void testSetAndGetMetadataPrefix() {
        System.out.println("getMetadataPrefix");
        RequestType instance = new RequestType();
         String value = null;
        String expResult = value;
        String result = instance.getMetadataPrefix();
        assertEquals(expResult, result);
        value = "prefix";
        expResult = value;
        instance.setMetadataPrefix(value);
        result = instance.getMetadataPrefix();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFrom method, of class RequestType.
     */
    @Test
    public void testSetAndGetFrom() {
        System.out.println("getFrom");
        RequestType instance = new RequestType();
         String value = null;
        String expResult = value;
        String result = instance.getFrom();
        assertEquals(expResult, result);
        value = "from";
        expResult = value;
        instance.setFrom(value);
        result = instance.getFrom();
        assertEquals(expResult, result);
    }

    /**
     * Test of getUntil method, of class RequestType.
     */
    @Test
    public void testSetAndGetUntil() {
        System.out.println("getUntil");
        RequestType instance = new RequestType();
         String value = null;
        String expResult = value;
        String result = instance.getUntil();
        assertEquals(expResult, result);
        value = "until";
        expResult = value;
        instance.setUntil(value);
        result = instance.getUntil();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSet method, of class RequestType.
     */
    @Test
    public void testSetAndGetSet() {
        System.out.println("getSet");
        RequestType instance = new RequestType();
         String value = null;
        String expResult = value;
        String result = instance.getSet();
        assertEquals(expResult, result);
        value = "set";
        expResult = value;
        instance.setSet(value);
        result = instance.getSet();
        assertEquals(expResult, result);
    }

    /**
     * Test of getResumptionToken method, of class RequestType.
     */
    @Test
    public void testSetAndGetResumptionToken() {
        System.out.println("getResumptionToken");
        RequestType instance = new RequestType();
         String value = null;
        String expResult = value;
        String result = instance.getResumptionToken();
        assertEquals(expResult, result);
        value = "set";
        expResult = value;
        instance.setResumptionToken(value);
        result = instance.getResumptionToken();
        assertEquals(expResult, result);
    }
    
}
