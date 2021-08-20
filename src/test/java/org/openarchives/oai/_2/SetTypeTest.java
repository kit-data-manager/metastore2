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
 */
public class SetTypeTest {
    
    public SetTypeTest() {
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
     * Test of setSetSpec method, of class SetType.
     */
    @Test
    public void testSetAndGetSetSpec() {
        System.out.println("testSetAndGetSetSpec");
        String value = null;
        SetType instance = new SetType();
        instance.setSetSpec(value);
        String expResult = value;
        String result = instance.getSetSpec();
        assertEquals(expResult, result);
        value = "newValue";
        expResult = value;
        instance.setSetSpec(value);
        result = instance.getSetSpec();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSetName method, of class SetType.
     */
    @Test
    public void testGetAndSetSetName() {
        System.out.println("getSetName");
        String value = null;
        SetType instance = new SetType();
        String expResult = value;
        String result = instance.getSetName();
        assertEquals(expResult, result);
        value = "newName";
        expResult = value;
        instance.setSetName(value);
        result = instance.getSetName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getSetDescription method, of class SetType.
     */
    @Test
    public void testGetSetDescription() {
        System.out.println("getSetDescription");
        SetType instance = new SetType();
        List<DescriptionType> expResult = instance.getSetDescription();
        assertTrue(expResult.isEmpty());
        expResult.add(new DescriptionType());
        List<DescriptionType> result = instance.getSetDescription();
        assertEquals(expResult, result);
        assertEquals(result.size(), 1);

    }
    
}
