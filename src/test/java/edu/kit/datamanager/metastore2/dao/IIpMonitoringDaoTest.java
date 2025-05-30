/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.IpMonitoring;
import edu.kit.datamanager.metastore2.service.MonitoringService;
import edu.kit.datamanager.metastore2.util.MonitoringUtil;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 *
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
@TestPropertySource(properties = {"server.port=41420"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_ip_monitoring_dao;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IIpMonitoringDaoTest {

  private static final String[] ips = {"ip1", "ip2", "ip3", "ip4"};
  @Autowired
  private MonitoringService monitoringService;
  @Autowired
  private IIpMonitoringDao monitoringDao;

  public IIpMonitoringDaoTest() {
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

  @Test
  public void testAccumulation() {
    System.out.println("testAccumulation");
    for (int noOfEntries = 1; noOfEntries < 20; noOfEntries++) {
      prepareDataBase(noOfEntries);
      Assert.assertEquals(Math.min(noOfEntries, ips.length), monitoringDao.count());
    }
  }

  @Test
  public void testCleanUp() {
    System.out.println("testCleanUp");
      prepareDataBase(20);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(ips.length, monitoringDao.count());
    prepareDataBase(28);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(ips.length, monitoringDao.count());
    prepareDataBase(29);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(ips.length - 1, monitoringDao.count());
    prepareDataBase(30);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(ips.length - 2, monitoringDao.count());
    prepareDataBase(31);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(ips.length - 3, monitoringDao.count());
    prepareDataBase(32);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(ips.length - 4, monitoringDao.count());
    prepareDataBase(33);
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(0, monitoringDao.count());
  }
  @Test
  public void testMonitoringUtil() {
    System.out.println("testMonitoringUtil");
    monitoringDao.deleteAll();
    // Test for null IP
    MonitoringUtil.registerIp(null);
    Assert.assertEquals(0, monitoringDao.count());
    // Test for empty IP
    MonitoringUtil.registerIp("");
    Assert.assertEquals(0, monitoringDao.count());
    // Test for empty IP
    MonitoringUtil.registerIp("  ");
    Assert.assertEquals(0, monitoringDao.count());
    // Test for valid IP
    MonitoringUtil.registerIp("ip1");
    Assert.assertEquals(1, monitoringDao.count());
    // Test for getting the number of unique users
    Assert.assertEquals(1, MonitoringUtil.getNoOfUniqueUsers());
    // Test for getting the number of unique users after cleanup
    monitoringService.cleanUpMetrics();
    Assert.assertEquals(1, MonitoringUtil.getNoOfUniqueUsers());
  }

  private void prepareDataBase(int noOfEntries) {
    IpMonitoring ipMonitoring = new IpMonitoring();
    monitoringDao.deleteAll();
    for (int i = 0; i < noOfEntries; i++) {
      ipMonitoring.setIpHash(ips[i % ips.length]);
      ipMonitoring.setLastVisit(nowMinusDays(i));
      monitoringDao.save(ipMonitoring);
    }
  }

  private Instant nowMinusDays(int days) {
    return Instant.now().minus(days, ChronoUnit.DAYS);
  }
}
