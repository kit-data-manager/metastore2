/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.oaipmh.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openarchives.oai._2.OAIPMHtype;
import org.openarchives.oai._2.VerbType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
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
@TestPropertySource(properties = {"server.port=41403"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OaiPmhControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/oaipmh/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static Boolean alreadyInitialized = Boolean.FALSE;
  private static final String SCHEMA_ID_1 = "schemaV1";
  private static final String SCHEMA_ID_2 = "schemaV2";
  private static final String SCHEMA_ID_3 = "schemaV3";
  private static final String TITLE_1 = "Title 1";
  private static final String TITLE_2 = "Title 2";
  private static final String TITLE_3 = "Title 3";
  private final static String SCHEMA = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/%s\"\n"
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

  private final static String XML_DOCUMENT = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<example:metadata xmlns:example=\"http://www.example.org/schema/xsd/%s\" >\n"
          + "  <example:title>%s</example:title>\n"
          + "  <example:date>2018-07-02</example:date>\n"
          + "</example:metadata>";
  private final static String JSON_SCHEMA = "{\n"
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

  private final static String JSON_DOCUMENT = "{\"title\":\"%s\",\"date\": \"2020-10-16\"}";

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
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

  public OaiPmhControllerTest() {
  }

  @Before
  public void setUp() {
    System.out.println("------OaiPmhControllerTest--------------------------");
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
              .addFilters(springSecurityFilterChain)
              .apply(documentationConfiguration(this.restDocumentation))
              .build();
      // Create schema only once.
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
   * Test of processRequest method, of class OaiPmhController.
   */
  @Test
  public void testProcessRequestEmptyRepository() throws Exception {
    System.out.println("Start OAI-PMH test");
    System.out.println("Get responses with no content!");
    System.out.println("--------------------------------------------------------------------");
    System.out.println("List all Schemas...");
    MvcResult res;
    OAIPMHtype result;
    String schema1 = String.format(SCHEMA, SCHEMA_ID_1);
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_METADATA_FORMATS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertEquals(2, result.getListMetadataFormats().getMetadataFormat().size());

    System.out.println("List all Identifiers...");
    System.out.println("Missing metadataFormat");
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_IDENTIFIERS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());
    System.out.println("Unknown metadataFormat");
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("metadataPrefix", "unknown")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());
    System.out.println("Known metadataFormat");
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

    System.out.println("Known metadataFormat with from");
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("from", Instant.now().minus(1, ChronoUnit.MINUTES).toString())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

    System.out.println("Known metadataFormat with until");
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("until", Instant.now().minus(1, ChronoUnit.MINUTES).toString())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

    System.out.println("Known metadataFormat with from and until");
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("from", Instant.now().minus(2, ChronoUnit.MINUTES).toString())
            .param("until", Instant.now().minus(1, ChronoUnit.MINUTES).toString())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

  }
  @Test
  public void testProcessRequestWithSchemaEntries() throws Exception {
    MvcResult res;
    OAIPMHtype result;
    System.out.println("Start OAI-PMH test");
    System.out.println("Get responses with content!");
    System.out.println("--------------------------------------------------------------------");
    System.out.println("List all Schemas...");
    String schema1 = String.format(SCHEMA, SCHEMA_ID_1);
    String schema2 = String.format(SCHEMA, SCHEMA_ID_2);
    String schema3 = String.format(SCHEMA, SCHEMA_ID_3);
    String jsonSchema1 = JSON_SCHEMA;
    System.out.println("Add schemas!");
    System.out.println("--------------------------------------------------------------------");
    // ADD ANOTHER SCHEMA *******************************************************
    ingestSchemaRecord(SCHEMA_ID_1, MetadataSchemaRecord.SCHEMA_TYPE.XML, schema1);
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_METADATA_FORMATS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertEquals(3, result.getListMetadataFormats().getMetadataFormat().size());
    // ADD ANOTHER SCHEMA *******************************************************
    ingestSchemaRecord(SCHEMA_ID_2, MetadataSchemaRecord.SCHEMA_TYPE.XML, schema2);
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_METADATA_FORMATS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertEquals(4, result.getListMetadataFormats().getMetadataFormat().size());
    // ADD ANOTHER SCHEMA *******************************************************
    ingestSchemaRecord(SCHEMA_ID_3, MetadataSchemaRecord.SCHEMA_TYPE.XML, schema3);
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_METADATA_FORMATS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertEquals(5, result.getListMetadataFormats().getMetadataFormat().size());
    // ADD ANOTHER JSON SCHEMA *******************************************************
    ingestSchemaRecord("jsonSchema", MetadataSchemaRecord.SCHEMA_TYPE.JSON, JSON_SCHEMA);
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_METADATA_FORMATS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertEquals(5, result.getListMetadataFormats().getMetadataFormat().size());

    System.out.println("List all Identifiers...");
    System.out.println("Missing metadataFormat");
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());
    System.out.println("Unknown metadataFormat");
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value()).param("metadataPrefix", "unknown")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());
    System.out.println("Known metadataFormat");
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value()).param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available


  }

  private void ingestSchemaRecord(String schemaId, MetadataSchemaRecord.SCHEMA_TYPE type, String schemaDocument) throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(schemaId);
    record.setType(type);
    switch (type) {
      case JSON:
        record.setMimeType(MediaType.APPLICATION_JSON.toString());
        break;
      case XML:
        record.setMimeType(MediaType.APPLICATION_XML.toString());
        break;
      default:
        System.out.println("Something is going totally wrong! Unknown type: " + type);
    }
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", schemaDocument.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  public void ingestMetadataRecord(String schemaId, String metadataDocument) throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchemaId(schemaId);
    UUID randomUUID = UUID.randomUUID();
    record.setRelatedResource(randomUUID.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", metadataDocument.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }

  private OAIPMHtype getResponse(MvcResult res) throws JAXBException, UnsupportedEncodingException {
    OAIPMHtype result = null;
    JAXBContext jaxbContext = JAXBContext.newInstance(OAIPMHtype.class);
    String resultString = res.getResponse().getContentAsString();
    System.out.println(resultString);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    //result = (OAIPMHtype) 
    Source source = new StreamSource(new ByteArrayInputStream(resultString.getBytes()));
    JAXBElement<OAIPMHtype> root = jaxbUnmarshaller.unmarshal(source, OAIPMHtype.class);
    result = root.getValue();
    return result;
  }

}
