/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

import jakarta.xml.bind.annotation.XmlEnumValue;
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
public class OAIPMHerrorcodeTypeTest {
    
    public OAIPMHerrorcodeTypeTest() {
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
     * Test of values method, of class OAIPMHerrorcodeType.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        OAIPMHerrorcodeType[] result = OAIPMHerrorcodeType.values();
        assertEquals(result.length, 8);
    }

   /**
     * Test of valueOf method, of class OAIPMHerrorcodeType.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String[] values = {"CANNOT_DISSEMINATE_FORMAT", "ID_DOES_NOT_EXIST", "BAD_ARGUMENT", "BAD_VERB", "NO_METADATA_FORMATS",
            "NO_RECORDS_MATCH", "BAD_RESUMPTION_TOKEN", "NO_SET_HIERARCHY"};
        OAIPMHerrorcodeType[] expResults = {OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, 
            OAIPMHerrorcodeType.BAD_ARGUMENT, OAIPMHerrorcodeType.BAD_VERB, OAIPMHerrorcodeType.NO_METADATA_FORMATS, 
            OAIPMHerrorcodeType.NO_RECORDS_MATCH, OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, OAIPMHerrorcodeType.NO_SET_HIERARCHY};
        for (int index1 = 0; index1 < values.length; index1++) {
            OAIPMHerrorcodeType result = OAIPMHerrorcodeType.valueOf(values[index1].toUpperCase());
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of value method, of class OAIPMHerrorcodeType.
     */
    @Test
    public void testValue() {
        System.out.println("value");
        String[] expResults = {"cannotDisseminateFormat","idDoesNotExist","badArgument",
    "badVerb","noMetadataFormats","noRecordsMatch","badResumptionToken","noSetHierarchy"};
        OAIPMHerrorcodeType[] verbTypes = {OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, 
            OAIPMHerrorcodeType.BAD_ARGUMENT, OAIPMHerrorcodeType.BAD_VERB, OAIPMHerrorcodeType.NO_METADATA_FORMATS, 
            OAIPMHerrorcodeType.NO_RECORDS_MATCH, OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, OAIPMHerrorcodeType.NO_SET_HIERARCHY};
        for (int index1 = 0; index1 < verbTypes.length; index1++) {
            String result = verbTypes[index1].value();
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class OAIPMHerrorcodeType.
     */
    @Test
    public void testFromValue() {
        System.out.println("fromValue");
        String[] values = {"cannotDisseminateFormat","idDoesNotExist","badArgument",
    "badVerb","noMetadataFormats","noRecordsMatch","badResumptionToken","noSetHierarchy"};
        OAIPMHerrorcodeType[] expResults = {OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, 
            OAIPMHerrorcodeType.BAD_ARGUMENT, OAIPMHerrorcodeType.BAD_VERB, OAIPMHerrorcodeType.NO_METADATA_FORMATS, 
            OAIPMHerrorcodeType.NO_RECORDS_MATCH, OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, OAIPMHerrorcodeType.NO_SET_HIERARCHY};
        for (int index1 = 0; index1 < values.length; index1++) {
            OAIPMHerrorcodeType result = OAIPMHerrorcodeType.fromValue(values[index1]);
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class OAIPMHerrorcodeType.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValueWithException() {
        System.out.println("fromValue");
        String value = "CANNOT_DISSEMINATE_FORMAT";
        OAIPMHerrorcodeType expResult = OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT;
        OAIPMHerrorcodeType result = OAIPMHerrorcodeType.fromValue(value);
        assertEquals(expResult, result);
    }
     
}
