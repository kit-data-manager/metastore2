/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.junit.Before;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author Torridity
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
@TestPropertySource(properties = {"server.port=41418"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_frontend;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@TestPropertySource(properties = {"spring.jpa.defer-datasource-initialization=true"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/frontend/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/frontend/metadata"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FrontendControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/frontend/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String SCHEMA_ID = "my_dc";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final String RELATED_RESOURCE_STRING = "anyResourceId";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING);

  private final static String DC_DOCUMENT = CreateSchemaUtil.KIT_DOCUMENT;

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
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
  private MetastoreConfiguration metadataConfig;
  @Autowired
  private MetastoreConfiguration schemaConfig;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  @Before
  public void setUp() throws Exception {
    System.out.println("------MetadataControllerTest--------------------------");
    System.out.println("------" + this.metadataConfig);
    System.out.println("------------------------------------------------------");

    contentInformationDao.deleteAll();
    dataResourceDao.deleteAll();
    metadataRecordDao.deleteAll();
    schemaRecordDao.deleteAll();
    dataRecordDao.deleteAll();
    allIdentifiersDao.deleteAll();

    try {
      // setup mockMvc
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
              .apply(springSecurity())
              .apply(documentationConfiguration(this.restDocumentation).uris()
                      .withPort(41418))
              .build();
      // Create schema only once.
      try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
        walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      }
      Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
      Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
      CreateSchemaUtil.ingestKitSchemaRecord(mockMvc, SCHEMA_ID, schemaConfig.getJwtSecret());
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

  @Test
  public void testGetMetadataRecords4Tabulator1() throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/ui/metadata")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", IsNot.not(Matchers.hasKey("data")))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
    //String metadataRecordId = 
    createDCMetadataRecord();

    res = this.mockMvc.perform(get("/api/v1/ui/metadata?page=1")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
  }

  @Test
  public void testGetMetadataRecords4Tabulator2() throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/ui/metadata").
            accept("application/tabulator+json")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", IsNot.not(Matchers.hasKey("data")))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
    //String metadataRecordId = 
    createDCMetadataRecord();

    res = this.mockMvc.perform(get("/api/v1/ui/metadata?page=1").
            accept("application/tabulator+json")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
  }

  @Test
  public void testGetSchemaRecords4Tabulator1() throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/ui/schemas")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
  }

  @Test
  public void testGetSchemaRecords4Tabulator2() throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/ui/schemas").
            accept("application/tabulator+json")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
  }

  @Test
  public void testGetSchemaRecords4Tabulator3() throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/ui/schemas?page=1")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
  }

  @Test
  public void testGetSchemaRecords4Tabulator4() throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/ui/schemas?page=1").
            accept("application/tabulator+json")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$.data", Matchers.hasSize(1))).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("last_page"))).
            andReturn();
  }

  private String createDCMetadataRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    MetadataRecord result = mapper.readValue(andReturn.getResponse().getContentAsString(), MetadataRecord.class);

    return result.getId();
  }
}
