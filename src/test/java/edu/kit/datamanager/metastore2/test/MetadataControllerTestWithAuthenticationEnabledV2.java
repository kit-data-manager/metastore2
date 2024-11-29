/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.javers.core.Javers;
import org.junit.Assert;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
@TestPropertySource(properties = {"server.port=41428"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md_aai_v2;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/v2/md/aai/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/v2/md/aai/metadata"})
@TestPropertySource(properties = {"repo.auth.enabled=true"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerTestWithAuthenticationEnabledV2 {

  private static final String API_BASE_PATH = "/api/v2";
  private static final String ALTERNATE_API_SCHEMA_PATH = API_BASE_PATH + "/schemas";
  private static final String API_SCHEMA_PATH = ALTERNATE_API_SCHEMA_PATH + "/";
  private static final String API_METADATA_PATH = API_BASE_PATH + "/metadata/";

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/v2/md/aai/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String SCHEMA_ID = "my_dc";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final String RELATED_RESOURCE_STRING = "anyResourceId";
  private static final String RELATED_RESOURCE_STRING_2 = "anyResourceId";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING);
  private static final ResourceIdentifier RELATED_RESOURCE_2 = ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING_2);
  private final static String KIT_SCHEMA = CreateSchemaUtil.KIT_SCHEMA;

  private final static String KIT_DOCUMENT = CreateSchemaUtil.KIT_DOCUMENT;
  private final static String KIT_DOCUMENT_VERSION_2 = CreateSchemaUtil.KIT_DOCUMENT_VERSION_2;
  private final static String KIT_DOCUMENT_WRONG_NAMESPACE = CreateSchemaUtil.KIT_DOCUMENT_WRONG_NAMESPACE;
  private final static String KIT_DOCUMENT_INVALID = CreateSchemaUtil.KIT_DOCUMENT_INVALID_1;

  private String adminToken;
  private String curatorToken;
  private String userToken;
  private String otherUserToken;
  private String guestToken;

  private final String adminPrincipal = "admin";
  private final String curatorPrincipal = "curator";
  private final String userPrincipal = "user";
  private final String otherUserPrincipal = "other_user";
  private final String guestPrincipal = "guest";

  private DataResource sampleResource;
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
    System.out.println("------MetadataControllerTest--------------------------");
    System.out.println("------" + this.metadataConfig);
    System.out.println("------------------------------------------------------");

    // setup mockMvc
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withPort(41428))
            .build();
    adminToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(adminPrincipal, RepoUserRole.ADMINISTRATOR).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("groupid", "USERS").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(applicationProperties.getJwtSecret());

    curatorToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(curatorPrincipal, RepoUserRole.ADMINISTRATOR).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(applicationProperties.getJwtSecret());

    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(userPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(applicationProperties.getJwtSecret());

    otherUserToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());

    guestToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(guestPrincipal, RepoUserRole.GUEST).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());

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
    ingestSchemaRecord();
  }

  @Test
  public void testCreateRecordWithoutAuthentication() throws Exception {
    String id = "testCreateRecordWithoutAuthentication";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            // no authorization
            //header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnauthorized());
  }

  @Test
  public void testCreateRecord() throws Exception {
    String id = "testCreateRecord";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(mvcResult.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);
  }

  @Test
  public void testCreateRecordWithValidUrlSchema() throws Exception {
    String id = "testCreateRecordWithValidUrlSchema";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    // Get URL of schema
    String schemaUrl = getSchemaUrl(SCHEMA_ID);
//    record.setId("my_id");
    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    schemaIdentifier.setValue(schemaUrl);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();
  }

  @Test
  public void testCreateRecordWithUrlSchemaNull() throws Exception {
    String id = "testCreateRecordWithUrlSchemaNull";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    schemaIdentifier.setValue(null);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidUrl() throws Exception {
    String id = "testCreateRecordWithInvalidUrl";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    // Get URL of schema and remove first character
    String invalidSchemaUrl = getSchemaUrl(SCHEMA_ID).substring(1);

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    schemaIdentifier.setValue(invalidSchemaUrl);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidUrlSchema() throws Exception {
    String id = "testCreateRecordWithInvalidUrlSchema";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    // Get URL of schema and replace schema by an invalid one
     String urlWithInvalidSchema = getSchemaUrl(SCHEMA_ID).replace(SCHEMA_ID, INVALID_SCHEMA);

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    schemaIdentifier.setValue(urlWithInvalidSchema);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithoutResourceType() throws Exception {
    String id = "testCreateRecordWithoutResourceType";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    // Empty resource type
    record.setResourceType(null);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();

    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(mvcResult.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);  
    Assert.assertNotNull(result.getResourceType());  
  }

  @Test
  public void testCreateRecordWithAnyValidUrl() throws Exception {
    String id = "testCreateRecordWithAnyValidUrl";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    // Set URL of schema to a broken url
    String schemaUrl = "http://anyurl.example.org/shouldNotExist";

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    schemaIdentifier.setValue(schemaUrl);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithId() throws Exception {
    String id = "testCreateRecordWithId";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();

    this.mockMvc.perform(get(API_METADATA_PATH + id).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithIdTwice() throws Exception {
    String id = "testCreateRecordWithIdTwice";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());


    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
    DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).setValue(RELATED_RESOURCE_2.getIdentifier());
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isConflict()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithLocationUri() throws Exception {
    String id = "testCreateRecordWithLocationUri";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String content = result.getResponse().getContentAsString();

    ObjectMapper map = new ObjectMapper();
    MvcResult result2 = this.mockMvc.perform(get(locationUri).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content2 = result2.getResponse().getContentAsString();
    System.out.println(content);
    System.out.println(content2);
    Assert.assertEquals(content, content2);
  }

  @Test
  public void testCreateInvalidRecord() throws Exception {
    String id = "testCreateInvalidRecord";
    String schemaId = INVALID_SCHEMA;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateInvalidMetadataRecord() throws Exception {
    String wrongTypeJson = "{\"id\":\"dc\",\"relatedResource\":\"anyResource\",\"createdAt\":\"right now!\"}";

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", wrongTypeJson.getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
    String wrongFormatJson = "<metadata><schemaId>dc</schemaId><type>XML</type></metadata>";
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", wrongFormatJson.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();

  }

  @Test
  public void testCreateEmptyMetadataSchemaRecord() throws Exception {

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte[]) null);
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", " ".getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
  }

  // @Test 
  public void testCreateRecordFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            with(remoteAddr("any.external.domain"))).
            andDo(print()).andExpect(status().isCreated()).
            andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateRecordUpdateFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier("my_dcExt"));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            with(remoteAddr("any.domain.com"))).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            with(remoteAddr("www.google.com"))).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
  }

  @Test
  public void testCreateMetadataUnknownSchemaId() throws Exception {
    String id = "testCreateMetadataUnknownSchemaId";
    String schemaId = "unknown_schema";
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithBadSchema() throws Exception {
    String id = "testCreateRecordWithBadSchema";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadataNamespace() throws Exception {
    String id = "testCreateRecordWithInvalidMetadataNamespace";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_WRONG_NAMESPACE.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadata() throws Exception {
    String id = "testCreateRecordWithInvalidMetadata";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_INVALID.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithoutRecord() throws Exception {
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithoutSchema() throws Exception {
    String id = "testCreateRecordWithoutSchema";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord() throws Exception {
    String id = "testCreateRecordWithBadRecord";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setValue(null);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord2() throws Exception {
    String id = "testCreateRecordWithBadRecord2";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    Set<RelatedIdentifier> relatedIdentifiers = record.getRelatedIdentifiers();
    for (RelatedIdentifier item : relatedIdentifiers) {
      if (item != schemaIdentifier) {
        //remove related resource
        relatedIdentifiers.remove(item);
      }
    }
 
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSameRecord() throws Exception {
    // Two records with same schema and same related resource are now allowed.
    String id = "testCreateRecordWithBadRecord2";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();

    DataResource result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(Long.valueOf(1L).toString(), result.getVersion());

    record = SchemaRegistryControllerTestV2.createDataResource4Document(id + "_2", schemaId);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
  }

  @Test
  public void testCreateTwoVersions() throws Exception {
    String id = "testCreateTwoVersions";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();

    DataResource result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(Long.valueOf(1L).toString(), result.getVersion());

    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    Set<RelatedIdentifier> relatedIdentifiers = record.getRelatedIdentifiers();
    for (RelatedIdentifier item : relatedIdentifiers) {
      if (item != schemaIdentifier) {
        //remove related resource
        relatedIdentifiers.remove(item);
      }
    }
    RelatedIdentifier relatedResource = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, RELATED_RESOURCE_STRING_2, null, null);
    relatedResource.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    relatedIdentifiers.add(relatedResource);
    record.getAlternateIdentifiers().clear();
    record.setId(null);

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();

    result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(Long.valueOf(1L).toString(), result.getVersion());
  }

  @Test
  public void testGetRecord() throws Exception {
    String id = "testCreateTwoVersions";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    Set<AclEntry> acl = new HashSet<>();
    acl.add(new AclEntry("test1", PERMISSION.ADMINISTRATE));
    acl.add(new AclEntry("test2", PERMISSION.WRITE));
    acl.add(new AclEntry("test3", PERMISSION.READ));
    acl.add(new AclEntry("test4", PERMISSION.NONE));
    record.setAcls(acl);
    
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
    String etag1 = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();
    DataResource mr = mapper.readValue(body, DataResource.class);
    String locationUri = result.getResponse().getHeader("Location");
    String recordId = mr.getId();

    result = this.mockMvc.perform(get(locationUri).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();
    String etag2 = result.getResponse().getHeader("ETag");
    DataResource mr2 = mapper.readValue(content, DataResource.class);

    Assert.assertEquals(etag1, etag2);
    Assert.assertEquals(mr, mr2);
    result = this.mockMvc.perform(get(API_METADATA_PATH + recordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    content = result.getResponse().getContentAsString();
    etag2 = result.getResponse().getHeader("ETag");
    mr2 = mapper.readValue(content, DataResource.class);

    Assert.assertEquals(etag1, etag2);
    Assert.assertEquals(mr, mr2);
  }

  @Test
  public void testGetRecordByIdWithVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "1").
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    
    DataResource record = map.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(record);
    Assert.assertEquals(Identifier.IDENTIFIER_TYPE.URL, DataResourceRecordUtil.getSchemaIdentifier(record).getIdentifierType());
    String schemaUrl = DataResourceRecordUtil.getSchemaIdentifier(record).getValue();
    Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
    Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
    Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
    Assert.assertNotEquals("file:///tmp/dc.xml", DataResourceRecordUtil.getSchemaIdentifier(record).getValue());
  }

  @Test
  public void testGetRecordByIdWithInvalidId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    this.mockMvc.perform(get(API_METADATA_PATH + "cd").
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testGetRecordByIdWithInvalidVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "13").
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().is4xxClientError()).
            andReturn();
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("schemaId", SCHEMA_ID).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByResourceId() throws Exception {
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("resoureId", RELATED_RESOURCE.getIdentifier()).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("resourceId", RELATED_RESOURCE.getIdentifier()).
            param("from", twoHoursBefore.toString()).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidResourceId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("resourceId", "invalid").
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByInvalidUploadDate() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("resourceId", RELATED_RESOURCE.getIdentifier()).
            param("until", oneHourBefore.toString()).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("resourceId", RELATED_RESOURCE.getIdentifier()).
            param("from", twoHoursBefore.toString()).
            param("until", oneHourBefore.toString()).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownParameter() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", "cd").
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(0)));
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = KIT_DOCUMENT;

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testGetMetadataDocumentWithUnknownSchema() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    this.mockMvc.perform(get(API_METADATA_PATH + "unknown_dc").
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testUpdateRecord() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    ObjectMapper mapper = new ObjectMapper();
    // Get ContentInformation of first version
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    String locationUri2 = result.getResponse().getHeader("Location");
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation2 = mapper.readValue(body, ContentInformation.class);
    Assert.assertEquals(contentInformation1.getFilename(), contentInformation2.getFilename());
    Assert.assertNotEquals(contentInformation1, contentInformation2);
    Assert.assertNotEquals(contentInformation1.getContentUri(), contentInformation2.getContentUri());
    Assert.assertNotEquals(contentInformation1.getVersion(), contentInformation2.getVersion());
    Assert.assertEquals(contentInformation1.getVersion() + 1, (long)contentInformation2.getVersion());
    Assert.assertNotEquals(contentInformation1.getHash(), contentInformation2.getHash());
    Assert.assertNotEquals(contentInformation1.getSize(), contentInformation2.getSize());

    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = KIT_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
  }

  @Test
  public void testUpdateAclOnly() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    String metadataRecordId = createDCMetadataRecord();
    // Get ContentInformation of first version
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);
    
// Get datacite record of first version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
    // make a copy of the record due to changes in the original one.
    DataResource oldRecord = mapper.readValue(body, DataResource.class);

    // add one more user
    record.getAcls().add(new AclEntry("testacl", PERMISSION.ADMINISTRATE));
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    // update record only
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()));// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation2 = mapper.readValue(body, ContentInformation.class);
    Assert.assertEquals(contentInformation1.getFilename(), contentInformation2.getFilename());
    Assert.assertEquals(contentInformation1.getVersion(), contentInformation2.getVersion());
    Assert.assertEquals(contentInformation1, contentInformation2);
    Assert.assertEquals(contentInformation1.getContentUri(), contentInformation2.getContentUri());
    Assert.assertEquals(contentInformation1.getHash(), contentInformation2.getHash());
    Assert.assertEquals(contentInformation1.getSize(), contentInformation2.getSize());
  }

  @Test
  public void testUpdateWrongAclOnly() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    // remove old users
    record.getAcls().clear();
    // add new user with administration rights
    record.getAcls().add(new AclEntry(otherUserPrincipal, PERMISSION.ADMINISTRATE));
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isBadRequest());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserPrincipal).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isForbidden());
  }

  @Test
  public void testUpdateRecordWithWrongACL() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource oldRecord = mapper.readValue(body, DataResource.class);
    DataResource record = mapper.readValue(body, DataResource.class);
    // Set all ACL to WRITE
    for (AclEntry entry : record.getAcls()) {
      entry.setPermission(PERMISSION.WRITE);
    }
    record.getAcls().add(new AclEntry(otherUserPrincipal, PERMISSION.ADMINISTRATE));
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isForbidden());
  }

  @Test
  public void testUpdateRecordWithWrongACLButAdminRole() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource oldRecord = mapper.readValue(body, DataResource.class);
    DataResource record = mapper.readValue(body, DataResource.class);
    // Set all ACL to WRITE
    for (AclEntry entry : record.getAcls()) {
      entry.setPermission(PERMISSION.WRITE);
    }
    record.getAcls().add(new AclEntry(otherUserPrincipal, PERMISSION.ADMINISTRATE));
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk());

  }

  @Test
  public void testUpdateRecordWithEmptyACLAsUser() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource oldRecord = mapper.readValue(body, DataResource.class);
    DataResource record = mapper.readValue(body, DataResource.class);
    Set<AclEntry> currentAcl = oldRecord.getAcls();
    // Set ACL to null
    record.setAcls(null);

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isForbidden()).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithEmptyACLAsAdmin() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource oldRecord = mapper.readValue(body, DataResource.class);
    DataResource record = mapper.readValue(body, DataResource.class);
    Set<AclEntry> currentAcl = oldRecord.getAcls();
    // Set ACL to null
    record.setAcls(null);

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();
    record = mapper.readValue(body, DataResource.class);
    Assert.assertTrue(record.getAcls().containsAll(currentAcl));
    Assert.assertTrue(currentAcl.containsAll(record.getAcls()));
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    String id = "testUpdateRecordWithoutExplizitGet";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();
    String locationUri = result.getResponse().getHeader("Location");

    DataResource record2 = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile2 = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile2 = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(locationUri).
            file(recordFile2).
            file(metadataFile2).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record3 = mapper.readValue(body, DataResource.class);
    SchemaRegistryControllerTestV2.validateCreateDates(record2.getDates(), record3.getDates());
    SchemaRegistryControllerTestV2.validateRelatedIdentifierSets(record2.getRelatedIdentifiers(), record2.getRelatedIdentifiers());
    Assert.assertEquals(Long.parseLong(record2.getVersion()), Long.parseLong(record3.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record2.getAcls(), record3.getAcls());
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isPreconditionRequired()).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isPreconditionFailed()).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithoutRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String metadataRecordId = createDCMetadataRecord();
    // Get ContentInformation of first version
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
//    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String locationUri2 = result.getResponse().getHeader("Location");
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation2 = mapper.readValue(body, ContentInformation.class);
    Assert.assertEquals(contentInformation1.getFilename(), contentInformation2.getFilename());
    Assert.assertNotEquals(contentInformation1, contentInformation2);
    Assert.assertNotEquals(contentInformation1.getContentUri(), contentInformation2.getContentUri());
    Assert.assertNotEquals(contentInformation1.getVersion(), contentInformation2.getVersion());
    Assert.assertEquals(contentInformation1.getVersion() + 1, (long)contentInformation2.getVersion());
    Assert.assertNotEquals(contentInformation1.getHash(), contentInformation2.getHash());
    Assert.assertNotEquals(contentInformation1.getSize(), contentInformation2.getSize());

    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_JSON)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = CreateSchemaUtil.KIT_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
  }

  @Test
  public void testUpdateRecordWithoutDocument() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String metadataRecordId = createDCMetadataRecord();
    // Get ContentInformation of first version
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);
    
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
//    this.mockMvc.perform(put(API_METADATA_PATH + "dc").contentType("application/json").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    String locationUri2 = result.getResponse().getHeader("Location");
    body = result.getResponse().getContentAsString();

   DataResource record2 = mapper.readValue(body, DataResource.class);
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()));// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation2 = mapper.readValue(body, ContentInformation.class);
    Assert.assertEquals(contentInformation1.getFilename(), contentInformation2.getFilename());
    Assert.assertEquals(contentInformation1, contentInformation2);
    Assert.assertEquals(contentInformation1.getContentUri(), contentInformation2.getContentUri());
    Assert.assertEquals(contentInformation1.getVersion(), contentInformation2.getVersion());
    Assert.assertEquals((long)(contentInformation1.getVersion()), (long)contentInformation2.getVersion());
    Assert.assertEquals(contentInformation1.getHash(), contentInformation2.getHash());
    Assert.assertEquals(contentInformation1.getSize(), contentInformation2.getSize());

    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_JSON)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = CreateSchemaUtil.KIT_DOCUMENT;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(locationUri, locationUri2);
  }

  @Test
  public void testDeleteRecordWithoutAuthentication() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag)).
            andDo(print()).
            andExpect(status().isUnauthorized()).
            andReturn();
  }

  @Test
  public void testDeleteRecordWithoutAuthenticationButAuthorization() throws Exception {
    // anonymousUser is not allowed to delete an digital object even the
    // access rights allow this. 
    String metadataRecordId = createDCMetadataRecordWithAdminForAnonymous();

    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag)).
            andDo(print()).
            andExpect(status().isUnauthorized()).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithoutResourceType() throws Exception {
    String id = "testCreateRecordWithoutResourceType";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    // Empty resource type
    record.setResourceType(null);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();

    String locationUri = mvcResult.getResponse().getHeader("Location");
    String etag = mvcResult.getResponse().getHeader("ETag");
    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(mvcResult.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);  
    Assert.assertNotNull(result.getResourceType());  
    
    result.setResourceType(null);

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(result).getBytes());
    
    mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.multipart(locationUri).
            file(recordFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(mvcResult.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);  
    Assert.assertNotNull(result.getResourceType());   }


  @Test
  public void testDeleteRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String metadataRecordId = createDCMetadataRecord();

    // Get a list of all records
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecords = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;

    // Get ETag
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    // Delete record
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
    // Delete second time
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isPreconditionRequired()).
            andReturn();
//    Recreation should be no problem.
//    //try to create after deletion (Should return HTTP GONE)
//    DataResource record = new DataResource();
//    record.setSchemaId("dc");
//    record.setRelatedResource(RELATED_RESOURCE);
//    ObjectMapper mapper = new ObjectMapper();
//
//    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
//    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());
//
//    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + METADATA_RECORD_ID).
//            file(recordFile).
//            file(metadataFile)).andDo(print()).andExpect(status().isGone()).andReturn();

    // List of records should be smaller afterwards
    result = this.mockMvc.perform(get(API_METADATA_PATH).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecordsAfter = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;
    Assert.assertEquals("No of records should be decremented!", noOfRecords - 1, noOfRecordsAfter);
  }

  @Test
  public void testDeleteRecordWithAdminRole() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String metadataRecordId = createDCMetadataRecord();

    // Get a list of all records
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecords = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;

    // Get ETag
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    // Delete record without appropriate access rights.
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken)).
            andDo(print()).
            andExpect(status().isForbidden()).
            andReturn();
    // Delete record
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + curatorToken)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
    // Delete second time
    // Get ETag
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + curatorToken)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
//    Recreation should be no problem.
//    //try to create after deletion (Should return HTTP GONE)
//    DataResource record = new DataResource();
//    record.setSchemaId("dc");
//    record.setRelatedResource(RELATED_RESOURCE);
//    ObjectMapper mapper = new ObjectMapper();
//
//    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
//    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());
//
//    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + METADATA_RECORD_ID).
//            file(recordFile).
//            file(metadataFile)).andDo(print()).andExpect(status().isGone()).andReturn();

    // List of records should be smaller afterwards
    result = this.mockMvc.perform(get(API_METADATA_PATH).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecordsAfter = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;
    Assert.assertEquals("No of records should be decremented!", noOfRecords - 1, noOfRecordsAfter);
  }

  @Test
  public void testGetAllVersionsOfRecord() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    // Get version of record as array
    // Read all versions (only 1 version available)
    this.mockMvc.perform(get(API_METADATA_PATH).
            param("id", metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header(HttpHeaders.ACCEPT, "application/json")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();
    String locationUri2 = result.getResponse().getHeader("Location");

    DataResource record2 = mapper.readValue(body, DataResource.class);
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = KIT_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
    // Get version of record as array
    // Read all versions (only 2 versions available)
    this.mockMvc.perform(get(API_METADATA_PATH).
            param("id", metadataRecordId).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            header(HttpHeaders.ACCEPT, "application/json")).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));
  }

  private String createDCMetadataRecordWithAdminForAnonymous() throws Exception {
     String id = "createDCMetadataRecordWithAdminForAnonymous";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    record.setId(null);
    record.getAlternateIdentifiers().clear();
    
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("anonymousUser", PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();
    DataResource result = mapper.readValue(andReturn.getResponse().getContentAsString(), DataResource.class);

    return result.getId();
  }

  private String createDCMetadataRecord() throws Exception {
     String id = "createDCMetadataRecord";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    record.setId(null);
    record.getAlternateIdentifiers().clear();

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();
    DataResource result = mapper.readValue(andReturn.getResponse().getContentAsString(), DataResource.class);

    return result.getId();
  }

  private static RequestPostProcessor remoteAddr(final String remoteAddr) { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setRemoteAddr(remoteAddr);
      return request;
    };
  }

  private static RequestPostProcessor putMultipart() { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setMethod("PUT");
      return request;
    };
  }

  private void ingestSchemaRecord() throws Exception {
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4XmlSchema(SCHEMA_ID);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("anonymousUser", PERMISSION.READ));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }

  private String getSchemaUrl(String schemaId) throws Exception {
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
            param("version", "1")).
            andDo(print()).andExpect(status().isOk()).
            andReturn();
    String result = res.getRequest().getRequestURL() + "?version=1";
    System.out.println("result " + result);
    return result.replaceFirst("8080", "41428");
  }
}
