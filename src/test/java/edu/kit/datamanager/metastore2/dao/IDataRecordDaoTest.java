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
import java.util.List;
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
@TestPropertySource(properties = {"server.port=41402"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IDataRecordDaoTest {

  @Autowired
  private IDataRecordDao dataRecordDao;
  private IDataRecordDao instance;

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
  public void testFindByMetadataId() {
    System.out.println("findByMetadataId");
    String metadataId = "";
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    DataRecord expResult = null;
    DataRecord result = instance.findByMetadataId(metadataId);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findBySchemaId method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaId() {
    System.out.println("findBySchemaId");
    String schemaId = "";
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    List<DataRecord> expResult = null;
    List<DataRecord> result = instance.findBySchemaId(schemaId);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findBySchemaIdAndLastUpdateAfter method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaIdAndLastUpdateAfter() {
    System.out.println("findBySchemaIdAndLastUpdateAfter");
    String schemaId = "";
    Instant from = null;
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    List<DataRecord> expResult = null;
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateAfter(schemaId, from);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of findBySchemaIdAndLastUpdateBefore method, of class IDataRecordDao.
   */
  @Test
  public void testFindBySchemaIdAndLastUpdateBefore() {
    System.out.println("findBySchemaIdAndLastUpdateBefore");
    String schemaId = "";
    Instant until = null;
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    List<DataRecord> expResult = null;
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateBefore(schemaId, until);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
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
    Pageable pgbl = PageRequest.of(0, 20);
    Instant from = LocalDateTime.parse(fromAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    Instant until = LocalDateTime.parse(untilAsString, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
//    IDataRecordDao instance = new IDataRecordDaoImpl();
    List<DataRecord> expResult = null;
    List<DataRecord> result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, until, pgbl);
    assertEquals(3, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, from, Instant.MAX, pgbl);
    assertEquals(4, result.size());
    result = instance.findBySchemaIdAndLastUpdateBetween(schemaId, Instant.MIN, until, pgbl);
    assertEquals(5, result.size());
  }

  private void prepareDataBase() {
    String[][] datasets = {
      {"metadataId", "documentUri", "123", "schemaId", "2021-03-15T13:00"},
      {"metadataId", "documentUri", "123", "schemaId", "2021-03-15T14:00"},
      {"metadataId", "documentUri", "123", "schemaId", "2021-03-15T15:00"},
      {"metadataId", "documentUri", "123", "schemaId", "2021-03-15T16:00"},
      {"metadataId", "documentUri", "123", "schemaId", "2021-03-15T17:00"},
      {"metadataId", "documentUri", "123", "schemaId", "2021-03-15T18:00"},};
    for (String[] dataset : datasets) {
      saveDataRecord(dataset[0], dataset[1], dataset[2], dataset[3], dataset[4]);
    }
  }

  private void saveDataRecord(String metadataId,
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
    dataRecord.setSchemaId(schemaId);
    dataRecord.setLastUpdate(instant);
    dataRecord.setMetadataDocumentUri(metadataDocumentUri);
    dataRecordDao.save(dataRecord);
  }
}
