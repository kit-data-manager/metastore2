/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import edu.kit.datamanager.entities.RepoServiceRole;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.util.JwtBuilder;
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

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

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
@TestPropertySource(properties = {"server.port=41422"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md_postaccesswithaai;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/md/aai/postaccess/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/md/aai/postaccess/metadata"})
@TestPropertySource(properties = {"repo.auth.enabled=true"})
@TestPropertySource(properties = {"metastore.postEnabledForRole=USER"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerTestPostForRole {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/md/aai/postaccess/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String SCHEMA_ID = "my_dc_access_aai";
  private static final String INVALID_SCHEMA = "invalid_dc";

  private String adminToken;
  private String userToken;
  private String otherUserToken;
  private String guestToken;
  private String serviceToken;
  private String adminTokenWithRole;
  private String userTokenWithRole;
  private String otherUserTokenWithRole;
  private String guestTokenWithRole;
  private String adminTokenWithOtherRole;
  private String userTokenWithOtherRole;
  private String otherUserTokenWithOtherRole;
  private String guestTokenWithOtherRole;

  private final String adminPrincipal = "admin";
  private final String userPrincipal = "user1";
  private final String otherUserPrincipal = "test_user";
  private final String guestPrincipal = "guest";
  private final String servicePrincipal = "any_service";

  private final String ANONYMOUS_ID = "id_for_public_available_do";

  private static Boolean alreadyInitialized = Boolean.FALSE;

  private MockMvc mockMvc;
  @Autowired
  private ApplicationProperties applicationProperties;
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

  @Before
  public void setUp() throws Exception {
    // setup mockMvc
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withPort(41422))
            .build();

    adminToken = createToken("adminWithoutRole", RepoUserRole.GUEST);
    adminTokenWithOtherRole = createToken("adminWithOtherRole", RepoUserRole.CURATOR);
    adminTokenWithRole = createToken("adminWithRole", RepoUserRole.ADMINISTRATOR, RepoUserRole.USER);

    userToken = createToken("userWithoutRole", RepoUserRole.GUEST);
    userTokenWithOtherRole = createToken("userWithOtherRole", RepoUserRole.CURATOR);
    userTokenWithRole = createToken("userWithRole", RepoUserRole.USER);

    otherUserToken = createToken("otherUserWithoutRole", RepoUserRole.GUEST);
    otherUserTokenWithOtherRole = createToken("otherUserWithOtherRole", RepoUserRole.CURATOR);
    otherUserTokenWithRole = createToken("otherUserWithRole", RepoUserRole.USER);

    guestToken = createToken("guestWithoutRole", RepoUserRole.GUEST);
    guestTokenWithOtherRole = createToken("guestWithOtherRole", RepoUserRole.CURATOR);
    guestTokenWithRole = createToken("guestWithRole", RepoUserRole.USER);



    serviceToken = edu.kit.datamanager.util.JwtBuilder.createServiceToken(servicePrincipal, RepoServiceRole.SERVICE_READ).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());

    if (!isInitialized()) {
      System.out.println("------MetadataControllerAccessTestWithAAI-------------");
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
        Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
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

  @Test
  public void testPutSchemaWithoutRole() throws Exception {
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, adminToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, userToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, otherUserToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, guestToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, serviceToken, MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  public void testPutSchemaWithOtherRole() throws Exception {
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, adminTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, userTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, otherUserTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "notallowedtopost", "doesn't matter", false, guestTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
  }

  @Test
public void testPutSchemaWithCorrectRole() throws Exception {
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "allowedtopost1", CreateSchemaUtil.KIT_SCHEMA, false, adminTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "allowedtopost2", CreateSchemaUtil.KIT_SCHEMA, false, userTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "allowedtopost3", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "allowedtopost4", CreateSchemaUtil.KIT_SCHEMA, false, guestTokenWithRole, MockMvcResultMatchers.status().isCreated());
  }

  @Test
  public void testPutMetadataWithoutRole() throws Exception {
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "testputmetadatawithoutrole", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithoutrole", 1l, "usertokenwithoutrole1", CreateSchemaUtil.KIT_DOCUMENT,false, adminToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithoutrole", 1l, "usertokenwithoutrole2", CreateSchemaUtil.KIT_DOCUMENT,false, userToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithoutrole", 1l, "usertokenwithoutrole3", CreateSchemaUtil.KIT_DOCUMENT,false, otherUserToken, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithoutrole", 1l, "usertokenwithoutrole4", CreateSchemaUtil.KIT_DOCUMENT,false, guestToken, MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  public void testPutMetadataWithOtherRole() throws Exception {
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "testputmetadatawithotherrole", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithotherrole", 1l, "usertokenothergroup1", CreateSchemaUtil.KIT_DOCUMENT,false, adminTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithotherrole", 1l, "usertokenothergroup2", CreateSchemaUtil.KIT_DOCUMENT,false, userTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithotherrole", 1l, "usertokenothergroup3", CreateSchemaUtil.KIT_DOCUMENT,false, otherUserTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithotherrole", 1l, "usertokenothergroup4", CreateSchemaUtil.KIT_DOCUMENT,false, guestTokenWithOtherRole, MockMvcResultMatchers.status().isForbidden());
  }

  @Test
  public void testPutMetadataWithCorrectRole() throws Exception {
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "testputmetadatawithcorrectrole1", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "testputmetadatawithcorrectrole2", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "testputmetadatawithcorrectrole3", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, "testputmetadatawithcorrectrole4", CreateSchemaUtil.KIT_SCHEMA, false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithcorrectrole1", 1l, "usertokencorrectgroup1", CreateSchemaUtil.KIT_DOCUMENT,false, adminTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithcorrectrole2", 1l, "usertokencorrectgroup2", CreateSchemaUtil.KIT_DOCUMENT,false, userTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithcorrectrole3", 1l, "usertokencorrectgroup3", CreateSchemaUtil.KIT_DOCUMENT,false, otherUserTokenWithRole, MockMvcResultMatchers.status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, "testputmetadatawithcorrectrole4", 1l, "usertokencorrectgroup4", CreateSchemaUtil.KIT_DOCUMENT,false, guestTokenWithRole, MockMvcResultMatchers.status().isCreated());
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }
  private String createToken(String principal, RepoUserRole... roles) {
    return JwtBuilder.createUserToken(principal, roles).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(applicationProperties.getJwtSecret());
  }
}
