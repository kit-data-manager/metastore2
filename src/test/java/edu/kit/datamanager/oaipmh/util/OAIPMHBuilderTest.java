/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.oaipmh.util;

import edu.kit.datamanager.oaipmh.configuration.OaiPmhConfiguration;
import edu.kit.datamanager.oaipmh.service.AbstractOAIPMHRepository;
import edu.kit.datamanager.oaipmh.service.MetastoreOAIPMHRepository;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openarchives.oai._2.DeletedRecordType;
import org.openarchives.oai._2.DescriptionType;
import org.openarchives.oai._2.GranularityType;
import org.openarchives.oai._2.MetadataFormatType;
import org.openarchives.oai._2.OAIPMHerrorcodeType;
import org.openarchives.oai._2.OAIPMHtype;
import org.openarchives.oai._2.ResumptionTokenType;
import org.openarchives.oai._2.VerbType;

/**
 *
 */
public class OAIPMHBuilderTest {
    
    public OAIPMHBuilderTest() {
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
//
//    @Test
//    public void testConstructor() {
//      OAIPMHBuilder oaipmhBuilder = new OAIPMHBuilder();
//      assertNotNull(oaipmhBuilder);
//    }
//    /**
//     * Test of init method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testInit() {
//        System.out.println("init");
//        AbstractOAIPMHRepository repository = new MetastoreOAIPMHRepository(null);
//        VerbType verb = null;
//        String metadataPrefix = "";
//        String identifier = "";
//        Date from = null;
//        Date until = null;
//        String resumptionToken = "";
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = OAIPMHBuilder.init(repository, verb, metadataPrefix, identifier, from, until, resumptionToken);
//        assertNotNull(result);
//        
//        assertEquals(expResult, result);
//    }
//
//    /**
//     * Test of initRequest method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testInitRequest() {
//        System.out.println("initRequest");
//        AbstractOAIPMHRepository repository = null;
//        VerbType verb = null;
//        String metadataPrefix = "";
//        String identifier = "";
//        Date from = null;
//        Date until = null;
//        String resumptionToken = "";
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.initRequest(repository, verb, metadataPrefix, identifier, from, until, resumptionToken);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getFromDate method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testGetFromDate() {
//        System.out.println("getFromDate");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        Date expResult = null;
//        Date result = instance.getFromDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getUntilDate method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testGetUntilDate() {
//        System.out.println("getUntilDate");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        Date expResult = null;
//        Date result = instance.getUntilDate();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getVerb method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testGetVerb() {
//        System.out.println("getVerb");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        VerbType expResult = null;
//        VerbType result = instance.getVerb();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getMetadataPrefix method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testGetMetadataPrefix() {
//        System.out.println("getMetadataPrefix");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        String expResult = "";
//        String result = instance.getMetadataPrefix();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getIdentifier method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testGetIdentifier() {
//        System.out.println("getIdentifier");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        String expResult = "";
//        String result = instance.getIdentifier();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getResumptionToken method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testGetResumptionToken() {
//        System.out.println("getResumptionToken");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        String expResult = "";
//        String result = instance.getResumptionToken();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addMetadataFormat method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddMetadataFormat() {
//        System.out.println("addMetadataFormat");
//        MetadataFormatType format = null;
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addMetadataFormat(format);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addSet method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddSet_String_String() {
//        System.out.println("addSet");
//        String name = "";
//        String spec = "";
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addSet(name, spec);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addSet method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddSet_3args() {
//        System.out.println("addSet");
//        String name = "";
//        String spec = "";
//        Object description = null;
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addSet(name, spec, description);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addRecord method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddRecord_3args() {
//        System.out.println("addRecord");
//        String identifier = "";
//        Date recordDatestamp = null;
//        List<String> setSpecs = null;
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addRecord(identifier, recordDatestamp, setSpecs);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addRecord method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddRecord_4args() {
//        System.out.println("addRecord");
//        String identifier = "";
//        Date recordDatestamp = null;
//        List<String> setSpecs = null;
//        Object metadata = null;
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addRecord(identifier, recordDatestamp, setSpecs, metadata);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addRecord method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddRecord_5args() {
//        System.out.println("addRecord");
//        String identifier = "";
//        Date recordDatestamp = null;
//        List<String> setSpecs = null;
//        Object metadata = null;
//        Object about = null;
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addRecord(identifier, recordDatestamp, setSpecs, metadata, about);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of addError method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testAddError() {
//        System.out.println("addError");
//        OAIPMHerrorcodeType code = null;
//        String message = "";
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.addError(code, message);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setResumptionToken method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testSetResumptionToken() {
//        System.out.println("setResumptionToken");
//        ResumptionTokenType token = null;
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHBuilder expResult = null;
//        OAIPMHBuilder result = instance.setResumptionToken(token);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isError method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testIsError() {
//        System.out.println("isError");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        boolean expResult = false;
//        boolean result = instance.isError();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of build method, of class OAIPMHBuilder.
//     */
//    @Test
//    public void testBuild() {
//        System.out.println("build");
//        OAIPMHBuilder instance = new OAIPMHBuilder();
//        OAIPMHtype expResult = null;
//        OAIPMHtype result = instance.build();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    
}
