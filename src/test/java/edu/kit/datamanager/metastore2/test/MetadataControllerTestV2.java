/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.Identifier.IDENTIFIER_TYPE;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.*;
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
import edu.kit.datamanager.repo.domain.Date;
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
@TestPropertySource(properties = {"server.port=41421"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md_v2;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
@TestPropertySource(properties = {"spring.jpa.defer-datasource-initialization=true"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/v2/md/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/v2/md/metadata"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@TestPropertySource(properties = {"metastore.metadata.landingpage=/metadata-landing-page-v2?id=$(id)&version=$(version)"})
@TestPropertySource(properties = {"metastore.schema.landingpage=/schema-landing-page-v2?schemaId=$(id)&version=$(version)"})
@TestPropertySource(properties = {"repo.search.url=http://localhost:41421"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerTestV2 {

  private static final String API_BASE_PATH = "/api/v2";
  private static final String ALTERNATE_API_SCHEMA_PATH = API_BASE_PATH + "/schemas";
  private static final String API_SCHEMA_PATH = ALTERNATE_API_SCHEMA_PATH + "/";
  private static final String API_METADATA_PATH = API_BASE_PATH + "/metadata/";

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/v2/md/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String SCHEMA_ID = "my_dc";
  private static final String JSON_SCHEMA_ID = "my_json";
  private static final String JSON_HTTP_SCHEMA_ID = "my_json_with_http";
  private static final String JSON_HTTP_SCHEMA_ID_URL = "http://localhost:41421/api/v2/schemas/my_json_with_http?version=1";
  private static final String JSON_HTTP_SCHEMA_ID_WITH_HASH = "my_json_with_hash";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final String UNKNOWN_RELATED_RESOURCE = "unknownHResourceId";
  private static final String RELATED_RESOURCE_STRING = "anyResourceId";
  private static final String APACHE_2_LICENSE = "https://spdx.org/licenses/Apache-2.0";
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
                      .withPort(41421))
              .build();
      // Create schema only once.
      try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
        walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      }
      Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
      Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
      CreateSchemaUtil.ingestKitSchemaRecordV2(mockMvc, SCHEMA_ID, schemaConfig.getJwtSecret());
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
    String id = "testCreateRecord";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);

    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithHttpSchema() throws Exception {
    ingestHttpJsonSchemaRecord();
    String id = "testCreateRecordWithHttpSchema";
    String schemaId = JSON_HTTP_SCHEMA_ID_URL;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4JsonDocument(id, schemaId);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.json", "application/json", JSON_DOCUMENT_VERSION_1.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithHttpSchemaAndHash() throws Exception {
    ingestHttpJsonSchemaRecordWithHash();
    String id = "testCreateRecordWithHttpSchemaAndHash";
    String schemaId = JSON_HTTP_SCHEMA_ID_WITH_HASH;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4JsonDocument(id, schemaId);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.json", "application/json", JSON_DOCUMENT_VERSION_1.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordAlternateEndpoint() throws Exception {
    String id = "testCreateRecordAlternateEndpoint";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithRelatedResourceOfTypeUrl() throws Exception {
    String id = "testCreateRecordWithRelatedResourceOfTypeUrl";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4XmlDocument(id, schemaId);
    RelatedIdentifier relatedResourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    relatedResourceIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn();

    DataResource result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    RelatedIdentifier relatedIdentifier1 = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    RelatedIdentifier relatedIdentifier2 = DataResourceRecordUtil.getRelatedIdentifier(result, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    Assert.assertEquals("Value of related resource should be unchanged!",
            relatedIdentifier1.getValue(),
            relatedIdentifier2.getValue());
    Assert.assertEquals("Type of related resource should be unchanged!",
            relatedIdentifier1.getIdentifierType(),
            relatedIdentifier2.getIdentifierType());
  }

  @Test
  public void testCreateRecordWithValidUrlSchema() throws Exception {
    String id = "testCreateRecordWithValidUrlSchema";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithUrlSchemaNull() throws Exception {
    String id = "testCreateRecordWithUrlSchemaNull";
    String schemaId = null;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    for (RelatedIdentifier item : record.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(DataResourceRecordUtil.RELATED_SCHEMA_TYPE)) {
        item.setValue(null);
        item.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      }
    }
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidUrl() throws Exception {
    String id = "testCreateRecordWithInvalidUrl";
    String invalidSchemaUrl = getSchemaUrl(SCHEMA_ID).substring(1);
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    for (RelatedIdentifier item : record.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(DataResourceRecordUtil.RELATED_SCHEMA_TYPE)) {
        item.setValue(invalidSchemaUrl);
        item.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      }
    }
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidUrlSchema() throws Exception {
    String id = "testCreateRecordWithInvalidUrlSchema";
    String schemaId = INVALID_SCHEMA;
    String urlWithInvalidSchema = getSchemaUrl(SCHEMA_ID).replace(SCHEMA_ID, INVALID_SCHEMA);
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    for (RelatedIdentifier item : record.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(DataResourceRecordUtil.RELATED_SCHEMA_TYPE)) {
        item.setValue(urlWithInvalidSchema);
        item.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      }
    }
//    DataResource record = new DataResource();
//    // Get URL of schema
//    String urlWithInvalidSchema = getSchemaUrl(SCHEMA_ID).replace(SCHEMA_ID, INVALID_SCHEMA);
////    record.setId("my_id");
//    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(urlWithInvalidSchema));
//    record.setRelatedResource(RELATED_RESOURCE);
//    Set<AclEntry> aclEntries = new HashSet<>();
////    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
////    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
////    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithAnyValidUrl() throws Exception {
    String id = "testCreateRecordWithAnyValidUrl";
    String schemaId = INVALID_SCHEMA;
    String schemaUrl = "http://anyurl.example.org/shouldNotExist";
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    for (RelatedIdentifier item : record.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(DataResourceRecordUtil.RELATED_SCHEMA_TYPE)) {
        item.setValue(schemaUrl);
        item.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      }
    }
//    DataResource record = new DataResource();
//    // Get URL of schema
//    String schemaUrl = "http://anyurl.example.org/shouldNotExist";
////    record.setId("my_id");
//    record.setSchema(ResourceIdentifier.factoryUrlResourceIdentifier(schemaUrl));
//    record.setRelatedResource(RELATED_RESOURCE);
//    Set<AclEntry> aclEntries = new HashSet<>();
////    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
////    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
////    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    String response = result.getResponse().getContentAsString();
    Assert.assertTrue(response, response.contains("URL"));
  }

  @Test
  public void testCreateRecordWithId() throws Exception {
    String id = "SomeValidId";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidId() throws Exception {
    String id = "http://localhost:8080/d1";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithIdTwice() throws Exception {
    String id = "AnyValidId";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    for (RelatedIdentifier item : record.getRelatedIdentifiers()) {
      if (item.getRelationType().equals(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE)) {
        item.setValue(RELATED_RESOURCE_2.getIdentifier());
      }
    }
//    record.setRelatedResource(RELATED_RESOURCE_2CE_2);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
  }

  @Test
  public void testCreateRecordWithLocationUri() throws Exception {
    String id = "testCreateRecordWithLocationUri";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    DataResource data1 = mapper.readValue(result.getResponse().getContentAsString(), DataResource.class);

    ObjectMapper map = new ObjectMapper();
    MvcResult result2 = this.mockMvc.perform(get(locationUri).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    DataResource data2 = mapper.readValue(result2.getResponse().getContentAsString(), DataResource.class);
    SchemaRegistryControllerTestV2.validateDataResources(data1, data2);
  }

  @Test
  public void testCreateInvalidRecord() throws Exception {
    String id = "testCreateInvalidRecord";
    String schemaId = INVALID_SCHEMA;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    MetadataRecord record = new MetadataRecord();
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(INVALID_SCHEMA));
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateInvalidMetadataRecord() throws Exception {
    String wrongTypeJson = "{\"id\":\"dc\",\"relatedResource\":\"anyResource\",\"createdAt\":\"right now!\"}";

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", wrongTypeJson.getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    String wrongFormatJson = "<metadata><schemaId>dc</schemaId><type>XML</type></metadata>";
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", wrongFormatJson.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

  }

  @Test
  public void testCreateEmptyMetadataSchemaRecord() throws Exception {

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", (byte[]) null);
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", " ".getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  // @Test 
  // Test is not active as remote address seems not to work
  public void testCreateRecordFromExternal() throws Exception {
    String id = "testCreateRecordFromExternal";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    DataResource record = new DataResource();
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).with(remoteAddr("any.external.domain"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateRecordUpdateFromExternal() throws Exception {
    String id = "testCreateRecordUpdateFromExternal";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    DataResource record = new DataResource();
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier("my_dcExt"));
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).with(remoteAddr("any.domain.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile).with(remoteAddr("www.google.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateMetadataUnknownSchemaId() throws Exception {
    String id = "testCreateMetadataUnknownSchemaId";
    String schemaId = "unknown_dc";
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    DataResource record = new DataResource();
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier("unknown_dc"));
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadMetadata() throws Exception {
    String id = "testCreateRecordWithBadMetadata";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    DataResource record = new DataResource();
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
//    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithEmptyAclSid() throws Exception {
    String id = "testCreateRecordWithEmptyAclSid";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(null, PERMISSION.READ));
    record.setAcls(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    Assert.assertTrue(res.getResponse().getContentAsString().contains("Subject ID of ACL entry must not be null."));
  }

  @Test
  public void testCreateRecordWithInvalidMetadataNamespace() throws Exception {
    String id = "testCreateRecordWithInvalidMetadataNamespace";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_WRONG_NAMESPACE.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadata() throws Exception {
    String id = "testCreateRecordWithInvalidMetadata";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_INVALID.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidorEmptyResource() throws Exception {
    String id = "testCreateRecordWithInvalidorEmptyResource";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();
    // empty resource type
    record.setResourceType(null);

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    
    // wrong resource type
    id = "testCreateRecordWithWrongType";
    record.setId(id);
    record.getAlternateIdentifiers().clear();
    record.setResourceType(ResourceType.createResourceType(DataResourceRecordUtil.XML_METADATA_TYPE));

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    
    // wrong resource value
    id = "testCreateRecordWithWrongValue";
    record.setId(id);
    record.getAlternateIdentifiers().clear();
    record.setResourceType(ResourceType.createResourceType(DataResourceRecordUtil.XML_METADATA_TYPE + "invalid", ResourceType.TYPE_GENERAL.MODEL));
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutRecord() throws Exception {
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            // no record defined!
            file(metadataFile)).
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
            // no schema defined!
            file(recordFile)).
            andDo(print()).
            andExpect(status().isBadRequest()).
            andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord() throws Exception {
    String id = "testCreateRecordWithBadRecord";
    String schemaId = null;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    //schemaId is missing
    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(record);
    schemaIdentifier.setValue(null);

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord2() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    String id = "testCreateRecordWithLocationUri";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    //remove related resource
    RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_SCHEMA_TYPE);
    record.getRelatedIdentifiers().remove(relatedIdentifier);

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutDocument() throws Exception {
    String id = "testCreateRecordWithoutDocument";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    DataResource record = new DataResource();
////    record.setId("my_id");
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
//    record.setRelatedResource(RELATED_RESOURCE);
//    Set<AclEntry> aclEntries = new HashSet<>();
////    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
////    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
////    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSameRecord() throws Exception {
    String id = "testCreateTwoVersionsOfSameRecord";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
//    DataResource record = new DataResource();
////    record.setId("my_id");
//    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
//    record.setRelatedResource(RELATED_RESOURCE);
//    Set<AclEntry> aclEntries = new HashSet<>();
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    DataResource result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(1L, Long.parseLong(result.getVersion()));

    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isConflict()).andReturn();

    Assert.assertTrue(res.getResponse().getContentAsString().contains("Conflict"));
    Assert.assertTrue(res.getResponse().getContentAsString().contains(id));
  }

  @Test
  public void testCreateTwoVersions() throws Exception {
    String id = "testCreateTwoVersions";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    DataResource result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(1L, Long.parseLong(result.getVersion()));

    DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).setValue(RELATED_RESOURCE_2.toString());
    record.getAlternateIdentifiers().clear();
    record.setId(null);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(1L, Long.parseLong(result.getVersion()));
  }

  @Test
  public void testGetRecordById() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);
    String locationUri = res.getResponse().getHeader("Location");
    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(result);
    Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
    String schemaUrl = schemaIdentifier.getValue();
    Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
    Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
    Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/dc.xml", locationUri);
  }

  @Test
  public void testGetRecords() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertNotNull(result);
    String locationUri = res.getResponse().getHeader("Location");
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
      //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
      Assert.assertNotEquals("file:///tmp/dc.xml", locationUri);
    }
  }

  @Test
  public void testGetRecordByIdWithVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "1").header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);
    String locationUri = res.getResponse().getHeader("Location");
    RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(result);
    Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
    String schemaUrl = schemaIdentifier.getValue();
    Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
    Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
    Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
    Assert.assertNotEquals("file:///tmp/dc.xml", locationUri);
  }

  @Test
  public void testGetRecordByIdWithInvalidId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    String wrongId = metadataRecordId.substring(1);
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + wrongId).
            header("Accept", MediaType.APPLICATION_JSON_VALUE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid id!", result.getResponse().getContentAsString().contains("No content information for identifier " + wrongId));
  }

  @Test
  public void testGetRecordByIdWithInvalidVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            param("version", "13").
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid version!", result.getResponse().getContentAsString().contains("Version '13' of ID '" + metadataRecordId + "' doesn't exist!"));
  }

  @Test
  public void testFindRecordsBySchemaIdWithAlternateEndpoint() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("schemaId", SCHEMA_ID)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("schemaId", SCHEMA_ID)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      Assert.assertTrue(schemaUrl.contains(SCHEMA_ID));
    }
  }

  @Test
  public void testFindRecordsByInvalidSchemaId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", "anyinvalidschemaid")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsOfMultipleVersionsBySchemaId() throws Exception {
    String schemaId = "multipleSchemas".toLowerCase(Locale.getDefault());
    String schema1 = SchemaRegistryControllerTestV2.ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, XML_SCHEMA_V1, metadataConfig.getJwtSecret(), false, status().isCreated());
    String schema2 = SchemaRegistryControllerTestV2.ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, XML_SCHEMA_V2, metadataConfig.getJwtSecret(), true, status().isOk());
    String schema3 = SchemaRegistryControllerTestV2.ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, XML_SCHEMA_V3, metadataConfig.getJwtSecret(), true, status().isOk());
    ObjectMapper map = new ObjectMapper();
    String[] multipleVersions = {"1", "2", "3"};
    // Ingest 1st version of document.
    CreateSchemaUtil.ingestXmlMetadataDocumentV2(mockMvc, schema1, 1L, multipleVersions[0], XML_DOCUMENT_V1, metadataConfig.getJwtSecret());
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);
    // Ingest 2nd version of document
    CreateSchemaUtil.ingestXmlMetadataDocumentV2(mockMvc, schema2, 2L, multipleVersions[1], XML_DOCUMENT_V2, metadataConfig.getJwtSecret());
    res = this.mockMvc.perform(get(API_METADATA_PATH).param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(2, result.length);
    // Ingest 3rd version of document
    CreateSchemaUtil.ingestXmlMetadataDocumentV2(mockMvc, schema3, 3L, multipleVersions[2], XML_DOCUMENT_V3, metadataConfig.getJwtSecret());
    res = this.mockMvc.perform(get(API_METADATA_PATH).param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(3, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
    }
  }

  @Test
  public void testFindRecordsByResourceId() throws Exception {
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("resoureId", RELATED_RESOURCE.getIdentifier())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
    res = this.mockMvc.perform(get(API_METADATA_PATH).param("resourceId", RELATED_RESOURCE.getIdentifier()).param("from", twoHoursBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(1, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertEquals(RELATED_RESOURCE.getIdentifier(), resourceIdentifier.getValue());
    }
  }

  @Test
  public void testFindRecordsBySchemaIdANDResourceId() throws Exception {
    String relatedResource = "testFindRecordsBySchemaIdANDResourceId";
    String relatedResource2 = "anotherTestFindRecordsBySchemaIdANDResourceId";
    String firstSchemaId = "first_schema_for_find_records_by_schema_and_resource_id";
    String secondSchemaId = "second_schema_for_find_records_by_schema_and_resource_id";
    CreateSchemaUtil.ingestKitSchemaRecordV2(mockMvc, firstSchemaId, schemaConfig.getJwtSecret());
    CreateSchemaUtil.ingestKitSchemaRecordV2(mockMvc, secondSchemaId, schemaConfig.getJwtSecret());
    String metadataRecordId = createDCMetadataRecordWithRelatedResource(relatedResource, firstSchemaId);
    String metadataRecordId2 = createDCMetadataRecordWithRelatedResource(relatedResource2, firstSchemaId);
    String metadataRecordIdv2 = createDCMetadataRecordWithRelatedResource(relatedResource, secondSchemaId);
    String metadataRecordId2v2 = createDCMetadataRecordWithRelatedResource(relatedResource2, secondSchemaId);
    // Looking for first schema
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(2, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }
    // Looking for second schema
    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", secondSchemaId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(2, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }
    // Looking for first AND second schema
    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId).
            param("schemaId", secondSchemaId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(4, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }
    // Looking for first, second AND invalid schema
    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId).
            param("schemaId", secondSchemaId).
            param("schemaId", "invalidschemaid")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(4, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }
    // Looking for first, second AND invalid schema AND resource1 and resource2
    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId).
            param("schemaId", secondSchemaId).
            param("schemaId", "invalidschemaid").
            param("resourceId", relatedResource).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(4, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId).
            param("resourceId", relatedResource).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(2, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId).
            param("resourceId", relatedResource)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertEquals(relatedResource, resourceIdentifier.getValue());
    }

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", firstSchemaId).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertEquals(relatedResource2, resourceIdentifier.getValue());
    }

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", secondSchemaId).
            param("resourceId", relatedResource).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(2, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertTrue(resourceIdentifier.getValue().endsWith("ResourceId"));
    }

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", secondSchemaId).
            param("resourceId", relatedResource)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertEquals(relatedResource, resourceIdentifier.getValue());
    }

    res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", secondSchemaId).
            param("resourceId", relatedResource2)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);
    for (DataResource dataResource : result) {
      RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(dataResource);
      Assert.assertEquals(IDENTIFIER_TYPE.URL, schemaIdentifier.getIdentifierType());
      String schemaUrl = schemaIdentifier.getValue();
      Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
      Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
      RelatedIdentifier resourceIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      Assert.assertEquals(IDENTIFIER_TYPE.INTERNAL, resourceIdentifier.getIdentifierType());
      Assert.assertEquals(relatedResource2, resourceIdentifier.getValue());
    }
  }

  @Test
  public void testFindRecordsByInvalidResourceId() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("resourceId", UNKNOWN_RELATED_RESOURCE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByInvalidUploadDate() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).param("resourceId", RELATED_RESOURCE.getIdentifier()).param("until", oneHourBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);

    res = this.mockMvc.perform(get(API_METADATA_PATH).param("resourceId", RELATED_RESOURCE.getIdentifier()).param("from", twoHoursBefore.toString()).param("until", oneHourBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownParameter() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH).
            param("schemaId", "cd")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).accept(MediaType.APPLICATION_XML)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT;

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testGetMetadataDocumentWithUnknownSchema() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    this.mockMvc.perform(get(API_METADATA_PATH + "unknown_dc")).andDo(print()).andExpect(status().isNotFound()).andReturn();
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
            accept(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).andExpect(status().isOk()).andReturn();

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
    Assert.assertEquals(contentInformation1.getVersion() + 1, contentInformation2.getVersion().longValue());
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

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
  }

  @Test
  public void testUpdateRecordWithWrongVersion() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    record.setVersion("0");
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
//    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
//    Assert.assertEquals(record.getSchema().getIdentifier(), record2.getSchema().getIdentifier());
    SchemaRegistryControllerTestV2.validateRelatedIdentifierSets(record.getRelatedIdentifiers(), record2.getRelatedIdentifiers());
    Assert.assertEquals(2L, Long.parseLong(record2.getVersion()));// version should be 2
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    // Check for old location URI.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId + "?version=1")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    // Check for old metadata document
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId + "?version=1").
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    content = result.getResponse().getContentAsString();

    dcMetadata = DC_DOCUMENT;

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testUpdateRecordIgnoreACL() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource oldRecord = mapper.readValue(body, DataResource.class);
    DataResource record = mapper.readValue(body, DataResource.class);
    // Set all ACL to WRITE
    record.setAcls(null);
//    for (AclEntry entry : record.getAcls()) {
//      entry.setPermission(PERMISSION.WRITE);
//    }
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    String locationUri = result.getResponse().getHeader("Location");

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(oldRecord.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(oldRecord), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(oldRecord.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(oldRecord.getAcls(), record2.getAcls());
    Assert.assertTrue(oldRecord.getLastUpdate().isBefore(record2.getLastUpdate()));
    String locationUri2 = result.getResponse().getHeader("Location");

    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    String id = "testUpdateRecordWithoutExplizitGet";
    String schemaId = SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(id, schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
            andDo(print()).
            andExpect(status().isCreated()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();
    String locationUri = result.getResponse().getHeader("Location");

    // Check ContentInformation of first version
    result = this.mockMvc.perform(get(locationUri).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String cibody = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(cibody, ContentInformation.class);

    DataResource record2 = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile2 = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile2 = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(locationUri).
            file(recordFile2).
            file(metadataFile2).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

    body = result.getResponse().getContentAsString();

    DataResource record3 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record2.getDates(), record3.getDates());
    SchemaRegistryControllerTestV2.validateRelatedIdentifierSets(record2.getRelatedIdentifiers(), record2.getRelatedIdentifiers());
    Assert.assertEquals(Long.parseLong(record2.getVersion()), Long.parseLong(record3.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record2.getAcls(), record3.getAcls());
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + id).
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
    Assert.assertEquals(contentInformation1.getVersion() + 1, contentInformation2.getVersion().longValue());
    Assert.assertNotEquals(contentInformation1.getHash(), contentInformation2.getHash());
    Assert.assertNotEquals(contentInformation1.getSize(), contentInformation2.getSize());
    Assert.assertEquals(DC_DOCUMENT.length(), contentInformation1.getSize());
    Assert.assertEquals(DC_DOCUMENT_VERSION_2.length(), contentInformation2.getSize());
  }

  @Test
  public void testUpdateRecordWithSameDocument() throws Exception {
    String id = "testUpdateRecordWithSameDocument";
    ObjectMapper mapper = new ObjectMapper();
    MvcResult result = CreateSchemaUtil.ingestXmlMetadataDocumentV2(mockMvc, SCHEMA_ID, 1L, id, DC_DOCUMENT, schemaConfig.getJwtSecret());
    String body = result.getResponse().getContentAsString();

    DataResource record1 = mapper.readValue(body, DataResource.class);
    // Update without any changes.
    result = CreateSchemaUtil.ingestOrUpdateXmlMetadataDocumentV2(mockMvc, SCHEMA_ID, 1L, id, DC_DOCUMENT, schemaConfig.getJwtSecret(), true, status().isOk());
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertEquals("Version shouldn't change!", record1.getVersion(), record2.getVersion());
  }

  @Test
  public void testUpdateRecordWithSmallChangesInDocument() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    MvcResult result = CreateSchemaUtil.ingestXmlMetadataDocumentV2(mockMvc, SCHEMA_ID, 1L, "document", DC_DOCUMENT, schemaConfig.getJwtSecret());
    String body = result.getResponse().getContentAsString();

    DataResource record1 = mapper.readValue(body, DataResource.class);
    // Update without any changes.
    result = CreateSchemaUtil.ingestOrUpdateXmlMetadataDocumentV2(mockMvc, SCHEMA_ID, 1L, "document", DC_DOCUMENT_SMALL_CHANGE, schemaConfig.getJwtSecret(), true, status().isOk());
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertNotEquals("Version should change!", record1.getVersion(), record2.getVersion());
    Assert.assertEquals("Version should incremented!", Long.parseLong(record1.getVersion()), Long.parseLong(record2.getVersion()) - 1L);
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).
            file(metadataFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isPreconditionFailed()).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithoutRecord() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    String locationUri2 = result.getResponse().getHeader("Location");
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutRecord4Json() throws Exception {
    String metadataRecordId = "testUpdateRecordWithoutRecord4Json";
    // Update only Json document
    CreateSchemaUtil.ingestOrUpdateJsonSchemaRecordV2(mockMvc, JSON_SCHEMA_ID, JSON_SCHEMA, schemaConfig.getJwtSecret(), false, status().isCreated());
    MvcResult result = CreateSchemaUtil.ingestOrUpdateXmlMetadataDocumentV2(mockMvc, JSON_SCHEMA_ID, 1L, metadataRecordId, JSON_DOCUMENT_VERSION_1, schemaConfig.getJwtSecret(), false, status().isCreated());
    String locationUri = result.getResponse().getHeader("Location");
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", JSON_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String locationUri2 = result.getResponse().getHeader("Location");
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertEquals(locationUri.replace("version=1", "version=2"), locationUri2);
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            param("version", "1").
            accept(MediaType.APPLICATION_JSON)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String jsonMetadata = JSON_DOCUMENT_VERSION_1;

    Assert.assertEquals(jsonMetadata, content);
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            param("version", "2").
            accept(MediaType.APPLICATION_JSON)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    content = result.getResponse().getContentAsString();
    jsonMetadata = JSON_DOCUMENT_VERSION_2;

    Assert.assertEquals(jsonMetadata, content);
    // Once more without explicit version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_JSON)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    content = result.getResponse().getContentAsString();

    Assert.assertEquals(jsonMetadata, content);
  }

  @Test
  public void testUpdateRecordWithoutDocument() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    ObjectMapper mapper = new ObjectMapper();
    // Check ContentInformation of first version
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put(API_METADATA_PATH + "dc").contentType("application/json").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()));// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertNotEquals(record.getEtag().replace("version=1", "version=2"), record2.getEtag());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation2 = mapper.readValue(body, ContentInformation.class);
    Assert.assertEquals(contentInformation1, contentInformation2);
  }

  @Test
  public void testUpdateRecordWithoutDocumentChangingRelatedResource() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    String expectedRelatedResource;
    ObjectMapper mapper = new ObjectMapper();
    // Check ContentInformation of first version
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);
    // Get dataresource record version 1
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    DataResource record = mapper.readValue(body, DataResource.class);
    DataResource record2 = mapper.readValue(body, DataResource.class);
    RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record2, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    expectedRelatedResource = relatedIdentifier.getValue() + "_NEW";
    relatedIdentifier.setValue(expectedRelatedResource);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put(API_METADATA_PATH + "dc").contentType("application/json").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();
    etag = result.getResponse().getHeader("ETag");

    record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(record.getVersion(), record2.getVersion());// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    RelatedIdentifier relatedIdentifier1 = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    RelatedIdentifier relatedIdentifier2 = DataResourceRecordUtil.getRelatedIdentifier(record2, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    Assert.assertNotEquals("Related resource should be updated!", relatedIdentifier1.getValue(), relatedIdentifier2.getValue());
    Assert.assertEquals("Related resource should be updated!", expectedRelatedResource, relatedIdentifier2.getValue());

    // Test also updating related type only...
    IDENTIFIER_TYPE expectedIdentifierType = IDENTIFIER_TYPE.ISBN;
    DataResourceRecordUtil.getRelatedIdentifier(record2, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).setIdentifierType(expectedIdentifierType);

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + metadataRecordId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put(API_METADATA_PATH + "dc").contentType("application/json").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record3 = mapper.readValue(body, DataResource.class);
    SchemaRegistryControllerTestV2.validateCreateDates(record2.getDates(), record3.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record2), DataResourceRecordUtil.getSchemaIdentifier(record3));
    Assert.assertEquals(record2.getVersion(), record3.getVersion());// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record2.getAcls(), record3.getAcls());
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
    relatedIdentifier1 = DataResourceRecordUtil.getRelatedIdentifier(record2, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    relatedIdentifier2 = DataResourceRecordUtil.getRelatedIdentifier(record3, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    Assert.assertEquals("Related resource should be the same!", relatedIdentifier1.getValue(), relatedIdentifier2.getValue());
    Assert.assertEquals("Related resource type should be updated!", expectedIdentifierType, relatedIdentifier2.getIdentifierType());

    // Check ContentInformation of second version
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation2 = mapper.readValue(body, ContentInformation.class);
    Assert.assertEquals(contentInformation1, contentInformation2);
  }

  @Test
  public void testUpdateRecordWithInvalidSetting4Json() throws Exception {
    String alternativeSchemaId = "testupdate";
    CreateSchemaUtil.ingestXmlSchemaRecordV2(mockMvc, alternativeSchemaId, CreateSchemaUtil.XML_SCHEMA_V1, schemaConfig.getJwtSecret());
    CreateSchemaUtil.ingestOrUpdateXmlSchemaRecordV2(mockMvc, alternativeSchemaId, CreateSchemaUtil.XML_SCHEMA_V2, schemaConfig.getJwtSecret(), true, status().isOk());
    CreateSchemaUtil.ingestXmlMetadataDocumentV2(mockMvc, alternativeSchemaId, 2L, "document", CreateSchemaUtil.XML_DOCUMENT_V2, schemaConfig.getJwtSecret());
    // Change only version of schema to a version which is not valid.
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocumentV2(mockMvc, alternativeSchemaId, 1L, "document", null, schemaConfig.getJwtSecret(), true, status().isUnprocessableEntity());
    // Change to a nonexistent version of schema.
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocumentV2(mockMvc, alternativeSchemaId, Long.MAX_VALUE, "document", null, schemaConfig.getJwtSecret(), true, status().isUnprocessableEntity());
    // Change to another schema
    CreateSchemaUtil.ingestOrUpdateXmlMetadataDocumentV2(mockMvc, SCHEMA_ID, 1L, "document", null, schemaConfig.getJwtSecret(), true, status().isUnprocessableEntity());
  }

  @Test
  public void testUpdateRecordWithLicense() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertTrue(record.getRights().isEmpty());
    record2.getRights().clear();
    Scheme apache = new Scheme();
    apache.setSchemeId("Apache-2.0");
    apache.setSchemeUri(APACHE_2_LICENSE);
    record2.getRights().add(apache);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();
    String locationUri = result.getResponse().getHeader("Location");

    record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    Assert.assertTrue("No license available", record.getRights().isEmpty());
    Assert.assertNotNull(record2.getRights());
    Assert.assertFalse(record2.getRights().isEmpty());
    Assert.assertEquals(APACHE_2_LICENSE, record2.getRights().iterator().next().getSchemeUri());
    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    record = mapper.readValue(body, DataResource.class);
    record.getRights().clear();
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record3 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record2.getDates(), record3.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record2.getVersion()), Long.parseLong(record3.getVersion()));// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record2.getAcls(), record3.getAcls());
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
    Assert.assertTrue("Set of rights should be 'empty'", record3.getRights().isEmpty());

  }

  @Test
  public void testUpdateRecordWithoutCreateDate() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertTrue(record.getRights().isEmpty());
    Date createDate = null;
    for (Date date : record2.getDates()) {
      if (date.getType().equals(Date.DATE_TYPE.CREATED)) {
        createDate = date;
      }
    }
    record2.getDates().remove(createDate);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();
    record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testUpdateRecordWithLicenseNull() throws Exception {
    String metadataRecordId = createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertTrue(record.getRights().isEmpty());
    record2.getRights().clear();
    Scheme apache = new Scheme();
    apache.setSchemeId("Apache-2.0");
    apache.setSchemeUri(APACHE_2_LICENSE);
    record2.getRights().add(apache);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()) - 1L);// version should be 1 higher
    SchemaRegistryControllerTestV2.validateSets(record.getAcls(), record2.getAcls());
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));

    Assert.assertTrue("No license available", record.getRights().isEmpty());
    Assert.assertNotNull(record2.getRights());
    Assert.assertFalse(record2.getRights().isEmpty());
    Assert.assertEquals(APACHE_2_LICENSE, record2.getRights().iterator().next().getSchemeUri());
    // Check for new metadata document.
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT_VERSION_2;

    Assert.assertEquals(dcMetadata, content);

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    record = mapper.readValue(body, DataResource.class);
    record.setRights(null);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();

//    result = this.mockMvc.perform(put(API_METADATA_PATH + "dc").header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record3 = mapper.readValue(body, DataResource.class);
//    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());
    SchemaRegistryControllerTestV2.validateCreateDates(record2.getDates(), record3.getDates());
    Assert.assertEquals(DataResourceRecordUtil.getSchemaIdentifier(record), DataResourceRecordUtil.getSchemaIdentifier(record2));
    Assert.assertEquals(Long.parseLong(record2.getVersion()), Long.parseLong(record3.getVersion()));// version should be the same
    SchemaRegistryControllerTestV2.validateSets(record2.getAcls(), record3.getAcls());
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
    Assert.assertTrue("Set of rights should be 'empty'", record3.getRights().isEmpty());

  }

  @Test
  public void testDeleteRecordWithoutAuthentication() throws Exception {
    String metadataRecordId = createDCMetadataRecord();

    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    //delete second time
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId)).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testDeleteRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String metadataRecordId = createDCMetadataRecord();

    // Get a list of all records
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecords = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;

    // Get Etag
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    // Delete record
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();

    // Delete second time
    this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId)).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
//    Recreation should be no problem.
//    //try to create after deletion (Should return HTTP GONE)
//    DataResource record = new DataResource();
//    record.setSchemaId("dc");
//    record.setRelatedResource(RELATED_RESOURCE);
//    ObjectMapper mapper = new ObjectMapper();
//
//    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
//    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());
//
//    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + METADATA_RECORD_ID).
//            file(recordFile).
//            file(metadataFile)).andDo(print()).andExpect(status().isGone()).andReturn();

    // List of records should be smaller afterwards
    result = this.mockMvc.perform(get(API_METADATA_PATH).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecordsAfter = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;
    Assert.assertEquals("No of records should be decremented!", noOfRecords - 1, noOfRecordsAfter);
  }

  @Test
  public void testGetAllVersionsOfRecord() throws Exception {
    String id = null;
    for (long version = 1; version <= 3; version++) {
      id = ingestMetadataRecordWithVersion(id, version);
      // Get version of record as array
      // Read all versions 
      this.mockMvc.perform(get(API_METADATA_PATH).param("id", id).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize((int) version)));

      MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + id).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      String body = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      DataResource record = mapper.readValue(body, DataResource.class);
      Assert.assertEquals("Expect current version '" + version + "'", (Long) version, Long.valueOf(record.getVersion()));// version should be 1 higher
      // Check for new metadata document.
      result = this.mockMvc.perform(get(API_METADATA_PATH + id).
              accept(MediaType.APPLICATION_XML)).
              andDo(print()).
              andExpect(status().isOk()).
              andReturn();
      String content = result.getResponse().getContentAsString();

      String dcMetadata = DC_DOCUMENT;

      if (version == 1) {

        Assert.assertTrue(content.startsWith(dcMetadata));

//    Assert.assertEquals(record.getEtag().replace("version=1", "version=2"), record2.getEtag());
        // Get version of record as array
        // Read all versions (2 version2 available)
        result = this.mockMvc.perform(get(API_METADATA_PATH).param("id", id).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize((int) version))).andReturn();
        mapper = new ObjectMapper();
        CollectionType mapCollectionType = mapper.getTypeFactory()
                .constructCollectionType(List.class, DataResource.class);
        List<DataResource> resultList = mapper.readValue(result.getResponse().getContentAsString(), mapCollectionType);
        HashSet<Long> versions = new HashSet<>();
        for (DataResource item : resultList) {
          versions.add(Long.valueOf(item.getVersion()));
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
            .perform(get(API_METADATA_PATH)
                    .param("id", metadataRecordId)
                    .header(HttpHeaders.ACCEPT, "application/json"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(version)))
            .andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    metadataRecordId = ingestNewMetadataRecord(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_METADATA_PATH).param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    metadataRecordId = ingestNewMetadataRecord(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_METADATA_PATH).param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    metadataRecordId = ingestMetadataRecordWithVersion(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_METADATA_PATH).param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));
    // check for higher versions which should be not available (if version > 2)
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "2")).andDo(print()).andExpect(status().isOk());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    metadataRecordId = ingestNewMetadataRecord(metadataRecordId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_METADATA_PATH).param("id", metadataRecordId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2))).andReturn();
    Assert.assertTrue("Reference to " + RELATED_RESOURCE_STRING + version + " is not available", result.getResponse().getContentAsString().contains("\"" + RELATED_RESOURCE_STRING + version + "\""));

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            param("version", "1").
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = DC_DOCUMENT;
//    Assert.assertEquals(dcMetadata, content);

    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            param("version", "2").
            accept(MediaType.APPLICATION_XML)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    content = result.getResponse().getContentAsString();

    Assert.assertNotEquals(dcMetadata, content);
    Assert.assertEquals("Length must differ!", dcMetadata.length() + 3, content.length());
    // check for higher versions which should be not available (if version > 2)
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testSearchProxy() throws Exception {

    // Skip test due to Spring Security 6
    this.mockMvc.perform(MockMvcRequestBuilders.post(API_METADATA_PATH + "search?page=0&size=20")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testSearchWithSchemaProxy() throws Exception {

    // Test for swagger definition
    this.mockMvc.perform(MockMvcRequestBuilders.post(API_METADATA_PATH + "index/search?page=0&size=20")
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

    MvcResult andReturn = this.mockMvc.perform(get(API_METADATA_PATH + "anything")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=anything&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testLandingPage4MetadataWithSchemaId() throws Exception {
    MvcResult andReturn = this.mockMvc.perform(get(API_METADATA_PATH + SCHEMA_ID)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=" + SCHEMA_ID + "&version="))
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

    MvcResult andReturn = this.mockMvc.perform(get(API_METADATA_PATH + documentId)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=" + documentId + "&version=2"))
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

    MvcResult andReturn = this.mockMvc.perform(get(API_METADATA_PATH + documentId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=" + documentId + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get(API_METADATA_PATH + documentId)
            .queryParam("version", "1")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=" + documentId + "&version=1"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    // Ingest a second version...
    MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + documentId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_VERSION_2.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    andReturn = this.mockMvc.perform(get(API_METADATA_PATH + documentId)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=" + documentId + "&version=2"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get(API_METADATA_PATH + documentId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/metadata-landing-page-v2?id=" + documentId + "&version="))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
  }

  @Test
  public void testLandingPage4Schema() throws Exception {

    MvcResult andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + SCHEMA_ID)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page-v2?schemaId=" + SCHEMA_ID + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + SCHEMA_ID)
            .queryParam("version", "1")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page-v2?schemaId=" + SCHEMA_ID + "&version=1"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + SCHEMA_ID)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page-v2?schemaId=" + SCHEMA_ID + "&version=2"))
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
    MvcResult result = mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etagSchema = result.getResponse().getHeader("ETag");
    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isConflict()).
            andReturn();
    // Get Etag
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");
    // Delete record
    result = this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();

    // Try deleting schema once more should also fail
    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isConflict()).
            andReturn();

    // Delete second time
    result = this.mockMvc.perform(get(API_METADATA_PATH + metadataRecordId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    result = this.mockMvc.perform(delete(API_METADATA_PATH + metadataRecordId).
            header("If-Match", etag)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();

    // Now it should be possible to delete schema
    result = this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
    // But it's still available
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etagSchema = result.getResponse().getHeader("ETag");
    // Remove it ones more
    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).
            header("If-Match", etagSchema)).
            andDo(print()).
            andExpect(status().isNoContent()).
            andReturn();
    // Now it' gone
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
  }

  private String createJsonMetadataRecord() throws Exception {
    String randomId = UUID.randomUUID().toString();
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4JsonDocument(randomId, SCHEMA_ID);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", JSON_DOCUMENT_VERSION_1.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    DataResource result = mapper.readValue(andReturn.getResponse().getContentAsString(), DataResource.class);

    return result.getId();
  }

  private String createDCMetadataRecord() throws Exception {
    return createDCMetadataRecordWithRelatedResource(RELATED_RESOURCE_STRING, SCHEMA_ID);
  }

  private String createDCMetadataRecordWithRelatedResource(String myRelatedResource, String schemaId) throws Exception {
    String randomId = UUID.randomUUID().toString();
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4JsonDocument(randomId, schemaId);
    RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    relatedIdentifier.setIdentifierType(IDENTIFIER_TYPE.INTERNAL);
    relatedIdentifier.setValue(myRelatedResource);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    DataResource result = mapper.readValue(andReturn.getResponse().getContentAsString(), DataResource.class);

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
      MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + id).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      String body = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      DataResource record = mapper.readValue(body, DataResource.class);
      String newDocument = DC_DOCUMENT;
      for (int i = 0; i < version; i++) {
        newDocument = newDocument.concat(" ");
      }
      MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", newDocument.getBytes());

      result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
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
      MvcResult result = this.mockMvc.perform(get(API_METADATA_PATH + id).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      String body = result.getResponse().getContentAsString();

      ObjectMapper mapper = new ObjectMapper();
      DataResource record = mapper.readValue(body, DataResource.class);
      RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
      relatedIdentifier.setValue(RELATED_RESOURCE_STRING + version);
      relatedIdentifier.setIdentifierType(IDENTIFIER_TYPE.INTERNAL);
//      record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING + version));

      MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());

      this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH + record.getId()).
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

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestHttpJsonSchemaRecord() throws Exception {
    String schemaId = JSON_HTTP_SCHEMA_ID;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4JsonSchema(schemaId);
//    MetadataSchemaRecord record = new MetadataSchemaRecord();
//    record.setSchemaId(JSON_HTTP_SCHEMA_ID);
//    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
//    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("test", PERMISSION.READ));
//    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_HTTP_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestHttpJsonSchemaRecordWithHash() throws Exception {
    String schemaId = JSON_HTTP_SCHEMA_ID_WITH_HASH;
    DataResource record = SchemaRegistryControllerTestV2.createDataResource4JsonSchema(schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_HTTP_SCHEMA_WITH_HASH.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private String getSchemaUrl(String schemaId) throws Exception {
    String schemaUrl = null;
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    String content = res.getResponse().getContentAsString();
    ContentInformation result = map.readValue(res.getResponse().getContentAsString(), ContentInformation.class);
    schemaUrl = result.getContentUri();
    if (schemaUrl != null) {
      schemaUrl = schemaUrl.replaceFirst("8080", "41421");
    }
    return schemaUrl;
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }
}
