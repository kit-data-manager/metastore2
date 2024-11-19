/*
 * Copyright 2023 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.runner;

import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Title;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.javers.core.Javers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.restdocs.JUnitRestDocumentation;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 *
 * @author hartmann-v
 */
@RunWith(SpringJUnit4ClassRunner.class)
// Or create a test version of Application.class that stubs out services used by the CommandLineRunner
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
@TestPropertySource(properties = {"server.port=41437"})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=update"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:file:/tmp/metastore2/migrationRunner/database/migrationDatabase;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/migrationRunner/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/migrationRunner/metadata"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class Migrate2DataciteTest {

  private static Boolean alreadyInitialized = Boolean.FALSE;

  private MockMvc mockMvc;

  @Autowired
  private ElasticIndexerRunner eir;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private MetastoreConfiguration metadataConfig;
  @Autowired
  private Migration2V2Runner migrate2V2Runner;

  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  public Migrate2DataciteTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    Path dataBaseV1Source = Path.of("./src/test/resources/migrateToV2/migrationDatabase.mv.db");
    Path dataBaseV1Target = Path.of("/tmp/metastore2/migrationRunner/database/migrationDatabase.mv.db");
    StandardCopyOption overwriteExisting = StandardCopyOption.REPLACE_EXISTING;
    try {
      FileUtils.copyFile(dataBaseV1Source.toFile(), dataBaseV1Target.toFile(), overwriteExisting);
    } catch (IOException ex) {
      Logger.getLogger(Migrate2DataciteTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    Assert.assertTrue("Database file does not exist!", dataBaseV1Target.toFile().exists());
  }

  @AfterClass
  public static void tearDownClass() {
    try {
      Path pathToBeDeleted = Path.of("/tmp/metastore2/migrationRunner");
      FileUtils.deleteDirectory(pathToBeDeleted.toFile());
      assertFalse("Directory still exists", Files.exists(pathToBeDeleted));
    } catch (IOException ex) {
      Logger.getLogger(Migrate2DataciteTest.class.getName()).log(Level.SEVERE, null, ex);
    }
    Logger.getLogger(Migrate2DataciteTest.class.getName()).log(Level.SEVERE, "Path should be deleted!");
  }

  @Before
  public void setUp() {
    // setup mockMvc
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withPort(41417))
            .build();
    eir.indices = null;
    eir.updateDate = null;
    eir.updateIndex = false;
    if (!isInitialized()) {
      System.out.println("------Migrate2DataciteTest--------");
      System.out.println("------" + this.metadataConfig);
      System.out.println("------------------------------------------------------");

    }
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testElasticRunnerMigration() throws Exception {
    eir.run("--migrate2DataCite");
    Assert.assertTrue(true);
  }

  /**
   * Test for argument null.
   */
  @Test
  public void testcopyDataResource() {
    Migration2V2Runner m2r = migrate2V2Runner;
    DataResource invalidTestResource = DataResource.factoryNewDataResource("invalidId");
    DataResource result = m2r.getCopyOfDataResource(invalidTestResource);
    Assert.assertEquals(invalidTestResource, result);
  }

  /**
   * Test for argument null.
   */
  @Test(expected = NullPointerException.class)
  public void testcopyDataResourceWithNullArgument() {
    Migration2V2Runner m2r = migrate2V2Runner;
    DataResource expected = null;
    DataResource invalidTestResource = null;
    DataResource result = m2r.getCopyOfDataResource(invalidTestResource);
    Assert.assertEquals(expected, result);
  }

  /**
   * Test for argument null.
   */
  @Test(expected = NullPointerException.class)
  public void testcopyDataResourceWithIdSetToNull() {
    Migration2V2Runner m2r = migrate2V2Runner;
    DataResource expected = null;
    DataResource invalidTestResource = DataResource.factoryNewDataResource();
    invalidTestResource.setId(null);
    DataResource result = m2r.getCopyOfDataResource(invalidTestResource);
    Assert.assertEquals(expected, result);
  }

  /**
   * Test for invalid data resources.
   */
  @Test
  public void testRemoveTitleType() {
    Migration2V2Runner m2r = new Migration2V2Runner();
    Set<Title> titles = null;
    Set<Title> expected = null;
    titles = new HashSet<>();
    expected = new HashSet<>();
    expected.addAll(titles);
    m2r.removeTitleType(titles);
    Assert.assertEquals(expected, titles);
    titles = new HashSet<>();
    titles.add(Title.factoryTitle("any", Title.TYPE.ALTERNATIVE_TITLE));
    expected = new HashSet<>();
    expected.addAll(titles);
    m2r.removeTitleType(titles);
    Assert.assertEquals(expected, titles);
    titles = new HashSet<>();
    titles.add(Title.factoryTitle("any", Title.TYPE.ALTERNATIVE_TITLE));
    titles.add(Title.factoryTitle("title", Title.TYPE.OTHER));
    expected = new HashSet<>();
    expected.addAll(titles);
    m2r.removeTitleType(titles);
    Assert.assertNotEquals(expected, titles);

  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }

}
