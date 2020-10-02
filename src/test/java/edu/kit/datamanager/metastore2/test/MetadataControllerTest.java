/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.dao.IMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.codec.binary.Hex;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT) //RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"server.port=41403"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String SCHEMA_ID = "my_dc";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final String RELATED_RESOURCE = "anyResourceId";
  private static final String RELATED_RESOURCE_2 = "anyOtherResourceId";
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
  private final static String DC_DOCUMENT_VERSION_2 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
          + "  <dc:creator>Else, Someone</dc:creator>\n"
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
  private final static String DC_DOCUMENT_WRONG_NAMESPACE = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/NOT_EXIST/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
          + "  <dc:creator>Mungall, Chris</dc:creator>\n"
          + "  <dc:date>2018-07-02</dc:date>\n"
          + "  <dc:description>Archival bundle of GO data release.</dc:description>\n"
          + "  <dc:identifier>oai:zenodo.org:3477535</dc:identifier>\n"
          + "  <dc:relation>url:https://zenodo.org/communities/zenodo</dc:relation>\n"
          + "  <dc:rights>http://creativecommons.org/licenses/by/4.0/legalcode</dc:rights>\n"
          + "  <dc:title>Gene Ontology Data Archive</dc:title>\n"
          + "  <dc:type>dataset</dc:type>\n"
          + "</oai_dc:dc>";
  private final static String DC_DOCUMENT_INVALID = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
          + "  <dc:creater>Carbon, Seth</dc:creater>\n"
          + "</oai_dc:dc>";

  private static Boolean alreadyInitialized = Boolean.FALSE;

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
  @Autowired
  private IMetadataRecordDao metadataRecordDao;
  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private IAuditService<MetadataRecord> schemaAuditService;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  @Before
  public void setUp() throws Exception {
    metadataRecordDao.deleteAll();
    try {
      // setup mockMvc
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
              .addFilters(springSecurityFilterChain)
              .apply(documentationConfiguration(this.restDocumentation))
              .build();
      // Create schema only once.
      if (!isInitialized()) {
        metadataSchemaDao.deleteAll();
        try ( Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
          walk.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
        Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
        Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
        ingestSchemaRecord();
      }
      try ( Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_METADATA)))) {
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
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
  }

  @Test
  public void testCreateRecordWithId() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    record.setId("SomeInvalidId");
    Set<AclEntry> aclEntries = new HashSet<>();
//    aclEntries.add(new AclEntry("SELF",PERMISSION.READ));
//    aclEntries.add(new AclEntry("test2",PERMISSION.ADMINISTRATE));
//    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithLocationUri() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("SELF", PERMISSION.READ));
    aclEntries.add(new AclEntry("test2", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn();
    String locationUri = result.getResponse().getHeader("Location");
    String content = result.getResponse().getContentAsString();

    ObjectMapper map = new ObjectMapper();
    MvcResult result2 = this.mockMvc.perform(get(locationUri).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content2 = result2.getResponse().getContentAsString();

    Assert.assertEquals(content, content2);
  }

  @Test
  public void testCreateInvalidRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(INVALID_SCHEMA);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  // @Test 
  public void testCreateRecordFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
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
    record.setSchemaId("my_dcExt");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
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
    record.setSchemaId("unknown_dc");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadMetadata() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadataNamespace() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_WRONG_NAMESPACE.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadata() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_INVALID.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutRecord() throws Exception {
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutSchema() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataRecord record = new MetadataRecord();
    //schemaId is missing
    record.setRelatedResource(RELATED_RESOURCE);

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord2() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    //related resource is missing

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfSameRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(Long.valueOf(1l), result.getRecordVersion());

    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isConflict()).andReturn();

    Assert.assertTrue(res.getResponse().getContentAsString().contains("Metadata record already exists"));
  }

  @Test
  public void testCreateTwoVersions() throws Exception {
    MetadataRecord record = new MetadataRecord();
//    record.setId("my_id");
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(Long.valueOf(1l), result.getRecordVersion());

    record.setRelatedResource(RELATED_RESOURCE_2);
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(Long.valueOf(1l), result.getRecordVersion());
  }

  @Test
  public void testGetRecordById() throws Exception {
    createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(SCHEMA_ID, result.getSchemaId());
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/dc.xml", result.getMetadataDocumentUri());
  }

  @Test
  public void testGetRecordByIdWithVersion() throws Exception {
    createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).param("version", "1").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals(SCHEMA_ID, result.getSchemaId());
    Assert.assertNotEquals("file:///tmp/dc.xml", result.getMetadataDocumentUri());
  }

  @Test
  public void testGetRecordByIdWithInvalidId() throws Exception {
    createDCMetadataRecord();
    this.mockMvc.perform(get("/api/v1/metadata/cd").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testGetRecordByIdWithInvalidVersion() throws Exception {
    createDCMetadataRecord();
    this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).param("version", "13").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", SCHEMA_ID)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByResourceId() throws Exception {
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);
    createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resoureId", RELATED_RESOURCE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
    res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", RELATED_RESOURCE).param("from", twoHoursBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByInvalidResourceId() throws Exception {
    createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", "invalid")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByInvalidUploadDate() throws Exception {
    createDCMetadataRecord();
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", RELATED_RESOURCE).param("until", oneHourBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);

    res = this.mockMvc.perform(get("/api/v1/metadata/").param("resourceId", RELATED_RESOURCE).param("from", twoHoursBefore.toString()).param("until", oneHourBefore.toString())).andDo(print()).andExpect(status().isOk()).andReturn();
    map = new ObjectMapper();
    result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testFindRecordsByUnknownParameter() throws Exception {
    createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", "cd")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(0, result.length);
  }

  @Test
  public void testGetSchemaDocument() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID)).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = new String(java.nio.file.Files.readAllBytes(Paths.get(URI.create("file:///tmp/dc.xml"))));

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testGetMetadataDocumentWithUnknownSchema() throws Exception {
    createDCMetadataRecord();

    //delete schema file
    Files.delete(Paths.get("/tmp/dc.xml"));

    this.mockMvc.perform(get("/api/v1/metadata/unknown_dc")).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testUpdateRecord() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + record.getId()).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1l);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutExplizitGet() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> acl = new HashSet<>();
    acl.add(new AclEntry("test", PERMISSION.READ));
    acl.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(acl);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();
    String locationUri = result.getResponse().getHeader("Location");

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile2 = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record2).getBytes());
    MockMultipartFile metadataFile2 = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(locationUri).
            file(recordFile2).
            file(metadataFile2).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record3 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record2.getDocumentHash(), record3.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record2.getCreatedAt(), record3.getCreatedAt());
    Assert.assertEquals(record2.getMetadataDocumentUri().replace("version=1", "version=2"), record3.getMetadataDocumentUri());
    Assert.assertEquals(record2.getSchemaId(), record3.getSchemaId());
    Assert.assertEquals((long) record2.getRecordVersion(), record3.getRecordVersion() - 1l);// version should be 1 higher
    if (record2.getAcl() != null) {
      Assert.assertTrue(record2.getAcl().containsAll(record3.getAcl()));
    }
    Assert.assertTrue(record2.getLastUpdate().isBefore(record3.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + METADATA_RECORD_ID).
            file(recordFile).
            file(metadataFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + METADATA_RECORD_ID).
            file(recordFile).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutRecord() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + METADATA_RECORD_ID).
            file(metadataFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1l);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutDocument() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + METADATA_RECORD_ID).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();
//    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri().replace("version=1", "version=2"), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals((long) record.getRecordVersion(), record2.getRecordVersion() - 1l);// version should be 1 higher
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testDeleteRecord() throws Exception {
    createDCMetadataRecord();

    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/" + METADATA_RECORD_ID).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/metadata/" + METADATA_RECORD_ID).header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    //delete second time
    this.mockMvc.perform(delete("/api/v1/metadata/" + METADATA_RECORD_ID)).andDo(print()).andExpect(status().isNoContent()).andReturn();
//    Recreation should be no problem.
//    //try to create after deletion (Should return HTTP GONE)
//    MetadataRecord record = new MetadataRecord();
//    record.setSchemaId("dc");
//    record.setRelatedResource(RELATED_RESOURCE);
//    ObjectMapper mapper = new ObjectMapper();
//
//    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
//    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
//
//    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/" + METADATA_RECORD_ID).
//            file(recordFile).
//            file(metadataFile)).andDo(print()).andExpect(status().isGone()).andReturn();
  }

  private void createDCMetadataRecord() throws FileNotFoundException, IOException {
    MetadataRecord record = new MetadataRecord();
    record.setId(METADATA_RECORD_ID);
    record.setCreatedAt(Instant.now());
    record.setLastUpdate(Instant.now());
    record.setSchemaId(SCHEMA_ID);
    record.setRecordVersion(1l);
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> acl = new HashSet<>();
    AclEntry entry = new AclEntry();
    entry.setSid("SELF");
    entry.setPermission(PERMISSION.WRITE);
    acl.add(entry);
    record.setAcl(acl);
    record.setMetadataDocumentUri("file:///tmp/dc.xml");
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      byte[] data = DC_DOCUMENT.getBytes();
      md.update(data, 0, data.length);
      record.setDocumentHash("sha1:" + Hex.encodeHexString(md.digest()));
    } catch (NoSuchAlgorithmException ex) {
      // ignore
    }
    record = metadataRecordDao.save(record);
    File dcFile = new File("/tmp/dc.xml");
    if (!dcFile.exists()) {
      try ( FileOutputStream fout = new FileOutputStream(dcFile)) {
        fout.write(DC_DOCUMENT.getBytes());
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

  private static RequestPostProcessor putMultipart() { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setMethod("PUT");
      return request;
    };
  }

  private void ingestSchemaRecord() throws Exception {
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(SCHEMA_ID);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test", PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF", PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }
}
