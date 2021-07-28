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
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.Description;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_xsd;DB_CLOSE_DELAY=-1"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/schematest/schema"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SchemaRegistryControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/schematest/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private static final String SCHEMA_ID = "dc";
  private static final String INVALID_SCHEMA_ID = "invalid/my_dc";
  private final static String DC_SCHEMA = "<schema targetNamespace=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
          + "        xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"\n"
          + "        xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
          + "        xmlns=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "        elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "\n"
          + "<import namespace=\"http://purl.org/dc/elements/1.1/\" schemaLocation=\"https://www.dublincore.org/schemas/xmls/qdc/2008/02/11/dc.xsd\"/>\n"
          + "\n"
          + "<element name=\"dc\" type=\"oai_dc:oai_dcType\"/>\n"
          + "\n"
          + "<complexType name=\"oai_dcType\">\n"
          + "  <choice minOccurs=\"0\" maxOccurs=\"unbounded\">\n"
          + "    <element ref=\"dc:title\"/>\n"
          + "    <element ref=\"dc:creator\"/>\n"
          + "    <element ref=\"dc:subject\"/>\n"
          + "    <element ref=\"dc:description\"/>\n"
          + "    <element ref=\"dc:publisher\"/>\n"
          + "    <element ref=\"dc:contributor\"/>\n"
          + "    <element ref=\"dc:date\"/>\n"
          + "    <element ref=\"dc:type\"/>\n"
          + "    <element ref=\"dc:format\"/>\n"
          + "    <element ref=\"dc:identifier\"/>\n"
          + "    <element ref=\"dc:source\"/>\n"
          + "    <element ref=\"dc:language\"/>\n"
          + "    <element ref=\"dc:relation\"/>\n"
          + "    <element ref=\"dc:coverage\"/>\n"
          + "    <element ref=\"dc:rights\"/>\n"
          + "  </choice>\n"
          + "</complexType>\n"
          + "\n"
          + "</schema>";

  private final static String DC_SCHEMA_V2 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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

  private final static String DC_DOCUMENT = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
          + "  <dc:creator>Carbon, Seth</dc:creator>\n"
          + "  <dc:creator>Mungall, Chris</dc:creator>\n"
          + "  <dc:date>2018-07-02</dc:date>\n"
          + "  <dc:description>Archival bundle of GO data release.</dc:description>\n"
          + "  <dc:identifier>https://zenodo.org/record/3477535</dc:identifier>\n"
          + "  <dc:identifier>10.5281/zenodo.3477535</dc:identifier>\n"
          + "  <dc:identifier>oai:zenodo.org:3477535</dc:identifier>\n"
          + "  <dc:relation>doi:10.5281/zenodo.1205166</dc:relation>\n"
          + "  <dc:relation>url:https://zenodo.org/communities/gene-ontology</dc:relation>\n"
          + "  <dc:relation>url:https://zenodo.org/communities/zenodo</dc:relation>\n"
          + "  <dc:rights>info:eu-repo/semantics/openAccess</dc:rights>\n"
          + "  <dc:rights>http://creativecommons.org/licenses/by/4.0/legalcode</dc:rights>\n"
          + "  <dc:title>Gene Ontology Data Archive</dc:title>\n"
          + "  <dc:type>info:eu-repo/semantics/other</dc:type>\n"
          + "  <dc:type>dataset</dc:type>\n"
          + "</oai_dc:dc>";
  private final static String INVALID_DC_DOCUMENT = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
          + "  <dc:creator>Carbon, Seth</dc:creator>\n"
          + "  <dc:date>2018-07-02</dc:date>\n"
          + "  <dc:description>Archival bundle of GO data release.</dc:description>\n"
          + "  <dc:identifier>https://zenodo.org/record/3477535</dc:identifier>\n" //bad namespace
          + "  <dc:id>oai:zenodo.org:3477535</dc:id>\n" //invalid field
          + "  <dc:relation>doi:10.5281/zenodo.1205166</dc:relation>\n"
          + "  <dc:rights>info:eu-repo/semantics/openAccess</dc:rights>\n"
          + "  <dc:title>Gene Ontology Data Archive</dc:title>\n"
          + "  <dc:type>dataset</dc:type>\n"
          + "</oai_dc:dc>";
  private final static String SCHEMA_V1 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "                elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "      <xs:element name=\"metadata\">\n"
          + "        <xs:complexType>\n"
          + "          <xs:sequence>\n"
          + "            <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "          </xs:sequence>\n"
          + "        </xs:complexType>\n"
          + "      </xs:element>\n"
          + "    </xs:schema>";
  private final static String SCHEMA_V2 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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
  private final static String SCHEMA_V3 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "                xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "                elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "      <xs:element name=\"metadata\">\n"
          + "        <xs:complexType>\n"
          + "          <xs:sequence>\n"
          + "            <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "            <xs:element name=\"date\" type=\"xs:date\"/>\n"
          + "            <xs:element name=\"note\" type=\"xs:string\"/>\n"
          + "          </xs:sequence>\n"
          + "        </xs:complexType>\n"
          + "      </xs:element>\n"
          + "    </xs:schema>";
  private final static String XML_DOCUMENT_V1 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of first version</ex:title>\n"
          + "</ex:metadata>";
  private final static String XML_DOCUMENT_V2 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of second version</ex:title>\n"
          + "  <ex:date>2021-06-15</ex:date>\n"
          + "</ex:metadata>";
  private final static String XML_DOCUMENT_V3 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of third version</ex:title>\n"
          + "  <ex:date>2021-06-16</ex:date>\n"
          + "  <ex:note>since version 3</ex:note>\n"
          + "</ex:metadata>";
  private final static String JSON_DOCUMENT = "{\"title\":\"any string\",\"date\": \"2020-10-16\"}";

  private String adminToken;
  private String userToken;
  private String otherUserToken;
  private String guestToken;

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;

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
            .addFilters(springSecurityFilterChain)
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
  }

  @Test
  public void testCreateSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithLocationUri() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc_new");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
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
    record.setSchemaId(INVALID_SCHEMA_ID);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateInvalidMetadataSchemaRecord() throws Exception {
    String wrongTypeJson = "{\"schemaId\":\"dc\",\"type\":\"Something totally strange!\"}";

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", wrongTypeJson.getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    String wrongFormatJson = "<metadata><schemaId>dc</schemaId><type>XML</type></metadata>";
    recordFile = new MockMultipartFile("record", "record.json", "application/json", wrongFormatJson.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

  }

  @Test
  public void testCreateEmptyMetadataSchemaRecord() throws Exception {

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", (byte[]) null);
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

    recordFile = new MockMultipartFile("record", "record.json", "application/json", " ".getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }
  // @Test 

  public void testCreateSchemaRecordFromExternal() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.external.domain"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateSchemaRecordUpdateFromExternal() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dcExt");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.domain.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile).with(remoteAddr("www.google.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWrongType() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.JSON);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordGuessingType() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    record = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(MetadataSchemaRecord.SCHEMA_TYPE.XML, record.getType());
  }

  @Test
  public void testCreateSchemaRecordGuessingTypeFails() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "?".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();

  }

  @Test
  public void testCreateSchemaRecordWithBadSchema() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithEmptySchema() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutRecord() throws Exception {
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithoutSchema() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateSchemaRecordWithBadRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataSchemaRecord record = new MetadataSchemaRecord();
    //schemaId is missing
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc_with_version");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataSchemaRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertEquals(result.getSchemaVersion(), Long.valueOf(1l));
    // Can't create same resource twice -> Conflict
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
  }

  @Test
  public void testGetSchemaRecordByIdWithoutVersion() throws Exception {
    ingestSchemaRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals("dc", result.getSchemaId());
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/dc.xsd", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithVersion() throws Exception {
    ingestSchemaRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/dc").param("version", "1").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals("dc", result.getSchemaId());
    Assert.assertNotEquals("file:///tmp/dc.xsd", result.getSchemaDocumentUri());
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidId() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(get("/api/v1/schemas/cd").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testGetSchemaRecordByIdWithInvalidVersion() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(get("/api/v1/schemas/dc").param("version", "13").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas").param("schemaId", "dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertTrue(result.length > 0);
  }

  @Test
  public void testFindRecordsByMimeType() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas").param("mimeType", MediaType.APPLICATION_XML.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidMimeType() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas").param("mimeType", "invalid")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownSchemaId() throws Exception {
    ingestSchemaRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas").param("schemaId", "cd")).andDo(print()).andExpect(status().isNotFound()).andReturn();
//    ObjectMapper map = new ObjectMapper();
//    MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);
//
//    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(DC_SCHEMA, content);
  }

  @Test
  public void testGetSchemaDocumentWithMissingSchemaFile() throws Exception {
    ingestSchemaRecord();
    String contentUri = contentInformationDao.findAll().get(0).getContentUri();
    //delete schema file
    URI uri = new URI(contentUri);
    Files.delete(Path.of(uri));

    this.mockMvc.perform(get("/api/v1/schemas/dc")).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testValidate() throws Exception {

    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate").file("document", DC_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownVersion() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate?version=666").file("document", DC_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testValidateKnownVersion() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate?version=1").file("document", DC_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNoContent()).andReturn();
  }

  @Test
  public void testValidateUnknownSchemaId() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + INVALID_SCHEMA_ID + "/validate").file("document", DC_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testValidateWithInvalidDocument() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate").file("document", INVALID_DC_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithEmptyDocument() throws Exception {
    ingestSchemaRecord();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate").file("document", "".getBytes())).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutDocument() throws Exception {
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate")).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testValidateWithoutValidator() throws Exception {
    ingestSchemaRecord();

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate").file("document", JSON_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testValidateWithMissingSchemaFile() throws Exception {
    ingestSchemaRecord();
    // Get location of schema file.
    String contentUri = contentInformationDao.findAll().get(0).getContentUri();
    //delete schema file
    URI uri = new URI(contentUri);
    Files.delete(Path.of(uri));

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc/validate").file("document", DC_DOCUMENT.getBytes())).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testUpdateRecord() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    String definitionBefore = record.getDefinition();
    String labelBefore = record.getLabel();
    String commentBefore = record.getComment();
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    record.setDefinition("");
    record.setComment("new comment");
    record.setLabel("label changed");
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion() + 1l, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    Assert.assertEquals("Check label: ", record.getLabel(),record2.getLabel());
    Assert.assertEquals("Check comment: ", record.getComment(), record2.getComment());
    Assert.assertNotEquals("Check label: ",labelBefore,record2.getLabel());
    Assert.assertNotEquals("Check comment: ", commentBefore, record2.getComment());
    Assert.assertNull("Check definition for 'null'", record2.getDefinition());
  }

  @Test
  public void testUpdateRecordWithoutChanges() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(record.getMimeType(), record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion() + 1l, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordAndDocument() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record.getMimeType();
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(recordFile).file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion() + 1l, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get("/api/v1/schemas/dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(DC_SCHEMA_V2, content);
  }

  @Test
  public void testUpdateOnlyDocument() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andExpect(redirectedUrlPattern("http://*:*/**/" + record.getSchemaId() + "?version=*")).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertEquals(record.getMimeType(), record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
//    Assert.assertEquals(record.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getSchemaVersion() + 1l, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
    // Test also document for update
    result = this.mockMvc.perform(get("/api/v1/schemas/dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    Assert.assertEquals(DC_SCHEMA_V2, content);
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("dc");
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    MetadataSchemaRecord record1 = mapper.readValue(body, MetadataSchemaRecord.class);
    String mimeTypeBefore = record1.getMimeType();
    record1.setMimeType(MediaType.APPLICATION_JSON.toString());
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record1).getBytes());
    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataSchemaRecord record2 = mapper.readValue(body, MetadataSchemaRecord.class);
    Assert.assertNotEquals(mimeTypeBefore, record2.getMimeType());//mime type was changed by update
    Assert.assertEquals(record1.getCreatedAt(), record2.getCreatedAt());
    testForNextVersion(record1.getSchemaDocumentUri(), record2.getSchemaDocumentUri());
    //Assert.assertEquals(record1.getSchemaDocumentUri().replace("version=1", "version=2"), record2.getSchemaDocumentUri());
    Assert.assertEquals(record1.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record1.getSchemaVersion() + 1l, (long) record2.getSchemaVersion());//version is not changing for metadata update
    if (record1.getAcl() != null) {
      Assert.assertTrue(record1.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record1.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    record.setMimeType(MediaType.APPLICATION_JSON.toString());
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(recordFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();
    ObjectMapper mapper = new ObjectMapper();
    MetadataSchemaRecord record = mapper.readValue(body, MetadataSchemaRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/dc").
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutBody() throws Exception {
    ingestSchemaRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(put("/api/v1/schemas/dc").header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content("{}")).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testDeleteSchemaRecord() throws Exception {
    ingestSchemaRecord();
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/schemas/dc").header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    // create should return conflict
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isConflict()).andReturn();
    //delete second time // should be really deleted -> gone
    result = this.mockMvc.perform(get("/api/v1/schemas/dc").header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");
    this.mockMvc.perform(delete("/api/v1/schemas/dc").header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();

    //try to create after deletion (Should return HTTP GONE)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isGone()).andReturn();
  }

  @Test
  public void testGetAllVersionsOfRecord() throws Exception {
    String schemaId = "testWithVersion";
    for (long version = 1; version <= 3; version++) {
      // Create a new version
      ingestSchemaWithVersion(schemaId, version);
      // Get version of record as array
      // Read all versions 
      MvcResult result = this.mockMvc.perform(get("/api/v1/schemas").param("schemaId", schemaId)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize((int) version))).andReturn();
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
            Assert.assertTrue("Unknown document: '" + document + "'", false);
        }

        ResultMatcher resultMatcher = null;
        if (version == document) {
          resultMatcher = status().isNoContent();
        } else {
          resultMatcher = status().isUnprocessableEntity();
        }
        this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + schemaId + "/validate").file("document", xmlDocument)).andDo(print()).andExpect(resultMatcher).andReturn();

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
          Assert.assertTrue("Unknown document: '" + document + "'", false);
      }

      ResultMatcher resultMatcher = status().isNoContent();
      this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + schemaId + "/validate?version=" + document).file("document", xmlDocument)).andDo(print()).andExpect(resultMatcher).andReturn();
    }
  }

  private void ingestSchemaRecord() throws Exception {
    DataResource dataResource = DataResource.factoryNewDataResource(SCHEMA_ID);
    dataResource.getCreators().add(Agent.factoryAgent(null, "SELF"));
    dataResource.getTitles().add(Title.factoryTitle(MediaType.APPLICATION_XML.toString(), Title.TYPE.OTHER));
    dataResource.setPublisher("SELF");
    Instant now = Instant.now();
    dataResource.setPublicationYear(Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
    dataResource.setResourceType(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE));
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
    ci.setContentUri("file:/tmp/schema_dc.xsd");
    ci.setUploader("SELF");
    ci.setMediaType("text/plain");
    ci.setHash("sha1:400dfe162fd702a619c4d11ddfb3b7550cb9dec7");
    ci.setSize(1097);

    schemaConfig.getDataResourceService().create(dataResource, "SELF");
//    dataResource = dataResourceDao.save(dataResource);
    ci = contentInformationDao.save(ci);

    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaId(dataResource.getId());
    schemaRecord.setVersion(1l);
    schemaRecord.setSchemaDocumentUri(ci.getContentUri());
    try {
      schemaRecordDao.save(schemaRecord);
      System.out.println("Schema record saved: " + schemaRecord);
    } catch (Exception npe) {
      System.out.println("Can't save schema record: " + schemaRecord);
      System.out.println("SQLExeption: " + npe.getMessage());
    }

    File dcFile = new File("/tmp/schema_dc.xsd");
    if (!dcFile.exists()) {
      try (FileOutputStream fout = new FileOutputStream(dcFile)) {
        fout.write(DC_SCHEMA.getBytes());
        fout.flush();
      }
    }
  }

  private void ingestSchemaWithVersion(String schemaId, long version) throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
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
        Assert.assertTrue("Unknown version: '" + version + "'", false);
    }
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", schemaContent);
    MvcResult result;
    if (version > 1) {
      // Read ETag
      result = this.mockMvc.perform(get("/api/v1/schemas/" + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
      String etag = result.getResponse().getHeader("ETag");
      result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + schemaId).
              file(recordFile).
              file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    } else {
      result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
              file(recordFile).
              file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    }
    String body = result.getResponse().getContentAsString();

    record = mapper.readValue(body, MetadataSchemaRecord.class);
    Long versionAfter = record.getSchemaVersion();
    Assert.assertEquals("Wrong version created!", version, (long) versionAfter);

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
}
