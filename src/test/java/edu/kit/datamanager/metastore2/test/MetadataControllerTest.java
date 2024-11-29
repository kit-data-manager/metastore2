/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier.IdentifierType;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static edu.kit.datamanager.metastore2.test.CreateSchemaUtil.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
@TestPropertySource(properties = {"server.port=41401"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@TestPropertySource(properties = {"spring.jpa.defer-datasource-initialization=true"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries=http://localhost:41401/api/v3/,http://localhost:41401/api/v1/"})
@TestPropertySource(properties = {"repo.search.url=http://localhost:41401"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String SCHEMA_ID = "my_dc";
  private static final String JSON_SCHEMA_ID = "my_json";
  private static final String JSON_HTTP_SCHEMA_ID = "my_json_with_http";
  private static final String JSON_HTTP_SCHEMA_ID_WITH_HASH = "my_json_with_hash";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final String UNKNOWN_RELATED_RESOURCE = "unknownHResourceId";
  private static final String RELATED_RESOURCE_STRING = "anyResourceId";
  private static final String APACHE_2_LICENSE = "https://spdx.org/licenses/Apache-2.0";
  private static final String MIT_LICENSE = "https://spdx.org/licenses/MIT";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING);
  private static final ResourceIdentifier RELATED_RESOURCE_URL = ResourceIdentifier.factoryUrlResourceIdentifier(RELATED_RESOURCE_STRING);
  private static final ResourceIdentifier RELATED_RESOURCE_2 = ResourceIdentifier.factoryInternalResourceIdentifier("anyOtherResourceId");
  private final static String DC_SCHEMA = CreateSchemaUtil.KIT_SCHEMA;

  private final static String DC_DOCUMENT = CreateSchemaUtil.KIT_DOCUMENT;
  private final static String DC_DOCUMENT_SMALL_CHANGE = CreateSchemaUtil.KIT_DOCUMENT_SMALL_CHANGE;
  private final static String DC_DOCUMENT_VERSION_2 = CreateSchemaUtil.KIT_DOCUMENT_VERSION_2;
  private final static String DC_DOCUMENT_WRONG_NAMESPACE = CreateSchemaUtil.KIT_DOCUMENT_WRONG_NAMESPACE;
  private final static String DC_DOCUMENT_INVALID = CreateSchemaUtil.KIT_DOCUMENT_INVALID_1;

  private static final String JSON_SCHEMA = "{\n"
          + "  \"type\": \"object\",\n"
          + "  \"title\": \"Json schema for tests\",\n"
          + "  \"default\": {},\n"
          + "  \"required\": [\n"
          + "      \"title\"\n"
          + " ],\n"
          + "  \"properties\": {\n"
          + "    \"title\": {\n"
          + "      \"type\": \"string\",\n"
          + "      \"title\": \"Title\",\n"
          + "      \"description\": \"Title of object.\"\n"
          + "    }\n"
          + "  },\n"
          + "  \"additionalProperties\": false\n"
          + "}\n";

  private final static String JSON_HTTP_SCHEMA = "{\n"
          + "    \"$schema\": \"http://json-schema.org/draft/2019-09/schema\",\n"
          + "    \"$id\": \"http://www.example.org/schema/json\",\n"
          + "    \"type\": \"object\",\n"
          + "    \"title\": \"Json schema for tests\",\n"
          + "    \"default\": {},\n"
          + "    \"required\": [\n"
          + "        \"title\"\n"
          + "    ],\n"
          + "    \"properties\": {\n"
          + "        \"title\": {\n"
          + "            \"type\": \"string\",\n"
          + "            \"title\": \"Title\",\n"
          + "            \"description\": \"Title of object.\"\n"
          + "        }\n"
          + "    },\n"
          + "    \"additionalProperties\": false\n"
          + "}";

  private final static String JSON_HTTP_SCHEMA_WITH_HASH = "{\n"
          + "    \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n"
          + "    \"$id\": \"http://www.example.org/schema/json\",\n"
          + "    \"type\": \"object\",\n"
          + "    \"title\": \"Json schema for tests\",\n"
          + "    \"default\": {},\n"
          + "    \"required\": [\n"
          + "        \"title\"\n"
          + "    ],\n"
          + "    \"properties\": {\n"
          + "        \"title\": {\n"
          + "            \"type\": \"string\",\n"
          + "            \"title\": \"Title\",\n"
          + "            \"description\": \"Title of object.\"\n"
          + "        }\n"
          + "    },\n"
          + "    \"additionalProperties\": false\n"
          + "}";
  private static final String JSON_DOCUMENT_VERSION_1 = "{\n"
          + "  \"title\": \"My first JSON document\" \n"
          + "}\n";

  private static final String JSON_DOCUMENT_VERSION_2 = "{\n"
          + "  \"title\": \"My updated JSON document\" \n"
          + "}\n";

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
                      .withPort(41401))
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
  public void testCreateRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithHttpSchema() throws Exception {
    ingestHttpJsonSchemaRecord();
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(JSON_HTTP_SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.json", "application/json", JSON_DOCUMENT_VERSION_1.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithHttpSchemaAndHash() throws Exception {
    ingestHttpJsonSchemaRecordWithHash();
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(JSON_HTTP_SCHEMA_ID_WITH_HASH));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.json", "application/json", JSON_DOCUMENT_VERSION_1.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordAlternateEndpoint() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithRelatedResourceOfTypeUrl() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE_URL);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();

    MetadataRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals("Type of related resource should be unchanged!", record.getRelatedResource().getIdentifierType(), result.getRelatedResource().getIdentifierType());
  }

  @Test
  public void testCreateRecordWithValidUrlSchema() throws Exception {
    MetadataRecord record = new MetadataRecord();
    // Get URL of schema
    String schemaUrl = getSchemaUrl(SCHEMA_ID);
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(schemaUrl));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithUrlSchemaNull() throws Exception {
    MetadataRecord record = new MetadataRecord();

    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(null));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidUrl() throws Exception {
    MetadataRecord record = new MetadataRecord();
    // Get URL of schema and remove first character
    String invalidSchemaUrl = getSchemaUrl(SCHEMA_ID).substring(1);
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(invalidSchemaUrl));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidUrlSchema() throws Exception {
    MetadataRecord record = new MetadataRecord();
    // Get URL of schema
    String urlWithInvalidSchema = getSchemaUrl(SCHEMA_ID).replace(SCHEMA_ID, INVALID_SCHEMA);
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(urlWithInvalidSchema));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithAnyValidUrl() throws Exception {
    MetadataRecord record = new MetadataRecord();
    // Get URL of schema
    String schemaUrl = "http://anyurl.example.org/shouldNotExist";
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(schemaUrl));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isUnprocessableEntity()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithId() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    record.setId("SomeValidId");
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidId() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    record.setId("http://localhost:8080/d1");
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithIdTwice() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    record.setId("AnyValidId");
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    record.setRelatedResource(RELATED_RESOURCE_2);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
  }

  @Test
  public void testCreateRecordWithLocationUri() throws Exception {
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

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    // Due to redirect from API v1 to API v2.
    locationUri = locationUri.replace("/v2/", "/v1/");

    String content = result.getResponse().getContentAsString();

    ObjectMapper map = new ObjectMapper();
    MvcResult result2 = this.mockMvc.perform(get(locationUri).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content2 = result2.getResponse().getContentAsString();

    Assert.assertEquals(content, content2);
  }

  @Test
  public void testCreateInvalidRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(INVALID_SCHEMA));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateInvalidMetadataRecord() throws Exception {
    String wrongTypeJson = "{\"id\":\"dc\",\"relatedResource\":\"anyResource\",\"createdAt\":\"right now!\"}";

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", wrongTypeJson.getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    String wrongFormatJson = "<metadata><schemaId>dc</schemaId><type>XML</type></metadata>";
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", wrongFormatJson.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

  }

  @Test
  public void testCreateEmptyMetadataSchemaRecord() throws Exception {

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte[]) null);
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", " ".getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  // @Test 
  // Test is not active as remote address seems not to work
  public void testCreateRecordFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile).with(remoteAddr("any.external.domain"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateRecordUpdateFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier("my_dcExt"));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile).with(remoteAddr("any.domain.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile).with(remoteAddr("www.google.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateMetadataUnknownSchemaId() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier("unknown_dc"));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadMetadata() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithEmptyAclSid() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(null, PERMISSION.READ));
    record.setAcl(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    Assert.assertTrue(res.getResponse().getContentAsString().contains("Subject ID of ACL entry must not be null."));
  }

  @Test
  public void testCreateRecordWithInvalidMetadataNamespace() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_WRONG_NAMESPACE.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadata() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_INVALID.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutRecord() throws Exception {
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutSchema() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataRecord record = new MetadataRecord();
    //schemaId is missing
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(null));
    record.setRelatedResource(RELATED_RESOURCE);

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord2() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    //related resource is missing

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutDocument() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSameRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(Long.valueOf(1L), result.getRecordVersion());

    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isConflict()).andReturn();

    Assert.assertTrue(res.getResponse().getContentAsString().contains("Conflict"));
    Assert.assertTrue(res.getResponse().getContentAsString().contains(SCHEMA_ID));
    Assert.assertTrue(res.getResponse().getContentAsString().contains(RELATED_RESOURCE_STRING));
  }

  @Test
  public void testCreateTwoVersions() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(Long.valueOf(1L), result.getRecordVersion());

    record.setRelatedResource(RELATED_RESOURCE_2);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(Long.valueOf(1L), result.getRecordVersion());
  }

  @Test
  public void testGetRecordById() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(IdentifierType.URL, result.getSchema().getIdentifierType());
    String schemaUrl = result.getSchema().getIdentifier();
    Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
    Assert.assertTrue(schemaUrl.contains("/api/v2/schemas/"));
    Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/dc.xml", result.getMetadataDocumentUri());
  }

  @Test
  public void testGetRecordByIdWithVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "1").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(IdentifierType.URL, result.getSchema().getIdentifierType());
    String schemaUrl = result.getSchema().getIdentifier();
    Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
    Assert.assertTrue(schemaUrl.contains("/api/v2/schemas/"));
    Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
    Assert.assertNotEquals("file:///tmp/dc.xml", result.getMetadataDocumentUri());
  }

  @Test
  public void testGetRecordByIdWithInvalidId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/cd").
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid id!", result.getResponse().getContentAsString().contains("Metadata document with ID 'cd' doesn't exist!"));
  }

  @Test
  public void testGetRecordByIdWithInvalidVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            param("version", "13").
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid version!", result.getResponse().getContentAsString().contains("Version '13' of ID '" + metadataRecordId + "' doesn't exist!"));
  }

  @Test
  public void testFindRecordsBySchemaIdWithAlternateEndpoint() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata").param("schemaId", SCHEMA_ID)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", SCHEMA_ID)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidSchemaId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", "anyinvalidschemaid")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsOfMultipleVersionsBySchemaId() throws Exception {
    String schemaId = "multipleSchemas".toLowerCase(Locale.getDefault());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, XML_SCHEMA_V1, metadataConfig.getJwtSecret(), false, status().isCreated());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, XML_SCHEMA_V2, metadataConfig.getJwtSecret(), true, status().isOk());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, XML_SCHEMA_V3, metadataConfig.getJwtSecret(), true, status().isOk());
    ObjectMapper map = new ObjectMapper();
    String[] multipleVersions = {"1", "2", "3"};
    // Ingest 1st version of document.
    CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, schemaId, 1L, multipleVersions[0], XML_DOCUMENT_V1, metadataConfig.getJwtSecret());
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(1, result.length);
    // Ingest 2nd version of document
    CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, schemaId, 2L, multipleVersions[1], XML_DOCUMENT_V2, metadataConfig.getJwtSecret());
    res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(2, result.length);
    // Ingest 3rd version of document
    CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, schemaId, 3L, multipleVersions[2], XML_DOCUMENT_V3, metadataConfig.getJwtSecret());
    res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(3, result.length);
  }

  @Test
  public void testFindRecordsByResourceId() throws Exception {
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resoureId", RELATED_RESOURCE.getIdentifier())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
    res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", RELATED_RESOURCE.getIdentifier()).param("from", twoHoursBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsBySchemaIdANDResourceId() throws Exception {
    String relatedResource = "testFindRecordsBySchemaIdANDResourceId";
    String relatedResource2 = "anotherTestFindRecordsBySchemaIdANDResourceId";
    String secondSchemaId = "schema_for_find_records_by_schema_and_resource_id";
    CreateSchemaUtil.ingestKitSchemaRecord(mockMvc, secondSchemaId, schemaConfig.getJwtSecret());
    String metadataRecordId = createDCMetadataRecordWithRelatedResource(relatedResource, SCHEMA_ID);
    String metadataRecordId2 = createDCMetadataRecordWithRelatedResource(relatedResource2, SCHEMA_ID);
    String metadataRecordIdv2 = createDCMetadataRecordWithRelatedResource(relatedResource, secondSchemaId);
    String metadataRecordId2v2 = createDCMetadataRecordWithRelatedResource(relatedResource2, secondSchemaId);
    // Looking for first schema
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    // Looking for second schema
    Assert.assertEquals(2, result.length);
    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", secondSchemaId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(2, result.length);
    // Looking for first AND second schema
    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID).
            param("schemaId", secondSchemaId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(4, result.length);
    // Looking for first, second AND invalid schema
    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID).
            param("schemaId", secondSchemaId).
            param("schemaId", "invalidschemaid")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(4, result.length);
    // Looking for first, second AND invalid schema AND resource1 and resource2
    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID).
            param("schemaId", secondSchemaId).
            param("schemaId", "invalidschemaid").
            param("resourceId", relatedResource).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(4, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID).
            param("resourceId", relatedResource).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(2, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID).
            param("resourceId", relatedResource)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(1, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", SCHEMA_ID).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(1, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", secondSchemaId).
            param("resourceId", relatedResource).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(2, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", secondSchemaId).
            param("resourceId", relatedResource)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(1, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", secondSchemaId).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);
    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidResourceId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", UNKNOWN_RELATED_RESOURCE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByInvalidUploadDate() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", RELATED_RESOURCE.getIdentifier()).param("until", oneHourBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", RELATED_RESOURCE.getIdentifier()).param("from", twoHoursBefore.toString()).param("until", oneHourBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownParameter() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").
            param("schemaId", "cd")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT;

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testGetMetadataDocumentWithUnknownSchema() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    this.mockMvc.perform(get("/api/v1/metadata/unknown_dc")).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testUpdateRecord() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1L);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check for new metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());

    // Get old version...
    result = this.mockMvc.perform(get("/api/v1/metadata/" + record.getId() + "?version=1").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record1 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record.getDocumentHash(), record1.getDocumentHash());
    Assert.assertNotEquals(record2.getDocumentHash(), record1.getDocumentHash());
    Assert.assertEquals(record.getCreatedAt(), record1.getCreatedAt());
    Assert.assertEquals(record.getSchema().getIdentifier(), record1.getSchema().getIdentifier());
    Assert.assertEquals(record.getRecordVersion(), record1.getRecordVersion());// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record1.getAcl()));
    }
    Assert.assertEquals(record.getLastUpdate(), record1.getLastUpdate());
    // Check for new metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId + "?version=1")).andDo(print()).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    String dcMetadata1 = DC_DOCUMENT;

    Assert.assertEquals(dcMetadata1, content);

    Assert.assertEquals(record.getMetadataDocumentUri(), record1.getMetadataDocumentUri());

  }

  @Test
  public void testUpdateRecordWithWrongVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    record.setRecordVersion(0L);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals(Long.valueOf(2L), record2.getRecordVersion());// version should be 2
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check for new metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    // Check for old metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId + "?version=1")).andDo(print()).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    dcMetadata = DC_DOCUMENT;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
  }

  @Test
  public void testUpdateRecordIgnoreACL() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord oldRecord = mapper.readValue(body, MetadataRecord.class);
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    // Set all ACL to WRITE
    for (AclEntry entry : record.getAcl()) {
      entry.setPermission(PERMISSION.WRITE);
    }
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1L);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(SchemaRegistryControllerTest.isSameSetOfAclEntries(record.getAcl(), record2.getAcl()));
      Assert.assertFalse(SchemaRegistryControllerTest.isSameSetOfAclEntries(oldRecord.getAcl(), record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check for new metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> acl = new HashSet<>();
    acl.add(new AclEntry("test", PERMISSION.READ));
    acl.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(acl);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();
    String locationUri = result.getResponse().getHeader("Location");
    // Due to redirect from API v1 to API v2.
    locationUri = locationUri.replace("/v2/", "/v1/");

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile2 = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile2 = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(locationUri).
            file(recordFile2).
            file(metadataFile2).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record3 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record2.getDocumentHash(), record3.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record2.getCreatedAt(), record3.getCreatedAt());
    Assert.assertEquals(record2.getMetadataDocumentUri().replace("version=1", "version=2"), record3.getMetadataDocumentUri());
    Assert.assertEquals(record2.getSchema().getIdentifier(), record3.getSchema().getIdentifier());
    Assert.assertEquals((long) record2.getRecordVersion(), record3.getRecordVersion() - 1L);// version should be 1 higher
    if (record2.getAcl() != null) {
      Assert.assertTrue(record2.getAcl().containsAll(record3.getAcl()));
    }
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithSameDocument() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    MvcResult result = CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, SCHEMA_ID, 1L, "document", DC_DOCUMENT, schemaConfig.getJwtSecret());
    String body = result.getResponse().getContentAsString();

    MetadataRecord record1 = mapper.readValue(body, MetadataRecord.class);
    // Update without any changes.
    result = CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, SCHEMA_ID, 1L, "document", DC_DOCUMENT, schemaConfig.getJwtSecret(), true, status().isOk());
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals("Version shouldn't change!", record1.getRecordVersion(), record2.getRecordVersion());
  }

  @Test
  public void testUpdateRecordWithSmallChangesInDocument() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    MvcResult result = CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, SCHEMA_ID, 1L, "document", DC_DOCUMENT, schemaConfig.getJwtSecret());
    String body = result.getResponse().getContentAsString();

    MetadataRecord record1 = mapper.readValue(body, MetadataRecord.class);
    // Update without any changes.
    result = CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, SCHEMA_ID, 1L, "document", DC_DOCUMENT_SMALL_CHANGE, schemaConfig.getJwtSecret(), true, status().isOk());
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals("Version should change!", record1.getRecordVersion(), record2.getRecordVersion());
    Assert.assertEquals("Version should incremented!", (long) record1.getRecordVersion(), record2.getRecordVersion() - 1L);
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(recordFile).
            file(metadataFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutRecord() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1L);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutRecord4Json() throws Exception {
    // Update only Json document
    ingestJsonSchemaRecord();
    String metadataRecordId = createJsonMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", JSON_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1L);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "1")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String jsonMetadata = JSON_DOCUMENT_VERSION_1;

    Assert.assertEquals(jsonMetadata, content);
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    jsonMetadata = JSON_DOCUMENT_VERSION_2;

    Assert.assertEquals(jsonMetadata, content);
  }

  @Test
  public void testUpdateRecordWithoutDocument() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri(), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals(record.getRecordVersion(), record2.getRecordVersion());// version should be  the same
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutDocumentChangingRelatedResource() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    String expectedRelatedResource;
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    expectedRelatedResource = record2.getRelatedResource().getIdentifier() + "_NEW";
    record2.getRelatedResource().setIdentifier(expectedRelatedResource);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();
    etag = result.getResponse().getHeader("ETag");

    record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri(), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals(record.getRecordVersion(), record2.getRecordVersion());// version should be  the same
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertNotEquals("Related resource should be updated!", record.getRelatedResource().getIdentifier(), record2.getRelatedResource().getIdentifier());
    Assert.assertEquals("Related resource should be updated!", expectedRelatedResource, record2.getRelatedResource().getIdentifier());

    // Test also updating related type only...
    IdentifierType expectedIdentifierType = IdentifierType.ISBN;
    record2.getRelatedResource().setIdentifierType(expectedIdentifierType);

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataRecordId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record3 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record.getDocumentHash(), record3.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record3.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri(), record3.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchema().getIdentifier(), record3.getSchema().getIdentifier());
    Assert.assertEquals(record.getRecordVersion(), record3.getRecordVersion());// version should be  the same
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record3.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record3.getLastUpdate()));
    Assert.assertEquals("Related resource should be the same!", record2.getRelatedResource().getIdentifier(), record3.getRelatedResource().getIdentifier());
    Assert.assertEquals("Related resource type should be changed!", expectedIdentifierType, record3.getRelatedResource().getIdentifierType());

  }

  @Test
  public void testUpdateRecordWithInvalidSetting4Json() throws Exception {
    String alternativeSchemaId = "testupdate";
    CreateSchemaUtil.ingestXmlSchemaRecord(mockMvc, alternativeSchemaId, CreateSchemaUtil.XML_SCHEMA_V1, schemaConfig.getJwtSecret());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, alternativeSchemaId, CreateSchemaUtil.XML_SCHEMA_V2, schemaConfig.getJwtSecret(), true, status().isOk());
    CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, alternativeSchemaId, 2L, "document", CreateSchemaUtil.XML_DOCUMENT_V2, schemaConfig.getJwtSecret());
    // Change only version of schema to a version which is not valid.
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, alternativeSchemaId, 1L, "document", null, schemaConfig.getJwtSecret(), true, status().isUnprocessableEntity());
    // Change to a nonexistent version of schema.
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, alternativeSchemaId, Long.MAX_VALUE, "document", null, schemaConfig.getJwtSecret(), true, status().isNotFound());
    // Change to another schema
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocument(mockMvc, SCHEMA_ID, 1L, "document", null, schemaConfig.getJwtSecret(), true, status().isUnprocessableEntity());
  }


  @Test
  public void testUpdateRecordAndDocument2NewSchemaVersion() throws Exception {
    String alternativeSchemaId = "testupdatealsoschema";
    CreateSchemaUtil.ingestXmlSchemaRecord(mockMvc, alternativeSchemaId, CreateSchemaUtil.XML_SCHEMA_V1, schemaConfig.getJwtSecret());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecord(mockMvc, alternativeSchemaId, CreateSchemaUtil.XML_SCHEMA_V2, schemaConfig.getJwtSecret(), true, status().isOk());
    CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, alternativeSchemaId, 1L, "document", CreateSchemaUtil.XML_DOCUMENT_V1, schemaConfig.getJwtSecret());
    // Change version of schema to a higher version whith additional fields.
    CreateSchemaUtil.ingestXmlMetadataDocument(mockMvc, alternativeSchemaId, 2L, "document", CreateSchemaUtil.XML_DOCUMENT_V2, schemaConfig.getJwtSecret());
  }

  @Test
  public void testUpdateRecordWithLicense() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    record.setLicenseUri(APACHE_2_LICENSE);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1L);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertNotNull(record2.getLicenseUri());
    Assert.assertEquals(APACHE_2_LICENSE, record2.getLicenseUri());
    // Check for new metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    record = mapper.readValue(body, MetadataRecord.class);
    record.setLicenseUri(MIT_LICENSE);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record3 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record2.getDocumentHash(), record3.getDocumentHash());
    Assert.assertEquals(record2.getCreatedAt(), record3.getCreatedAt());
    Assert.assertEquals(record2.getSchema().getIdentifier(), record3.getSchema().getIdentifier());
    Assert.assertEquals((long) record2.getRecordVersion(), (long) record3.getRecordVersion());// version should be the same
    if (record.getAcl() != null) {
      Assert.assertTrue(record2.getAcl().containsAll(record3.getAcl()));
    }
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
    Assert.assertNotNull(record3.getLicenseUri());
    Assert.assertEquals(MIT_LICENSE, record3.getLicenseUri());
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    record = mapper.readValue(body, MetadataRecord.class);
    record.setLicenseUri(null);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record4 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record2.getDocumentHash(), record4.getDocumentHash());
    Assert.assertEquals(record2.getCreatedAt(), record4.getCreatedAt());
    Assert.assertEquals(record2.getSchema().getIdentifier(), record4.getSchema().getIdentifier());
    Assert.assertEquals((long) record2.getRecordVersion(), (long) record4.getRecordVersion());// version should be the same
    if (record.getAcl() != null) {
      Assert.assertTrue(record2.getAcl().containsAll(record4.getAcl()));
    }
    Assert.assertTrue(record3.getLastUpdate().isBefore(record4.getLastUpdate()));
    Assert.assertNull(record4.getLicenseUri());

  }


  @Test
  public void testUpdateRecordWithoutCreateDate() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    Instant createDate = record.getCreatedAt();
    record.setCreatedAt(null);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    Assert.assertEquals(createDate, record2.getCreatedAt());
    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1L);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check for new metadata document.
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
  }

  @Test
  public void testDeleteRecordWithoutAuthentication() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/metadata/" + metadataRecordId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    //delete second time
    this.mockMvc.perform(delete("/api/v1/metadata/" + metadataRecordId)).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testDeleteRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String metadataRecordId = createDCMetadataRecord();

    // Get a list of all records
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/").
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecords = mapper.readValue(result.getResponse().getContentAsString(), MetadataRecord[].class).length;

    // Get Etag
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    // Delete record
    this.mockMvc.perform(delete("/api/v1/metadata/" + metadataRecordId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();

    // Delete second time
    this.mockMvc.perform(delete("/api/v1/metadata/" + metadataRecordId)).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
//    Recreation should be no problem.
//    //try to create after deletion (Should return HTTP GONE)
//    MetadataRecord record = new MetadataRecord();
//    record.setSchemaId("dc");
//    record.setRelatedResource(RELATED_RESOURCE);
//    ObjectMapper mapper = new ObjectMapper();
//
//    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
//    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
//
//    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + METADATA_RECORD_ID).
//            file(recordFile).
//            file(metadataFile)).andDo(print()).andExpect(status().isGone()).andReturn();

    // List of records should be smaller afterwards
    result = this.mockMvc.perform(get("/api/v1/metadata/").
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecordsAfter = mapper.readValue(result.getResponse().getContentAsString(), MetadataRecord[].class).length;
    Assert.assertEquals("No of records should be decremented!", noOfRecords - 1, noOfRecordsAfter);
  }

  @Test
  public void testGetAllVersionsOfRecord() throws Exception {
    String id = null;
    for (long version = 1; version <= 3; version++) {
      id = ingestMetadataRecordWithVersion(id, version);
      // Get version of record as array
      // Read all versions 
      this.mockMvc.perform(get("/api/v1/metadata/").param("id", id).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize((int) version)));

      MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + id).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      String body = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
      Assert.assertEquals("Expect current version '" + version + "'", (Long) version, record.getRecordVersion());// version should be 1 higher
      // Check for new metadata document.
      result = this.mockMvc.perform(get("/api/v1/metadata/" + id)).andDo(print()).andExpect(status().isOk()).andReturn();
      String content = result.getResponse().getContentAsString();

      String dcMetadata = DC_DOCUMENT;

      if (version == 1) {

        Assert.assertTrue(content.startsWith(dcMetadata));

//    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
        // Get version of record as array
        // Read all versions (2 version2 available)
        result = this.mockMvc.perform(get("/api/v1/metadata/").param("id", id).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize((int) version))).andReturn();
        mapper = new ObjectMapper();
        CollectionType mapCollectionType = mapper.getTypeFactory()
                .constructCollectionType(List.class, MetadataRecord.class);
        List<MetadataRecord> resultList = mapper.readValue(result.getResponse().getContentAsString(), mapCollectionType);
        HashSet<Long> versions = new HashSet<>();
        for (MetadataRecord item : resultList) {
          versions.add(item.getRecordVersion());
        }
        Assert.assertEquals(version, versions.size());
        for (long index = 1; index <= version; index++) {
          Assert.assertTrue("Test for version: " + index, versions.contains(index));
        }
      }
    }
  }

  @Test
  public void testIssue52() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    int version = 1;

    // Test get record with one version
    // Read all versions 
    MvcResult result = this.mockMvc
            .perform(get("/api/v1/metadata/")
                    .param("id", metadataRecordId)
                    .header(HttpHeaders.ACCEPT, "application/json"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(version)))
            .andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    metadataRecordId = ingestNewMetadataRecord(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get("/api/v1/metadata/").param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    metadataRecordId = ingestNewMetadataRecord(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get("/api/v1/metadata/").param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    metadataRecordId = ingestMetadataRecordWithVersion(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get("/api/v1/metadata/").param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));
    // check for higher versions which should be not available (if version > 2)
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isOk());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    metadataRecordId = ingestNewMetadataRecord(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get("/api/v1/metadata/").param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));

    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "1")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT;
//    Assert.assertEquals(dcMetadata, content);

    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    Assert.assertNotEquals(dcMetadata, content);
    Assert.assertEquals("Length must differ!", dcMetadata.length() + 3, content.length());
    // check for higher versions which should be not available (if version > 2)
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testSearchProxy() throws Exception {

    // Skip test due to Spring Security 6
    this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/metadata/search?page=0&size=20")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testSearchWithSchemaProxy() throws Exception {

    // Test for swagger definition
    this.mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/metadata/index/search?page=0&size=20")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testSwaggerUI() throws Exception {

    // Test for swagger definition
    this.mockMvc.perform(get("/v3/api-docs"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.info.title", Matchers.startsWith("MetaStore")));
  }

  @Test
  public void testLandingPage4MetadataUnknownID() throws Exception {
    String documentId = createDCMetadataRecord();

    MvcResult andReturn = this.mockMvc.perform(get("/api/v1/metadata/anything")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=anything&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testLandingPage4MetadataWithSchemaId() throws Exception {
    MvcResult andReturn = this.mockMvc.perform(get("/api/v1/metadata/" + SCHEMA_ID)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=" + SCHEMA_ID + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isBadRequest());
  }

  @Test
  public void testLandingPage4MetadataWrongVersion() throws Exception {
    String documentId = createDCMetadataRecord();

    MvcResult andReturn = this.mockMvc.perform(get("/api/v1/metadata/" + documentId)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=" + documentId + "&version=2"))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testLandingPage4Metadata() throws Exception {
    String documentId = createDCMetadataRecord();

    MvcResult andReturn = this.mockMvc.perform(get("/api/v1/metadata/" + documentId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=" + documentId + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get("/api/v1/metadata/" + documentId)
            .queryParam("version", "1")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=" + documentId + "&version=1"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    // Ingest a second version...
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + documentId).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    andReturn = this.mockMvc.perform(get("/api/v1/metadata/" + documentId)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=" + documentId + "&version=2"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get("/api/v1/metadata/" + documentId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page?id=" + documentId + "&version="))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
  }

  @Test
  public void testLandingPage4Schema() throws Exception {

    MvcResult andReturn = this.mockMvc.perform(get("/api/v1/schemas/" + SCHEMA_ID)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + SCHEMA_ID + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get("/api/v1/schemas/" + SCHEMA_ID)
            .queryParam("version", "1")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + SCHEMA_ID + "&version=1"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get("/api/v1/schemas/" + SCHEMA_ID)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + SCHEMA_ID + "&version=2"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isNotFound());
   }

  @Test
  public void testDeleteSchemaWithLinkedDocument() throws Exception {
    String schemaId = "deleteschema";
    String metadataRecordId = "deletedocument";
    String jwtSecret = schemaConfig.getJwtSecret();
    ingestKitSchemaRecord(this.mockMvc, schemaId, jwtSecret);
    ingestXmlMetadataDocument(this.mockMvc, schemaId, null, metadataRecordId, DC_DOCUMENT, jwtSecret);
    // Deletion of schema shouldn't work
    // Get ETag.
    MvcResult result = mockMvc.perform(get("/api/v1/schemas/" + schemaId).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etagSchema = result.getResponse().getHeader("ETag");
    this.mockMvc.perform(delete("/api/v1/schemas/" + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isConflict()).
            andReturn();
    // Get Etag
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    // Delete record
    result = this.mockMvc.perform(delete("/api/v1/metadata/" + metadataRecordId).
            header("If-Match", etag)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();

    // Try deleting schema once more should also fail
    this.mockMvc.perform(delete("/api/v1/schemas/" + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isConflict()).
            andReturn();

    // Delete second time
    result = this.mockMvc.perform(get("/api/v1/metadata/" + metadataRecordId).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    result = this.mockMvc.perform(delete("/api/v1/metadata/" + metadataRecordId).
            header("If-Match", etag)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();

    // Now it should be possible to delete schema
    result = this.mockMvc.perform(delete("/api/v1/schemas/" + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
    // But it's still available
    result = this.mockMvc.perform(get("/api/v1/schemas/" + schemaId).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etagSchema = result.getResponse().getHeader("ETag");
    // Remove it ones more
    this.mockMvc.perform(delete("/api/v1/schemas/" + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
    // Now it' gone
    result = this.mockMvc.perform(get("/api/v1/schemas/" + schemaId).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  private String createJsonMetadataRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(JSON_SCHEMA_ID));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", JSON_DOCUMENT_VERSION_1.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    MetadataRecord result = mapper.readValue(andReturn.getResponse().getContentAsString(), MetadataRecord.class);

    return result.getId();
  }

  private String createDCMetadataRecord() throws Exception {
    return createDCMetadataRecordWithRelatedResource(RELATED_RESOURCE_STRING, SCHEMA_ID);
  }

  private String createDCMetadataRecordWithRelatedResource(String myRelatedResource, String schemaId) throws Exception {
    ResourceIdentifier relatedResource = ResourceIdentifier.factoryInternalResourceIdentifier(myRelatedResource);
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
    record.setRelatedResource(relatedResource);
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

  /**
   * Ingest new metadata document and create new version (if not already exists)
   *
   * @param id id of the DO to update
   * @param version version of the new document.
   * @return id of the DO
   * @throws Exception
   */
  private String ingestMetadataRecordWithVersion(String id, long version) throws Exception {
    if (id == null) {
      id = createDCMetadataRecord();
    } else {
      // add new version
      MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + id).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      String body = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
      String newDocument = DC_DOCUMENT;
      for (int i = 0; i < version; i++) {
        newDocument = newDocument.concat(" ");
      }
      MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", newDocument.getBytes());

      result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
              file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    }
    return id;
  }

  /**
   * Ingest new metadata document (if no id is given) and/or create new version
   * of record
   *
   * @param id id of the DO to update
   * @param version version of the new document.
   * @return id of the DO
   * @throws Exception
   */
  private String ingestNewMetadataRecord(String id, long version) throws Exception {
    if (id == null) {
      id = createDCMetadataRecord();
    } else {
      // add new version
      MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + id).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      String body = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
      record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING + version));

      MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());

      this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
              file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    }
    return id;
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

  private void ingestJsonSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(JSON_SCHEMA_ID);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestHttpJsonSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(JSON_HTTP_SCHEMA_ID);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_HTTP_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestHttpJsonSchemaRecordWithHash() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(JSON_HTTP_SCHEMA_ID_WITH_HASH);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_HTTP_SCHEMA_WITH_HASH.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private String getSchemaUrl(String schemaId) throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/" + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    return result.getSchemaDocumentUri().replaceFirst("8080", "41401");
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }
}
