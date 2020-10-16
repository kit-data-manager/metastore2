/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import edu.kit.datamanager.service.IAuditService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.web.FilterChainProxy;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author Torridity
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/jsontest/schema"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class JsonSchemaRegistryControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/jsontest/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private static final String INVALID_SCHEMA = "invalid_json";
  private final static String JSON_SCHEMA =  "{\n"
          + "    \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n"
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
          + "            \"$id\": \"#/properties/string\",\n"
          + "            \"type\": \"string\",\n"
          + "            \"title\": \"Title\",\n"
          + "            \"description\": \"Title of object.\"\n"
          + "        },\n"
          + "        \"date\": {\n"
          + "            \"$id\": \"#/properties/string\",\n"
          + "            \"type\": \"string\",\n"
          + "            \"format\": \"date\",\n"
          + "            \"title\": \"Date\",\n"
          + "            \"description\": \"Date of object\"\n"
          + "        }\n"
          + "    },\n"
          + "    \"additionalProperties\": false\n"
          + "}";
  
  private final static String JSON_DOCUMENT = "{\"title\":\"any string\",\"date\": \"2020-10-16\"}";
  private final static String INVALID_JSON_DOCUMENT = "{\"title\":\"any string\",\"date\":\"2020-10-16T10:13:24\"}";

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private IAuditService<MetadataSchemaRecord> schemaAuditService;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  @Before
  public void setUp() throws Exception {
    metadataSchemaDao.deleteAll();
    try {
      try ( Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
        walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      }
      Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
      Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .addFilters(springSecurityFilterChain)
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
  }

  @Test
  public void testCreateSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithLocationUri() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json_new");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=1")).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String content = result.getResponse().getContentAsString();

    MvcResult result2 = this.mockMvc.perform(get(locationUri).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content2 = result2.getResponse().getContentAsString();

    Assert.assertEquals(content, content2);
  }

  @Test
  public void testCreateInvalidSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(INVALID_SCHEMA);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }
  // @Test 

  public void testCreateSchemaRecordFromExternal() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.external.domain"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateSchemaRecordUpdateFromExternal() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_jsonExt");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.domain.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile).with(remoteAddr("www.google.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWrongType() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordGuessingType() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    record = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(MetadataSchemaRecord.SCHEMA_TYPE.JSON, record.getType());
  }

  @Test
  public void testCreateSchemaRecordGuessingTypeFails() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "?".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();

  }

  @Test
  public void testCreateSchemaRecordWithBadSchema() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithEmptySchema() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutRecord() throws Exception {
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutSchema() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithBadRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataSchemaRecord record = new MetadataSchemaRecord();
    //schemaId is missing
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_json_with_version");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataSchemaRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(result.getSchemaVersion(), Long.valueOf(1l));

    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    result = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(result.getSchemaVersion(), Long.valueOf(2l));
  }

  @Test
  public void testGetSchemaRecordByIdWithoutVersion() throws Exception {
    createJsonSchema();

    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/json").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals("json", result.getSchemaId());
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/json.json", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithVersion() throws Exception {
    createJsonSchema();

    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/json").param("version", "1").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals("json", result.getSchemaId());
    Assert.assertNotEquals("file:///tmp/json.json", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidId() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(get("/api/v1/schemas/nosj").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidVersion() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(get("/api/v1/schemas/json").param("version", "13").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    createJsonSchema();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/").param("schemaId", "json")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByMimeType() throws Exception {
    createJsonSchema();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/").param("mimeType", MediaType.APPLICATION_JSON.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidMimeType() throws Exception {
    createJsonSchema();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/").param("mimeType", "invalid")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownSchemaId() throws Exception {
    createJsonSchema();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/").param("schemaId", "cd")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    createJsonSchema();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/json")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String jsonSchema = new String(java.nio.file.Files.readAllBytes(Paths.get(URI.create("file:///tmp/json.json"))));

    Assert.assertEquals(jsonSchema, content);
  }

  @Test
  public void testGetSchemaDocumentWithMissingSchemaFile() throws Exception {
    createJsonSchema();

    //delete schema file
    Files.delete(Paths.get("/tmp/json.json"));

    this.mockMvc.perform(get("/api/v1/schemas/json")).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testValidate() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownVersion() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate?version=666").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testValidateKnownVersion() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate?version=1").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownSchemaId() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + INVALID_SCHEMA + "/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testValidateWithInvalidDocument() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate").file("document", INVALID_JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithEmptyDocument() throws Exception {
    createJsonSchema();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate").file("document", "".getBytes())).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutDocument() throws Exception {
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate")).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutValidator() throws Exception {
    createJsonSchema();

    //modify DC schema so that it has no validator
    MetadataSchemaRecord record = metadataSchemaDao.findById("json").get();
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    metadataSchemaDao.save(record);

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithMissingSchemaFile() throws Exception {
    createJsonSchema();

    //delete schema file
    Files.delete(Paths.get("/tmp/json.json"));

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/json/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testUpdateRecord() throws Exception {
    createJsonSchema();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/json").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    record.setMimeType(MediaType.APPLICATION_XML.toString());

    result = this.mockMvc.perform(put("/api/v1/schemas/json").header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=1")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(record.getSchemaVersion(), record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("json");
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    MetadataSchemaRecord record1 = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record1.getMimeType();
    record1.setMimeType(MediaType.APPLICATION_XML.toString());

    result = this.mockMvc.perform(put("/api/v1/schemas/json").header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record1))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record1.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record1.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    Assert.assertEquals(record1.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(record1.getSchemaVersion(), record2.getSchemaVersion());//version is not changing for metadata update
    if (record1.getAcl() != null) {
      Assert.assertTrue(record1.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record1.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    createJsonSchema();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/json").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);

    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    this.mockMvc.perform(put("/api/v1/schemas/json").contentType("application/json").contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    createJsonSchema();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/json").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    this.mockMvc.perform(put("/api/v1/schemas/json").contentType("application/json").header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutBody() throws Exception {
    createJsonSchema();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/json").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(put("/api/v1/schemas/json").header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content("{}")).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testDeleteSchemaRecord() throws Exception {
    createJsonSchema();

    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/json").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/schemas/json").header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    //delete second time
    this.mockMvc.perform(delete("/api/v1/schemas/json")).andDo(print()).andExpect(status().isNoContent()).andReturn();

    //try to create after deletion (Should return HTTP GONE)
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", JSON_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isGone()).andReturn();
  }

  private void createJsonSchema() throws FileNotFoundException, IOException {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setCreatedAt(Instant.now());
    record.setLastUpdate(Instant.now());
    record.setSchemaId("json");
    record.setSchemaVersion(1l);
    record.setMimeType("application/json");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    Set<AclEntry> acl = new HashSet<>();
    AclEntry entry = new AclEntry();
    entry.setSid("SELF");
    entry.setPermission(PERMISSION.WRITE);
    acl.add(entry);
    record.setAcl(acl);
    record.setSchemaDocumentUri("file:///tmp/json.json");
    record = metadataSchemaDao.save(record);
    File jsonFile = new File("/tmp/json.json");
    if (!jsonFile.exists()) {
      try ( FileOutputStream fout = new FileOutputStream(jsonFile)) {
        fout.write(JSON_SCHEMA.getBytes());
        fout.flush();
      }
    }

    schemaAuditService.captureAuditInformation(record, "TEST");
  }

  private static RequestPostProcessor remoteAddr(final String remoteAddr) { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setRemoteAddr(remoteAddr);
      return request;
    };
  }
}
