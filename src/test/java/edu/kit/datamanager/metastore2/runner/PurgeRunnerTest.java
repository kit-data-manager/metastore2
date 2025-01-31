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

import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.security.filter.JwtAuthenticationToken;
import edu.kit.datamanager.util.JwtBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
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
@TestPropertySource(properties = {"server.port=41438"})
@TestPropertySource(properties = {"repo.auth.enabled=false"})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=update"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:file:///tmp/metastore2/testCleanUp/database/cleanUpDatabase;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/testCleanUp/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/testCleanUp/metadata"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PurgeRunnerTest {

  private static final Logger LOG = LoggerFactory.getLogger(PurgeRunnerTest.class);

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
  private PurgeRunner purgeRunner;

  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  public PurgeRunnerTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    removeSourceDirectory();
    Path dataBaseV1Source = Path.of("./src/test/resources/testCleanUp/");
    Path dataBaseV1Target = Path.of("/tmp/metastore2/testCleanUp/");
    StandardCopyOption overwriteExisting = StandardCopyOption.REPLACE_EXISTING;
    try {
      FileUtils.copyDirectory(dataBaseV1Source.toFile(), dataBaseV1Target.toFile(), null, true, overwriteExisting);
    } catch (IOException ex) {
      LOG.error("Unknown error", ex);
    }
    Assert.assertTrue("Source directory does not exist.", dataBaseV1Target.toFile().exists());
  }

  @AfterClass
  public static void tearDownClass() {
    removeSourceDirectory();
  }

  @Before
  public void setUp() {
    // setup mockMvc
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withPort(41438))
            .build();
    eir.indices = null;
    eir.updateDate = null;
    eir.updateIndex = false;
    if (!isInitialized()) {
      System.out.println("------PurgeRunnerTest--------");
      System.out.println("------" + this.metadataConfig);
      System.out.println("------------------------------------------------------");

    }
  }

  @After
  public void tearDown() {
  }

  public static void removeSourceDirectory() {
    try {
      Path pathToBeDeleted = Path.of("/tmp/metastore2/testCleanUp");
      FileUtils.deleteDirectory(pathToBeDeleted.toFile());
      assertFalse("Directory still exists", Files.exists(pathToBeDeleted));
    } catch (IOException ex) {
      LOG.error("Unknown exception.", ex);
    }
    LOG.error("Path should be deleted!");
  }

  @Test
  public void testPurgeRunnerExecution() throws Exception {
    // Count available file 
    int noOfSchemaFilesAtStartup, noOfSchemaFiles;
    int noOfMetadataFileAtStartUp, noOfMetadataFiles;
    Path schemaDir = Path.of("/tmp/metastore2/testCleanUp/schema");
    Path metadataDir = Path.of("/tmp/metastore2/testCleanUp/metadata");
    noOfSchemaFilesAtStartup = countFilesInDirectory(schemaDir);
    noOfMetadataFileAtStartUp = countFilesInDirectory(metadataDir);

    eir.run("--purgeRepo", "--removeId", "acomplete_metadata", "-r", "atestdelete_metadata");
    noOfSchemaFiles = countFilesInDirectory(schemaDir);
    noOfMetadataFiles = countFilesInDirectory(metadataDir);
    // no of schema files should be the same
    Assert.assertEquals(noOfSchemaFilesAtStartup, noOfSchemaFiles);
    // 5 metadata documents should be deleted
    // complete_metadata -> 4 files
    // testdelete_metadata -> 1 file.
    Assert.assertEquals(noOfMetadataFileAtStartUp, noOfMetadataFiles);// + 5);

    eir.run("--purgeRepo", "--removeId", "all");
    eir.run("--purgeRepo", "--removeId", "all");
    eir.run("--removeId", "acomplete_metadata", "-r", "atestdelete_metadata");
    eir.run("--purgeRepo", "--removeId", "acomplete_metadata", "-r", "atestdelete_metadata");
    noOfSchemaFiles = countFilesInDirectory(schemaDir);
    noOfMetadataFiles = countFilesInDirectory(metadataDir);
    // 7 schema files should be deleted
    // completeexample   -> 2 files
    // minimalexample    -> 2 files
    // testdeleteexample -> 2 files
    // oneonlyexample    -> 1 file
    Assert.assertEquals(noOfSchemaFilesAtStartup, noOfSchemaFiles + 7);
    // 8 metadata documents should be deleted
    // complete_metadata   -> 4 files
    // testdelete_metadata -> 2 files
    // minimal_metadata    -> 1 file.
    // oneonly_metadata    -> 1 file.
    Assert.assertEquals(noOfMetadataFileAtStartUp, noOfMetadataFiles + 8);
  }

  private int countFilesInDirectory(Path dir) {
    int noOfFiles = 0;
    Iterator<File> fileIterator = FileUtils.iterateFiles(dir.toFile(), null, true);
    while (fileIterator.hasNext()) {
      noOfFiles++;
      File file = fileIterator.next();
      LOG.info("File '{}': '{}'", noOfFiles, file.getAbsolutePath());
    }
    return noOfFiles;
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }

}
