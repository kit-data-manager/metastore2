/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.DataRecord;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

/**
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
@TestPropertySource(properties = {"server.port=41411"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IDataRecordDaoTest {

  @Autowired
  private IDataRecordDao dataRecordDao;
  private IDataRecordDao instance;

  private static final Instant MIN = LocalDateTime.parse("2021-03-01T00:00", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
          .atZone(ZoneId.of("UTC"))
          .toInstant();
  private static final Instant MAX = Instant.now().plus(1, ChronoUnit.DAYS);

  public IDataRecordDaoTest() {
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
    instance = dataRecordDao;
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of findByMetadataId method, of class IDataRecordDao.
   */
  @Test
  public void testFindByMetadataIdAndVersion() {
    System.out.println("findByMetadataIdAndVersion");
    String metadataId = "metadataId1";
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    Optional<DataRecord> result = instance.findByMetadataIdAndVersion(metadataId, 3l);
    assertNotNull(result);
    assertTrue(result.isPresent());

    result = instance.findByMetadataIdAndVersion(metadataId, 1l);
    assertNotNull(result);
    assertFalse(result.isPresent());
    
    result = instance.findByMetadataIdAndVersion("unknownId", 1l);
    assertNotNull(result);
    assertFalse(result.isPresent());
  }

  /**
   * Test of findByMetadataId method, of class IDataRecordDao.
   */
  @Test
  public void testFindByMetadataId() {
    System.out.println("findByMetadataId");
    String metadataId = "metadataId1";
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    Optional<DataRecord> result = instance.findTopByMetadataIdOrderByVersionDesc(metadataId);
    assertNotNull(result);
    assertTrue(result.isPresent());

    result = instance.findTopByMetadataIdOrderByVersionDesc("unknownId");
    assertNotNull(result);
    assertFalse(result.isPresent());
  }

  /**
   * Test of findBySchemaId method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaId() {
    System.out.println("findBySchemaId");
    String schemaId = "schemaId";
    List<DataRecord> result = instance.findBySchemaId(schemaId);
    assertEquals(6, result.size());
    result = instance.findBySchemaId("invalidId");
    assertEquals(0, result.size());
  }

  /**
   * Test of findBySchemaIdAndLastUpdateAfter method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaIdAndLastUpdateAfter() {
    System.out.println("findBySchemaIdAndLastUpdateAfter");
    String schemaId = "schemaId";
    String fromAsString = "2021-03-15T15:00";
    String untilAsString = "2021-03-15T17:00";
    Instant from = LocalDateTime.parse(fromAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    Instant until = LocalDateTime.parse(untilAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateAfter(schemaId, from);
    assertEquals(3, result.size());
  }

  /**
   * Test of findBySchemaIdAndLastUpdateBefore method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaIdAndLastUpdateBefore() {
    System.out.println("findBySchemaIdAndLastUpdateBefore");
    String schemaId = "schemaId";
    String fromAsString = "2021-03-15T15:00";
    String untilAsString = "2021-03-15T17:00";
    Instant from = LocalDateTime.parse(fromAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    Instant until = LocalDateTime.parse(untilAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateBefore(schemaId, until);
    assertEquals(4, result.size());
  }

  /**
   * Test of findBySchemaIdAndLastUpdateBetween method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaIdAndLastUpdateBetween() {
    System.out.println("findBySchemaIdAndLastUpdateBetween");
    String schemaId = "schemaId";
    String fromAsString = "2021-03-15T15:00";
    String untilAsString = "2021-03-15T17:00";
    Pageable page1 = PageRequest.of(0, 20);
    Pageable page2 = PageRequest.of(1, 3);
    Instant from = LocalDateTime.parse(fromAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    Instant until = LocalDateTime.parse(untilAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    List<DataRecord> expResult = null;
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, until, page1);
    assertEquals(3, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, MAX, page1);
    assertEquals(4, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, MIN, until, page1);
    assertEquals(5, result.size());
    page1 = PageRequest.of(0, 3);
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, until, page1);
    assertEquals(3, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, MAX, page1);
    assertEquals(3, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, MAX, page2);
    assertEquals(1, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, MIN, until, page1);
    assertEquals(3, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, MIN, until, page2);
    assertEquals(2, result.size());
  }

  @Test
  public void testFindBySchemaIdAndLastUpdateBetweenWithWrongSchemaId() {
    System.out.println("findBySchemaIdAndLastUpdateBetweenWithWrongSchemaId");
    String schemaId = "wrongSchemaId";
    String fromAsString = "2021-03-15T15:00";
    String untilAsString = "2021-03-15T17:00";
    Pageable page1 = PageRequest.of(0, 20);
    Pageable page2 = PageRequest.of(1, 3);
    Instant from = LocalDateTime.parse(fromAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    Instant until = LocalDateTime.parse(untilAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    List<DataRecord> expResult = null;
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, until, page1);
    assertEquals(0, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, MAX, page1);
    assertEquals(0, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, MIN, until, page1);
    assertEquals(0, result.size());
    page1 = PageRequest.of(0, 3);
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, until, page1);
    assertEquals(0, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, MAX, page1);
    assertEquals(0, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, MAX, page2);
    assertEquals(0, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, MIN, until, page1);
    assertEquals(0, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, MIN, until, page2);
    assertEquals(0, result.size());
  }

  private void prepareDataBase() {
    String[][] datasets = {
      {"metadataId1", "3", "documentUri", "123", "schemaId", "2021-03-15T13:00"},
      {"metadataId2", "1", "documentUri", "123", "schemaId", "2021-03-15T14:00"},
      {"metadataId3", "1", "documentUri", "123", "schemaId", "2021-03-15T15:00"},
      {"metadataId4", "1", "documentUri", "123", "schemaId", "2021-03-15T16:00"},
      {"metadataId5", "1", "documentUri", "123", "schemaId", "2021-03-15T17:00"},
      {"metadataId6", "1", "documentUri", "123", "schemaId", "2021-03-15T18:00"},};
    dataRecordDao.deleteAll();
    for (String[] dataset : datasets) {
      saveDataRecord(dataset[0], dataset[1], dataset[2], dataset[3], dataset[4], dataset[5]);
    }
  }

  private void saveDataRecord(String metadataId,
          String version,
          String metadataDocumentUri,
          String documentHash,
          String schemaId,
          String instantAsString) {
    Instant instant = LocalDateTime.parse(instantAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();

    DataRecord dataRecord = new DataRecord();
    dataRecord.setDocumentHash(documentHash);
    dataRecord.setMetadataId(metadataId);
    dataRecord.setVersion(Long.parseLong(version));
    dataRecord.setSchemaId(schemaId);
    dataRecord.setLastUpdate(instant);
    dataRecord.setMetadataDocumentUri(metadataDocumentUri);
    dataRecordDao.save(dataRecord);
  }
}
