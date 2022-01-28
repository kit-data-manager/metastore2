/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.ILinkedMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.ISchemaRecordDao;
import edu.kit.datamanager.metastore2.dao.IUrl2PathDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.dao.IAllIdentifiersDao;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
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
import org.hamcrest.Matchers;
import org.javers.core.Javers;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
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
@TestPropertySource(properties = {"server.port=41412"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_md_accesswithaai;DB_CLOSE_DELAY=-1"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/md/aai/access/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/md/aai/access/metadata"})
@TestPropertySource(properties = {"repo.auth.enabled=true"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataControllerTestAccessWithAuthenticationEnabled {

  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/md/aai/access/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private static final String METADATA_RECORD_ID = "test_id";
  private static final String SCHEMA_ID = "my_dc_access_aai";
  private static final String INVALID_SCHEMA = "invalid_dc";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryUrlResourceIdentifier("anyResourceId");
  private static final ResourceIdentifier RELATED_RESOURCE_2 = ResourceIdentifier.factoryUrlResourceIdentifier("anyOtherResourceId");
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

  private String adminToken;
  private String userToken;
  private String otherUserToken;
  private String guestToken;

  private String adminPrincipal = "admin";
  private String userPrincipal = "user1";
  private String otherUserPrincipal = "user2";
  private String guestPrincipal = "guest";

  private DataResource sampleResource;
  private static Boolean alreadyInitialized = Boolean.FALSE;

  private MockMvc mockMvc;
  @Autowired
  private ApplicationProperties applicationProperties;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
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
  private IUrl2PathDao url2PathDao;
  @Autowired
  private MetastoreConfiguration metadataConfig;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  @Before
  public void setUp() throws Exception {
    // setup mockMvc
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .addFilters(springSecurityFilterChain)
            .apply(documentationConfiguration(this.restDocumentation).uris()
                    .withPort(41412))
            .build();
    adminToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(adminPrincipal, RepoUserRole.ADMINISTRATOR).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("groupid", "USERS").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(applicationProperties.getJwtSecret());

    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(userPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).
            getCompactToken(applicationProperties.getJwtSecret());

    otherUserToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());

    guestToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(guestPrincipal, RepoUserRole.GUEST).
            addSimpleClaim("email", "thomas.jejkal@kit.edu").
            addSimpleClaim("orcid", "0000-0003-2804-688X").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(applicationProperties.getJwtSecret());
    if (!isInitialized()) {
      System.out.println("------MetadataControllerAccessTestWithAAI-------------");
      System.out.println("------" + this.metadataConfig);
      System.out.println("------------------------------------------------------");

      contentInformationDao.deleteAll();
      dataResourceDao.deleteAll();
      metadataRecordDao.deleteAll();
      schemaRecordDao.deleteAll();
      dataRecordDao.deleteAll();
      allIdentifiersDao.deleteAll();
      url2PathDao.deleteAll();

      try {
        // Create schema only once.
        try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_SCHEMAS)))) {
          walk.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
        Paths.get(TEMP_DIR_4_SCHEMAS).toFile().mkdir();
        Paths.get(TEMP_DIR_4_SCHEMAS + INVALID_SCHEMA).toFile().createNewFile();
        try (Stream<Path> walk = Files.walk(Paths.get(URI.create("file://" + TEMP_DIR_4_METADATA)))) {
          walk.sorted(Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
        }
        Paths.get(TEMP_DIR_4_METADATA).toFile().mkdir();
      } catch (IOException ex) {
        ex.printStackTrace();
      }

      ingestSchemaRecord();
      int schemaNo = 1;
      for (PERMISSION user1 : PERMISSION.values()) {
        for (PERMISSION guest : PERMISSION.values()) {
          ingestMetadataRecord(SCHEMA_ID + "_" + schemaNo, user1, guest);
          schemaNo++;
        }
      }
    }
  }

  @Test
  public void testAccessRecordListAdmin() throws Exception {

    this.mockMvc.perform(get("/api/v1/metadata").
            param("size", Integer.toString(200)).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).
            andDo(print()).andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(16)));
  }

  @Test
  public void testAccessRecordListUser() throws Exception {

    this.mockMvc.perform(get("/api/v1/metadata").
            param("size", Integer.toString(200)).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(12)));
  }

  @Test
  public void testAccessRecordListGuest() throws Exception {

    this.mockMvc.perform(get("/api/v1/metadata").
            param("size", Integer.toString(200)).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + guestToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(12)));
  }

  @Test
  public void testAccessRecordListOtherUser() throws Exception {

    this.mockMvc.perform(get("/api/v1/metadata").
            param("size", Integer.toString(200)).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken)).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(16)));
  }

  @Test
  public void testAccessRecordListWithoutAuthentication() throws Exception {

    this.mockMvc.perform(get("/api/v1/metadata").
            param("size", Integer.toString(200))).
            andDo(print()).
            andExpect(status().isOk()).
            andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(0)));
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
    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", DC_SCHEMA.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken)).
            andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  /**
   * Ingest metadata with 'otheruser' set permissions for admin, user and guest.
   *
   * @param schemaId
   * @param admin
   * @param user
   * @param guest
   * @throws Exception
   */
  private void ingestMetadataRecord(String schemaId, PERMISSION user, PERMISSION guest) throws Exception {
    MetadataRecord record = new MetadataRecord();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(SCHEMA_ID));
    record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier("resource of " + schemaId));
    Set<AclEntry> aclEntries = new HashSet<>();
    if (user != PERMISSION.NONE) {
      aclEntries.add(new AclEntry(userPrincipal, user));
    }
    if (guest != PERMISSION.NONE) {
      aclEntries.add(new AclEntry(guestPrincipal, guest));
    }
    if (!aclEntries.isEmpty()) {
      record.setAcl(aclEntries);
    }
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata").
            file(recordFile).
            file(schemaFile).
            header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken)).
            andDo(print()).andExpect(status().isCreated()).andReturn();
  }

  public static synchronized boolean isInitialized() {
    boolean returnValue = alreadyInitialized;
    alreadyInitialized = Boolean.TRUE;

    return returnValue;
  }

  private String getSchemaUrl(String schemaId) throws Exception {
    MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/" + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    ObjectMapper map = new ObjectMapper();
    MetadataSchemaRecord result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord.class);
    return result.getSchemaDocumentUri().replaceFirst("8080", "41408");
  }
}
