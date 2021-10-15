/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
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
public class ResumptionTokenTypeTest {
    
    public ResumptionTokenTypeTest() {
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
     * Test of getValue method, of class ResumptionTokenType.
     */
    @Test
    public void testSetAndGetValue() {
        System.out.println("testSetAndGetValue");
        ResumptionTokenType instance = new ResumptionTokenType();
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
     * Test of getExpirationDate method, of class ResumptionTokenType.
     */
    @Test
    public void testSetAndGetExpirationDate() throws DatatypeConfigurationException {
        System.out.println("testSetAndGetExpirationDate");
        ResumptionTokenType instance = new ResumptionTokenType();
        XMLGregorianCalendar value = null;
        XMLGregorianCalendar expResult = value;
        XMLGregorianCalendar result = instance.getExpirationDate();
        assertEquals(expResult, result);
        value = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        expResult = value;
        instance.setExpirationDate(value);
        result = instance.getExpirationDate();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCompleteListSize method, of class ResumptionTokenType.
     */
    @Test
    public void testSetAndGetCompleteListSize() {
        System.out.println("getCompleteListSize");
        ResumptionTokenType instance = new ResumptionTokenType();
        BigInteger value = null;
        BigInteger expResult = value;
        BigInteger result = instance.getCompleteListSize();
        assertEquals(expResult, result);
        value = BigInteger.TEN;
        expResult = value;
        instance.setCompleteListSize(value);
        result = instance.getCompleteListSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCursor method, of class ResumptionTokenType.
     */
    @Test
    public void testSetAndGetCursor() {
        System.out.println("getCursor");
        ResumptionTokenType instance = new ResumptionTokenType();
        BigInteger value = null;
        BigInteger expResult = value;
        BigInteger result = instance.getCursor();
        assertEquals(expResult, result);
        value = BigInteger.TEN;
        expResult = value;
        instance.setCursor(value);
        result = instance.getCursor();
        assertEquals(expResult, result);
    }
    
}
