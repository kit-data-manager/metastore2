/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

import java.util.List;
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
public class OAIPMHtypeTest {
    
    public OAIPMHtypeTest() {
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
     * Test of getResponseDate method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetResponseDate() throws DatatypeConfigurationException {
        System.out.println("getResponseDate");
        OAIPMHtype instance = new OAIPMHtype();
        XMLGregorianCalendar value = null;
        XMLGregorianCalendar expResult = value;
        XMLGregorianCalendar result = instance.getResponseDate();
        assertEquals(expResult, result);
        value = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        expResult = value;
        instance.setResponseDate(value);
        result = instance.getResponseDate();
        assertEquals(expResult, result);
    }

    /**
     * Test of getRequest method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetRequest() {
        System.out.println("getRequest");
        OAIPMHtype instance = new OAIPMHtype();
        RequestType value = null;
        RequestType expResult = value;
        RequestType result = instance.getRequest();
        assertEquals(expResult, result);
        value = new RequestType();
        expResult = value;
        instance.setRequest(value);
        result = instance.getRequest();
        assertEquals(expResult, result);
    }

    /**
     * Test of getError method, of class OAIPMHtype.
     */
    @Test
    public void testGetError() {
        System.out.println("getError");
        OAIPMHtype instance = new OAIPMHtype();
        List<OAIPMHerrorType>  result = instance.getError();
        assertEquals(result.size(), 0);
    }

    /**
     * Test of getIdentify method, of class OAIPMHtype.
     */
    @Test
    public void testSetSAndGetIdentify() {
        System.out.println("getIdentify");
        OAIPMHtype instance = new OAIPMHtype();
        IdentifyType value = null;
        IdentifyType expResult = value;
        IdentifyType result = instance.getIdentify();
        assertEquals(expResult, result);
        value = new IdentifyType();
        expResult = value;
        instance.setIdentify(value);
        result = instance.getIdentify();
        assertEquals(expResult, result);
    }

    /**
     * Test of getListMetadataFormats method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetListMetadataFormats() {
        System.out.println("getListMetadataFormats");
        OAIPMHtype instance = new OAIPMHtype();
        ListMetadataFormatsType value = null;
        ListMetadataFormatsType expResult = value;
        ListMetadataFormatsType result = instance.getListMetadataFormats();
        assertEquals(expResult, result);
        value = new ListMetadataFormatsType();
        expResult = value;
        instance.setListMetadataFormats(value);
        result = instance.getListMetadataFormats();
        assertEquals(expResult, result);
    }

    /**
     * Test of getListSets method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetListSets() {
        System.out.println("getListSets");
        OAIPMHtype instance = new OAIPMHtype();
        ListSetsType value = null;
        ListSetsType expResult = value;
        ListSetsType result = instance.getListSets();
        assertEquals(expResult, result);
        value = new ListSetsType();
        expResult = value;
        instance.setListSets(value);
        result = instance.getListSets();
        assertEquals(expResult, result);
    }

    /**
     * Test of getGetRecord method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetGetRecord() {
        System.out.println("getGetRecord");
        OAIPMHtype instance = new OAIPMHtype();
         GetRecordType value = null;
        GetRecordType expResult = value;
        GetRecordType result = instance.getGetRecord();
        assertEquals(expResult, result);
        value = new GetRecordType();
        expResult = value;
        instance.setGetRecord(value);
        result = instance.getGetRecord();
        assertEquals(expResult, result);
    }

    /**
     * Test of getListIdentifiers method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetListIdentifiers() {
        System.out.println("getListIdentifiers");
        OAIPMHtype instance = new OAIPMHtype();
        ListIdentifiersType value = null;
        ListIdentifiersType expResult = value;
        ListIdentifiersType result = instance.getListIdentifiers();
        assertEquals(expResult, result);
        value = new ListIdentifiersType();
        expResult = value;
        instance.setListIdentifiers(value);
        result = instance.getListIdentifiers();
        assertEquals(expResult, result);
    }

    /**
     * Test of getListRecords method, of class OAIPMHtype.
     */
    @Test
    public void testSetAndGetListRecords() {
        System.out.println("getListRecords");
        OAIPMHtype instance = new OAIPMHtype();
        ListRecordsType value = null;
        ListRecordsType expResult = value;
        ListRecordsType result = instance.getListRecords();
        assertEquals(expResult, result);
        value = new ListRecordsType();
        expResult = value;
        instance.setListRecords(value);
        result = instance.getListRecords();
        assertEquals(expResult, result);
    }
}
