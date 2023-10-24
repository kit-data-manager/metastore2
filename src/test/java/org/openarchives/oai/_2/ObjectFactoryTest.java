/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openarchives.oai._2;

import jakarta.xml.bind.JAXBElement;
import javax.crypto.spec.OAEPParameterSpec;
import javax.xml.namespace.QName;
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
public class ObjectFactoryTest {

    public ObjectFactoryTest() {
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
     * Test of createOAIPMHtype method, of class ObjectFactory.
     */
    @Test
    public void testCreateOAIPMHtype() {
        System.out.println("createOAIPMHtype");
        ObjectFactory instance = new ObjectFactory();
        OAIPMHtype expResult = new OAIPMHtype();
        OAIPMHtype result = instance.createOAIPMHtype();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createAboutType method, of class ObjectFactory.
     */
    @Test
    public void testCreateAboutType() {
        System.out.println("createAboutType");
        ObjectFactory instance = new ObjectFactory();
        AboutType expResult = new AboutType();
        AboutType result = instance.createAboutType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createMetadataFormatType method, of class ObjectFactory.
     */
    @Test
    public void testCreateMetadataFormatType() {
        System.out.println("createMetadataFormatType");
        ObjectFactory instance = new ObjectFactory();
        MetadataFormatType expResult = new MetadataFormatType();
        MetadataFormatType result = instance.createMetadataFormatType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createListRecordsType method, of class ObjectFactory.
     */
    @Test
    public void testCreateListRecordsType() {
        System.out.println("createListRecordsType");
        ObjectFactory instance = new ObjectFactory();
        ListRecordsType expResult = new ListRecordsType();
        ListRecordsType result = instance.createListRecordsType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createListMetadataFormatsType method, of class ObjectFactory.
     */
    @Test
    public void testCreateListMetadataFormatsType() {
        System.out.println("createListMetadataFormatsType");
        ObjectFactory instance = new ObjectFactory();
        ListMetadataFormatsType expResult = new ListMetadataFormatsType();
        ListMetadataFormatsType result = instance.createListMetadataFormatsType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createDescriptionType method, of class ObjectFactory.
     */
    @Test
    public void testCreateDescriptionType() {
        System.out.println("createDescriptionType");
        ObjectFactory instance = new ObjectFactory();
        DescriptionType expResult = new DescriptionType();
        DescriptionType result = instance.createDescriptionType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createResumptionTokenType method, of class ObjectFactory.
     */
    @Test
    public void testCreateResumptionTokenType() {
        System.out.println("createResumptionTokenType");
        ObjectFactory instance = new ObjectFactory();
        ResumptionTokenType expResult = new ResumptionTokenType();
        ResumptionTokenType result = instance.createResumptionTokenType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createListSetsType method, of class ObjectFactory.
     */
    @Test
    public void testCreateListSetsType() {
        System.out.println("createListSetsType");
        ObjectFactory instance = new ObjectFactory();
        ListSetsType expResult = new ListSetsType();
        ListSetsType result = instance.createListSetsType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createListIdentifiersType method, of class ObjectFactory.
     */
    @Test
    public void testCreateListIdentifiersType() {
        System.out.println("createListIdentifiersType");
        ObjectFactory instance = new ObjectFactory();
        ListIdentifiersType expResult = new ListIdentifiersType();
        ListIdentifiersType result = instance.createListIdentifiersType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createRequestType method, of class ObjectFactory.
     */
    @Test
    public void testCreateRequestType() {
        System.out.println("createRequestType");
        ObjectFactory instance = new ObjectFactory();
        RequestType expResult = new RequestType();
        RequestType result = instance.createRequestType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createRecordType method, of class ObjectFactory.
     */
    @Test
    public void testCreateRecordType() {
        System.out.println("createRecordType");
        ObjectFactory instance = new ObjectFactory();
        RecordType expResult = new RecordType();
        RecordType result = instance.createRecordType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createGetRecordType method, of class ObjectFactory.
     */
    @Test
    public void testCreateGetRecordType() {
        System.out.println("createGetRecordType");
        ObjectFactory instance = new ObjectFactory();
        GetRecordType expResult = new GetRecordType();
        GetRecordType result = instance.createGetRecordType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createSetType method, of class ObjectFactory.
     */
    @Test
    public void testCreateSetType() {
        System.out.println("createSetType");
        ObjectFactory instance = new ObjectFactory();
        SetType expResult = new SetType();
        SetType result = instance.createSetType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createIdentifyType method, of class ObjectFactory.
     */
    @Test
    public void testCreateIdentifyType() {
        System.out.println("createIdentifyType");
        ObjectFactory instance = new ObjectFactory();
        IdentifyType expResult = new IdentifyType();
        IdentifyType result = instance.createIdentifyType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createOAIPMHerrorType method, of class ObjectFactory.
     */
    @Test
    public void testCreateOAIPMHerrorType() {
        System.out.println("createOAIPMHerrorType");
        ObjectFactory instance = new ObjectFactory();
        OAIPMHerrorType expResult = new OAIPMHerrorType();
        OAIPMHerrorType result = instance.createOAIPMHerrorType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createHeaderType method, of class ObjectFactory.
     */
    @Test
    public void testCreateHeaderType() {
        System.out.println("createHeaderType");
        ObjectFactory instance = new ObjectFactory();
        HeaderType expResult = new HeaderType();
        HeaderType result = instance.createHeaderType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createMetadataType method, of class ObjectFactory.
     */
    @Test
    public void testCreateMetadataType() {
        System.out.println("createMetadataType");
        ObjectFactory instance = new ObjectFactory();
        MetadataType expResult = new MetadataType();
        MetadataType result = instance.createMetadataType();
        assertEquals(expResult.getClass(), result.getClass());
    }

    /**
     * Test of createOAIPMH method, of class ObjectFactory.
     */
    @Test
    public void testCreateOAIPMH() {
        System.out.println("createOAIPMH");
        OAIPMHtype value = null;
        ObjectFactory instance = new ObjectFactory();
        JAXBElement<OAIPMHtype> expResult;
        expResult = new JAXBElement<OAIPMHtype>(new QName("http://www.openarchives.org/OAI/2.0/", "OAI-PMH"), OAIPMHtype.class, null, value);
        JAXBElement<OAIPMHtype> result = instance.createOAIPMH(value);
        assertEquals(expResult.getClass(), result.getClass());
        assertEquals(expResult.getName(), result.getName());
        assertEquals(expResult.getValue(), result.getValue());
    }

}
