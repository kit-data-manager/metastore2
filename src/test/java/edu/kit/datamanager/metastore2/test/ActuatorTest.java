/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import edu.kit.datamanager.configuration.SearchConfiguration;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.javers.core.Javers;
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
import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
@TestPropertySource(properties = {"server.port=41416"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/actuator/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/actuator/metadata"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@TestPropertySource(properties = {"spring.jpa.defer-datasource-initialization=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ActuatorTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/actuator/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";

  private static Boolean alreadyInitialized = Boolean.FALSE;

  private MockMvc mockMvc;
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
  private MetastoreConfiguration metadataConfig;
  @Autowired
  private MetastoreConfiguration schemaConfig;
  @Autowired
  private SearchConfiguration elasticConfig;
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
                      .withPort(41416))
              .build();
      // Create schema only once.
      try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
        walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      }
      Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
      Paths.get(TEMP_DIR_4_METADATA).toFile().mkdir();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  @Test
  public void testActuator() throws Exception {
    // /actuator/info
    this.mockMvc.perform(get("/actuator/info")).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadataRepo.['Total space']", Matchers.is(notNullValue())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schemaRepo.['Total space']", Matchers.is(notNullValue())))
            .andReturn();
    // /actuator/health
    this.mockMvc.perform(get("/actuator/health")).andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.MetadataRepo.status", is("UP")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.SchemaRepo.status", is("UP")))
            .andReturn();
    ///////////////////////////////////////////////////////////////////////////
    // Remove path of metadata repo
    ///////////////////////////////////////////////////////////////////////////
    Path metadataPath = Path.of(TEMP_DIR_4_METADATA);
    metadataPath.toFile().delete();

    // /actuator/info
    this.mockMvc.perform(get("/actuator/info")).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasKey("schemaRepo")))
            .andExpect(MockMvcResultMatchers.jsonPath("$", IsNot.not(Matchers.hasKey("metadataRepo"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.schemaRepo.['Total space']", Matchers.is(notNullValue())))
            .andReturn();

    // /actuator/health
    this.mockMvc.perform(get("/actuator/health")).andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.MetadataRepo.status", is("DOWN")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.SchemaRepo.status", is("UP")))
            .andReturn();
    ///////////////////////////////////////////////////////////////////////////
    // Remove path of schema repo
    ///////////////////////////////////////////////////////////////////////////
    Path schemaPath = Path.of(TEMP_DIR_4_SCHEMAS);
    schemaPath.toFile().delete();

    // /actuator/info
    this.mockMvc.perform(get("/actuator/info")).andDo(print()).andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", IsNot.not(Matchers.hasKey("schemaRepo"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$", IsNot.not(Matchers.hasKey("metadataRepo"))))
            .andReturn();

    // /actuator/health
    this.mockMvc.perform(get("/actuator/health")).andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.MetadataRepo.status", is("DOWN")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.SchemaRepo.status", is("DOWN")))
            .andReturn();
    elasticConfig.setSearchEnabled(true);
    elasticConfig.setUrl(URI.create("http://localhost:41400/").toURL());
    this.mockMvc.perform(get("/actuator/health")).andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.Elasticsearch.status", is("DOWN")))
            .andReturn();
    elasticConfig.setUrl(URI.create("http://localhost:41416/api/v1/schemas/").toURL());
    this.mockMvc.perform(get("/actuator/health")).andDo(print())
            .andExpect(MockMvcResultMatchers.jsonPath("$.components.Elasticsearch.status", is("DOWN")))
            .andReturn();
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }
}
