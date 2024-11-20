/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.Identifier.IDENTIFIER_TYPE;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtilTest;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.*;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import org.hamcrest.Matchers;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author Torridity
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
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
@TestPropertySource(properties = {"server.port=41429"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_schema_v2_xsd;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/v2/schematest/schema"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SchemaRegistryControllerTestV2 {

  private static final String API_BASE_PATH = "/api/v2";
  private static final String ALTERNATE_API_SCHEMA_PATH = API_BASE_PATH + "/schemas";
  private static final String API_SCHEMA_PATH = ALTERNATE_API_SCHEMA_PATH + "/";
  private static final String API_METADATA_PATH = API_BASE_PATH + "/metadata/";

  private static final String TEMP_DIR_4_ALL = "/tmp/metastore2/v2/schematest/";
  private static final String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private static final String PID = "anyPID";
  private static final ResourceIdentifier.IdentifierType PID_TYPE = ResourceIdentifier.IdentifierType.HANDLE;
  private static final String SCHEMA_ID = "dc";
  private static final String INVALID_SCHEMA_ID = "invalid/my_dc";
  private static final String LABEL = "any unique label for test";
  private static final String DEFINITION = "any unique definition for test";
  private static final String COMMENT = "any unique comment for test";
  private static final String KIT_SCHEMA = CreateSchemaUtil.KIT_SCHEMA;
  private static final String APACHE_2_LICENSE = "https://spdx.org/licenses/Apache-2.0";
  private static final String KIT_SCHEMA_V2 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "                elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "      <xs:element name=\"metadata\">\n"
          + "        <xs:complexType>\n"
          + "          <xs:sequence>\n"
          + "            <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "            <xs:element name=\"date\" type=\"xs:date\"/>\n"
          + "            <xs:element name=\"note\" type=\"xs:string\" minOccurs=\"0\"/>\n"
          + "          </xs:sequence>\n"
          + "        </xs:complexType>\n"
          + "      </xs:element>\n"
          + "    </xs:schema>";

  private static final String KIT_DOCUMENT = CreateSchemaUtil.KIT_DOCUMENT;
  private static final String INVALID_KIT_DOCUMENT = CreateSchemaUtil.KIT_DOCUMENT_INVALID_1;
  private static final String SCHEMA_V1 = CreateSchemaUtil.XML_SCHEMA_V1;
  private static final String SCHEMA_V2 = CreateSchemaUtil.XML_SCHEMA_V2;
  private static final String SCHEMA_V3 = CreateSchemaUtil.XML_SCHEMA_V3;
  private static final String XML_DOCUMENT_V1 = CreateSchemaUtil.XML_DOCUMENT_V1;
  private static final String XML_DOCUMENT_V2 = CreateSchemaUtil.XML_DOCUMENT_V2;
  private static final String XML_DOCUMENT_V3 = CreateSchemaUtil.XML_DOCUMENT_V3;
  private static final String JSON_DOCUMENT = "{\"title\":\"any string\",\"date\": \"2020-10-16\"}";
  private static final String RELATED_RESOURCE_STRING = "anyResourceId";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryInternalResourceIdentifier(RELATED_RESOURCE_STRING);
  private final static String JSON_SCHEMA = "{\n"
          + "    \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
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

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;

  @Autowired
  private IDataResourceDao dataResourceDao;
  @Autowired
  private ISchemaRecordDao schemaRecordDao;
  @Autowired
  private IContentInformationDao contentInformationDao;
  @Autowired
  private IAllIdentifiersDao allIdentifiersDao;

  @Autowired
  private MetastoreConfiguration schemaConfig;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  @Before
  public void setUp() throws Exception {

    System.out.println("------SchemaRegistryControllerTest--------------------");
    System.out.println("------" + this.schemaConfig);
    System.out.println("------------------------------------------------------");
    contentInformationDao.deleteAll();
    dataResourceDao.deleteAll();
    schemaRecordDao.deleteAll();
    allIdentifiersDao.deleteAll();
    try {
      try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
        walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      }
      Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
  }

  @Test
  public void testCreateSchemaRecord() throws Exception {
    DataResource record = createDataResource4Schema("my_dc");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithMinimalInput() throws Exception {
    String id = "my_dc_minimal_record";
    DataResource record = new DataResource();
    record.setId(id);
    // mandatory element title has to be set
    setTitle(record, id);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithMinimalInputAndNoContentType() throws Exception {
    String id = "my_dc_minimal_record";
    DataResource record = new DataResource();
    record.setId(id);
    // mandatory element title has to be set
    setTitle(record, id);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", null, KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithAlternateEndpoint() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_alternate");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(ALTERNATE_API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithCapitalLetter() throws Exception {
    String schemaIDWithCapitalLetters = "myFirstTest";
    DataResource record = createDataResource4Schema(schemaIDWithCapitalLetters);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    DataResource ms_record = mapper.readValue(result.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(record.getResourceType().getValue(), ms_record.getResourceType().getValue());
    Assert.assertEquals(record.getResourceType().getTypeGeneral(), ms_record.getResourceType().getTypeGeneral());
    Assert.assertEquals(record.getFormats(), ms_record.getFormats());
    Assert.assertNotEquals(record.getId(), ms_record.getId());
    Assert.assertEquals(schemaIDWithCapitalLetters.toLowerCase(), ms_record.getId());
  }

  @Test
  public void testCreateRegisterSchemaRecordWithSameIdButCapitalLetter() throws Exception {
    String schemaIDWithCapitalLetters = "mySecondTest";
    DataResource record = createDataResource4Schema(schemaIDWithCapitalLetters);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    DataResource ms_record = mapper.readValue(result.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(record.getResourceType().getValue(), ms_record.getResourceType().getValue());
    Assert.assertEquals(record.getResourceType().getTypeGeneral(), ms_record.getResourceType().getTypeGeneral());
    Assert.assertEquals(record.getFormats(), ms_record.getFormats());
    Assert.assertEquals(record.getId().toLowerCase(), ms_record.getId());
    Assert.assertNotEquals(schemaIDWithCapitalLetters, ms_record.getId());

    record.setId("MySecondTest");
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
  }

  //@Test
  public void testCreateSchemaRecordWithIdentifierWithoutType() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_without_type");
    Identifier ri = Identifier.factoryIdentifier("any", null);
    record.getAlternateIdentifiers().add(ri);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutMimeType() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_2");
    record.getFormats().clear();
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutContentType() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_3");
    record.getFormats().clear();
    record.setResourceType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", null, KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithLocationUri() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_new");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=1")).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String content = result.getResponse().getContentAsString();

    MvcResult result2 = this.mockMvc.perform(get(locationUri).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content2 = result2.getResponse().getContentAsString();

    validateDataResources(content, content2);
//    Assert.assertEquals(content, content2);
  }

  @Test
  public void testCreateInvalidSchemaRecord() throws Exception {
    DataResource record = createDataResource4Schema(INVALID_SCHEMA_ID);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithEmptyAclSid() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_empty_sid");
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(null, PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    Assert.assertTrue(res.getResponse().getContentAsString().contains("Subject ID of ACL entry must not be null."));
  }

  @Test
  public void testCreateInvalidMetadataSchemaRecord() throws Exception {
    String wrongTypeJson = "{\"schemaId\":\"dc\",\"type\":\"Something totally strange!\"}";

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", wrongTypeJson.getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    String wrongFormatJson = "<metadata><schemaId>dc</schemaId><type>XML</type></metadata>";
    recordFile = new MockMultipartFile("record", "record.json", "application/json", wrongFormatJson.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

  }

  @Test
  public void testCreateEmptyMetadataSchemaRecord() throws Exception {

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", (byte[]) null);
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    recordFile = new MockMultipartFile("record", "record.json", "application/json", " ".getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  // @Test 
  public void testCreateSchemaRecordFromExternal() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_from_extern");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.external.domain"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateSchemaRecordUpdateFromExternal() throws Exception {
    DataResource record = createDataResource4Schema("my_dcExt");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.domain.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile).with(remoteAddr("www.google.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWrongType() throws Exception {
    DataResource record = createDataResource4Schema("my_dc");
    record.setResourceType(ResourceType.createResourceType(MetadataSchemaRecord.SCHEMA_TYPE.JSON + DataResourceRecordUtil.SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordGuessingType() throws Exception {
    DataResource record = createDataResource4Schema("my_dc");
    record.setResourceType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    record = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(MetadataSchemaRecord.SCHEMA_TYPE.XML + "_Schema", record.getResourceType().getValue());
  }

  @Test
  public void testCreateSchemaRecordGuessingTypeFails() throws Exception {
    DataResource record = createDataResource4Schema("my_dc");
    record.setResourceType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "?".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();

  }

  @Test
  public void testCreateSchemaRecordWithBadSchema() throws Exception {
    DataResource record = createDataResource4Schema("bad_schema");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithEmptySchema() throws Exception {
    DataResource record = createDataResource4Schema("empty_schema");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutRecord() throws Exception {
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutSchema() throws Exception {
    DataResource record = createDataResource4Schema("without_schema");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithBadRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    DataResource record = createDataResource4Schema(null);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSchemaRecord() throws Exception {
    DataResource record = createDataResource4Schema("my_dc_with_version");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    DataResource result = mapper.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertEquals(result.getVersion(), Long.toString(1L));
    // Can't create same resource twice -> Conflict
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
  }

  @Test
  public void testGetSchemaRecordByIdWithoutVersion() throws Exception {
    String schemaId = "testGetSchemaRecordByIdWithoutVersion".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);

    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(schemaId, result.getId());
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
//    Assert.assertNotEquals("file:///tmp/dc.xsd", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithVersion() throws Exception {
    String schemaId = "testGetSchemaRecordByIdWithVersion".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);

    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "1").header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource result = map.readValue(res.getResponse().getContentAsString(), DataResource.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(schemaId, result.getId());
//    Assert.assertNotEquals("file:///tmp/dc.xsd", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidId() throws Exception {
    String schemaId = "testGetSchemaRecordByIdWithInvalidId".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + "cd").
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid schema ID!", result.getResponse().getContentAsString().contains("Document with ID 'cd' doesn't exist!"));
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidVersion() throws Exception {
    String schemaId = "testGetSchemaRecordByIdWithInvalidVersion".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            param("version", "13").
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid version!", result.getResponse().getContentAsString().contains("Version '13' of ID '" + schemaId + "' doesn't exist!"));
  }

  @Test
  public void testFindRecordsBySchemaIdWithAlternateEndpoint() throws Exception {
    String schemaId = "testFindRecordsBySchemaIdWithAlternateEndpoint".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    // alternate endpoint is no longer available!
    MvcResult res = this.mockMvc.perform(get(ALTERNATE_API_SCHEMA_PATH).param("schemaId", schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    String schemaId = "testFindRecordsBySchemaId".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertTrue(result.length > 0);
  }

  @Test
  public void testFindRecordsByMimeType() throws Exception {
    ingestXmlDataResource("byMimeType");
    ingestJsonDataResource("byMimeTypeJson");
    ObjectMapper map = new ObjectMapper();
    //  Search without mimetype
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(2, result.length);

    // Search only for xml schemas
    res = this.mockMvc.perform(get(API_SCHEMA_PATH).
            param("mimeType", MediaType.APPLICATION_XML_VALUE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);

    // Search only for json schemas
    res = this.mockMvc.perform(get(API_SCHEMA_PATH).
            param("mimeType", MediaType.APPLICATION_JSON_VALUE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);
    Assert.assertEquals(1, result.length);

    // Search for both mimetypes.
    res = this.mockMvc.perform(get(API_SCHEMA_PATH).
            param("mimeType", MediaType.APPLICATION_XML_VALUE).
            param("mimeType", MediaType.APPLICATION_JSON_VALUE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(2, result.length);
    // Search for unkown mimetypes. (ignore them)
    res = this.mockMvc.perform(get(API_SCHEMA_PATH).
            param("mimeType", MediaType.TEXT_PLAIN_VALUE).
            param("mimeType", MediaType.TEXT_HTML_VALUE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByInvalidMimeType() throws Exception {
    String schemaId = "testFindRecordsByInvalidMimeType".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).param("mimeType", "invalid")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownSchemaId() throws Exception {
    String schemaId = "testFindRecordsByUnknownSchemaId".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).
            param("schemaId", "schema_id_which_is_not_known")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    String schemaId = "testGetSchemaDocument".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA, content);
  }

  @Test
  public void testGetSchemaDocumentWithMissingSchemaFile() throws Exception {
    String schemaId = "testGetSchemaDocumentWithMissingSchemaFile".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    String contentUri = contentInformationDao.findAll(PageRequest.of(0, 2)).getContent().get(0).getContentUri();
    //delete schema file
    URI uri = new URI(contentUri);
    Files.delete(Paths.get(uri));

    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testValidate() throws Exception {
    String schemaId = "testValidate".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownVersion() throws Exception {
    String schemaId = "testValidateUnknownVersion".toLowerCase(Locale.getDefault());
    String version = "666";
    ingestXmlDataResource(schemaId);
    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate?version=" + version).file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
    Assert.assertTrue(andReturn.getResponse().getContentAsString().contains(version));
    Assert.assertTrue(andReturn.getResponse().getContentAsString().contains(schemaId));
  }

  @Test
  public void testValidateKnownVersion() throws Exception {
    String schemaId = "testValidateKnownVersion".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate?version=1").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownSchemaId() throws Exception {
    String schemaId = "testValidateUnknownSchemaId".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + INVALID_SCHEMA_ID + "/validate").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testValidateWithInvalidDocument() throws Exception {
    String schemaId = "testValidateWithInvalidDocument".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate").file("document", INVALID_KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithEmptyDocument() throws Exception {
    String schemaId = "testValidateWithEmptyDocument".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate").file("document", "".getBytes())).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutDocument() throws Exception {
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate")).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutValidator() throws Exception {
    String schemaId = "testValidateWithoutValidator".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithMissingSchemaFile() throws Exception {
    String schemaId = "testValidateWithMissingSchemaFile";
    ingestXmlDataResource(schemaId);
    // Get location of schema file.
    DataResource dataRes = DataResource.factoryNewDataResource();
    dataRes.setId(schemaId.toLowerCase());
    String contentUri = contentInformationDao.findByParentResource(dataRes, PageRequest.of(0, 2)).getContent().get(0).getContentUri();
    //delete schema file
    URI uri = new URI(contentUri);
    Files.delete(Paths.get(uri));

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().is5xxServerError()).andReturn();
  }

  // Update only record
  @Test
  public void testUpdateRecord() throws Exception {
    String schemaId = "updateRecord";
    String newComment = "new comment";
    String newLabel = "label changed!";
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    String mimeTypeBefore = record.getFormats().iterator().next();
    validateDescriptions(record, schemaId, DEFINITION, COMMENT);
//    Assert.assertEquals(DEFINITION, record.getDesDefinition());
//    Assert.assertEquals(LABEL, record.getTitle());
//    Assert.assertEquals(COMMENT, record.getComment());
    setDefinition(record, null);
    setComment(record, newComment);
    setTitle(record, newLabel);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type is not allowed to be changed.

//    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    validateCreateDates(record.getDates(), record2.getDates());
    // Version shouldn't be updated
//    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertEquals("Check label: ", getTitle(record), getTitle(record2));
    Assert.assertEquals("Check comment: ", getComment(record), getComment(record2));
    Assert.assertEquals("Check definition: ", getDefinition(record), getDefinition(record2));
    Assert.assertEquals("Check label: ", newLabel, getTitle(record2));
    Assert.assertEquals("Check comment: ", newComment, getComment(record2));
    Assert.assertNull("Check definition for 'null'", getDefinition(record2));
  }

  // Update only record
  @Test
  public void testUpdateRecordRemovingLabel() throws Exception {
    String schemaId = "updateRecord".toLowerCase(Locale.getDefault());
    String newComment = "new comment";
    String newLabel = "label changed!";
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    String mimeTypeBefore = record.getFormats().iterator().next();
    Assert.assertEquals(DEFINITION, getDefinition(record));
    Assert.assertEquals(schemaId, getTitle(record));
    Assert.assertEquals(COMMENT, getComment(record));
    setDefinition(record, null);
    setComment(record, null);
    setTitle(record, null);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type is not allowed to be changed.
    //    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());    validateCreateDates(record.getDates(), record2.getDates());
    // Version shouldn't be updated
//    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
//    if (recUpdateord.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
//    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    validateUpdateDates(record.getDates(), record2.getDates());
    validateDescriptions(record.getDescriptions(), record2.getDescriptions());
    validateTitles(record.getTitles(), record2.getTitles());
//    Assert.assertEquals(" label: ", record.getTitle(), record2.getTitle());
//    Assert.assertEquals("Check comment: ", record.getComment(), record2.getComment());
//    Assert.assertEquals("ChCheckeck definition: ", record.getDefinition(), record2.getComment());
//    Assert.assertNull("Check label: ", record2.getTitle());
//    Assert.assertNull("Check comment: ", record2.getComment());
//    Assert.assertNull("Check definition for 'null'", record2.getDefinition());
  }

  @Test
  public void testUpdateRecordIgnoreACL() throws Exception {
    String schemaId = "updateRecord".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource oldRecord = mapper.readValue(body, DataResource.class);
    DataResource record = mapper.readValue(body, DataResource.class);
    // Set all ACL to WRITE
    for (AclEntry entry : record.getAcls()) {
      entry.setPermission(PERMISSION.WRITE);
    }
    String mimeTypeBefore = record.getFormats().iterator().next();
    String definitionBefore = getDefinition(record);
    String labelBefore = getTitle(record);
    String commentBefore = getComment(record);
    setDefinition(record, "");
    setComment(record, "new comment");
    setTitle(record, "label changed");
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type is not allowed to be changed.
//    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    validateCreateDates(record.getDates(), record2.getDates());
    // Version shouldn't be updated
//    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record.getVersion()), Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    if (record.getAcls() != null) {
      Assert.assertTrue(isSameSetOfAclEntries(record.getAcls(), record2.getAcls()));
      Assert.assertFalse(isSameSetOfAclEntries(oldRecord.getAcls(), record.getAcls()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertEquals("Check label: ", getTitle(record), getTitle(record2));
    Assert.assertEquals("Check comment: ", getComment(record), getComment(record2));
    Assert.assertNotEquals("Check label: ", labelBefore, getTitle(record2));
    Assert.assertNotEquals("Check comment: ", commentBefore, getComment(record2));
    Assert.assertNull("Check definition for 'null'", getDefinition(record2));
  }

  @Test
  public void testUpdateRecordWithIgnoringInvalidSetting4Xml() throws Exception {
    String schemaId = "updateMimetypeOfRecord".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    record.getFormats().clear();
    record.getFormats().add(MetadataSchemaRecord.SCHEMA_TYPE.JSON.name());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    // Should not fail as invalid mimetype is not used validating schema!
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*"));
  }

  @Test
  public void testUpdateRecordWithInvalidSetting4Xml() throws Exception {
    String schemaId = "updateTypeOfRecord".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    record.setResourceType(ResourceType.createResourceType(DataResourceRecordUtil.JSON_SCHEMA_TYPE, ResourceType.TYPE_GENERAL.MODEL));
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    // Should fail due to invalid type!
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void testUpdateRecordWithoutChanges() throws Exception {
    String schemaId = "updateRecordWithoutChanges".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    validateDataResources(record, record2, true);
//    Assert.assertEquals(record.getFormats().iterator().next(), record2.getFormats().iterator().next());//mime type was changed by update
//    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
//    // Version shouldn't be updated
//    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
//    Assert.assertEquals(record.getId(), record2.getId());
//    Assert.assertEquals((long) Long.parseLong(record.getVersion()), (long) Long.parseLong(record2.getVersion()));//version is not changing for metadata update
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
//    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordAndDocument() throws Exception {
    String schemaId = "updateRecordAndDocument";
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    String mimeTypeBefore = record.getFormats().iterator().next();
    record.getFormats().clear();
    record.getFormats().add(MediaType.APPLICATION_JSON.toString());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
//    Assert.assertNull(record2.getLicenseUri());
//    Assert.assertEquals(record.getLicenseUri(), record2.getLicenseUri());
    validateRights(record.getRights(), record2.getRights());

    Assert.assertNotEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type was changed by update
//    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    validateCreateDates(record.getDates(), record2.getDates());

    testForNextVersion(record.getVersion(), record2.getVersion());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
//    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getId(), record2.getId());
    validateSets(record.getAcls(), record2.getAcls());
    validateUpdateDates(record.getDates(), record2.getDates());
//    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
  }

  @Test
  public void testUpdateRecordAndDocumentWithLicense() throws Exception {
    String schemaId = "updateRecordAndDocumentWithLicense".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    String mimeTypeBefore = record.getFormats().iterator().next();
    record.getFormats().clear();
    record.getFormats().add(MetadataSchemaRecord.SCHEMA_TYPE.JSON.name());
    record.getRights().add(Scheme.factoryScheme("URL", APACHE_2_LICENSE));
    System.out.println("****************************************************************************************");
    System.out.println("****************************************************************************************");
    System.out.println(mapper.writeValueAsString(record));
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            file(schemaFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();
    etag = result.getResponse().getHeader("ETag");

    DataResource record2 = mapper.readValue(body, DataResource.class);
    validateRights(record.getRights(), record2.getRights());
//    Assert.assertNotNull(record2.getLicenseUri());
//    Assert.assertEquals(record.getLicenseUri(), record2.getLicenseUri());
    validateRights(record.getRights(), record2.getRights());
    Assert.assertNotEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type was changed by update
    //    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());    validateCreateDates(record.getDates(), record2.getDates());
    testForNextVersion(record.getVersion(), record2.getVersion());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
//    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record.getVersion()) + 1L, Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
    // Remove license
    record2.getRights().clear();
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record3 = mapper.readValue(body, DataResource.class);
    validateDataResources(record2, record3, true);
//    Assert.assertNull(record3.getLicenseUri());
//    Assert.assertEquals(record2.getFormats().iterator().next(), record3.getFormats().iterator().next());//mime type was changed by update
//    Assert.assertEquals(record2.getCreatedAt(), record3.getCreatedAt());
//    Assert.assertEquals(record2.getSchemaDocumentUri(), record3.getSchemaDocumentUri());
////    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record2.getSchemaHash(), record3.getSchemaHash());
//    Assert.assertEquals(record2.getId(), record3.getId());
//    Assert.assertEquals((long) Long.parseLong(record.getVersion()) + 1l, (long) Long.parseLong(record2.getVersion()));//version is not changing for metadata update
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record2.getAcl().containsAll(record3.getAcl()));
//    }
//    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordAndDocumentWithWrongVersion() throws Exception {
    String schemaId = "updateRecordAndDocumentWithWrongVersion".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    record.setVersion(Long.toString(0L));
    String mimeTypeBefore = record.getFormats().iterator().next();
    record.getFormats().clear();
    record.getFormats().add(MetadataSchemaRecord.SCHEMA_TYPE.JSON.name());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type was changed by update
    validateCreateDates(record.getDates(), record2.getDates());
    Assert.assertEquals(record.getId(), record2.getId());
    Assert.assertEquals(2L, Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
    validateUpdateDates(record.getDates(), record2.getDates());
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
    // Test also old document
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId + "?version=1")).andDo(print()).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();
    Assert.assertEquals(KIT_SCHEMA, content);
  }

  @Test
  public void testUpdateOnlyDocument() throws Exception {
    String schemaId = "updateRecordDocumentOnly".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    
    // Get ContentInformation of first version
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            accept(ContentInformation.CONTENT_INFORMATION_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    body = result.getResponse().getContentAsString();

    ContentInformation contentInformation1 = mapper.readValue(body, ContentInformation.class);

    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(schemaFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + schemaId + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertEquals(record.getFormats().iterator().next(), record2.getFormats().iterator().next());//mime type was changed by update
    //    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());    validateCreateDates(record.getDates(), record2.getDates());
    testForNextVersion(record.getVersion(), record2.getVersion());
//    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record.getVersion()) + 1L, Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
    // Test also contentInformation after update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
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
  }

  @Test
  public void testUpdateRecordWithSmallChangesInDocument() throws Exception {
    String schemaId = "updateRecordWithSmallChanges";
    DataResource schemaRecord = createDataResource4Schema(schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", CreateSchemaUtil.XML_SCHEMA_V1.getBytes());
////
    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
//    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", CreateSchemaUtil.XML_SCHEMA_V1_TYPO.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(schemaFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + schemaRecord.getId().toLowerCase() + "?version=*")).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    String schemaId = "updateWithoutGet";
    DataResource record = createDataResource4Schema(schemaId);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    DataResource record1 = mapper.readValue(body, DataResource.class);
    String mimeTypeBefore = record1.getFormats().iterator().next();
    record1.getFormats().clear();
    record1.getFormats().add(MetadataSchemaRecord.SCHEMA_TYPE.JSON.name());
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record1).getBytes());
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getFormats().iterator().next());//mime type was changed by update
    validateDates(record1.getDates(), record2.getDates());
    // Version shouldn't be updated
    Assert.assertEquals(record1.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record1.getVersion()), Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
    validateUpdateDates(record1.getDates(), record2.getDates());
//    Assert.assertTrue(record1.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    String schemaId = "testUpdateRecordWithoutETag".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    record.getFormats().clear();
    record.getFormats().add(MetadataSchemaRecord.SCHEMA_TYPE.JSON.name());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    String schemaId = "testUpdateRecordWithWrongETag".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutBody() throws Exception {
    String schemaId = "testUpdateRecordWithoutBody".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(put(API_SCHEMA_PATH + schemaId).header("If-Match", etag).contentType(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE).content("{}")).andDo(print()).andExpect(status().isUnsupportedMediaType()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithUpdateWithoutChanges() throws Exception {
    // Test with a schema missing schema property.
    String schemaId = "updateWithoutChanges_xsd".toLowerCase(Locale.getDefault());
    DataResource record = createDataResource4Schema(schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", SCHEMA_V3.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    DataResource record1 = mapper.readValue(body, DataResource.class);
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + record.getId()).
            file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    DataResource record2 = mapper.readValue(body, DataResource.class);
    validateStrings(record1.getFormats(), record2.getFormats());
    validateDates(record1.getDates(), record2.getDates());
//    Assert.assertEquals(record1.getFormats().iterator().next(), record2.getFormats().iterator().next());//mime type was changed by update
//    Assert.assertEquals(record1.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record1.getId(), record2.getId());
    Assert.assertEquals(Long.parseLong(record1.getVersion()), Long.parseLong(record2.getVersion()));//version is not changing for metadata update
    validateSets(record.getAcls(), record2.getAcls());
//    if (record.getAcl() != null) {
//      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
//    }
    validateUpdateDates(record1.getDates(), record2.getDates());
  }

  @Test
  public void testDeleteSchemaRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String schemaId = "testDelete".toLowerCase(Locale.getDefault());
    ingestXmlDataResource(schemaId);

    // Get a list of all records
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecords = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;

    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    // create should return conflict
    DataResource schemaRecord = createDataResource4Schema(schemaId);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
    //delete second time // should be really deleted -> gone
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");
    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();

    //try to create after deletion (Should return HTTP GONE)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isGone()).andReturn();

    // List of records should be smaller afterwards
    result = this.mockMvc.perform(get(API_SCHEMA_PATH).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    int noOfRecordsAfter = mapper.readValue(result.getResponse().getContentAsString(), DataResource[].class).length;
    Assert.assertEquals("No of records should be decremented!", noOfRecords - 1, noOfRecordsAfter);
  }

  @Test
  public void testGetAllVersionsOfRecord() throws Exception {
    String schemaId = "testWithVersion".toLowerCase(Locale.getDefault());
    for (long version = 1; version <= 3; version++) {
      // Create a new version
      ingestSchemaWithVersion(schemaId, version);
      // Get version of record as array
      // Read all versions 
      MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize((int) version))).andReturn();
      ObjectMapper mapper = new ObjectMapper();
      CollectionType mapCollectionType = mapper.getTypeFactory()
              .constructCollectionType(List.class, DataResource.class);
      List<DataResource> resultList = mapper.readValue(result.getResponse().getContentAsString(), mapCollectionType);
      HashSet<Long> versions = new HashSet<>();
      for (DataResource item : resultList) {
        versions.add(Long.parseLong(item.getVersion()));
      }
      Assert.assertEquals(version, versions.size());
      for (long index = 1; index <= version; index++) {
        Assert.assertTrue("Test for version: " + index, versions.contains(index));
      }
      // Validate document with last version
      byte[] xmlDocument = null;
      for (int document = 1; document <= version; document++) {
        switch (document) {
          case 1:
            xmlDocument = XML_DOCUMENT_V1.getBytes();
            break;
          case 2:
            xmlDocument = XML_DOCUMENT_V2.getBytes();
            break;
          case 3:
            xmlDocument = XML_DOCUMENT_V3.getBytes();
            break;
          default:
            Assert.fail("Unknown document: '" + document + "'");
        }

        ResultMatcher resultMatcher = null;
        if (version == document) {
          resultMatcher = status().isNoContent();
        } else {
          resultMatcher = status().isUnprocessableEntity();
        }
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate").file("document", xmlDocument)).andDo(print()).andExpect(resultMatcher).andReturn();

      }
    }
    // Separate test of each document with its specific version
    for (int document = 1; document <= 3; document++) {
      byte[] xmlDocument = null;
      switch (document) {
        case 1:
          xmlDocument = XML_DOCUMENT_V1.getBytes();
          break;
        case 2:
          xmlDocument = XML_DOCUMENT_V2.getBytes();
          break;
        case 3:
          xmlDocument = XML_DOCUMENT_V3.getBytes();
          break;
        default:
          Assert.fail("Unknown document: '" + document + "'");
      }

      ResultMatcher resultMatcher = status().isNoContent();
      this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId + "/validate?version=" + document).file("document", xmlDocument)).andDo(print()).andExpect(resultMatcher).andReturn();
    }
  }

  /**
   * **************************************************************************
   * Moved tests from MetadataSchemaRecordUtilTest
   * **************************************************************************
   */
  /**
   * Test of migrateToDataResource method, of class MetadataSchemaRecordUtil.
   */
  @Test
  public void testMigrateToDataResource() {
    System.out.println("migrateToDataResource");
    RepoBaseConfiguration applicationProperties = schemaConfig;
    // Test with all possible values PID shouldn't be an URL
    MetadataSchemaRecord metadataSchemaRecord = new MetadataSchemaRecordUtilTest().createSchemaRecord(5, 7, 11, 12);
    MetadataSchemaRecord expResult = null;
    DataResource result = MetadataSchemaRecordUtil.migrateToDataResource(applicationProperties, metadataSchemaRecord);
    expResult = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(applicationProperties, result, false);
    assertEquals(metadataSchemaRecord, expResult);
    // Test with all possible values containing valid PID.
    metadataSchemaRecord = new MetadataSchemaRecordUtilTest().createSchemaRecord(5, 7, 11, 12);
    ResourceIdentifier correctPid = ResourceIdentifier.factoryResourceIdentifier(PID, PID_TYPE);
    metadataSchemaRecord.setPid(correctPid);
    result = MetadataSchemaRecordUtil.migrateToDataResource(applicationProperties, metadataSchemaRecord);
    expResult = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(applicationProperties, result, false);
    assertEquals(metadataSchemaRecord, expResult);
    // Test skipping pid
    metadataSchemaRecord = new MetadataSchemaRecordUtilTest().createSchemaRecord(5, 7, 10, 11, 12);
    result = MetadataSchemaRecordUtil.migrateToDataResource(applicationProperties, metadataSchemaRecord);
    expResult = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(applicationProperties, result, false);
    assertEquals(metadataSchemaRecord, expResult);
  }

  @Test
  public void testIssue52() throws Exception {
    String schemaId = "test4Issue52".toLowerCase(Locale.getDefault());
    int version = 1;
    ingestSchemaWithVersion(schemaId, version);
    // Test get record with one version
    // Read all versions 
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(version))).andReturn();
    Assert.assertTrue("Reference to '" + COMMENT + version + "' is not available", result.getResponse().getContentAsString().contains("\"" + COMMENT + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    ingestNewSchemaRecord(schemaId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).andReturn();
    Assert.assertTrue("Reference to " + COMMENT + version + " is not available", result.getResponse().getContentAsString().contains("\"" + COMMENT + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    ingestNewSchemaRecord(schemaId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1))).andReturn();
    Assert.assertTrue("Reference to " + COMMENT + version + " is not available", result.getResponse().getContentAsString().contains("\"" + COMMENT + version + "\""));
    // check for higher versions which should be not available
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "2")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    ingestSchemaWithVersion(schemaId, 2);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2))).andReturn();
    Assert.assertTrue("Reference to " + COMMENT + version + " is not available", result.getResponse().getContentAsString().contains("\"" + COMMENT + version + "\""));
    // check for higher versions which should be not available (if version > 2)
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "2")).andDo(print()).andExpect(status().isOk());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());

    version++;
    ingestNewSchemaRecord(schemaId, version);
    // Read all versions (should be still one version)
    result = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", schemaId).header(HttpHeaders.ACCEPT, "application/json")).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2))).andReturn();
    Assert.assertTrue("Reference to " + COMMENT + version + " is not available", result.getResponse().getContentAsString().contains("\"" + COMMENT + version + "\""));

    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "1")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcSchema = SCHEMA_V1;

//    Assert.assertEquals(dcMetadata, content);
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "2")).andDo(print()).andExpect(status().isOk()).andReturn();
    content = result.getResponse().getContentAsString();

    Assert.assertNotEquals(dcSchema, content);
    Assert.assertEquals("Length must differ!", SCHEMA_V2.length(), content.length());
    // check for higher versions which should be not available (if version > 2)
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "3")).andDo(print()).andExpect(status().isNotFound());
    this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).param("version", "4")).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testLandingPage4SchemaUnknownID() throws Exception {
    MvcResult andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + "anything")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=anything&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testLandingPage4SchemaWithMetadataDocumentId() throws Exception {
    String schemaId = "metadata_document_id";
    ingestXmlDataResource(schemaId);
    String documentId = createKitMetadataRecord(schemaId);

    MvcResult andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + documentId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + documentId + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isBadRequest());
  }

  @Test
  public void testLandingPage4SchemaWrongVersion() throws Exception {
    MvcResult andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + SCHEMA_ID)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + SCHEMA_ID + "&version=2"))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isNotFound());
  }

  @Test
  public void testLandingPage4Schema() throws Exception {
    String schemaId = "landingpage";
    ingestSchemaWithVersion(schemaId, 1);

    MvcResult andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + schemaId + "&version="))
            .andReturn();
    String redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)
            .queryParam("version", "1")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + schemaId + "&version=1"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    // Ingest a second version...
    ingestSchemaWithVersion(schemaId, 2);
    andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)
            .queryParam("version", "2")
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + schemaId + "&version=2"))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
    andReturn = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/schema-landing-page?schemaId=" + schemaId + "&version="))
            .andReturn();
    redirectedUrl = andReturn.getResponse().getRedirectedUrl();
    this.mockMvc.perform(get(redirectedUrl)
            .accept("text/html"))
            .andDo(print())
            .andExpect(status().isOk());
  }

  private void ingestXmlDataResource(String schemaId) throws Exception {
    DataResource record = createDataResource4Schema(schemaId);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestJsonDataResource(String schemaId) throws Exception {
    DataResource record = createDataResource4Schema(schemaId);
    record.setResourceType(ResourceType.createResourceType(DataResourceRecordUtil.JSON_SCHEMA_TYPE, ResourceType.TYPE_GENERAL.MODEL));
    record.getFormats().clear();
    record.getFormats().add(MediaType.APPLICATION_JSON_VALUE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestSchemaRecord() throws Exception {
    DataResource dataResource = DataResource.factoryNewDataResource(SCHEMA_ID);
    dataResource.getCreators().add(Agent.factoryAgent(null, "SELF"));
    dataResource.getTitles().add(Title.factoryTitle(LABEL, null));
    dataResource.setPublisher("SELF");
    Instant now = Instant.now();
    dataResource.setPublicationYear(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    dataResource.setResourceType(ResourceType.createResourceType(DataResourceRecordUtil.XML_SCHEMA_TYPE, ResourceType.TYPE_GENERAL.MODEL));
    dataResource.getDates().add(Date.factoryDate(now, Date.DATE_TYPE.CREATED));
    dataResource.getFormats().add(MediaType.APPLICATION_XML_VALUE);
    dataResource.setLastUpdate(now);
    dataResource.setState(DataResource.State.VOLATILE);
    dataResource.setVersion("1");
    Set<AclEntry> aclEntries = dataResource.getAcls();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    Set<Description> descriptions = dataResource.getDescriptions();
    descriptions.add(Description.factoryDescription(LABEL, Description.TYPE.OTHER));
    descriptions.add(Description.factoryDescription(COMMENT, Description.TYPE.ABSTRACT));
    descriptions.add(Description.factoryDescription(DEFINITION, Description.TYPE.TECHNICAL_INFO));
    descriptions.add(Description.factoryDescription("not used yet", Description.TYPE.METHODS));
    ContentInformation ci = ContentInformation.createContentInformation(
            SCHEMA_ID, "schema.xsd", (String[]) null);
    ci.setVersion(1);
    ci.setFileVersion("1");
    ci.setVersioningService("simple");
    ci.setDepth(1);
    ci.setContentUri("file:///tmp/schema_dc.xsd");
    ci.setUploader("SELF");
    ci.setMediaType("text/plain");
    ci.setHash("sha1:400dfe162fd702a619c4d11ddfb3b7550cb9dec7");
    ci.setSize(1097);

    schemaConfig.getDataResourceService().create(dataResource, "SELF");
//    dataResource = dataResourceDao.save(dataResource);
    ci = contentInformationDao.save(ci);

    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(dataResource.getId() + "/1");
    schemaRecord.setVersion(1L);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    schemaRecord.setSchemaDocumentUri(ci.getContentUri());
    schemaRecord.setDocumentHash(ci.getHash());
    schemaRecordDao.save(schemaRecord);

    File dcFile = new File("/tmp/schema_dc.xsd");
    if (!dcFile.exists()) {
      try (FileOutputStream fout = new FileOutputStream(dcFile)) {
        fout.write(KIT_SCHEMA.getBytes());
        fout.flush();
      }
    }
  }

  private void ingestSchemaWithVersion(String schemaId, long version) throws Exception {
    DataResource record = createDataResource4Schema(schemaId);
    setComment(record, COMMENT + version);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    byte[] schemaContent = null;
    switch ((int) version) {
      case 1:
        schemaContent = SCHEMA_V1.getBytes();
        break;
      case 2:
        schemaContent = SCHEMA_V2.getBytes();
        break;
      case 3:
        schemaContent = SCHEMA_V3.getBytes();
        break;
      default:
        Assert.fail("Unknown version: '" + version + "'");
    }
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", schemaContent);
    MvcResult result;
    if (version > 1) {
      // Read ETag
      result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String body = result.getResponse().getContentAsString();

      DataResource oldRecord = mapper.readValue(body, DataResource.class);
      setComment(oldRecord, COMMENT + version);
      recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(oldRecord).getBytes());
      String etag = result.getResponse().getHeader("ETag");
      result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
              file(recordFile).
              file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    } else {
      result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
              file(recordFile).
              file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    }
    String body = result.getResponse().getContentAsString();

    record = mapper.readValue(body, DataResource.class);
    Long versionAfter = Long.parseLong(record.getVersion());
    Assert.assertEquals("Wrong version created!", version, (long) versionAfter);

  }

  private void ingestNewSchemaRecord(String schemaId, long version) throws Exception {
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    DataResource record = mapper.readValue(body, DataResource.class);
    setComment(record, COMMENT + version);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
  }

  public static DataResource createDataResource4JsonSchema(String id) {
    DataResource record = createDataResource4Schema(id);
    record.setResourceType(ResourceType.createResourceType(MetadataSchemaRecord.SCHEMA_TYPE.JSON + DataResourceRecordUtil.SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    record.getFormats().clear();
    record.getFormats().add(MediaType.APPLICATION_JSON.toString());

    return record;
  }

  public static DataResource createDataResource4XmlSchema(String id) {
    return createDataResource4Schema(id);
  }

  public static DataResource createDataResource4Schema(String id) {
    DataResource record = new DataResource();
    record.setId(id);
    // mandatory element title has to be set
    setTitle(record, id);
    // the following fields are optional
    setComment(record, COMMENT);
    setDefinition(record, DEFINITION);
    record.setResourceType(ResourceType.createResourceType(MetadataSchemaRecord.SCHEMA_TYPE.XML + DataResourceRecordUtil.SCHEMA_SUFFIX, ResourceType.TYPE_GENERAL.MODEL));
    record.getFormats().add(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    return record;
  }

  public static DataResource createDataResource4JsonDocument(String id, String schemaId) {
    return createDataResource4Document(id, schemaId, null, DataResourceRecordUtil.JSON_METADATA_TYPE);
  }

  public static DataResource createDataResource4XmlDocument(String id, String schemaId) {
    return createDataResource4Document(id, schemaId, null, DataResourceRecordUtil.XML_METADATA_TYPE);
  }

  public static DataResource createDataResource4Document(String id, String schemaId) {
    return createDataResource4Document(id, schemaId, null, DataResourceRecordUtil.XML_METADATA_TYPE);
  }

  public static DataResource createDataResource4JsonDocument(String id, String schemaId, String version) {
    return createDataResource4Document(id, schemaId, version, DataResourceRecordUtil.JSON_METADATA_TYPE);
  }

  public static DataResource createDataResource4XmlDocument(String id, String schemaId, String version) {
    return createDataResource4Document(id, schemaId, version, DataResourceRecordUtil.XML_METADATA_TYPE);
  }

  public static DataResource createDataResource4Document(String id, String schemaId, String version) {
    return createDataResource4Document(id, schemaId, version, DataResourceRecordUtil.XML_METADATA_TYPE);
  }

  public static DataResource createDataResource4Document(String id, String schemaId, String version, String metadataType) {
    DataResource record = new DataResource();
    record.setId(id);
    record.getAlternateIdentifiers().add(Identifier.factoryInternalIdentifier(id));
    // mandatory element title has to be set
    setTitle(record, id);
    record.setResourceType(ResourceType.createResourceType(metadataType, ResourceType.TYPE_GENERAL.MODEL));

    RelatedIdentifier relatedResource = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, RELATED_RESOURCE_STRING, null, null);
    relatedResource.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    record.getRelatedIdentifiers().add(relatedResource);
    relatedResource = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_SCHEMA_TYPE, schemaId, null, null);
    if ((schemaId != null) && schemaId.startsWith("http")) {
      relatedResource.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    } else {
      relatedResource.setIdentifierType(Identifier.IDENTIFIER_TYPE.INTERNAL);
      if (version != null) {
        relatedResource.setValue(schemaId + DataResourceRecordUtil.SCHEMA_VERSION_SEPARATOR + version);
      }
    }
    record.getRelatedIdentifiers().add(relatedResource);
    if (metadataType.contains("XML")) {
      record.getFormats().add(MediaType.APPLICATION_XML.toString());
    } else {
      record.getFormats().add(MediaType.APPLICATION_JSON.toString());
    }
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    return record;
  }
  
  public static void setRelatedResource(DataResource dataResource, String relatedResource) {
    RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    if (relatedIdentifier != null) { 
      relatedIdentifier.setValue(relatedResource);
    } else {
       relatedIdentifier = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, relatedResource, null, null);
    dataResource.getRelatedIdentifiers().add(relatedIdentifier);
    }
    relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
 
  }
  public static void setRelatedSchema(DataResource dataResource, String relatedSchema) {
    RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(dataResource, DataResourceRecordUtil.RELATED_SCHEMA_TYPE);
    if (relatedIdentifier != null) { 
      relatedIdentifier.setValue(relatedSchema);
    } else {
       relatedIdentifier = RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_SCHEMA_TYPE, relatedSchema, null, null);
    dataResource.getRelatedIdentifiers().add(relatedIdentifier);
    }
    if (relatedSchema.startsWith("http"))   {
      relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    } else {
      relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.INTERNAL);
    }
 
  }

  private String createKitMetadataRecord(String schemaId) throws Exception {
    String documentId = "kit";
    DataResource record = createDataResource4Document(documentId, schemaId);

    ObjectMapper mapper = new ObjectMapper();
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).
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

  /**
   * Update schema in MetaStore as user 'test_user'. If schema already exists
   * and noUpdate is false update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @param noUpdate Only ingest or do update also
   * @return
   * @throws Exception
   */
  public static String ingestOrUpdateXmlSchemaRecord(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    String locationUri = null;
    jwtSecret = (jwtSecret == null) ? "jwtSecret" : jwtSecret;
    String userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken("test_user", RepoUserRole.USER).
            addSimpleClaim("email", "any@example.org").
            addSimpleClaim("orcid", "0000-0001-2345-6789").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(jwtSecret);
    DataResource record = createDataResource4XmlSchema(schemaId);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));
    record.setAcls(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile;
    MockMultipartFile schemaFile;
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", schemaContent.getBytes());
    // Test if schema is already registered.
    MvcResult result = mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andReturn();
    if (result.getResponse().getStatus() != HttpStatus.OK.value()) {

      result = mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
              file(recordFile).
              file(schemaFile).
              header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
              andDo(print()).andExpect(expectedStatus).andReturn();
      if (result.getResponse().getStatus() == HttpStatus.CREATED.value()) {
        locationUri = result.getResponse().getHeader("Location");
      }
    } else {
      if (update) {
        String etag = result.getResponse().getHeader("ETag");
        String body = result.getResponse().getContentAsString();
        record = mapper.readValue(body, DataResource.class);
        recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
        // Update metadata document
        MockHttpServletRequestBuilder header = MockMvcRequestBuilders.
                multipart(API_SCHEMA_PATH + schemaId).
                file(recordFile).
                file(schemaFile).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
                header("If-Match", etag).
                with(putMultipart());
        result = mockMvc.perform(header).
                andDo(print()).
                andExpect(expectedStatus).
                andReturn();
        if (result.getResponse().getStatus() == HttpStatus.OK.value()) {
          locationUri = result.getResponse().getHeader("Location");
        }
      }

    }
    return locationUri;
  }

  private void testForNextVersion(String first, String second) {
    int index = first.lastIndexOf("=");
    int firstVersion = Integer.parseInt(first.substring(index + 1));
    int secondVersion = Integer.parseInt(second.substring(index + 1));
    Assert.assertEquals(firstVersion + 1, secondVersion);
//    Assert.assertEquals(first.substring(0, index), second.substring(0, index));
  }

  public static boolean isSameSetOfAclEntries(Set<AclEntry> firstSet, Set<AclEntry> secondSet) {
    boolean isSameSet = false;
    if (firstSet.size() == secondSet.size()) {
      isSameSet = true;
      for (AclEntry item : firstSet) {
        if (!isPartOfAclEntries(item, secondSet)) {
          isSameSet = false;
          break;
        }
      }
    }
    return isSameSet;
  }

  public static boolean isPartOfAclEntries(AclEntry entry, Set<AclEntry> allEntries) {
    boolean isPart = false;
    for (AclEntry item : allEntries) {
      if (item.getSid().equals(entry.getSid()) && item.getPermission().equals(entry.getPermission())) {
        isPart = true;
        break;
      }
    }
    return isPart;
  }

  public static void validateDataResources(String first, String second) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    DataResource firstRecord = mapper.readValue(first, DataResource.class);
    DataResource secondRecord = mapper.readValue(second, DataResource.class);
    validateDataResources(firstRecord, secondRecord);
  }

  public static void validateDataResources(DataResource first, DataResource second) {
    validateDataResources(first, second, false);
  }

  public static void validateDataResources(DataResource first, DataResource second, boolean update) {
    if (first == second) {
      return;
    }
    if (first.equals(second)) {
      return;
    }
    Assert.assertEquals(first.getId(), second.getId());
    Assert.assertEquals(first.getEmbargoDate(), second.getEmbargoDate());
    Assert.assertEquals(first.getIdentifier().getValue(), second.getIdentifier().getValue());
    Assert.assertEquals(first.getIdentifier().getIdentifierType(), second.getIdentifier().getIdentifierType());
    Assert.assertEquals(first.getLanguage(), second.getLanguage());
    if (update) {
      Assert.assertTrue(first.getLastUpdate().isBefore(second.getLastUpdate()));
    } else {
      Assert.assertEquals(first.getLastUpdate(), second.getLastUpdate());
    }
    Assert.assertEquals(first.getPublicationYear(), second.getPublicationYear());
    Assert.assertEquals(first.getPublisher(), second.getPublisher());
    Assert.assertEquals(first.getResourceType().getValue(), second.getResourceType().getValue());
    Assert.assertEquals(first.getResourceType().getTypeGeneral(), second.getResourceType().getTypeGeneral());
    Assert.assertEquals(first.getState(), second.getState());
    Assert.assertEquals(first.getVersion(), second.getVersion());
    validateSets(first.getAcls(), second.getAcls());
    validateIdentifierSets(first.getAlternateIdentifiers(), second.getAlternateIdentifiers());
    validateContributors(first.getContributors(), second.getContributors());
    validateCreators(first.getCreators(), second.getCreators());
    if (update) {
      validateCreateDates(first.getDates(), second.getDates());
      validateUpdateDates(first.getDates(), second.getDates());
    } else {
      validateDates(first.getDates(), second.getDates());
    }
    validateDescriptions(first.getDescriptions(), second.getDescriptions());
    validateStrings(
            first.getFormats(), second.getFormats());
    validateRights(first.getRights(), second.getRights());
    validateStrings(first.getSizes(), second.getSizes());
    validateTitles(first.getTitles(), second.getTitles());
    // not used yet
    //first.getFundingReferences();
    first.getGeoLocations();
    first.getRelatedIdentifiers();
    first.getSubjects();
  }

  public static void validateSets(Set<AclEntry> first, Set<AclEntry> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<AclEntry> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (AclEntry item : first) {
      identical = false;
      for (AclEntry item2 : copy) {
        identical = (item.getPermission() == item2.getPermission())
                && (item.getSid().equals(item2.getSid()));
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateIdentifierSets(Set<Identifier> first, Set<Identifier> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Identifier> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Identifier item : first) {
      identical = false;
      for (Identifier item2 : copy) {
        identical = (item.getIdentifierType() == item2.getIdentifierType())
                && (item.getValue().equals(item2.getValue()));
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateRelatedIdentifierSets(Set<RelatedIdentifier> first, Set<RelatedIdentifier> second) {
    // Check for provenance first...
    RelatedIdentifier provenanceFirstRelatedIdentifier = null;
    RelatedIdentifier provenanceSecondRelatedIdentifier = null;
    Set<RelatedIdentifier> copyFirst = new HashSet<>();
    Set<RelatedIdentifier> copySecond = new HashSet<>();
    copyFirst.addAll(first);
    copySecond.addAll(second);
    for (RelatedIdentifier item : copyFirst) {
      if (item.getRelationType() == DataResourceRecordUtil.RELATED_NEW_VERSION_OF) {
        provenanceFirstRelatedIdentifier = item;
        Assert.assertEquals(DataResourceRecordUtil.RELATED_NEW_VERSION_OF, item.getRelationType());
        Assert.assertEquals(IDENTIFIER_TYPE.URL, item.getIdentifierType());
        break;
      }
    }
    for (RelatedIdentifier item : copySecond) {
      if (item.getRelationType() == DataResourceRecordUtil.RELATED_NEW_VERSION_OF) {
       Assert.assertEquals(DataResourceRecordUtil.RELATED_NEW_VERSION_OF, item.getRelationType());
      Assert.assertEquals(IDENTIFIER_TYPE.URL, item.getIdentifierType());
       provenanceSecondRelatedIdentifier = item;
       if (provenanceFirstRelatedIdentifier != null) {
         int indexOfDifference = StringUtils.indexOfDifference(provenanceFirstRelatedIdentifier.getValue(), provenanceSecondRelatedIdentifier.getValue());
         Assert.assertTrue("Provenance differ to much : " + provenanceFirstRelatedIdentifier + " <-> " + provenanceSecondRelatedIdentifier, indexOfDifference > "http://localhost:41112/metastore".length());
       }
        break;
      }
    }
    if (provenanceFirstRelatedIdentifier != null) {
      copyFirst.remove(provenanceFirstRelatedIdentifier);
    }
    if (provenanceSecondRelatedIdentifier != null) {
      copySecond.remove(provenanceSecondRelatedIdentifier);
    }
 
    Assert.assertEquals(copyFirst.size(), copySecond.size());
    boolean identical;
    for (RelatedIdentifier item : copyFirst) {
      identical = false;
      for (RelatedIdentifier item2 : copySecond) {
        identical = (item.getIdentifierType() == item2.getIdentifierType())
                && ((item.getValue() == item2.getValue()) || item.getValue().equals(item2.getValue()))
                && (item.getRelationType() == item2.getRelationType())
                && ((item.getScheme() == item2.getScheme()) || (item.getScheme().getSchemeId().equals(item2.getScheme().getSchemeId())
                && item.getScheme().getSchemeUri().equals(item2.getScheme().getSchemeUri())));
        if (identical) {
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateContributors(Set<Contributor> first, Set<Contributor> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Contributor> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Contributor item : first) {
      identical = false;
      for (Contributor item2 : copy) {
        identical = (item.getContributionType() == item2.getContributionType())
                && (item.getUser().getFamilyName().equals(item2.getUser().getFamilyName()))
                && (item.getUser().getGivenName().equals(item2.getUser().getGivenName()));
        if (identical) {
          copy.remove(item2);
        }
        break;
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateCreators(Set<Agent> first, Set<Agent> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Agent> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Agent item : first) {
      identical = false;
      for (Agent item2 : copy) {
        identical = ((item.getFamilyName() == item2.getFamilyName()) || item.getFamilyName().equals(item2.getFamilyName()))
                && ((item.getGivenName() == item2.getGivenName()) || item.getGivenName().equals(item2.getGivenName()))
                && validateStrings(item.getAffiliations(), item2.getAffiliations());
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateDates(Set<Date> first, Set<Date> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Date> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Date item : first) {
      identical = false;
      for (Date item2 : copy) {
        identical = (item.getType() == item2.getType())
                && (item.getValue().equals(item2.getValue()));
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateCreateDates(Set<Date> first, Set<Date> second) {
    if (first == second) {
      return;
    }
    boolean identical;
    for (Date item : first) {
      if (item.getType() == Date.DATE_TYPE.CREATED) {
        identical = false;
        for (Date item2 : second) {
          identical = (item.getType() == item2.getType())
                  && (item.getValue().equals(item2.getValue()));
          if (identical) {
            break;
          }
        }
        Assert.assertTrue(identical);
      }
    }
  }

  public static void validateUpdateDates(Set<Date> first, Set<Date> second) {
    if (first == second) {
      return;
    }
    boolean isBefore;
    for (Date item : first) {
      if (item.getType() == Date.DATE_TYPE.UPDATED) {
        isBefore = false;
        for (Date item2 : second) {
          isBefore = (item.getType() == item2.getType())
                  && (item.getValue().isBefore(item2.getValue()));
          if (isBefore) {
            break;
          }
        }
        Assert.assertTrue(isBefore);
      }
    }
  }

  public static void validateDescriptions(Set<Description> first, Set<Description> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Description> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Description item : first) {
      identical = false;
      for (Description item2 : copy) {
        identical = ((item.getDescription() == item2.getDescription()) || item.getDescription().equals(item2.getDescription()))
                && ((item.getLang() == item2.getLang()) || item.getLang().equals(item2.getLang()))
                && (item.getType() == item2.getType());
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateDescriptions(DataResource record,
          String label,
          String definition,
          String comment) {
    if (record == null
            && label == null
            && definition == null
            && comment == null) {
      return;
    }
    boolean titleValidated = false;
    for (Title item : record.getTitles()) {
      if ((item.getValue() == label) || item.getTitleType() == null) {
        if (item.getValue().equals(label)) {
          titleValidated = true;
          break;
        }
      }
    }
    Assert.assertTrue(titleValidated);
    boolean definitionValidated = false;
    boolean commentValidated = false;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.TECHNICAL_INFO) {
        if ((item.getDescription() == definition) || item.getDescription().equals(definition)) {
          definitionValidated = true;
        }
      }
      if (item.getType() == Description.TYPE.OTHER) {
        if ((item.getDescription() == comment) || item.getDescription().equals(comment)) {
          commentValidated = true;
        }
      }
    }
    Assert.assertTrue(definitionValidated);
    Assert.assertTrue(commentValidated);
  }

  private static boolean validateStrings(Set<String> first, Set<String> second) {
    if (first == second) {
      return true;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<String> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (String item : first) {
      identical = false;
      for (String item2 : copy) {
        identical = item.equals(item2);
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
    return true;
  }

  public static void validateRights(Set<Scheme> first, Set<Scheme> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Scheme> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Scheme item : first) {
      identical = false;
      for (Scheme item2 : copy) {
        identical = (item.getSchemeId().equals(item2.getSchemeId()))
                && (item.getSchemeUri().equals(item2.getSchemeUri()));
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static void validateTitles(Set<Title> first, Set<Title> second) {
    if (first == second) {
      return;
    }
    Assert.assertEquals(first.size(), second.size());
    Set<Title> copy = new HashSet<>();
    copy.addAll(second);
    boolean identical;
    for (Title item : first) {
      identical = false;
      for (Title item2 : copy) {
        identical = (item.getTitleType() == item2.getTitleType())
                && ((item.getValue() == item2.getValue()) || item.getValue().equals(item2.getValue()));
        if (identical) {
          copy.remove(item2);
          break;
        }
      }
      Assert.assertTrue(identical);
    }
  }

  public static String getTitle(DataResource record) {
    String returnValue = null;
    for (Title item : record.getTitles()) {
      if (item.getTitleType() == null) {
        returnValue = item.getValue();
        break;
      }
    }
    return returnValue;
  }

  public static void setTitle(DataResource record, String title) {
    boolean addTitle = true;
    for (Title item : record.getTitles()) {
      if (item.getTitleType() == null) {
        item.setValue(title);
        addTitle = false;
        break;
      }
    }
    if (addTitle) {
      record.getTitles().add(Title.factoryTitle(title, null));
    }
  }

  public static String getDefinition(DataResource record) {
    String returnValue = null;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.TECHNICAL_INFO) {
        returnValue = item.getDescription();
        break;
      }
    }
    return returnValue;
  }

  public static void setDefinition(DataResource record, String definition) {
    boolean addDefinition = true;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.TECHNICAL_INFO) {
        item.setDescription(definition);
        addDefinition = false;
        break;
      }
    }
    if (addDefinition) {
      record.getDescriptions().add(Description.factoryDescription(definition, Description.TYPE.TECHNICAL_INFO));
    }
  }

  public static String getLabel(DataResource record) {
    String returnValue = null;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.ABSTRACT) {
        returnValue = item.getDescription();
        break;
      }
    }
    return returnValue;
  }

  public static void setLabel(DataResource record, String label) {
    boolean addDefinition = true;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.ABSTRACT) {
        item.setDescription(label);
        addDefinition = false;
        break;
      }
    }
    if (addDefinition) {
      record.getDescriptions().add(Description.factoryDescription(label, Description.TYPE.ABSTRACT));
    }
  }

  public static String getRights(DataResource record) {
    String returnValue = null;
    for (Scheme item : record.getRights()) {
        returnValue = item.getSchemeId();
        break;
    }
    return returnValue;
  }

  public static void setRights(DataResource record, String schemeId) {
    boolean addDefinition = true;
    for (Scheme item : record.getRights()) {
      if (item.getSchemeId().equals(schemeId)) {
        addDefinition = false;
        break;
      }
    }
    if (addDefinition) {
      Scheme scheme = Scheme.factoryScheme(schemeId, "https://spdx.org/licenses/" + schemeId + ".html");
      record.getRights().add(scheme);
    }
  }

  public static String getComment(DataResource record) {
    String returnValue = null;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.OTHER) {
        returnValue = item.getDescription();
        break;
      }
    }
    return returnValue;
  }

  public static void setComment(DataResource record, String comment) {
    boolean addComment = true;
    for (Description item : record.getDescriptions()) {
      if (item.getType() == Description.TYPE.OTHER) {
        item.setDescription(comment);
        addComment = false;
        break;
      }
    }
    if (addComment) {
      record.getDescriptions().add(Description.factoryDescription(comment, Description.TYPE.OTHER));
    }
  }
}
