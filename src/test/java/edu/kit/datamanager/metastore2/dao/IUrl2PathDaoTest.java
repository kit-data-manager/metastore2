/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.Url2Path;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class IUrl2PathDaoTest {

  @Autowired
  private IUrl2PathDao dataRecordDao;
  private IUrl2PathDao instance;

  private static final Instant MIN = LocalDateTime.parse("2021-03-01T00:00", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm"))
          .atZone(ZoneId.of("UTC"))
          .toInstant();
  private static final Instant MAX = Instant.now().plus(1, ChronoUnit.DAYS);

  public IUrl2PathDaoTest() {
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
   * Test of findByMetadataId method, of class IUrl2PathDao.
   */
  @Test
  public void testFindByUrl() {
    System.out.println("testFindByUrl");
    String metadataId = "metadataId";
//    IUrl2PathDao instance = new IUrl2PathDaoImpl();
    Optional<Url2Path> result;
    for (int i = 1; i < 7; i++) {
      result = instance.findByUrl(metadataId + i);
      assertNotNull(result);
      assertTrue(result.isPresent());
//    assertEquals(2, result.get().getNameCount());
      assertTrue(result.get().getUrl().endsWith(Integer.toString(i)));
      assertTrue(result.get().getPath().endsWith(Integer.toString(i)));
    }
    result = instance.findByUrl("unknownUrl");
    assertNotNull(result);
    assertFalse(result.isPresent());
  }

  /**
   * Test of findByMetadataId method, of class IUrl2PathDao.
   */
  @Test
  public void testFindByPath() {
    System.out.println("testFindByPath");
    String basePath = "documentUri";
//    IUrl2PathDao instance = new IUrl2PathDaoImpl();
    List<Url2Path> result;
    Path path;
    // one entry available from 1 to 5
    for (int i = 1; i < 5; i++) {
      path = Paths.get(basePath, Integer.toString(i));
      result = instance.findByPath(path.toString());
      assertNotNull(result);
      assertFalse(result.isEmpty());
      assertEquals(1, result.size());

//    assertEquals(2, result.get().getNameCount());
      assertTrue(result.get(0).getUrl().endsWith(Integer.toString(i)));
      assertTrue(result.get(0).getPath().endsWith(Integer.toString(i)));
    }
    // two entries for 5
    int i = 5;
    path = Paths.get(basePath, Integer.toString(i));
    result = instance.findByPath(path.toString());
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());

//    assertEquals(2, result.get().getNameCount());
    assertTrue(result.get(0).getPath().endsWith(Integer.toString(i)));
    assertTrue(result.get(1).getPath().endsWith(Integer.toString(i)));
    // Unknown path
    result = instance.findByPath("unknownPath");
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /**
   * Test of findByMetadataId method, of class IUrl2PathDao.
   */
  @Test
  public void testDelete() {
    System.out.println("testFindByUrl");
    String metadataId = "metadataId";
    Optional<Url2Path> result;
    long noOfDataSets = dataRecordDao.count();
    assertEquals(7l, noOfDataSets);
    for (int i = 1; i < 7; i++) {
      result = instance.findByUrl(metadataId + i);
      assertNotNull(result);
      assertTrue(result.isPresent());
      assertTrue(result.get().getUrl().endsWith(Integer.toString(i)));
      assertTrue(result.get().getPath().endsWith(Integer.toString(i)));
      instance.delete(result.get());
      // Test after deletion
      result = instance.findByUrl(metadataId + i);
      assertNotNull(result);
      assertFalse(result.isPresent());
      // count should be decreasedWob
      assertEquals(noOfDataSets - i, instance.count());
    }
  }

  private void prepareDataBase() {
    String[][] datasets = {
      {"metadataId1", "documentUri", "1"},
      {"metadataId2", "documentUri", "2"},
      {"metadataId3", "documentUri", "3"},
      {"metadataId4", "documentUri", "4"},
      {"metadataId5", "documentUri", "5"},
      {"metadataId6", "documentUri", "6"},
      {"metadataId7", "documentUri", "5"},};
    dataRecordDao.deleteAll();
    assertEquals(0, dataRecordDao.count());
    for (String[] dataset : datasets) {
      saveUrl2Path(dataset[0], Paths.get(dataset[1], dataset[2]));
    }
    assertEquals(datasets.length, dataRecordDao.count());
  }

  private void saveUrl2Path(String metadataId,
          Path metadataDocumentUri) {

    Url2Path dataRecord = new Url2Path();
    dataRecord.setUrl(metadataId);
    dataRecord.setPath(metadataDocumentUri.toString());
    dataRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    dataRecordDao.save(dataRecord);
  }
}
