/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.oaipmh.service;

import edu.kit.datamanager.metastore2.oaipmh.util.OAIPMHBuilder;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.openarchives.oai._2.DeletedRecordType;
import org.openarchives.oai._2.DescriptionType;
import org.openarchives.oai._2.GranularityType;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 */
//@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
//  DependencyInjectionTestExecutionListener.class,
//  DirtiesContextTestExecutionListener.class,
//  TransactionalTestExecutionListener.class,
//  WithSecurityContextTestExecutionListener.class})
//@ActiveProfiles("test")
//@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/schematest/schema"})
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetastoreOAIPMHRepositoryTest {
    
    public MetastoreOAIPMHRepositoryTest() {
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

//    /**
//     * Test of getRepositoryDescription method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetRepositoryDescription() {
//        System.out.println("getRepositoryDescription");
//        MetastoreOAIPMHRepository instance = null;
//        List<DescriptionType> expResult = null;
//        List<DescriptionType> result = instance.getRepositoryDescription();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getDeletedRecordSupport method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetDeletedRecordSupport() {
//        System.out.println("getDeletedRecordSupport");
//        MetastoreOAIPMHRepository instance = null;
//        DeletedRecordType expResult = null;
//        DeletedRecordType result = instance.getDeletedRecordSupport();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAdminEmail method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetAdminEmail() {
//        System.out.println("getAdminEmail");
//        MetastoreOAIPMHRepository instance = null;
//        List<String> expResult = null;
//        List<String> result = instance.getAdminEmail();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getEarliestDatestamp method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetEarliestDatestamp() {
//        System.out.println("getEarliestDatestamp");
//        MetastoreOAIPMHRepository instance = null;
//        String expResult = "";
//        String result = instance.getEarliestDatestamp();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getGranularity method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetGranularity() {
//        System.out.println("getGranularity");
//        MetastoreOAIPMHRepository instance = null;
//        GranularityType expResult = null;
//        GranularityType result = instance.getGranularity();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getBaseUrl method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetBaseUrl() {
//        System.out.println("getBaseUrl");
//        MetastoreOAIPMHRepository instance = null;
//        String expResult = "";
//        String result = instance.getBaseUrl();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isPrefixSupported method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testIsPrefixSupported() {
//        System.out.println("isPrefixSupported");
//        String prefix = "";
//        MetastoreOAIPMHRepository instance = null;
//        boolean expResult = false;
//        boolean result = instance.isPrefixSupported(prefix);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of identify method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testIdentify() {
//        System.out.println("identify");
//        OAIPMHBuilder builder = null;
//        MetastoreOAIPMHRepository instance = null;
//        instance.identify(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listSets method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testListSets() {
//        System.out.println("listSets");
//        OAIPMHBuilder builder = null;
//        MetastoreOAIPMHRepository instance = null;
//        instance.listSets(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listMetadataFormats method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testListMetadataFormats() {
//        System.out.println("listMetadataFormats");
//        OAIPMHBuilder builder = null;
//        MetastoreOAIPMHRepository instance = null;
//        instance.listMetadataFormats(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listIdentifiers method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testListIdentifiers() {
//        System.out.println("listIdentifiers");
//        OAIPMHBuilder builder = null;
//        MetastoreOAIPMHRepository instance = null;
//        instance.listIdentifiers(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getRecord method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testGetRecord() {
//        System.out.println("getRecord");
//        OAIPMHBuilder builder = null;
//        MetastoreOAIPMHRepository instance = null;
//        instance.getRecord(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listRecords method, of class SimpleOAIPMHRepository.
//     */
//    @Test
//    public void testListRecords() {
//        System.out.println("listRecords");
//        OAIPMHBuilder builder = null;
//        MetastoreOAIPMHRepository instance = null;
//        instance.listRecords(builder);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    
}
