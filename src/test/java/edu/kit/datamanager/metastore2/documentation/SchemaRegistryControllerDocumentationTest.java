/*
 * Copyright 2018 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.documentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.security.web.FilterChainProxy;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 */
//@ActiveProfiles("doc")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT) //RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {"server.port=41405"})
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_doc;DB_CLOSE_DELAY=-1"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/restdocu/schema"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries=http://localhost:41405/api/v1/"})
public class SchemaRegistryControllerDocumentationTest {

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private FilterChainProxy springSecurityFilterChain;
//  @Autowired
//  private IDataResourceDao dataResourceDao;
//  @Autowired
//  private IDataResourceService dataResourceService;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();
  
  private final static String EXAMPLE_SCHEMA_ID = "my_first_xsd";
  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/restdocu/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String EXAMPLE_SCHEMA = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "        xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "        elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "\n"
          + "<xs:element name=\"metadata\">\n"
          + "  <xs:complexType>\n"
          + "    <xs:sequence>\n"
          + "      <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "      <xs:element name=\"date\" type=\"xs:date\"/>\n"
          + "    </xs:sequence>\n"
          + "  </xs:complexType>\n"
          + "</xs:element>\n"
          + "\n"
          + "</xs:schema>";
  private final static String NEW_EXAMPLE_SCHEMA = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "        xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "        elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "\n"
          + "<xs:element name=\"metadata\">\n"
          + "  <xs:complexType>\n"
          + "    <xs:sequence>\n"
          + "      <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "      <xs:element name=\"date\" type=\"xs:date\"/>\n"
          + "      <xs:element name=\"note\" type=\"xs:string\" minOccurs=\"0\"/>\n"
          + "    </xs:sequence>\n"
          + "  </xs:complexType>\n"
          + "</xs:element>\n"
          + "\n"
          + "</xs:schema>";

  private final static String DC_DOCUMENT_V1 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<example:metadata xmlns:example=\"http://www.example.org/schema/xsd/\" >\n"
          + "  <example:title>My first XML document</example:title>\n"
          + "  <example:date>2018-07-02</example:date>\n"
          + "</example:metadata>";

  private final static String DC_DOCUMENT_V2 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<example:metadata xmlns:example=\"http://www.example.org/schema/xsd/\" >\n"
          + "  <example:title>My first XML document</example:title>\n"
          + "  <example:date>2018-07-02</example:date>\n"
          + "  <example:note>since version 2 notes are allowed</example:note>\n"
          + "</example:metadata>";
  private static final String RELATED_RESOURCE = "anyResourceId";

  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;

  @Before
  public void setUp() throws JsonProcessingException {
    metadataSchemaDao.deleteAll();
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
  public void documentSchemaRegistry() throws Exception {
    MetadataSchemaRecord schemaRecord = new MetadataSchemaRecord();
    schemaRecord.setSchemaId(EXAMPLE_SCHEMA_ID);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    schemaRecord.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", EXAMPLE_SCHEMA.getBytes());
    MockMultipartFile recordFile = new MockMultipartFile("record", "schema-record.json", "application/json", new ByteArrayInputStream(mapper.writeValueAsString(schemaRecord).getBytes()));

    //create resource and obtain location from response header
    String location = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(schemaFile).
            file(recordFile)).
            andExpect(status().isCreated()).
            andDo(document("register-schema", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).
            andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);
    // List all meatadata schema records
    this.mockMvc.perform(get("/api/v1/schemas/")).andExpect(status().isOk()).andDo(document("get-all-schemas", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    this.mockMvc.perform(get("/api/v1/schemas/").param("page", Integer.toString(0)).param("size", Integer.toString(20))).andExpect(status().isOk()).andDo(document("get-all-schemas-pagination", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Get single metadata schema record
    String etag = this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID).accept("application/vnd.datamanager.schema-record+json")).andExpect(status().isOk()).andDo(document("get-schema-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");

    // Get metadata schema
    this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID)).andExpect(status().isOk()).andDo(document("get-schema", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    //update schema document and create new version
    schemaFile = new MockMultipartFile("schema", "schema_v2.xsd", "application/xml", NEW_EXAMPLE_SCHEMA.getBytes());
    etag = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(schemaFile).
            file(recordFile).header("If-Match", etag)).
            andExpect(status().isCreated()).
            andDo(document("update-schema", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).
            andReturn().getResponse().getHeader("ETag");

    // Get metadata schema version 2
    this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID)).andExpect(status().isOk()).andDo(document("get-schemav2", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Get metadata schema version 1
    this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID).param("version", "1")).andExpect(status().isOk()).andDo(document("get-schemav1", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    MockMultipartFile metadataFile_v2 = new MockMultipartFile("document", "metadata_v2.xml", "application/xml", DC_DOCUMENT_V2.getBytes());
    // Validate XML against schema version 1 (is invalid)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID + "/validate").file(metadataFile_v2).queryParam("version", "1")).andDo(document("validate-document-v1", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Validate XML against schema version 2 (should be valid)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID + "/validate").file(metadataFile_v2)).andDo(document("validate-document-v2", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Update metadata record to allow admin to edit schema as well.
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    schemaRecord = mapper.readValue(body, MetadataSchemaRecord.class);
    schemaRecord.getAcl().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));

    this.mockMvc.perform(put("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID).header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(schemaRecord))).andDo(document("update-schema-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Create a metadata record.
    MetadataRecord metadataRecord = new MetadataRecord();
//    record.setId("my_id");
    metadataRecord.setSchemaId(EXAMPLE_SCHEMA_ID);
    metadataRecord.setRelatedResource(RELATED_RESOURCE);

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(metadataRecord).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DC_DOCUMENT_V1.getBytes());

    location = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(document("create-metadata-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).andReturn().getResponse().getHeader("Location");

    // Get metadata
    this.mockMvc.perform(get(location).accept("application/xml")).andExpect(status().isOk()).andDo(document("get-metadata", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Get metadata record
    this.mockMvc.perform(get(location).accept(MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andExpect(status().isOk()).andDo(document("get-metadata-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Update metadata record (add ACL entry)
    result = this.mockMvc.perform(get(location).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    record.getAcl().add(new AclEntry("guest", PERMISSION.READ));
    recordFile = new MockMultipartFile("record", "metadata-record-acl.json", "application/json", mapper.writeValueAsString(record).getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart(location).
            file(recordFile).header("If-Match", etag).with(putMultipart())).andDo(print()).andDo(document("update-metadata-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn();

    // Update metadata
    // Get URL
    String newLocation = location.split("[?]")[0];
    result = this.mockMvc.perform(get(newLocation).header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).andDo(print()).andDo(document("get-metadata-record-v2", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");


    location = this.mockMvc.perform(MockMvcRequestBuilders.multipart(location).
            file(metadataFile_v2).header("If-Match", etag).with(putMultipart())).andDo(print()).andDo(document("update-metadata", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("Location");

    // get updated metadata
    this.mockMvc.perform(get(location)).andDo(print()).andDo(document("get-metadata-v3", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isOk()).andReturn();
    // find all metadata for a resource
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);
    this.mockMvc.perform(get("/api/v1/metadata/").param("resoureId", RELATED_RESOURCE)).andDo(print()).andDo(document("find-metadata-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isOk()).andReturn();

    this.mockMvc.perform(get("/api/v1/metadata/").param("from", twoHoursBefore.toString())).andDo(print()).andDo(document("find-metadata-record-from", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isOk()).andReturn();

    this.mockMvc.perform(get("/api/v1/metadata/").param("from", twoHoursBefore.toString()).param("until", oneHourBefore.toString())).andDo(print()).andDo(document("find-metadata-record-from-to", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isOk()).andReturn();

  }

  private static RequestPostProcessor putMultipart() { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setMethod("PUT");
      return request;
    };
  }

}
