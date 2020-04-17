/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.dao.IMetadataRecordDao;
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
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
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
public class MetadataControllerTest {
  private final static String TEMP_DIR_4_SCHEMAS = "/tmp/metastore2/";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final String RELATED_RESOURCE = "anyResourceId";
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

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private IMetadataRecordDao metadataSchemaDao;
  @Autowired
  private IAuditService<MetadataRecord> schemaAuditService;
 
  @Before
  public void setUp() throws Exception {
    metadataSchemaDao.deleteAll();
    try {
      try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
        walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
      }
      Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
      Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
      ingestSchemaRecord();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  @Test
  public void testCreateRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test",PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF",PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateInvalidRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId(INVALID_SCHEMA);
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  // @Test 
  public void testCreateRecordFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
    RequestPostProcessor rpp = new RequestPostProcessor() {
      @Override
      public MockHttpServletRequest postProcessRequest(MockHttpServletRequest mhsr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.external.domain"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  //@Test @ToDo Set external remote address.
  public void testCreateRecordUpdateFromExternal() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dcExt");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile).with(remoteAddr("any.domain.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile).with(remoteAddr("www.google.com"))).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  @Test
  public void testCreateRecordWrongType() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadMetadata() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", "<>".getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isUnprocessableEntity()).andReturn();
  }

  @Test
  public void testCreateRecordWithInvalidMetadata() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT_WRONG_NAMESPACE.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutRecord() throws Exception {
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithoutSchema() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
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
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateRecordWithBadRecord2() throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc");
    //related resource is missing

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testCreateTwoVersionsOfRecord() throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("my_dc_with_version");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    MvcResult res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    MetadataRecord result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(result.getRecordVersion(), Long.valueOf(1l));

    res = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();

    result = mapper.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertEquals(result.getRecordVersion(), Long.valueOf(2l));
  }

  @Test
  public void testGetRecordByIdWithoutVersion() throws Exception {
    createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals("dc", result.getSchemaId());
    //Schema URI must not be the actual file URI but the link to the REST endpoint for downloading the schema
    Assert.assertNotEquals("file:///tmp/dc.xml", result.getMetadataDocumentUri());
  }

  @Test
  public void testGetRecordByIdWithVersion() throws Exception {
    createDCMetadataRecord();

    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/dc").param("version", "1").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord.class);
    Assert.assertNotNull(result);
    Assert.assertEquals("dc", result.getSchemaId());
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
    this.mockMvc.perform(get("/api/v1/metadata/dc").param("version", "13").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isNotFound()).andReturn();
  }

  @Test
  public void testFindRecordsBySchemaId() throws Exception {
    createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("schemaId", "dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

    Assert.assertEquals(1, result.length);
  }

  @Test
  public void testFindRecordsByResourceId() throws Exception {
    createDCMetadataRecord();
    MvcResult res = this.mockMvc.perform(get("/api/v1/metadata/").param("resoureId", RELATED_RESOURCE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataRecord[].class);

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
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();

    String dcMetadata = java.nio.file.Files.readString(Paths.get(URI.create("file:///tmp/dc.xml")));

    Assert.assertEquals(dcMetadata, content);
  }

  @Test
  public void testGetSchemaDocumentWithMissingSchemaFile() throws Exception {
    createDCMetadataRecord();

    //delete schema file
    Files.delete(Paths.get("/tmp/dc.xml"));

    this.mockMvc.perform(get("/api/v1/metadata/dc")).andDo(print()).andExpect(status().isInternalServerError()).andReturn();
  }

  @Test
  public void testUpdateRecord() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isOk()).andReturn();

//    result = this.mockMvc.perform(put("/api/v1/metadata/dc").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isOk()).andReturn();
    body = result.getResponse().getContentAsString();

    MetadataRecord record2 = mapper.readValue(body, MetadataRecord.class);
    Assert.assertNotEquals(record.getDocumentHash(), record2.getDocumentHash());//mime type was changed by update
    Assert.assertEquals(record.getCreatedAt(), record2.getCreatedAt());
    Assert.assertEquals(record.getMetadataDocumentUri(), record2.getMetadataDocumentUri());
    Assert.assertEquals(record.getSchemaId(), record2.getSchemaId());
    Assert.assertEquals(record.getRecordVersion(), record2.getRecordVersion());//version is not changing for metadata update
    if (record.getAcl() != null) {
      Assert.assertTrue(record.getAcl().containsAll(record2.getAcl()));
    }
    Assert.assertTrue(record.getLastUpdate().isBefore(record2.getLastUpdate()));
  }

  @Test
  public void testUpdateRecordWithoutETag() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
  }

  @Test
  public void testUpdateRecordWithWrongETag() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag") + "unknown";
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isPreconditionFailed()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutRecord() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(schemaFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testUpdateRecordWithoutDocument() throws Exception {
    createDCMetadataRecord();
    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT_VERSION_2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andExpect(status().isPreconditionRequired()).andReturn();
    this.mockMvc.perform(put("/api/v1/metadata/dc").contentType("application/json").header("If-Match", etag).contentType(MetadataRecord.METADATA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(record))).andDo(print()).andExpect(status().isBadRequest()).andReturn();
  }

  @Test
  public void testDeleteRecord() throws Exception {
    createDCMetadataRecord();

    MvcResult result = this.mockMvc.perform(get("/api/v1/metadata/dc").header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    String etag = result.getResponse().getHeader("ETag");

    this.mockMvc.perform(delete("/api/v1/metadata/dc").header("If-Match", etag)).andDo(print()).andExpect(status().isNoContent()).andReturn();
    //delete second time
    this.mockMvc.perform(delete("/api/v1/metadata/dc")).andDo(print()).andExpect(status().isNoContent()).andReturn();

    //try to create after deletion (Should return HTTP GONE)
    MetadataRecord record = new MetadataRecord();
    record.setSchemaId("dc");
    record.setRelatedResource(RELATED_RESOURCE);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isGone()).andReturn();
  }

  private void createDCMetadataRecord() throws FileNotFoundException, IOException {
    MetadataRecord record = new MetadataRecord();
    record.setCreatedAt(Instant.now());
    record.setLastUpdate(Instant.now());
    record.setSchemaId("dc");
    record.setRelatedResource(RELATED_RESOURCE);
    Set<AclEntry> acl = new HashSet<>();
    AclEntry entry = new AclEntry();
    entry.setSid("SELF");
    entry.setPermission(PERMISSION.WRITE);
    acl.add(entry);
    record.setAcl(acl);
    record.setMetadataDocumentUri("file:///tmp/dc.xml");
    record = metadataSchemaDao.save(record);
    File dcFile = new File("/tmp/dc.xml");
    if (!dcFile.exists()) {
      try (FileOutputStream fout = new FileOutputStream(dcFile)) {
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
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test",PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF",PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).andDo(print()).andExpect(status().isCreated()).andReturn();
  }

}
