/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
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
@TestPropertySource(properties = {"server.port=41409"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_schema_xsd;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/schematest/schema"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SchemaRegistryControllerTest {
  
  private static final String API_BASE_PATH = "/api/v1";
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
    MetadataSchemaRecord record = createMetadataRecord("my_dc");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithAlternateEndpoint() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_alternate");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(ALTERNATE_API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithCapitalLetter() throws Exception {
    String schemaIDWithCapitalLetters = "myFirstTest";
    MetadataSchemaRecord record = createMetadataRecord(schemaIDWithCapitalLetters);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    // No longer valid, as capital letters are no longer allowed.
/*    MetadataSchemaRecord ms_record = mapper.readValue(result.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(record.getType(), ms_record.getType());
    Assert.assertEquals(record.getMimeType(), ms_record.getMimeType());
    Assert.assertEquals(record.getSchemaId(), ms_record.getSchemaId());
    Assert.assertNotEquals(schemaIDWithCapitalLetters, ms_record.getSchemaId());
    Assert.assertEquals(schemaIDWithCapitalLetters.toLowerCase(), ms_record.getSchemaId());
    // Test getting record and schema
    result = this.mockMvc.perform(get("/api/v1/schemas/" + schemaIDWithCapitalLetters.toLowerCase()).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ms_record = mapper.readValue(result.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(record.getType(), ms_record.getType());
    Assert.assertEquals(record.getMimeType(), ms_record.getMimeType());
    Assert.assertEquals(record.getSchemaId(), ms_record.getSchemaId());
    Assert.assertNotEquals(schemaIDWithCapitalLetters, ms_record.getSchemaId());
    Assert.assertEquals(schemaIDWithCapitalLetters.toLowerCase(), ms_record.getSchemaId());

    result = this.mockMvc.perform(get("/api/v1/schemas/" + schemaIDWithCapitalLetters).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ms_record = mapper.readValue(result.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(record.getType(), ms_record.getType());
    Assert.assertEquals(record.getMimeType(), ms_record.getMimeType());
    Assert.assertEquals(record.getSchemaId(), ms_record.getSchemaId());
    Assert.assertNotEquals(schemaIDWithCapitalLetters, ms_record.getSchemaId());
    Assert.assertEquals(schemaIDWithCapitalLetters.toLowerCase(), ms_record.getSchemaId());
*/
  }

  @Test
  public void testCreateRegisterSchemaRecordWithSameIdButCapitalLetter() throws Exception {
    String schemaIDWithCapitalLetters = "mySecondTest";
    MetadataSchemaRecord record = createMetadataRecord(schemaIDWithCapitalLetters.toLowerCase(Locale.getDefault()));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    MetadataSchemaRecord ms_record = mapper.readValue(result.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(record.getType(), ms_record.getType());
    Assert.assertEquals(record.getMimeType(), ms_record.getMimeType());
    Assert.assertEquals(record.getSchemaId(), ms_record.getSchemaId());
    Assert.assertNotEquals(schemaIDWithCapitalLetters, ms_record.getSchemaId());

    record.setSchemaId("MySecondTest");
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithIdentifierWithoutType() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_without_type");
    ResourceIdentifier ri = new ResourceIdentifier();
    ri.setIdentifier("any");
    record.setPid(ri);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutMimeType() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_2");
    record.setMimeType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutContentType() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_3");
    record.setMimeType(null);
    record.setType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", null, KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithLocationUri() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_new");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=1")).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String content = result.getResponse().getContentAsString();
    // Location Uri should point to API v2. Therefor accept header is not valid.
    this.mockMvc.perform(get(locationUri).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotAcceptable());
  }

  @Test
  public void testCreateInvalidSchemaRecord() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord(INVALID_SCHEMA_ID);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithEmptyAclSid() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_empty_sid");
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(null, PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
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
    MetadataSchemaRecord record = createMetadataRecord("my_dc_from_extern");
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
    MetadataSchemaRecord record = createMetadataRecord("my_dcExt");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
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
    MetadataSchemaRecord record = createMetadataRecord("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordGuessingType() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc");
    record.setType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    record = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(MetadataSchemaRecord.SCHEMA_TYPE.XML, record.getType());
  }

  @Test
  public void testCreateSchemaRecordGuessingTypeFails() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc");
    record.setType(null);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "?".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();

  }

  @Test
  public void testCreateSchemaRecordWithBadSchema() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("bad_schema");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithEmptySchema() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("empty_schema");
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
    MetadataSchemaRecord record = createMetadataRecord("without_schema");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithBadRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataSchemaRecord record = createMetadataRecord(null);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSchemaRecord() throws Exception {
    MetadataSchemaRecord record = createMetadataRecord("my_dc_with_version");
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataSchemaRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(result.getSchemaVersion(), Long.valueOf(1L));
    // Can't create same resource twice -> Conflict
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
  }

  @Test
  public void testGetSchemaRecordByIdWithoutVersion() throws Exception {
    ingestSchemaRecord();

    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH + "dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(SCHEMA_ID, result.getSchemaId());
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/dc.xsd", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithVersion() throws Exception {
    ingestSchemaRecord();

    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH + "dc").param("version", "1").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(SCHEMA_ID, result.getSchemaId());
    Assert.assertNotEquals("file:///tmp/dc.xsd", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidId() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + "cd").
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid schema ID!", result.getResponse().getContentAsString().contains("Schema document with ID 'cd' doesn't exist!"));
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidVersion() throws Exception {
    String schemaId = SCHEMA_ID;
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            param("version", "13").
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isNotFound()).
            andReturn();
    Assert.assertTrue("Try to access invalid version!", result.getResponse().getContentAsString().contains("Version '13' of ID '" + schemaId + "' doesn't exist!"));
  }

  @Test
  public void testFindRecordsBySchemaIdWithAlternateEndpoint() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get(ALTERNATE_API_SCHEMA_PATH).param("schemaId", SCHEMA_ID).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertTrue(result.length > 0);
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).param("schemaId", SCHEMA_ID).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertTrue(result.length > 0);
  }

  @Test
  public void testFindRecordsByMimeType() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).param("mimeType", MediaType.APPLICATION_XML.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidMimeType() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).param("mimeType", "invalid")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownSchemaId() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH).
            param("schemaId", "schema_id_which_is_not_known")).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + "dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA, content);
  }

  @Test
  public void testGetSchemaDocumentWithMissingSchemaFile() throws Exception {
    ingestSchemaRecord();
    String contentUri = contentInformationDao.findAll(PageRequest.of(0, 2)).getContent().get(0).getContentUri();
    //delete schema file
    URI uri = new URI(contentUri);
    Files.delete(Paths.get(uri));

    this.mockMvc.perform(get(API_SCHEMA_PATH + "dc")).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testValidate() throws Exception {

    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownVersion() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate?version=666").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateKnownVersion() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate?version=1").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownSchemaId() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + INVALID_SCHEMA_ID + "/validate").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testValidateWithInvalidDocument() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate").file("document", INVALID_KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithEmptyDocument() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate").file("document", "".getBytes())).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutDocument() throws Exception {
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate")).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutValidator() throws Exception {
    ingestSchemaRecord();

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithMissingSchemaFile() throws Exception {
    ingestSchemaRecord();
    // Get location of schema file.
    String contentUri = contentInformationDao.findAll(PageRequest.of(0, 2)).getContent().get(0).getContentUri();
    //delete schema file
    URI uri = new URI(contentUri);
    Files.delete(Paths.get(uri));

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc/validate").file("document", KIT_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  // Update only record
  @Test
  public void testUpdateRecord() throws Exception {
    String schemaId = "updateRecord".toLowerCase(Locale.getDefault());
    String newComment = "new comment";
    String newLabel = "label changed!";
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    Assert.assertEquals(DEFINITION, record.getDefinition());
    Assert.assertEquals(LABEL, record.getLabel());
    Assert.assertEquals(COMMENT, record.getComment());
    record.setDefinition(null);
    record.setComment(newComment);
    record.setLabel(newLabel);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type is not allowed to be changed.
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion(), (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertEquals("Check label: ", record.getLabel(), record2.getLabel());
    Assert.assertEquals("Check comment: ", record.getComment(), record2.getComment());
    Assert.assertEquals("Check definition: ", record.getDefinition(), record2.getDefinition());
    Assert.assertEquals("Check label: ", newLabel, record2.getLabel());
    Assert.assertEquals("Check comment: ", newComment, record2.getComment());
    Assert.assertNull("Check definition for 'null'", record2.getDefinition());
  }

  // Update only record
  @Test
  public void testUpdateRecordRemovingLabel() throws Exception {
    String schemaId = "updateRecord".toLowerCase(Locale.getDefault());
    String newComment = "new comment";
    String newLabel = "label changed!";
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    Assert.assertEquals(DEFINITION, record.getDefinition());
    Assert.assertEquals(LABEL, record.getLabel());
    Assert.assertEquals(COMMENT, record.getComment());
    record.setDefinition(null);
    record.setComment(null);
    record.setLabel(null);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type is not allowed to be changed.
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion(), (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertEquals("Check label: ", record.getLabel(), record2.getLabel());
    Assert.assertEquals("Check comment: ", record.getComment(), record2.getComment());
    Assert.assertEquals("Check definition: ", record.getDefinition(), record2.getComment());
    Assert.assertNull("Check label: ", record2.getLabel());
    Assert.assertNull("Check comment: ", record2.getComment());
    Assert.assertNull("Check definition for 'null'", record2.getDefinition());
  }

  @Test
  public void testUpdateRecordIgnoreACL() throws Exception {
    String schemaId = "updateRecord".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord oldRecord = mapper.readValue(body, MetadataSchemaRecord.class);
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    // Set all ACL to WRITE
    for (AclEntry entry : record.getAcl()) {
      entry.setPermission(PERMISSION.WRITE);
    }
    String mimeTypeBefore = record.getMimeType();
    String definitionBefore = record.getDefinition();
    String labelBefore = record.getLabel();
    String commentBefore = record.getComment();
    record.setDefinition("");
    record.setComment("new comment");
    record.setLabel("label changed");
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type is not allowed to be changed.
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion(), (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(isSameSetOfAclEntries(record.getAcl(), record2.getAcl()));
      Assert.assertFalse(isSameSetOfAclEntries(oldRecord.getAcl(), record.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertEquals("Check label: ", record.getLabel(), record2.getLabel());
    Assert.assertEquals("Check comment: ", record.getComment(), record2.getComment());
    Assert.assertNotEquals("Check label: ", labelBefore, record2.getLabel());
    Assert.assertNotEquals("Check comment: ", commentBefore, record2.getComment());
    Assert.assertNull("Check definition for 'null'", record2.getDefinition());
  }

  @Test
  public void testUpdateRecordWithIgnoringInvalidSetting4Xml() throws Exception {
    String schemaId = "updateMimetypeOfRecord".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    // Should not fail as invalid mimetype is not used validating schema!
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*"));
  }

  @Test
  public void testUpdateRecordWithInvalidSetting4Xml() throws Exception {
    String schemaId = "updateTypeOfRecord".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
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
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(record.getMimeType(), record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion(), (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordAndDocument() throws Exception {
    String schemaId = "updateRecordAndDocument".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    record.setMimeType(MediaType.APPLICATION_JSON_VALUE);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNull(record2.getLicenseUri());
    Assert.assertEquals(record.getLicenseUri(), record2.getLicenseUri());
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type depends on document and can't be changed
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(record.getSchemaVersion() + 1L, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
  }

  @Test
  public void testUpdateRecordAndDocumentWithLicense() throws Exception {
    String schemaId = "updateRecordAndDocumentWithLicense".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    record.setLicenseUri(APACHE_2_LICENSE);
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
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();
    etag = result.getResponse().getHeader("ETag");

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNotNull(record2.getLicenseUri());
    Assert.assertEquals(record.getLicenseUri(), record2.getLicenseUri());
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type depends on document and can't be changed
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(record.getSchemaVersion() + 1L, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
    // Remove license
    record2.setLicenseUri(null);
    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record2).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record3 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNull(record3.getLicenseUri());
    Assert.assertEquals(record2.getMimeType(), record3.getMimeType());//mime type was changed by update
    Assert.assertEquals(record2.getCreatedAt(), record3.getCreatedAt());
    Assert.assertEquals(record2.getSchemaDocumentUri(), record3.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertEquals(record2.getSchemaHash(), record3.getSchemaHash());
    Assert.assertEquals(record2.getSchemaId(), record3.getSchemaId());
    Assert.assertEquals(record.getSchemaVersion() + 1L, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record2.getAcl().containsAll(record3.getAcl()));
    }
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordAndDocumentWithWrongVersion() throws Exception {
    String schemaId = "updateRecordAndDocumentWithWrongVersion".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    record.setSchemaVersion(0L);
    String mimeTypeBefore = record.getMimeType();
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type depends on document and can't be changed
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(2L, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
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
    ingestSchemaRecord(schemaId);
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(schemaFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).
            andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(record.getMimeType(), record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertNotEquals(record.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(record.getSchemaVersion() + 1L, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(KIT_SCHEMA_V2, content);
  }

  @Test
  public void testUpdateRecordWithSmallChangesInDocument() throws Exception {
    String schemaId = "updateRecordWithSmallChanges".toLowerCase(Locale.getDefault());
    MetadataSchemaRecord schemaRecord = new MetadataSchemaRecord();
    schemaRecord.setSchemaId(schemaId);
    schemaRecord.setSchemaVersion(1L);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", CreateSchemaUtil.XML_SCHEMA_V1.getBytes());
////
    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
//    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", CreateSchemaUtil.XML_SCHEMA_V1_TYPO.getBytes());
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(schemaFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(redirectedUrlPattern("http://*:*/**/" + schemaRecord.getSchemaId() + "?version=*")).
            andReturn();
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    String schemaId = "updateWithoutGet".toLowerCase(Locale.getDefault());
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(schemaId);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    MetadataSchemaRecord record1 = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record1.getMimeType();
    record1.setMimeType(MediaType.APPLICATION_JSON.toString());
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record1).getBytes());
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(mimeTypeBefore, record2.getMimeType());//mime type depends on document and can't be changed
    Assert.assertEquals(record1.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record1.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record1.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record1.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record1.getSchemaVersion(), (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record1.getAcl() != null) {
      Assert.assertTrue(record1.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record1.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + "dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc").
            file(recordFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + "dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + "dc").
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutBody() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + "dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(put(API_SCHEMA_PATH + "dc").header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content("{}")).andDo(print()).andExpect(status().isUnsupportedMediaType()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithUpdateWithoutChanges() throws Exception {
    // Test with a schema missing schema property.
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("updateWithoutChanges_xsd".toLowerCase(Locale.getDefault()));
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", SCHEMA_V3.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record1 = mapper.readValue(body, MetadataSchemaRecord.class);
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + record.getSchemaId()).
            file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(record1.getMimeType(), record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record1.getCreatedAt(), record2.getCreatedAt());
    // Version shouldn't be updated
    Assert.assertEquals(record1.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record1.getSchemaHash(), record2.getSchemaHash());
    Assert.assertEquals(record1.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record1.getSchemaVersion(), (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record1.getAcl() != null) {
      Assert.assertTrue(record1.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record1.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testDeleteSchemaRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    String schemaId = "testDelete".toLowerCase(Locale.getDefault());
    ingestSchemaRecord(schemaId);

    // Get a list of all records
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    int noOfRecords = mapper.readValue(result.getResponse().getContentAsString(), MetadataSchemaRecord[].class).length;

    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    // create should return conflict
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(schemaId);
    schemaRecord.setVersion(1L);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
    //delete second time // should be really deleted -> gone
    result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");
    this.mockMvc.perform(delete(API_SCHEMA_PATH + schemaId).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();

    //try to create after deletion (Should return HTTP GONE)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isGone()).andReturn();

    // List of records should be still the same size 
    result = this.mockMvc.perform(get(API_SCHEMA_PATH).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    int noOfRecordsAfter = mapper.readValue(result.getResponse().getContentAsString(), MetadataSchemaRecord[].class).length;
    Assert.assertEquals("No of records should be still the same!", noOfRecords, noOfRecordsAfter);
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
              .constructCollectionType(List.class, MetadataSchemaRecord.class);
      List<MetadataSchemaRecord> resultList = mapper.readValue(result.getResponse().getContentAsString(), mapCollectionType);
      HashSet<Long> versions = new HashSet<>();
      for (MetadataSchemaRecord item : resultList) {
        versions.add(item.getSchemaVersion());
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
    ingestSchemaRecord(schemaId);
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

  private void ingestSchemaRecord(String schemaId) throws Exception {
    MetadataSchemaRecord schemaRecord = new MetadataSchemaRecord();
    schemaRecord.setSchemaId(schemaId);
    schemaRecord.setLabel(LABEL);
    schemaRecord.setDefinition(DEFINITION);
    schemaRecord.setComment(COMMENT);
    schemaRecord.setSchemaVersion(1L);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", KIT_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  private void ingestSchemaRecord() throws Exception {
    DataResource dataResource = DataResource.factoryNewDataResource(SCHEMA_ID);
    dataResource.getCreators().add(Agent.factoryAgent(null, "SELF"));
    dataResource.getTitles().add(Title.factoryTitle(MediaType.APPLICATION_XML.toString(), Title.TYPE.OTHER));
    dataResource.setPublisher("SELF");
    Instant now = Instant.now();
    dataResource.setPublicationYear(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    dataResource.setResourceType(ResourceType.createResourceType(DataResourceRecordUtil.XML_SCHEMA_TYPE, ResourceType.TYPE_GENERAL.MODEL));
    dataResource.getDates().add(Date.factoryDate(now, Date.DATE_TYPE.CREATED));
    dataResource.getFormats().add(MetadataSchemaRecord.SCHEMA_TYPE.XML.name());
    dataResource.setLastUpdate(now);
    dataResource.setState(DataResource.State.VOLATILE);
    dataResource.setVersion("1");
    Set<AclEntry> aclEntries = dataResource.getAcls();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    Set<Description> descriptions = dataResource.getDescriptions();
    descriptions.add(Description.factoryDescription("other", Description.TYPE.OTHER));
    descriptions.add(Description.factoryDescription("abstract", Description.TYPE.ABSTRACT));
    descriptions.add(Description.factoryDescription("technical info", Description.TYPE.TECHNICAL_INFO));
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

    dataResource = schemaConfig.getDataResourceService().create(dataResource, "SELF");
    ci.setParentResource(dataResource);

    contentInformationDao.save(ci);
    schemaConfig.getContentInformationAuditService().captureAuditInformation(ci, "SELF");

    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(dataResource.getId() + "/1");
    schemaRecord.setVersion(1L);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.valueOf(dataResource.getFormats().iterator().next()));
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
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setComment(COMMENT + version);
    record.setSchemaId(schemaId);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
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
      result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
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

    record = mapper.readValue(body, MetadataSchemaRecord.class);
    Long versionAfter = record.getSchemaVersion();
    Assert.assertEquals("Wrong version created!", version, (long) versionAfter);

  }

  private void ingestNewSchemaRecord(String schemaId, long version) throws Exception {
    MvcResult result = this.mockMvc.perform(get(API_SCHEMA_PATH + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    record.setComment(COMMENT + version);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH + schemaId).
            file(recordFile).
            header("If-Match", etag).
            with(putMultipart())).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
  }
  
  private MetadataSchemaRecord createMetadataRecord(String id) {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(id);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    return record;
  }

  private String createKitMetadataRecord(String schemaId) throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", KIT_DOCUMENT.getBytes());

    MvcResult andReturn = this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    MetadataRecord result = mapper.readValue(andReturn.getResponse().getContentAsString(), MetadataRecord.class);

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

  private void testForNextVersion(String first, String second) {
    int index = first.lastIndexOf("=");
    int firstVersion = Integer.parseInt(first.substring(index + 1));
    int secondVersion = Integer.parseInt(second.substring(index + 1));
    Assert.assertEquals(firstVersion + 1, secondVersion);
    Assert.assertEquals(first.substring(0, index), second.substring(0, index));
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
}
