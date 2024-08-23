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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
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
import org.springframework.mock.web.MockMultipartFile;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static com.github.stefanbirkner.systemlambda.SystemLambda.*;
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
@TestPropertySource(properties = {"server.port=41417"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md_accesswithaai4json;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/elasticRunner/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/elasticRunner/metadata"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ElasticIndexerRunnerTest {

  private static Boolean alreadyInitialized = Boolean.FALSE;

  private MockMvc mockMvc;

  @Autowired
  private ElasticIndexerRunner eir;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  Javers javers = null;
  @Autowired
  private ILinkedMetadataRecordDao metadataRecordDao;
  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private IDataRecordDao dataRecordDao;
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  @Autowired
  private IContentInformationDao contentInformationDao;
  @Autowired
  private IAllIdentifiersDao allIdentifiersDao;
  @Autowired
  private IUrl2PathDao url2PathDao;
  @Autowired
  private MetastoreConfiguration metadataConfig;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/elasticRunner/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private final static String JSON_SCHEMA = "{\n"
          + "    \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
          + "    \"$id\": \"http://www.example.org/schema/json\",\n"
          + "    \"type\": \"object\",\n"
          + "    \"title\": \"Json schema for tests\",\n"
          + "    \"default\": {},\n"
          + "    \"required\": [\n"
          + "        \"title\",\n"
          + "        \"date\"\n"
          + "    ],\n"
          + "    \"properties\": {\n"
          + "        \"title\": {\n"
          + "            \"type\": \"string\",\n"
          + "            \"title\": \"Title\",\n"
          + "            \"description\": \"Title of object.\"\n"
          + "        },\n"
          + "        \"date\": {\n"
          + "            \"type\": \"string\",\n"
          + "            \"format\": \"date\",\n"
          + "            \"title\": \"Date\",\n"
          + "            \"description\": \"Date of object\"\n"
          + "        }\n"
          + "    },\n"
          + "    \"additionalProperties\": false\n"
          + "}";
  private final static String JSON_DOCUMENT = "{\n"
          + "    \"title\": \"Json schema for tests\",\n"
          + "    \"date\": \"2022-07-29\"\n"
          + "}";

  public ElasticIndexerRunnerTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
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
      System.out.println("------ElasticIndexerRunnerTest--------");
      System.out.println("------" + this.metadataConfig);
      System.out.println("------------------------------------------------------");

      contentInformationDao.deleteAll();
      dataResourceDao.deleteAll();
      metadataRecordDao.deleteAll();
      schemaRecordDao.deleteAll();
      dataRecordDao.deleteAll();
      allIdentifiersDao.deleteAll();
      url2PathDao.deleteAll();

      try {
        // Create schema only once.
        try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
          walk.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
        Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
        try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_METADATA)))) {
          walk.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
        Paths.get(TEMP_DIR_4_METADATA).toFile().mkdir();
      } catch (IOException ex) {
        ex.printStackTrace();
      }

    }
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testElasticRunnerWithWrongParameter() throws Exception {
    Integer mainVersion = Runtime.version().version().get(0);
    if (mainVersion.compareTo(17) <= 0) {
      int statusCode = catchSystemExit(() -> {
        eir.run("hallo");
      });
      Assert.assertEquals(0, statusCode);
    }
  }

  @Test
  public void testElasticRunnerWithDate() throws Exception {
    Date now = new Date();
    Date oneHourBack = new Date(now.getTime() - 3600000);
    String date = new SimpleDateFormat("yyyy-MM-dd").format(oneHourBack);
    System.out.println(oneHourBack);
    System.out.println(date);
    eir.run("-u", date);
    Assert.assertTrue(true);
  }

  @Test
  public void testElasticRunnerWithAnyIndices() throws Exception {
    eir.run("-i", "any");
    Assert.assertTrue(true);
  }

  @Test
  public void testElasticRunnerWithWrongIndices() throws Exception {
    eir.run("--reindex", "-i", "any");
    Assert.assertTrue(true);
  }

  @Test
  public void testElasticRunnerWithAllIndices() throws Exception {
    ingestSchemaRecord("elastic");
    eir.run("--reindex");
    Assert.assertTrue(true);
  }

  @Test
  public void testElasticRunnerWithAllIndicesAndMetadataDocument() throws Exception {
    String schemaLocation = ingestSchemaRecord("elastic2");
    ingestMetadataRecord("elastic2");
    ingestMetadataRecord(schemaLocation, true);
    eir.run("--reindex");
    Assert.assertTrue(true);
  }

  private String ingestSchemaRecord(String schemaId) throws Exception {
    MetadataSchemaRecord schemaRecord = new MetadataSchemaRecord();
    schemaRecord.setSchemaId(schemaId);
    schemaRecord.setSchemaVersion(1L);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));
    schemaRecord.setAcl(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_SCHEMA.getBytes());

    String schemaLocation = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn().getResponse().getHeader("Location");
    return schemaLocation;
  }

  /**
   * Ingest metadata with 'otheruser' set permissions for admin, user and guest.
   *
   * @param schemaId
   * @throws Exception
   */
  private void ingestMetadataRecord(String schemaId) throws Exception {
    ingestMetadataRecord(schemaId, false);
  }

  /**
   * Ingest metadata with 'otheruser' set permissions for admin, user and guest.
   *
   * @param schemaId
   * @param isUrl
   * @throws Exception
   */
  private void ingestMetadataRecord(String schemaId, boolean isUrl) throws Exception {
    MetadataRecord record = new MetadataRecord();
    if (isUrl) {
      record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(schemaId));
    } else {
      record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
    }
    record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier("resource of " + schemaId));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.json", "application/json", JSON_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).
            andDo(print()).andExpect(status().isCreated()).
            andReturn();
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }

}
