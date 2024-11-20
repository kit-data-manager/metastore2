/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author hartmann-v
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT) //RANDOM_PORT)
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
@TestPropertySource(properties = {"server.port=41419"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ISchemaRecordDaoTest {

  @Autowired
  ISchemaRecordDao schemaRecordDao;

  public ISchemaRecordDaoTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
    prepareDataBase();
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of existsSchemaRecordBySchemaIdAndVersion method, of class
   * ISchemaRecordDao.
   */
  @Test
  public void testExistsSchemaRecordBySchemaIdAndVersion() {
    System.out.println("existsSchemaRecordBySchemaIdAndVersion");
    String schemaId = "schemaId";
    Long version = 1L;
    ISchemaRecordDao instance = schemaRecordDao;
    boolean expResult = false;
    boolean result = instance.existsSchemaRecordBySchemaIdAndVersion(schemaId, version);
    assertEquals(expResult, result);
    schemaId = "schemaId1";
    version = 4L;
    result = instance.existsSchemaRecordBySchemaIdAndVersion(schemaId, version);
    assertEquals(expResult, result);
    schemaId = "schemaId1";
    version = 3L;
    result = instance.existsSchemaRecordBySchemaIdAndVersion(schemaId, version);
    assertEquals(expResult, result);    // TODO review the generated test code and remove the default call to fail.
    schemaId = "schemaId1/3";
    version = 2L;
    result = instance.existsSchemaRecordBySchemaIdAndVersion(schemaId, version);
    assertEquals(expResult, result);    // TODO review the generated test code and remove the default call to fail.
    expResult = true;
    schemaId = "schemaId1/3";
    version = 3L;
    result = instance.existsSchemaRecordBySchemaIdAndVersion(schemaId, version);
    assertEquals(expResult, result);    // TODO review the generated test code and remove the default call to fail.
  }

  /**
   * Test of existsSchemaRecordBySchemaIdAndVersion method, of class
   * ISchemaRecordDao.
   */
  @Test
  public void testExistsSchemaRecordBySchemaIdStartWithAndVersion() {
    System.out.println("existsSchemaRecordBySchemaIdStartsWithAndVersion");
    String schemaId = "schemaId/";
    Long version = 1L;
    ISchemaRecordDao instance = schemaRecordDao;
    boolean expResult = false;
    boolean result = instance.existsSchemaRecordBySchemaIdStartsWithAndVersion(schemaId, version);
    assertEquals(expResult, result);
    schemaId = "schemaId1";
    version = 4L;
    result = instance.existsSchemaRecordBySchemaIdStartsWithAndVersion(schemaId, version);
    expResult = true;
    schemaId = "schemaId1";
    version = 3L;
    result = instance.existsSchemaRecordBySchemaIdStartsWithAndVersion(schemaId, version);
    assertEquals(expResult, result);   
  }

  /**
   * Test of findBySchemaId method, of class ISchemaRecordDao.
   */
  @Test
  public void testFindBySchemaId() {
    System.out.println("findBySchemaId");
    ISchemaRecordDao instance = schemaRecordDao;
    String schemaIdWithVersion = "schemaId";
    SchemaRecord result = instance.findBySchemaId(schemaIdWithVersion);
    assertNull(result);
    schemaIdWithVersion = null;
    result = instance.findBySchemaId(schemaIdWithVersion);
    assertNull(result);
    schemaIdWithVersion = "schemaId1/";
    result = instance.findBySchemaId(schemaIdWithVersion);
    assertNull(result);
    schemaIdWithVersion = "schemaId/1/1";
    result = instance.findBySchemaId(schemaIdWithVersion);
    assertNull(result);
    schemaIdWithVersion = "schemaId3/1";
    result = instance.findBySchemaId(schemaIdWithVersion);
    assertNotNull(result);
    System.out.println(result);

  }

  /**
   * Test of findByAlternateId method, of class ISchemaRecordDao.
   */
  @Test
  public void testFindByAlternateId() {
    System.out.println("findByAlternateId");
    ISchemaRecordDao instance = schemaRecordDao;
    String alternateId = "";
    SchemaRecord result = instance.findByAlternateId(alternateId);
    assertNull(result);
    alternateId = "schemaId4";
    result = instance.findByAlternateId(alternateId);
    assertNull(result);
    alternateId = "1234";
    result = instance.findByAlternateId(alternateId);
    assertNull(result);
    alternateId = "documentUri2";
    result = instance.findByAlternateId(alternateId);
    assertNull(result);
    alternateId = "alternate";
    result = instance.findByAlternateId(alternateId);
    assertNotNull(result);
  }

  /**
   * Test of findBySchemaIdStartsWithOrderByVersionDesc method, of class
   * ISchemaRecordDao.
   */
  @Test
  public void testFindBySchemaIdStartsWithOrderByVersionDesc() {
    System.out.println("findBySchemaIdStartsWithOrderByVersionDesc");
    ISchemaRecordDao instance = schemaRecordDao;
    String schemaId = "";
    int expResult = 6;
    List<SchemaRecord> result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
    expResult = 0;
    schemaId = "schemaId/";
    result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
    schemaId = "documentUri4";
    result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
    schemaId = "12345";
    result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
    schemaId = "alter";
    result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
    expResult = 1;
    schemaId = "schemaId3";
    result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
    expResult = 3;
    schemaId = "schemaId1";
    result = instance.findBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertEquals(expResult, result.size());
  }

  /**
   * Test of findFirstBySchemaIdStartsWithOrderByVersionDesc method, of class
   * ISchemaRecordDao.
   */
  @Test
  public void testFindFirstBySchemaIdStartsWithOrderByVersionDesc() {
    System.out.println("findFirstBySchemaIdStartsWithOrderByVersionDesc");
    ISchemaRecordDao instance = schemaRecordDao;
    String schemaId = null;
    SchemaRecord expResult = null;
    SchemaRecord result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNull(result);
    schemaId = "";
    int expectedVersion = 0;
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNotNull(result);
    schemaId = "schemaId/";
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNull(result);
    schemaId = "documentUri4";
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNull(result);
    schemaId = "12345";
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNull(result);
    schemaId = "alter";
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNull(result);
    expectedVersion = 1;
    schemaId = "schemaId3";
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNotNull(result);
    assertEquals(expectedVersion, result.getVersion().intValue());
    expectedVersion = 3;
    schemaId = "schemaId1";
    result = instance.findFirstBySchemaIdStartsWithOrderByVersionDesc(schemaId);
    assertNotNull(result);
    assertEquals(expectedVersion, result.getVersion().intValue());
  }

  private void prepareDataBase() {
    String[][] datasets = {
      {"schemaId1", "3", "documentUri13", "123", "alter"},
      {"schemaId1", "2", "documentUri12", "1234", "alterna"},
      {"schemaId1", "1", "documentUri11", "12345", "alternat"},
      {"schemaId2", "1", "documentUri2", "123456", "alternate"},
      {"schemaId3", "1", "documentUri3", "1234567", "alternateI"},
      {"schemaId4", "1", "documentUri4", "12345678", "alternateId"}};
    schemaRecordDao.deleteAll();
    for (String[] dataset : datasets) {
      saveSchemaRecord(dataset[0], dataset[1], dataset[2], dataset[3], dataset[4]);
    }
  }

  private void saveSchemaRecord(String schemaId,
          String version,
          String schemaDocumentUri,
          String documentHash,
          String alternateId) {

    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(schemaId + "/" + version);
    schemaRecord.setVersion(Long.parseLong(version));
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    schemaRecord.setSchemaDocumentUri(schemaDocumentUri);
    schemaRecord.setDocumentHash(documentHash);
    schemaRecord.setAlternateId(alternateId);
    schemaRecordDao.save(schemaRecord);
  }

}
