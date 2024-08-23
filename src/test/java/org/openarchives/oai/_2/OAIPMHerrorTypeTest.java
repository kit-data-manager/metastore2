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
public class OAIPMHerrorTypeTest {

    public OAIPMHerrorTypeTest() {
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
     * Test of getValue method, of class OAIPMHerrorType.
     */
    @Test
    public void testSetAndGetValue() {
        System.out.println("getValue");
        OAIPMHerrorType instance = new OAIPMHerrorType();
        String value = null;
        String expResult = value;
        String result = instance.getValue();
        assertEquals(expResult, result);
        value = "value";
        expResult = value;
        instance.setValue(value);
        result = instance.getValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCode method, of class OAIPMHerrorType.
     */
    @Test
    public void testSetAndGetCode() {
        System.out.println("getCode");
        OAIPMHerrorType instance = new OAIPMHerrorType();
        OAIPMHerrorcodeType value = null;
        OAIPMHerrorcodeType expResult = value;
        OAIPMHerrorcodeType result = instance.getCode();
        assertEquals(expResult, result);
        for (OAIPMHerrorcodeType item : OAIPMHerrorcodeType.values()) {
            value = item;
            expResult = value;
            instance.setCode(value);
            result = instance.getCode();
            assertEquals(expResult, result);
        }
    }

}
