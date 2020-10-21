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
 */
public class VerbTypeTest {

    public VerbTypeTest() {
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
     * Test of values method, of class VerbType.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        VerbType[] result = VerbType.values();
        assertEquals(result.length, 6);
    }

    /**
     * Test of valueOf method, of class VerbType.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String[] values = {"IDENTIFY", "LIST_METADATA_FORMATS", "LIST_SETS", "GET_RECORD", "LIST_IDENTIFIERS", "LIST_RECORDS"};
        VerbType[] expResults = {VerbType.IDENTIFY, VerbType.LIST_METADATA_FORMATS, VerbType.LIST_SETS, VerbType.GET_RECORD, VerbType.LIST_IDENTIFIERS, VerbType.LIST_RECORDS};
        for (int index1 = 0; index1 < values.length; index1++) {
            VerbType result = VerbType.valueOf(values[index1].toUpperCase());
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of value method, of class VerbType.
     */
    @Test
    public void testValue() {
        System.out.println("value");
        String[] expResults = {"Identify", "ListMetadataFormats", "ListSets", "GetRecord", "ListIdentifiers", "ListRecords"};
        VerbType[] verbTypes = {VerbType.IDENTIFY, VerbType.LIST_METADATA_FORMATS, VerbType.LIST_SETS, VerbType.GET_RECORD, VerbType.LIST_IDENTIFIERS, VerbType.LIST_RECORDS};
        for (int index1 = 0; index1 < verbTypes.length; index1++) {
            String result = verbTypes[index1].value();
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class VerbType.
     */
    @Test
    public void testFromValue() {
        System.out.println("fromValue");
        String[] values = {"Identify", "ListMetadataFormats", "ListSets", "GetRecord", "ListIdentifiers", "ListRecords"};
        VerbType[] expResults = {VerbType.IDENTIFY, VerbType.LIST_METADATA_FORMATS, VerbType.LIST_SETS, VerbType.GET_RECORD, VerbType.LIST_IDENTIFIERS, VerbType.LIST_RECORDS};
        for (int index1 = 0; index1 < values.length; index1++) {
            VerbType result = VerbType.fromValue(values[index1]);
            assertEquals(expResults[index1], result);
        }
    }

    /**
     * Test of fromValue method, of class VerbType.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromValueWithException() {
        System.out.println("fromValue");
        String value = "IDENTIFY";
        VerbType expResult = VerbType.IDENTIFY;
        VerbType result = VerbType.fromValue(value);
        assertEquals(expResult, result);
    }
}
