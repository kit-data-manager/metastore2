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
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openarchives.oai._2.HeaderType;
import org.openarchives.oai._2.OAIPMHerrorcodeType;
import org.openarchives.oai._2.OAIPMHtype;
import org.openarchives.oai._2.RecordType;
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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.xml.sax.SAXException;

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
//@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/oaipmh/schema"})
//@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/oaipmh/metadata"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_oaipmh;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SuppressWarnings("java:S2925")
public class OaiPmhControllerTest {

  private final static String TEMP_DIR_4_ALL = "/tmp/oaipmh/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static Boolean alreadyInitialized = Boolean.FALSE;
  private static final String SCHEMA_ID_1 = "schema_v1";
  private static final String SCHEMA_ID_2 = "schema_v2";
  private static final String SCHEMA_ID_3 = "schema_v3";
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

  private final static String JSON_DOCUMENT = "{\"title\":\"%s\",\"date\": \"2020-10-16\"}";

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
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
  @Autowired
  private IMetadataFormatDao metadataFormatDao;
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
    metadataFormatDao.deleteAll();

    try {
      // setup mockMvc
      this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
              .apply(springSecurity()) 
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
            .param("from", Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES).toString())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

    System.out.println("Known metadataFormat with until");
    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("until", Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES).toString())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

    System.out.println("Known metadataFormat with from and until");
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value())
            .param("from", Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(2, ChronoUnit.MINUTES).toString())
            .param("until", Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES).toString())
            .param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

  }

  @Test
  public void testProcessRequestListAllIdentifiersWithSchemaEntries() throws Exception {
    MvcResult res;
    OAIPMHtype result;
    System.out.println("Start OAI-PMH test");
    System.out.println("Get responses with content!");
    System.out.println("--------------------------------------------------------------------");
    System.out.println("List all Schemas...");
    String schema1 = String.format(SCHEMA, SCHEMA_ID_1);
    String schema2 = String.format(SCHEMA, SCHEMA_ID_2);
    String schema3 = String.format(SCHEMA, SCHEMA_ID_3);
    String metadata1_1 = String.format(XML_DOCUMENT, SCHEMA_ID_1, "Title 1.1");
    String metadata1_2 = String.format(XML_DOCUMENT, SCHEMA_ID_1, "Title 1.2");
    String metadata1_3 = String.format(XML_DOCUMENT, SCHEMA_ID_1, "Title 1.3");
    String metadata2_1 = String.format(XML_DOCUMENT, SCHEMA_ID_2, "Title 2.1");
    String metadata2_2 = String.format(XML_DOCUMENT, SCHEMA_ID_2, "Title 2.2");
    String metadata2_3 = String.format(XML_DOCUMENT, SCHEMA_ID_2, "Title 2.3");
    String metadata3_1 = String.format(XML_DOCUMENT, SCHEMA_ID_3, "Title 3.1");
    String metadata3_2 = String.format(XML_DOCUMENT, SCHEMA_ID_3, "Title 3.2");
    String metadata3_3 = String.format(XML_DOCUMENT, SCHEMA_ID_3, "Title 3.3");
    String jsonSchema1 = JSON_SCHEMA;
    System.out.println("Add schemas!");
    Instant startDate = truncateDate(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    Thread.sleep(1000);

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
    ingestSchemaRecord("json_schema", MetadataSchemaRecord.SCHEMA_TYPE.JSON, JSON_SCHEMA);
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
    System.out.println("Known metadataFormat but no entries");
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value()).param("metadataPrefix", "oai_dc")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);

    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());  // No values available

    System.out.println("Add first entry...");
    ingestMetadataRecord(SCHEMA_ID_1, metadata1_1);
    Thread.sleep(1000);
    System.out.println("Known metadataFormat with one entry");
    Instant future = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, startDate, 0);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListIdentifiers("oai_dc", null, null, 1);
    checkResponseListIdentifiers("oai_dc", startDate, null, 1);
    checkResponseListIdentifiers("oai_dc", future, null, 0);
    checkResponseListIdentifiers("oai_dc", null, startDate, 0);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListIdentifiers("datacite", null, null, 1);
    checkResponseListIdentifiers("datacite", startDate, null, 1);
    checkResponseListIdentifiers("datacite", future, null, 0);
    checkResponseListIdentifiers("datacite", null, startDate, 0);
    System.out.println("metadataFormat with no entry");
    checkResponseListIdentifiers(SCHEMA_ID_2, null, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, startDate, 0);
    // Get timestamp and sleep for 1 second
    Instant date1 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Thread.sleep(1000);
    System.out.println("Add second entry...");
    ingestMetadataRecord(SCHEMA_ID_2, metadata2_1);
    Thread.sleep(1000);
    future = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    System.out.println("Known metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date1, 1);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListIdentifiers("oai_dc", null, null, 2);
    checkResponseListIdentifiers("oai_dc", startDate, null, 2);
    checkResponseListIdentifiers("oai_dc", date1, null, 1);
    checkResponseListIdentifiers("oai_dc", future, null, 0);
    checkResponseListIdentifiers("oai_dc", null, startDate, 0);
    checkResponseListIdentifiers("oai_dc", null, date1, 1);
    checkResponseListIdentifiers("oai_dc", null, future, 2);
    checkResponseListIdentifiers("oai_dc", startDate, date1, 1);
    System.out.println("metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_2, null, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, date1, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, future, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date1, 0);
    System.out.println("metadataFormat with no entry");
    checkResponseListIdentifiers(SCHEMA_ID_3, null, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, date1, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, future, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date1, 0);
    // Get timestamp and sleep for 1 second
    Instant date2 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Thread.sleep(1000);
    System.out.println("Add third entry...");
    ingestMetadataRecord(SCHEMA_ID_3, metadata3_1);
    Thread.sleep(1000);
    future = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    System.out.println("Known metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date2, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, date2, 0);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListIdentifiers("oai_dc", null, null, 3);
    checkResponseListIdentifiers("oai_dc", startDate, null, 3);
    checkResponseListIdentifiers("oai_dc", date1, null, 2);
    checkResponseListIdentifiers("oai_dc", date2, null, 1);
    checkResponseListIdentifiers("oai_dc", future, null, 0);
    checkResponseListIdentifiers("oai_dc", null, startDate, 0);
    checkResponseListIdentifiers("oai_dc", null, date1, 1);
    checkResponseListIdentifiers("oai_dc", null, date2, 2);
    checkResponseListIdentifiers("oai_dc", null, future, 3);
    checkResponseListIdentifiers("oai_dc", startDate, date1, 1);
    checkResponseListIdentifiers("oai_dc", startDate, date2, 2);
    checkResponseListIdentifiers("oai_dc", date1, date2, 1);
    System.out.println("metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_2, null, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, date1, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, date2, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date2, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date2, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, date1, date2, 1);
    System.out.println("metadataFormat with no entry");
    checkResponseListIdentifiers(SCHEMA_ID_3, null, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, date1, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, date2, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date2, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, future, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date2, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, date1, date2, 0);

    Instant date3 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Thread.sleep(1000);
    System.out.println("Additional three entries...");
    ingestMetadataRecord(SCHEMA_ID_1, metadata1_2);
    ingestMetadataRecord(SCHEMA_ID_2, metadata2_2);
    ingestMetadataRecord(SCHEMA_ID_3, metadata3_2);
    Thread.sleep(1000);
    future = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    System.out.println("Known metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, date3, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, date3, 0);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListIdentifiers("oai_dc", null, null, 6);
    checkResponseListIdentifiers("oai_dc", startDate, null, 6);
    checkResponseListIdentifiers("oai_dc", date1, null, 5);
    checkResponseListIdentifiers("oai_dc", date2, null, 4);
    checkResponseListIdentifiers("oai_dc", date3, null, 3);
    checkResponseListIdentifiers("oai_dc", future, null, 0);
    checkResponseListIdentifiers("oai_dc", null, startDate, 0);
    checkResponseListIdentifiers("oai_dc", null, date1, 1);
    checkResponseListIdentifiers("oai_dc", null, date2, 2);
    checkResponseListIdentifiers("oai_dc", null, date3, 3);
    checkResponseListIdentifiers("oai_dc", null, future, 6);
    checkResponseListIdentifiers("oai_dc", startDate, date1, 1);
    checkResponseListIdentifiers("oai_dc", startDate, date2, 2);
    checkResponseListIdentifiers("oai_dc", startDate, date3, 3);
    checkResponseListIdentifiers("oai_dc", date1, date2, 1);
    System.out.println("metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_2, null, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_2, date2, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date2, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, date1, date3, 1);
    System.out.println("metadataFormat with no entry");
    checkResponseListIdentifiers(SCHEMA_ID_3, null, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_3, date2, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_3, date3, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date2, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, future, 2);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date2, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, date1, date2, 0);

    checkResponseListIdentifiers("oai_dc", null, null, 6);

    Instant date4 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Thread.sleep(1000);
    System.out.println("Additional three entries...");
    ingestMetadataRecord(SCHEMA_ID_1, metadata1_3);
    ingestMetadataRecord(SCHEMA_ID_2, metadata2_3);
    ingestMetadataRecord(SCHEMA_ID_3, metadata3_3);
    Thread.sleep(1000);
    future = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    System.out.println("Known metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 3);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, null, 3);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_1, date4, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, future, null, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, startDate, 0);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, null, date4, 2);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date1, 1);
    checkResponseListIdentifiers(SCHEMA_ID_1, startDate, date4, 2);
    checkResponseListIdentifiers(SCHEMA_ID_1, date1, date4, 1);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListIdentifiers("oai_dc", null, null, 9);
    checkResponseListIdentifiers("oai_dc", startDate, null, 9);
    checkResponseListIdentifiers("oai_dc", date1, null, 8);
    checkResponseListIdentifiers("oai_dc", date2, null, 7);
    checkResponseListIdentifiers("oai_dc", date3, null, 6);
    checkResponseListIdentifiers("oai_dc", date4, null, 3);
    checkResponseListIdentifiers("oai_dc", future, null, 0);
    checkResponseListIdentifiers("oai_dc", null, startDate, 0);
    checkResponseListIdentifiers("oai_dc", null, date1, 1);
    checkResponseListIdentifiers("oai_dc", null, date2, 2);
    checkResponseListIdentifiers("oai_dc", null, date3, 3);
    checkResponseListIdentifiers("oai_dc", null, date4, 6);
    checkResponseListIdentifiers("oai_dc", null, future, 9);
    checkResponseListIdentifiers("oai_dc", startDate, date1, 1);
    checkResponseListIdentifiers("oai_dc", startDate, date2, 2);
    checkResponseListIdentifiers("oai_dc", startDate, date3, 3);
    checkResponseListIdentifiers("oai_dc", startDate, date4, 6);
    checkResponseListIdentifiers("oai_dc", date1, date2, 1);
    System.out.println("metadataFormat with one entry");
    checkResponseListIdentifiers(SCHEMA_ID_2, null, null, 3);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, null, 3);
    checkResponseListIdentifiers(SCHEMA_ID_2, date3, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_2, date4, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, null, date4, 2);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date1, 0);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date2, 1);
    checkResponseListIdentifiers(SCHEMA_ID_2, startDate, date4, 2);
    checkResponseListIdentifiers(SCHEMA_ID_2, date1, date4, 2);
    System.out.println("metadataFormat with no entry");
    checkResponseListIdentifiers(SCHEMA_ID_3, null, null, 3);
    checkResponseListIdentifiers(SCHEMA_ID_3, date3, null, 2);
    checkResponseListIdentifiers(SCHEMA_ID_3, date4, null, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date2, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, date4, 2);
    checkResponseListIdentifiers(SCHEMA_ID_3, null, future, 3);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date2, 0);
    checkResponseListIdentifiers(SCHEMA_ID_3, startDate, date3, 1);
    checkResponseListIdentifiers(SCHEMA_ID_3, date3, date4, 1);

    checkResponseListIdentifiers("datacite", null, null, 9);
   //checkInvalidResponseListRecords(from, until, metadataPrefix, token, errorcode)
    checkInvalidResponseListIdentifiers("today", null, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseListIdentifiers(null, "yesterday", null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseListIdentifiers("today", "yesterday", null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseListIdentifiers(null, null, "unknownPrefix", null, OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT);
    checkInvalidResponseListIdentifiers(null, null, SCHEMA_ID_1, "invalidToken", OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN);
    checkInvalidResponseListIdentifiers("today", null, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    //*************************************************************************
    // List records
    //*************************************************************************
    checkResponseListRecords(SCHEMA_ID_1, null, null, 3);
    checkResponseListRecords(SCHEMA_ID_1, startDate, null, 3);
    checkResponseListRecords(SCHEMA_ID_1, date1, null, 2);
    checkResponseListRecords(SCHEMA_ID_1, date4, null, 1);
    checkResponseListRecords(SCHEMA_ID_1, future, null, 0);
    checkResponseListRecords(SCHEMA_ID_1, null, startDate, 0);
    checkResponseListRecords(SCHEMA_ID_1, null, date1, 1);
    checkResponseListRecords(SCHEMA_ID_1, null, date4, 2);
    checkResponseListRecords(SCHEMA_ID_1, startDate, date1, 1);
    checkResponseListRecords(SCHEMA_ID_1, startDate, date4, 2);
    checkResponseListRecords(SCHEMA_ID_1, date1, date4, 1);
    System.out.println("Generic metadataFormat with one entry");
    checkResponseListRecords("oai_dc", null, null, 9);
    checkResponseListRecords("oai_dc", startDate, null, 9);
    checkResponseListRecords("oai_dc", date1, null, 8);
    checkResponseListRecords("oai_dc", date2, null, 7);
    checkResponseListRecords("oai_dc", date3, null, 6);
    checkResponseListRecords("oai_dc", date4, null, 3);
    checkResponseListRecords("oai_dc", future, null, 0);
    checkResponseListRecords("oai_dc", null, startDate, 0);
    checkResponseListRecords("oai_dc", null, date1, 1);
    checkResponseListRecords("oai_dc", null, date2, 2);
    checkResponseListRecords("oai_dc", null, date3, 3);
    checkResponseListRecords("oai_dc", null, date4, 6);
    checkResponseListRecords("oai_dc", null, future, 9);
    checkResponseListRecords("oai_dc", startDate, date1, 1);
    checkResponseListRecords("oai_dc", startDate, date2, 2);
    checkResponseListRecords("oai_dc", startDate, date3, 3);
    checkResponseListRecords("oai_dc", startDate, date4, 6);
    checkResponseListRecords("oai_dc", date1, date2, 1);
    System.out.println("metadataFormat with one entry");
    checkResponseListRecords(SCHEMA_ID_2, null, null, 3);
    checkResponseListRecords(SCHEMA_ID_2, startDate, null, 3);
    checkResponseListRecords(SCHEMA_ID_2, date3, null, 2);
    checkResponseListRecords(SCHEMA_ID_2, date4, null, 1);
    checkResponseListRecords(SCHEMA_ID_2, null, date1, 0);
    checkResponseListRecords(SCHEMA_ID_2, null, date3, 1);
    checkResponseListRecords(SCHEMA_ID_2, null, date4, 2);
    checkResponseListRecords(SCHEMA_ID_2, startDate, date1, 0);
    checkResponseListRecords(SCHEMA_ID_2, startDate, date2, 1);
    checkResponseListRecords(SCHEMA_ID_2, startDate, date4, 2);
    checkResponseListRecords(SCHEMA_ID_2, date1, date4, 2);
    System.out.println("metadataFormat with no entry");
    checkResponseListRecords(SCHEMA_ID_3, null, null, 3);
    checkResponseListRecords(SCHEMA_ID_3, date3, null, 2);
    checkResponseListRecords(SCHEMA_ID_3, date4, null, 1);
    checkResponseListRecords(SCHEMA_ID_3, null, date2, 0);
    checkResponseListRecords(SCHEMA_ID_3, null, date3, 1);
    checkResponseListRecords(SCHEMA_ID_3, null, date4, 2);
    checkResponseListRecords(SCHEMA_ID_3, null, future, 3);
    checkResponseListRecords(SCHEMA_ID_3, startDate, date2, 0);
    checkResponseListRecords(SCHEMA_ID_3, startDate, date3, 1);
    checkResponseListRecords(SCHEMA_ID_3, date3, date4, 1);
    //checkInvalidResponseListRecords(from, until, metadataPrefix, token, errorcode)
    checkInvalidResponseListRecords("today", null, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseListRecords(null, "yesterday", null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseListRecords("today", "yesterday", null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseListRecords(null, null, "unknownPrefix", null, OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT);
    checkInvalidResponseListRecords(null, null, SCHEMA_ID_1, "invalidToken", OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN);
    String invalidToken = new String(Base64.encodeBase64("3.0/24".getBytes()));
    checkInvalidResponseListRecords(null, null, SCHEMA_ID_1, invalidToken, OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN);
        checkInvalidResponseListRecords("today", null, null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
  }

  @Test
  public void testGetRecord() throws Exception {
    MvcResult res;
    OAIPMHtype result;
    System.out.println("Start OAI-PMH test for verb GetRecord");
    System.out.println("--------------------------------------------------------------------");
    String schema1 = String.format(SCHEMA, SCHEMA_ID_1);
    String metadata1_1 = String.format(XML_DOCUMENT, SCHEMA_ID_1, "Title 1.1");
    // ADD ANOTHER SCHEMA *******************************************************
    ingestSchemaRecord(SCHEMA_ID_1, MetadataSchemaRecord.SCHEMA_TYPE.XML, schema1);
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_METADATA_FORMATS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertEquals(3, result.getListMetadataFormats().getMetadataFormat().size());

    System.out.println("Add first entry...");
    ingestMetadataRecord(SCHEMA_ID_1, metadata1_1);
    Set<String> allIdentifiers = checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 1);
    // Test GetRecord
    for (String id : allIdentifiers) {
      checkResponseGetRecord(id, "oai_dc");
      checkResponseGetRecord(id, "datacite");
    }
    allIdentifiers = checkResponseListIdentifiers(SCHEMA_ID_1, null, null, 1);
    for (String id : allIdentifiers) {
      checkResponseGetRecord(id, SCHEMA_ID_1);
    }

    checkInvalidResponseGetRecord(null, null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseGetRecord("unknownId", null, OAIPMHerrorcodeType.BAD_ARGUMENT);
    checkInvalidResponseGetRecord("unknownId", "unknown_prefix", OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT);
    checkInvalidResponseGetRecord("unknownId", SCHEMA_ID_1, OAIPMHerrorcodeType.ID_DOES_NOT_EXIST);
    checkInvalidResponseGetRecord(allIdentifiers.iterator().next(), "unknown_prefix", OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT);

  }

  @Test
  public void testIdentify() throws Exception {
    MvcResult res;
    OAIPMHtype result;
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.IDENTIFY.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.getError().size());
    Assert.assertNotNull(result.getIdentify());

    res = this.mockMvc.perform(get("/oaipmh")
            .param("verb", VerbType.IDENTIFY.value())
            .param("until", "2000-01-01")).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertNull(result.getIdentify());
    Assert.assertEquals(1, result.getError().size());
    Assert.assertEquals(OAIPMHerrorcodeType.BAD_ARGUMENT, result.getError().get(0).getCode());

  }

  @Test
  public void testListSets() throws Exception {
    MvcResult res;
    OAIPMHtype result;
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_SETS.value())).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());
    Assert.assertEquals(OAIPMHerrorcodeType.NO_SET_HIERARCHY, result.getError().get(0).getCode());
    // test set parameter with listIdentifiers
    res = this.mockMvc.perform(get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value())
    .param("set","default").param("metadataPrefix", SCHEMA_ID_1)).andDo(print()).andExpect(status().isOk()).andReturn();
    result = getResponse(res);
    Assert.assertNotNull(result);
    Assert.assertEquals(1, result.getError().size());
    Assert.assertEquals(OAIPMHerrorcodeType.NO_SET_HIERARCHY, result.getError().get(0).getCode());

  }
  
  @Test
  public void testBadVerb() throws Exception {
    MockHttpServletRequestBuilder param = get("/oaipmh").param("verb", "invalidVerb");
    MvcResult res = this.mockMvc.perform(param).andDo(print()).andExpect(status().isOk()).andReturn();
    OAIPMHtype result = getResponse(res);
    Assert.assertNotNull(result);

    Assert.assertNull(result.getGetRecord());
    Assert.assertNull(result.getIdentify());
    Assert.assertNull(result.getListIdentifiers());
    Assert.assertNull(result.getListMetadataFormats());
    Assert.assertNull(result.getListRecords());
    Assert.assertNull(result.getListSets());
    // Check for errors
    Assert.assertEquals(1, result.getError().size());
    Assert.assertEquals(OAIPMHerrorcodeType.BAD_VERB, result.getError().get(0).getCode());

  }

  private void checkResponseGetRecord(String id, String metadataPrefix) throws Exception {
    MockHttpServletRequestBuilder param = get("/oaipmh").param("verb", VerbType.GET_RECORD.value());
    param = param.param("metadataPrefix", metadataPrefix);
    param = param.param("identifier", id);
    MvcResult res = this.mockMvc.perform(param).andDo(print()).andExpect(status().isOk()).andReturn();
    OAIPMHtype result = getResponse(res, false);

    Assert.assertNotNull(result);

    Assert.assertNotNull("No record found!", result.getGetRecord());
    RecordType getRecord = result.getGetRecord().getRecord();
    Assert.assertEquals(id, getRecord.getHeader().getIdentifier());
    Assert.assertTrue(getRecord.getHeader().getDatestamp().startsWith(Instant.now().truncatedTo(ChronoUnit.DAYS).toString().substring(0, 10)));
    Assert.assertNotNull(getRecord.getMetadata());
    // validateResponse(metadataPrefix, getRecord.getMetadata().toString());

  }

  private void checkInvalidResponseGetRecord(String id, String metadataPrefix, OAIPMHerrorcodeType errorType) throws Exception {
    MockHttpServletRequestBuilder param = get("/oaipmh").param("verb", VerbType.GET_RECORD.value());
    if (metadataPrefix != null) {
      param = param.param("metadataPrefix", metadataPrefix);
    }
    if (id != null) {
      param = param.param("identifier", id);
    }
    MvcResult res = this.mockMvc.perform(param).andDo(print()).andExpect(status().isOk()).andReturn();
    OAIPMHtype result = getResponse(res);

    Assert.assertNotNull(result);

    Assert.assertNull("Record found!?", result.getGetRecord());
    // Check for errors
    Assert.assertEquals(1, result.getError().size());
    Assert.assertEquals(errorType, result.getError().get(0).getCode());

  }

  private void checkInvalidResponseListIdentifiers(String from, String until, String metadataPrefix, String token, OAIPMHerrorcodeType errorType) throws Exception {
    checkInvalidResponseList(VerbType.LIST_IDENTIFIERS, from, until, metadataPrefix, token, errorType);
  }

  private void checkInvalidResponseListRecords(String from, String until, String metadataPrefix, String token, OAIPMHerrorcodeType errorType) throws Exception {
    checkInvalidResponseList(VerbType.LIST_RECORDS, from, until, metadataPrefix, token, errorType);
  }

  private void checkInvalidResponseList(VerbType type, String from, String until, String metadataPrefix, String token, OAIPMHerrorcodeType errorType) throws Exception {
    MockHttpServletRequestBuilder param = get("/oaipmh").param("verb", type.value());
    if (from != null) {
      param = param.param("from", from);
    }
    if (until != null) {
      param = param.param("until", until);
    }
    if (metadataPrefix != null) {
      param = param.param("metadataPrefix", metadataPrefix);
    }
    if (token != null) {
      param = param.param("resumptionToken", token);
    }
    MvcResult res = this.mockMvc.perform(param).andDo(print()).andExpect(status().isOk()).andReturn();
    OAIPMHtype result = getResponse(res);

    Assert.assertNotNull(result);

    Assert.assertNull("Record found!?", result.getGetRecord());
    // Check for errors
    Assert.assertEquals(1, result.getError().size());
    Assert.assertEquals(errorType, result.getError().get(0).getCode());
  }

  private Set<String> checkResponseListIdentifiers(String metadataPrefix, Instant from, Instant until, int noOfExpectedItems, String token) throws Exception {
    Set<String> identifiers = new HashSet<>();
    int itemsPerPage = 3;
    MockHttpServletRequestBuilder param = get("/oaipmh").param("verb", VerbType.LIST_IDENTIFIERS.value());
    param = param.param("metadataPrefix", metadataPrefix);
    if (until != null) {
      param = param.param("until", until.toString());
    }
    if (from != null) {
      param = param.param("from", from.toString());
    }
    if (token != null) {
      param = param.param("resumptionToken", token);
    }
    MvcResult res = this.mockMvc.perform(param).andDo(print()).andExpect(status().isOk()).andReturn();
    OAIPMHtype result = getResponse(res);

    Assert.assertNotNull(result);
    if (noOfExpectedItems == 0) {
      Assert.assertEquals(1, result.getError().size());
      Assert.assertNull(result.getListIdentifiers());
      Assert.assertEquals(OAIPMHerrorcodeType.NO_RECORDS_MATCH, result.getError().get(0).getCode());
    } else {
      List<HeaderType> header = result.getListIdentifiers().getHeader();
      for (HeaderType item : header) {
        identifiers.add(item.getIdentifier());
      }
      int numberOfItems = (noOfExpectedItems > itemsPerPage) ? 3 : noOfExpectedItems;
      Assert.assertEquals(numberOfItems, result.getListIdentifiers().getHeader().size());
      if (noOfExpectedItems > itemsPerPage) {
        System.out.println("Check next page....");
        identifiers.addAll(checkResponseListIdentifiers(metadataPrefix, from, until, noOfExpectedItems - itemsPerPage, result.getListIdentifiers().getResumptionToken().getValue()));
      } else {
        Assert.assertNull(result.getListIdentifiers().getResumptionToken());
      }
    }
    Assert.assertEquals(noOfExpectedItems, identifiers.size());
    return identifiers;
  }

  private Set<String> checkResponseListIdentifiers(String metadataPrefix, Instant from, Instant until, int noOfExpectedItems) throws Exception {
    return checkResponseListIdentifiers(metadataPrefix, from, until, noOfExpectedItems, null);
  }

  private Set<String> checkResponseListRecords(String metadataPrefix, Instant from, Instant until, int noOfExpectedItems, String token) throws Exception {
    Set<String> identifiers = new HashSet<>();
    int itemsPerPage = 3;
    MockHttpServletRequestBuilder param = get("/oaipmh").param("verb", VerbType.LIST_RECORDS.value());
    param = param.param("metadataPrefix", metadataPrefix);
    if (until != null) {
      param = param.param("until", until.toString());
    }
    if (from != null) {
      param = param.param("from", from.toString());
    }
    if (token != null) {
      param = param.param("resumptionToken", token);
    }
    MvcResult res = this.mockMvc.perform(param).andDo(print()).andExpect(status().isOk()).andReturn();
    OAIPMHtype result = getResponse(res, false);

    Assert.assertNotNull(result);
    if (noOfExpectedItems == 0) {
      Assert.assertEquals(1, result.getError().size());
      Assert.assertNull(result.getListRecords());
      Assert.assertEquals(OAIPMHerrorcodeType.NO_RECORDS_MATCH, result.getError().get(0).getCode());
    } else {
      List<RecordType> records = result.getListRecords().getRecord();
      for (RecordType item : records) {
        identifiers.add(item.getHeader().getIdentifier());
      }
      int numberOfItems = (noOfExpectedItems > itemsPerPage) ? 3 : noOfExpectedItems;
      Assert.assertEquals(numberOfItems, result.getListRecords().getRecord().size());
      if (noOfExpectedItems > itemsPerPage) {
        System.out.println("Check next page....");
        identifiers.addAll(checkResponseListRecords(metadataPrefix, from, until, noOfExpectedItems - itemsPerPage, result.getListRecords().getResumptionToken().getValue()));
      } else {
        Assert.assertNull(result.getListRecords().getResumptionToken());
      }
    }
    Assert.assertEquals(noOfExpectedItems, identifiers.size());
    return identifiers;
  }

  private Set<String> checkResponseListRecords(String metadataPrefix, Instant from, Instant until, int noOfExpectedItems) throws Exception {
    return checkResponseListRecords(metadataPrefix, from, until, noOfExpectedItems, null);
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
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
    UUID randomUUID = UUID.randomUUID();
    record.setRelatedResource(ResourceIdentifier.factoryUrlResourceIdentifier("http://example.org/" + randomUUID));
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
    return getResponse(res, true);
  }

  private OAIPMHtype getResponse(MvcResult res, boolean validate) throws JAXBException, UnsupportedEncodingException {
    OAIPMHtype result = null;
    JAXBContext jaxbContext = JAXBContext.newInstance(OAIPMHtype.class);
    String resultString = res.getResponse().getContentAsString();
    if (validate) {
      validateResponse(resultString);
    }
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    //result = (OAIPMHtype) 
    Source source = new StreamSource(new ByteArrayInputStream(resultString.getBytes()));
    JAXBElement<OAIPMHtype> root = jaxbUnmarshaller.unmarshal(source, OAIPMHtype.class);
    result = root.getValue();
    return result;
  }

  private void validateResponse(String response) {
    File schemaFile = new File("src/test/resources/OAI-PMH.xsd"); // etc.
    Source xmlFile = new StreamSource(new ByteArrayInputStream(response.getBytes()));
    SchemaFactory schemaFactory = SchemaFactory
            .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    try {
      Schema schema = schemaFactory.newSchema(schemaFile);
      Validator validator = schema.newValidator();
      validator.validate(xmlFile);
    } catch (SAXException e) {
      Assert.fail("Response is not valid!" + e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

  }

  private void validateResponse(String schemaId, String response) throws MalformedURLException {
    Optional<MetadataFormat> findById = metadataFormatDao.findById(schemaId);
    String schemaUrl;
    if (findById.isPresent()) {
      schemaUrl = findById.get().getSchema();
    } else {
      if (schemaId.equals("oai_dc")) {
        schemaUrl = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
      } else {
        schemaUrl = "http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";
      }
    }
    URL schemaFile = new URL(schemaUrl);
    System.out.println("*****" + schemaUrl + "++++++++");
    System.out.println("*****" + response.trim() + "++++++++");
    Source xmlFile = new StreamSource(new ByteArrayInputStream(response.trim().getBytes()));
    SchemaFactory schemaFactory = SchemaFactory
            .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    try {
      Schema schema = schemaFactory.newSchema(schemaFile);
      Validator validator = schema.newValidator();
      validator.validate(xmlFile);
    } catch (SAXException e) {
      Assert.fail("Response is not valid!" + e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

  }

  private Instant truncateDate(Instant date) {
    return date.truncatedTo(ChronoUnit.SECONDS);
  }

}
