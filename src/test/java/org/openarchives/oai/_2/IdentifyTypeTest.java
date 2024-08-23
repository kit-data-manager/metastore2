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
 * @author hartmann-v
 */
public class IdentifyTypeTest {
    
    public IdentifyTypeTest() {
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
     * Test of getRepositoryName method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetRepositoryName() {
        System.out.println("getRepositoryName");
        IdentifyType instance = new IdentifyType();
        String value = null;
        String expResult = value;
        String result = instance.getRepositoryName();
        assertEquals(expResult, result);
        value = "repoName";
        expResult = value;
        instance.setRepositoryName(value);
        result = instance.getRepositoryName();
        assertEquals(expResult, result);
    }
    /**
     * Test of getBaseURL method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetBaseURL() {
        System.out.println("getBaseURL");
        IdentifyType instance = new IdentifyType();
        String value = null;
        String expResult = value;
        String result = instance.getBaseURL();
        assertEquals(expResult, result);
        value = "baseUrl";
        expResult = value;
        instance.setBaseURL(value);
        result = instance.getBaseURL();
        assertEquals(expResult, result);
    }

    /**
     * Test of getProtocolVersion method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetProtocolVersion() {
        System.out.println("getProtocolVersion");
        IdentifyType instance = new IdentifyType();
        String value = null;
        String expResult = value;
        String result = instance.getProtocolVersion();
        assertEquals(expResult, result);
        value = "protocolVersion";
        expResult = value;
        instance.setProtocolVersion(value);
        result = instance.getProtocolVersion();
        assertEquals(expResult, result);
    }

    /**
     * Test of getAdminEmail method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetAdminEmail() {
        System.out.println("getAdminEmail");
        IdentifyType instance = new IdentifyType();
        List<String> value = null;
        List<String> expResult = value;
        List<String> result = instance.getAdminEmail();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add("admin@localhost");
        result = instance.getAdminEmail();
        assertEquals(expResult, result);
    }

    /**
     * Test of getEarliestDatestamp method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetEarliestDatestamp() {
        System.out.println("getEarliestDatestamp");
        IdentifyType instance = new IdentifyType();
        String value = null;
        String expResult = value;
        String result = instance.getEarliestDatestamp();
        assertEquals(expResult, result);
        value = "firstDateStamp";
        expResult = value;
        instance.setEarliestDatestamp(value);
        result = instance.getEarliestDatestamp();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDeletedRecord method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetDeletedRecord() {
        System.out.println("getDeletedRecord");
        IdentifyType instance = new IdentifyType();
        DeletedRecordType value = null;
        DeletedRecordType expResult = value;
        DeletedRecordType result = instance.getDeletedRecord();
        assertEquals(expResult, result);
        value = DeletedRecordType.NO;
        expResult = value;
        instance.setDeletedRecord(value);
        result = instance.getDeletedRecord();
        assertEquals(expResult, result);
    }

    /**
     * Test of getGranularity method, of class IdentifyType.
     */
    @Test
    public void testSetAndGetGranularity() {
        System.out.println("getGranularity");
        IdentifyType instance = new IdentifyType();
        GranularityType value = null;
        GranularityType expResult = value;
        GranularityType result = instance.getGranularity();
        assertEquals(expResult, result);
        value = GranularityType.YYYY_MM_DD;
        expResult = value;
        instance.setGranularity(value);
        result = instance.getGranularity();
        assertEquals(expResult, result);
    }

    /**
     * Test of getCompression method, of class IdentifyType.
     */
    @Test
    public void testGetCompression() {
        System.out.println("getCompression");
        IdentifyType instance = new IdentifyType();
        List<String> value = null;
        List<String> expResult = value;
        List<String> result = instance.getCompression();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add("gzip");
        result = instance.getCompression();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDescription method, of class IdentifyType.
     */
    @Test
    public void testGetDescription() {
        System.out.println("getDescription");
        IdentifyType instance = new IdentifyType();
        List<DescriptionType> value = null;
        List<DescriptionType> expResult = value;
        List<DescriptionType> result = instance.getDescription();
        assertTrue(result.isEmpty());
        expResult = result;
        expResult.add(new DescriptionType());
        result = instance.getDescription();
        assertEquals(expResult, result);
    }
    
}
