/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.service.IContentInformationService;
import java.io.ByteArrayInputStream;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.time.Instant;
import java.util.function.Function;
import org.javers.core.Javers;
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
import org.springframework.test.context.TestPropertySource;
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
 * Test for exceptions
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
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_util;DB_CLOSE_DELAY=-1"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataRecordUtilTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String PID = "anyPID";
  private static final String PRINCIPAL = "principal";
  private static final String SCHEMA_ID = "my_dc";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryInternalResourceIdentifier("anyResourceId");
  private static final ResourceIdentifier RELATED_RESOURCE_2 = ResourceIdentifier.factoryInternalResourceIdentifier("anyOtherResourceId");
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
  @Autowired
  Javers javers = null;
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
   * Constructor
   */
  @Test
  public void testConstructor() {
    assertNotNull(new MetadataRecordUtil());
  }

  /**
   * Test of createMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption1() {
    System.out.println("createMetadataRecord");
    // all null
    MetastoreConfiguration applicationProperties = null;
    MultipartFile recordDocument = null;
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption2() {
    // valid MetastoreConfiguration
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
    // empty record document
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte[]) null);
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption3a() {
    System.out.println("createMetadataRecord");
    // invalid record document
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", new String("{nonsense}").getBytes());
    MultipartFile document = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  ////////////////////////////////////////////////////////////////////////
  // Test with invalid records
  ////////////////////////////////////////////////////////////////////////
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption4() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    // set schema to null
    record.setSchema(null);
    record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier("any"));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption4a() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    // set schema identifier to null
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(null));
    record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier("any"));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption4b() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    // set related resource to null
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption4c() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    // set related resource identifier to null
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier(null));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption4d() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    // set id which is not allowed for create 
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setSchemaVersion(1l);
    record.setRelatedResource(RELATED_RESOURCE);
    record.setId("anyId");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  ////////////////////////////////////////////////////////////////////////
  // Test with invalid document
  ////////////////////////////////////////////////////////////////////////
  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption5() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    // empty document
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[]) null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption5a() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", new String("{nonsense}").getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption6() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(null));
    record.setRelatedResource(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption6a() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(null));
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
  public void testCreateMetadataRecordExeption6b() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", mapper.writeValueAsString(record).getBytes());
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.UnprocessableEntityException.class)
  public void testCreateMetadataRecordExeption6c() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", SCHEMA_ID.getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[]) null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption10() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[]) null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption11() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    // set schema identifier to null
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(null));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[]) null);;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.createMetadataRecord(applicationProperties, recordDocument, document);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataRecordExeption12() throws JsonProcessingException {
    System.out.println("createMetadataRecord");
    MetadataRecord record = new MetadataRecord();
    record.setId(SCHEMA_ID);
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MetastoreConfiguration applicationProperties = metadataConfig;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[]) null);;
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
    MockMultipartFile recordDocument = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte[]) null);
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();
    MetastoreConfiguration applicationProperties = null;
    String resourceId = "";
    String eTag = "";
    MockMultipartFile recordDocument = null;
    MultipartFile document = new MockMultipartFile("document", "metadata.xml", "application/xml", (byte[]) null);
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
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
  @Test(expected = ResourceNotFoundException.class)
  public void testMigrateToDataResource() {
    System.out.println("migrateToDataResource");
    MetadataRecord record = new MetadataRecord();
    record.setId("08/15");
    record.setRecordVersion(1l);
    record.setCreatedAt(Instant.now());
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    RepoBaseConfiguration applicationProperties = metadataConfig;
    DataResource expResult = null;
    DataResource result = MetadataRecordUtil.migrateToDataResource(applicationProperties, record);
    fail("Don't reach this line!");
  }

  /**
   * Test of migrateToMetadataRecord method, of class MetadataRecordUtil.
   */
  @Test
  public void testMigrateToMetadataRecord() {
    System.out.println("migrateToMetadataRecord");
    RepoBaseConfiguration applicationProperties = metadataConfig;
    DataResource dataResource = null;
    MetadataRecord expResult = new MetadataRecord();
    MetadataRecord result = MetadataRecordUtil.migrateToMetadataRecord(applicationProperties, dataResource, false);
    assertEquals(expResult, result);
    dataResource = DataResource.factoryNewDataResource();
    dataResource.getAlternateIdentifiers().clear();
    // Test with id &  PrimaryIdentifier
    result = MetadataRecordUtil.migrateToMetadataRecord(applicationProperties, dataResource, false);
    assertNotNull(result.getId());
    assertEquals("Id should be the same!", result.getId(), dataResource.getId());
    assertEquals("Version should be '1'", Long.valueOf(1l), result.getRecordVersion());
    assertTrue("ACL should be empty", result.getAcl().isEmpty());
    assertNull("PID should be empty", result.getPid());
    assertNull("Create date should be empty!", result.getCreatedAt());
    assertNull("Last update date should be empty!", result.getLastUpdate());
    // Test with one (internal) alternate identifier.
    dataResource = DataResource.factoryNewDataResource();
    dataResource.getDates().add(Date.factoryDate(Instant.now(), Date.DATE_TYPE.ISSUED));
    result = MetadataRecordUtil.migrateToMetadataRecord(applicationProperties, dataResource, false);
    assertNotNull(result.getId());
    assertEquals("Id should be the same!", result.getId(), dataResource.getId());
    assertEquals("Version should be '1'", Long.valueOf(1l), result.getRecordVersion());
    assertTrue("ACL should be empty", result.getAcl().isEmpty());
    assertNull("PID should be empty", result.getPid());
    assertNull("Create date should be empty!", result.getCreatedAt());
    assertNull("Last update date should be empty!", result.getLastUpdate());
    
    
    // Test migration of PID with two alternate identifiers (internal & UPC)
    dataResource.getAlternateIdentifiers().add(Identifier.factoryIdentifier(PID, Identifier.IDENTIFIER_TYPE.UPC));
    result = MetadataRecordUtil.migrateToMetadataRecord(applicationProperties, dataResource, false);
    assertNotNull(result.getId());
    assertEquals("Id should be the same!", result.getId(), dataResource.getId());
    assertEquals("Version should be '1'", Long.valueOf(1l), result.getRecordVersion());
    assertTrue("ACL should be empty", result.getAcl().isEmpty());
    assertNull("Create date should be empty!", result.getCreatedAt());
    assertNull("Last update date should be empty!", result.getLastUpdate());
    // PID should be set
    assertNotNull("PID shouldn't be NULL", result.getPid());
    assertEquals(PID, result.getPid().getIdentifier());
    assertEquals(ResourceIdentifier.IdentifierType.UPC, result.getPid().getIdentifierType());
    // Add schemaID, resourceType, relatedIdentifier for schema
    //@ToDo Make this working again
//    dataResource.getTitles().add(Title.factoryTitle(SCHEMA_ID));
//    dataResource.setResourceType(ResourceType.createResourceType(SCHEMA_ID));
//    RelatedIdentifier relId = RelatedIdentifier.factoryRelatedIdentifier(RelatedIdentifier.RELATION_TYPES.IS_DERIVED_FROM, SCHEMA_ID, null, null);
//    relId.setIdentifierType(Identifier.IDENTIFIER_TYPE.INTERNAL);
//    dataResource.getRelatedIdentifiers().add(relId);
//    // dataResourceService adds ACL
//    dataResource = applicationProperties.getDataResourceService().create(dataResource, PRINCIPAL);
//    IContentInformationService contentInformationService = applicationProperties.getContentInformationService();
//    MetadataSchemaRecord msr = new MetadataSchemaRecord();
//    msr.setSchemaId(SCHEMA_ID);
//    msr.setSchemaVersion(1l);
//    msr.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
//    DataResource dr_schema = MetadataSchemaRecordUtil.migrateToDataResource(applicationProperties, msr);
//    applicationProperties.getDataResourceService().create(dr_schema, PRINCIPAL);
//    contentInformationService.create(ContentInformation.createContentInformation("anyFile"), dataResource, SCHEMA_ID, new ByteArrayInputStream(SCHEMA_ID.getBytes()), true);
//    result = MetadataRecordUtil.migrateToMetadataRecord(applicationProperties, dataResource, false);
//    assertNotNull(result.getId());
//    assertEquals("Id should be the same!", result.getId(), dataResource.getId());
//    assertEquals("Version should be '1'", Long.valueOf(1l), result.getRecordVersion());
//    assertFalse("ACL shouldn't be empty", result.getAcl().isEmpty());
//    assertEquals("ACL should contain one entry for 'SELF'", 1, result.getAcl().size());
//    edu.kit.datamanager.repo.domain.acl.AclEntry next = result.getAcl().iterator().next();
//    assertEquals("SID should be principal set before!'", PRINCIPAL, next.getSid());
//    assertEquals("Persmission should be 'ADMINISTRATE'", PERMISSION.ADMINISTRATE, next.getPermission());
//    // Dates should be set bei content information service.
//    assertNotNull("Create date should be set!", result.getCreatedAt());
//    assertNotNull("Last update date should be set!", result.getLastUpdate());
//    // PID should be set
//    assertEquals(PID, result.getPid().getIdentifier());
//    assertEquals(ResourceIdentifier.IdentifierType.UPC, result.getPid().getIdentifierType());
  }

  /**
   * Test of mergeRecords method, of class MetadataRecordUtil.
   */
  @Test
  public void testMergeRecords() {
    System.out.println("mergeRecords");
    MetadataRecord managed = null;
    MetadataRecord provided = null;
    MetadataRecord expResult = null;
    MetadataRecord result = MetadataRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);

    provided = new MetadataRecord();
    result = MetadataRecordUtil.mergeRecords(managed, provided);
    assertNotNull(result);
    assertEquals(provided, result);
    managed = provided;
    provided = null;
    result = MetadataRecordUtil.mergeRecords(managed, provided);
    assertNotNull(result);
    assertEquals(managed, result);

    managed = new MetadataRecord();
    provided = new MetadataRecord();
    provided.setPid(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    result = MetadataRecordUtil.mergeRecords(managed, provided);
    assertNotNull(result);
    assertEquals(provided, result);
    
    managed = new MetadataRecord();
    provided = new MetadataRecord();
    provided.getAcl().add(new edu.kit.datamanager.repo.domain.acl.AclEntry(SCHEMA_ID, PERMISSION.WRITE));
    result = MetadataRecordUtil.mergeRecords(managed, provided);
    assertNotNull(result);
    provided.setPid(result.getPid());
    assertEquals(provided, result);
    
    managed = new MetadataRecord();
    provided = new MetadataRecord();
    provided.setRelatedResource(RELATED_RESOURCE);
    result = MetadataRecordUtil.mergeRecords(managed, provided);
    assertNotNull(result);
    provided.setPid(result.getPid());
    assertEquals(provided, result);
  }

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
