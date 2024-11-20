/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_filter;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/jsonfilter/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/jsonfilter/metadata"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerFilterTestV2 {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/jsonfilter/";
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
  private final static String JSON_DOCUMENT = "{\"title\":\"any string\",\"date\": \"2020-10-16\"}";
  private final static String XML_SCHEMA = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "                elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "      <xs:element name=\"metadata\">\n"
          + "        <xs:complexType>\n"
          + "          <xs:sequence>\n"
          + "            <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "            <xs:element name=\"date\" type=\"xs:date\"/>\n"
          + "          </xs:sequence>\n"
          + "        </xs:complexType>\n"
          + "      </xs:element>\n"
          + "    </xs:schema>";

  private final static String XML_DOCUMENT = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of second version</ex:title>\n"
          + "  <ex:date>2021-06-15</ex:date>\n"
          + "</ex:metadata>";
  public static boolean initialize = true;
  public final static int MAX_NO_OF_SCHEMAS = 4;
  public final static int NO_OF_DOCUMENTS_PER_TYPE = ((MAX_NO_OF_SCHEMAS + 1) * MAX_NO_OF_SCHEMAS) / 2;
  private static final String JSON_SCHEMA_ID = "json_schema_";
  private static final String XML_SCHEMA_ID = "xml_schema_";
  private static final String RELATED_RESOURCE = "resource_";
  private static final String INVALID_MIMETYPE = "application/invalid";

  private static final String API_BASE_PATH = "/api/v2";
  private static final String ALTERNATE_API_SCHEMA_PATH = API_BASE_PATH + "/schemas";
  private static final String API_SCHEMA_PATH = ALTERNATE_API_SCHEMA_PATH + "/";
  private static final String API_METADATA_PATH = API_BASE_PATH + "/metadata/";

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
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity())
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
    // preparation will be done only once.
    prepareRepo();
  }

  @Test
  public void testFindSchemaRecordsBySchemaId() throws Exception {
    ObjectMapper map = new ObjectMapper();
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      String schemaId = JSON_SCHEMA_ID + i;
      MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)
              .param("schemaId", schemaId)
              .header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE))
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for schema '" + i + "'", 1, result.length);
      Assert.assertEquals("SchemaID '" + schemaId + "'", schemaId, result[0].getId());
    }
  }

  @Test
  public void testFindAllSchemaRecords() throws Exception {
    ObjectMapper map = new ObjectMapper();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)
            .header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals("No of schema records:", MAX_NO_OF_SCHEMAS * 2, result.length);
    for (DataResource schemaRecord : result) {
      Assert.assertTrue("Resource type should end with '_Schema'",
              schemaRecord.getResourceType().getValue().endsWith(DataResourceRecordUtil.SCHEMA_SUFFIX));
    }
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    ObjectMapper map = new ObjectMapper();
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      String schemaId = JSON_SCHEMA_ID + i;
      MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH)
              .param("schemaId", schemaId)
              .header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE))
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for schema '" + i + "'", i, result.length);
      for (DataResource item : result) {
        edu.kit.datamanager.repo.domain.RelatedIdentifier schemaIdentifier = DataResourceRecordUtil.getSchemaIdentifier(item);
        Assert.assertEquals(schemaIdentifier.getIdentifierType().URL, schemaIdentifier.getIdentifierType());
        String schemaUrl = DataResourceRecordUtil.getSchemaIdentifier(item).getValue();
        Assert.assertTrue(schemaUrl.startsWith("http://localhost:"));
        Assert.assertTrue(schemaUrl.contains(API_SCHEMA_PATH));
        Assert.assertTrue(schemaUrl.contains(schemaId));
      }
    }
  }

  @Test
  public void testFindRecordsByMultipleSchemaIds() throws Exception {
    ObjectMapper map = new ObjectMapper();
    int noOfResults;
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      MockHttpServletRequestBuilder get = get(API_METADATA_PATH);
      noOfResults = 0;
      for (int j = 1; j <= i; j++) {
        noOfResults += j;
        String schemaId = JSON_SCHEMA_ID + j;
        get.param("schemaId", schemaId);
      }
      get.header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE);
      MvcResult res = this.mockMvc
              .perform(get)
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for schema '1 - " + i + "'", noOfResults, result.length);
    }
  }

  @Test
  public void testFindRecordsByMultipleButWrongSchemaIds() throws Exception {
    ObjectMapper map = new ObjectMapper();
    int noOfResults;
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      MockHttpServletRequestBuilder get = get(API_METADATA_PATH);
      noOfResults = 0;
      for (int j = 1; j <= i; j++) {
        noOfResults += (MAX_NO_OF_SCHEMAS - j + 1) * 2;
        String relatedResource = RELATED_RESOURCE + j;
        get.param("schemaId", relatedResource);
      }
      get.param("size", Integer.toString(noOfResults * 2));
      get.header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE);
      MvcResult res = this.mockMvc
              .perform(get)
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for schema should be always '0'!", 0, result.length);
    }
  }

  @Test
  public void testFindRecordsByMultipleSchemaIdsPlusInvalidSchemaId() throws Exception {
    ObjectMapper map = new ObjectMapper();
    int noOfResults;
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      MockHttpServletRequestBuilder get = get(API_METADATA_PATH);
      noOfResults = 0;
      for (int j = 1; j <= i; j++) {
        noOfResults += j;
        String schemaId = JSON_SCHEMA_ID + j;
        get.param("schemaId", schemaId);
      }
      get.param("schemaId", "unknownSchemaId");
      get.header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE);
      MvcResult res = this.mockMvc
              .perform(get)
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for schema '1 - " + i + "'", noOfResults, result.length);
    }
  }

  @Test
  public void testFindSchemaRecordsByInvalidMimeType() throws Exception {
    String mimeType = INVALID_MIMETYPE;
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)
            .param("mimeType", mimeType))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindSchemaRecordsByMimeType() throws Exception {
    String mimeType = MediaType.APPLICATION_JSON.toString();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)
            .param("mimeType", mimeType))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(MAX_NO_OF_SCHEMAS, result.length);
    for (DataResource item : result) {
      Assert.assertTrue(item.getFormats().contains(mimeType));
    }
    mimeType = MediaType.APPLICATION_XML.toString();
    res = this.mockMvc.perform(get(API_SCHEMA_PATH)
            .param("mimeType", mimeType))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(MAX_NO_OF_SCHEMAS, result.length);
    for (DataResource item : result) {
      Assert.assertTrue(item.getFormats().contains(mimeType));
    }
  }

  @Test
  public void testFindSchemaRecordsByMultipleMimeTypes() throws Exception {
    String mimeType1 = MediaType.APPLICATION_JSON.toString();
    String mimeType2 = MediaType.APPLICATION_XML.toString();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)
            .param("mimeType", mimeType1)
            .param("mimeType", mimeType2))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(MAX_NO_OF_SCHEMAS * 2, result.length);
  }

  @Test
  public void testFindSchemaRecordsByMultipleMimeTypesIncludingInvalidMimeType() throws Exception {
    String mimeType1 = MediaType.APPLICATION_JSON.toString();
    MvcResult res = this.mockMvc.perform(get(API_SCHEMA_PATH)
            .param("mimeType", mimeType1)
            .param("mimeType", INVALID_MIMETYPE))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(MAX_NO_OF_SCHEMAS, result.length);
  }

  @Test
  public void testFindRecordsByResourceId() throws Exception {
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      edu.kit.datamanager.repo.domain.RelatedIdentifier relatedIdentifier = edu.kit.datamanager.repo.domain.RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, RELATED_RESOURCE + i, null, null);
      relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH)
              .param("resourceId", relatedIdentifier.getValue()))
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      ObjectMapper map = new ObjectMapper();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals((MAX_NO_OF_SCHEMAS - i + 1) * 2, result.length);
      for (DataResource item : result) {
        Assert.assertEquals(relatedIdentifier.getValue(), DataResourceRecordUtil.getRelatedIdentifier(item, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).getValue());
        Assert.assertEquals(relatedIdentifier.getIdentifierType(), DataResourceRecordUtil.getRelatedIdentifier(item, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).getIdentifierType());
        Assert.assertEquals(relatedIdentifier.getRelationType(), DataResourceRecordUtil.getRelatedIdentifier(item, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).getRelationType());
      }
    }
  }

  @Test
  public void testFindRecordsByMultipleResourceIds() throws Exception {
    ObjectMapper map = new ObjectMapper();
    int noOfResults;
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      MockHttpServletRequestBuilder get = get(API_METADATA_PATH);
      noOfResults = 0;
      for (int j = 1; j <= i; j++) {
        noOfResults += (MAX_NO_OF_SCHEMAS - j + 1) * 2;
        String relatedResource = RELATED_RESOURCE + j;
        get.param("resourceId", relatedResource);
      }
      get.param("size", Integer.toString(noOfResults * 2));
      get.header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE);
      MvcResult res = this.mockMvc
              .perform(get)
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for schema '1 - " + i + "'", noOfResults, result.length);
    }
  }

  @Test
  public void testFindRecordsByMultipleButWronResourceIds() throws Exception {
    ObjectMapper map = new ObjectMapper();
    int noOfResults;
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      MockHttpServletRequestBuilder get = get(API_METADATA_PATH);
      noOfResults = 0;
      for (int j = 1; j <= i; j++) {
        noOfResults += j;
        String schemaId = JSON_SCHEMA_ID + j;
        get.param("resourceId", schemaId);
      }
      get.header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE);
      MvcResult res = this.mockMvc
              .perform(get)
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals("No of records for resourceIDs should be always '0'!", 0, result.length);
    }
  }

  @Test
  public void testFindRecordsByInvalidResourceId() throws Exception {

    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH)
            .param("resourceId", "invalid"))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    ObjectMapper map = new ObjectMapper();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByMultipleResourceIdsIncludingInvalidResourceId() throws Exception {
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      edu.kit.datamanager.repo.domain.RelatedIdentifier relatedIdentifier = edu.kit.datamanager.repo.domain.RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, RELATED_RESOURCE + i, null, null);
      relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
      MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH)
              .param("resourceId", relatedIdentifier.getValue())
              .param("resourceId", INVALID_MIMETYPE))
              .andDo(print())
              .andExpect(status().isOk())
              .andReturn();
      ObjectMapper map = new ObjectMapper();
      DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

      Assert.assertEquals((MAX_NO_OF_SCHEMAS - i + 1) * 2, result.length);
      for (DataResource item : result) {
        Assert.assertEquals(relatedIdentifier.getValue(), DataResourceRecordUtil.getRelatedIdentifier(item, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).getValue());
        Assert.assertEquals(relatedIdentifier.getIdentifierType(), DataResourceRecordUtil.getRelatedIdentifier(item, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).getIdentifierType());
        Assert.assertEquals(relatedIdentifier.getRelationType(), DataResourceRecordUtil.getRelatedIdentifier(item, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE).getRelationType());
      }
    }
  }

  @Test
  public void testFindRecordsByUnknownSchemaId() throws Exception {
    ObjectMapper map = new ObjectMapper();
    String schemaId = "UnknownSchemaId";
    MvcResult res = this.mockMvc.perform(get(API_METADATA_PATH)
            .param("schemaId", schemaId)
            .header("Accept", DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
    DataResource[] result = map.readValue(res.getResponse().getContentAsString(), DataResource[].class);

    Assert.assertEquals("No of records for schema '" + schemaId + "'", 0, result.length);
  }

  public void registerSchemaDocument(String schemaType, String schemaId) throws Exception {
    DataResource record = new DataResource();
    record.getTitles().add(Title.factoryTitle("Title for " + schemaId));
    record.setResourceType(ResourceType.createResourceType(schemaType + "_Schema", ResourceType.TYPE_GENERAL.MODEL));
    record.setId(schemaId);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile schemaFile;
    switch (schemaType) {
      case "JSON":
        record.getFormats().add(MediaType.APPLICATION_JSON_VALUE);
        schemaFile = new MockMultipartFile("schema", "schema.json", "application/json", JSON_SCHEMA.getBytes());
        break;
      case "XML":
        record.getFormats().add(MediaType.APPLICATION_XML_VALUE);
        schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", XML_SCHEMA.getBytes());
        break;
      default:
        throw new Exception("Unknown schema type!");
    }
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_SCHEMA_PATH).
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  /**
   * Ingest metadata document for given schema and related resource.
   *
   * @param schemaId schema
   * @param resource related resource
   * @throws Exception
   */
  public void ingestMetadataDocument(String schemaId, String resource) throws Exception {
    DataResource record = new DataResource();
    edu.kit.datamanager.repo.domain.RelatedIdentifier schemaIdentifier = edu.kit.datamanager.repo.domain.RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_SCHEMA_TYPE, schemaId, null, null);
    schemaIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.INTERNAL);
    edu.kit.datamanager.repo.domain.RelatedIdentifier relatedIdentifier = edu.kit.datamanager.repo.domain.RelatedIdentifier.factoryRelatedIdentifier(DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE, resource, null, null);
    relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
    record.getRelatedIdentifiers().add(schemaIdentifier);
    record.getRelatedIdentifiers().add(relatedIdentifier);
    record.getTitles().add(Title.factoryTitle("Document for schemaID: " + schemaId));
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
    record.setAcls(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile;
    if (schemaId.startsWith(JSON_SCHEMA_ID)) {
      metadataFile = new MockMultipartFile("document", "metadata.json", "application/json", JSON_DOCUMENT.getBytes());
    } else {
      metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", XML_DOCUMENT.getBytes());
    }

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_METADATA_PATH).
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  /**
   * Prepare repository with schemas and metadata documents.
   *
   * @throws Exception
   */
  private void prepareRepo() throws Exception {
    if (initialize) {
      initialize = false;
      prepareEnvironment();
      prepareSchemas();
      prepareMetadataDocuments();
    }
  }

  /**
   * Prepare filesystem (remove old files)
   */
  private void prepareEnvironment() {
    System.out.println("------JsonSchemaRegistryControllerTest----------------");
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

  /**
   * Register MAX_NO_OF_SCHEMAS schemas for json and xml
   *
   * @throws Exception
   */
  private void prepareSchemas() throws Exception {
    // Prepare 5 different schemas
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      registerSchemaDocument("JSON", JSON_SCHEMA_ID + i);
      registerSchemaDocument("XML", XML_SCHEMA_ID + i);
    }

  }

  /**
   * For first schema (xml and json) add one metadata document For second schema
   * add two metadata documents For ...
   *
   * @throws Exception
   */
  private void prepareMetadataDocuments() throws Exception {
    for (int i = 1; i <= MAX_NO_OF_SCHEMAS; i++) {
      String schemaId = JSON_SCHEMA_ID + i;
      for (int j = 1; j <= i; j++) {
        String resource = RELATED_RESOURCE + j;
        ingestMetadataDocument(schemaId, resource);
      }
      schemaId = XML_SCHEMA_ID + i;
      for (int j = 1; j <= i; j++) {
        String resource = RELATED_RESOURCE + j;
        ingestMetadataDocument(schemaId, resource);
      }
    }

  }
}
