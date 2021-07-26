/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.DataResource;
import java.util.function.Function;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 *Test for exceptions
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) //RANDOM_PORT)
@EntityScan("edu.kit.datamanager")
@EnableJpaRepositories("edu.kit.datamanager")
@ComponentScan({"edu.kit.datamanager"})
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataRecordUtilTest {
  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String SCHEMA_ID = "my_dc";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryUrlResourceIdentifier("anyResourceId");
  private static final ResourceIdentifier RELATED_RESOURCE_2= ResourceIdentifier.factoryUrlResourceIdentifier("anyOtherResourceId");
  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
  @Autowired
  private ILinkedMetadataRecordDao metadataRecordDao;
  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private IContentInformationDao contentInformationDao;
  @Autowired
  private IAllIdentifiersDao allIdentifiersDao;
  @Autowired
  private MetastoreConfiguration metadataConfig;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();
  
  public MetadataRecordUtilTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
    System.out.println("------MetadataControllerTest--------------------------");
    System.out.println("------" + this.metadataConfig);
    System.out.println("------------------------------------------------------");

    contentInformationDao.deleteAll();
    dataResourceDao.deleteAll();
    metadataRecordDao.deleteAll();
    allIdentifiersDao.deleteAll();

      // setup mockMvc
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
              .addFilters(springSecurityFilterChain)
              .apply(documentationConfiguration(this.restDocumentation))
              .build();
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of createMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption1() {
    System.out.println("createMetadataRecord");
    MetastoreConfiguration applicationProperties = null;
    MultipartFile recordDocument = null;
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption2() {
    System.out.println("createMetadataRecord");
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile recordDocument = null;
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption3() {
    System.out.println("createMetadataRecord");
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte [])null);
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption4() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption5() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte [])null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.UnprocessableEntityException.class)
  public void testCreateMetadataRecordExeption6() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption7() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = null;
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption8() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte[]) null);
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption9() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", SCHEMA_ID.getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte [])null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption10() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte [])null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption11() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
//    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte [])null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption12() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setId(SCHEMA_ID);
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
   MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte [])null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  /**
   * Test of updateMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataRecord1() {
    System.out.println("updateMetadataRecord");
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MultipartFile recordDocument = null;
    MultipartFile document = null;
    Function<String, String> supplier = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.updateMetadataRecord(applicationProperties, resourceId, eTag, recordDocument, document, supplier);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("Don't reach this line!");
  }

  /**
   * Test of updateMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataRecord2() {
    System.out.println("updateMetadataRecord");
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte []) null);
    MultipartFile document = null;
    Function<String, String> supplier = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.updateMetadataRecord(applicationProperties, resourceId, eTag, recordDocument, document, supplier);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("Don't reach this line!");
  }

  /**
   * Test of updateMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataRecord3() throws JsonProcessingException {
    System.out.println("updateMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", new String().getBytes());
    MultipartFile document = null;
    Function<String, String> supplier = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.updateMetadataRecord(applicationProperties, resourceId, eTag, recordDocument, document, supplier);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("Don't reach this line!");
  }

  /**
   * Test of updateMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataRecord4() throws JsonProcessingException {
    System.out.println("updateMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MockMultipartFile recordDocument = null;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[])null);
    Function<String, String> supplier = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.updateMetadataRecord(applicationProperties, resourceId, eTag, recordDocument, document, supplier);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("Don't reach this line!");
  }

  /**
   * Test of updateMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataRecord5() throws JsonProcessingException {
    System.out.println("updateMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MockMultipartFile recordDocument = null;
       MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", new String().getBytes());
    Function<String, String> supplier = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.updateMetadataRecord(applicationProperties, resourceId, eTag, recordDocument, document, supplier);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("Don't reach this line!");
  }

  /**
   * Test of updateMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataRecord6() throws JsonProcessingException {
    System.out.println("updateMetadataRecord");
   MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", SCHEMA_ID.getBytes());
       MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    Function<String, String> supplier = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.updateMetadataRecord(applicationProperties, resourceId, eTag, recordDocument, document, supplier);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("Don't reach this line!");
  }
//
//  /**
//   * Test of deleteMetadataRecord method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testDeleteMetadataRecord() {
//    System.out.println("deleteMetadataRecord");
//    MetastoreConfiguration applicationProperties = null;
//    String id = "";
//    String eTag = "";
//    Function<String, String> supplier = null;
//    MetadataRecordUtil.deleteMetadataRecord(applicationProperties, id, eTag, supplier);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }
//
  /**
   * Test of migrateToDataResource method, of class MetadataRecordUtil.
   */
  @Test(expected = NullPointerException.class)
  public void testMigrateToDataResource() {
    System.out.println("migrateToDataResource");
   MetadataRecord record = new MetadataRecord();
   record.setId("08/15");
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    RepoBaseConfiguration applicationProperties = null;
    DataResource expResult = null;
    DataResource result = MetadataRecordUtil.migrateToDataResource(applicationProperties, record);
    assertTrue(true);
  }
//
//  /**
//   * Test of migrateToMetadataRecord method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testMigrateToMetadataRecord() {
//    System.out.println("migrateToMetadataRecord");
//    RepoBaseConfiguration applicationProperties = null;
//    DataResource dataResource = null;
//    MetadataRecord expResult = null;
//    MetadataRecord result = MetadataRecordUtil.migrateToMetadataRecord(applicationProperties, dataResource);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }
//
//  /**
//   * Test of getRecordByIdAndVersion method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testGetRecordByIdAndVersion_MetastoreConfiguration_String() {
//    System.out.println("getRecordByIdAndVersion");
//    MetastoreConfiguration metastoreProperties = null;
//    String recordId = "";
//    MetadataRecord expResult = null;
//    MetadataRecord result = MetadataRecordUtil.getRecordByIdAndVersion(metastoreProperties, recordId);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }
//
//  /**
//   * Test of getRecordByIdAndVersion method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testGetRecordByIdAndVersion_3args() {
//    System.out.println("getRecordByIdAndVersion");
//    MetastoreConfiguration metastoreProperties = null;
//    String recordId = "";
//    Long version = null;
//    MetadataRecord expResult = null;
//    MetadataRecord result = MetadataRecordUtil.getRecordByIdAndVersion(metastoreProperties, recordId, version);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }
//
//  /**
//   * Test of getMetadataDocumentByIdAndVersion method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testGetMetadataDocumentByIdAndVersion_MetastoreConfiguration_String() {
//    System.out.println("getMetadataDocumentByIdAndVersion");
//    MetastoreConfiguration metastoreProperties = null;
//    String recordId = "";
//    Path expResult = null;
//    Path result = MetadataRecordUtil.getMetadataDocumentByIdAndVersion(metastoreProperties, recordId);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }
//
//  /**
//   * Test of getMetadataDocumentByIdAndVersion method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testGetMetadataDocumentByIdAndVersion_3args() {
//    System.out.println("getMetadataDocumentByIdAndVersion");
//    MetastoreConfiguration metastoreProperties = null;
//    String recordId = "";
//    Long version = null;
//    Path expResult = null;
//    Path result = MetadataRecordUtil.getMetadataDocumentByIdAndVersion(metastoreProperties, recordId, version);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }
//
//  /**
//   * Test of mergeRecords method, of class MetadataRecordUtil.
//   */
//  @Test
//  public void testMergeRecords() {
//    System.out.println("mergeRecords");
//    MetadataRecord managed = null;
//    MetadataRecord provided = null;
//    MetadataRecord expResult = null;
//    MetadataRecord result = MetadataRecordUtil.mergeRecords(managed, provided);
//    assertEquals(expResult, result);
//    // TODO review the generated test code and remove the default call to fail.
//    fail("Don't reach this line!");
//  }

  /**
   * Test of setToken method, of class MetadataRecordUtil.
   */
  @Test
  public void testSetToken() {
    System.out.println("setToken");
    String bearerToken = "";
    MetadataRecordUtil.setToken(bearerToken);
  }
  
}
