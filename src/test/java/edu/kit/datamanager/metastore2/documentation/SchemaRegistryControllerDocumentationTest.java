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
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:db_xml_doc;DB_CLOSE_DELAY=-1;MODE=LEGACY;NON_KEYWORDS=VALUE"})
@TestPropertySource(properties = {"metastore.schema.schemaFolder=file:///tmp/metastore2/restdocu/xml/schema"})
@TestPropertySource(properties = {"metastore.metadata.metadataFolder=file:///tmp/metastore2/restdocu/xml/metadata"})
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries="})
@TestPropertySource(properties = {"server.error.include-message=always"})
public class SchemaRegistryControllerDocumentationTest {

  private MockMvc mockMvc;
  @Autowired
  private WebApplicationContext context;
  @Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

  private final static String EXAMPLE_SCHEMA_ID = "my_first_xsd";
  private final static String ANOTHER_SCHEMA_ID = "another_xsd";
  private final static String TEMP_DIR_4_ALL = "/tmp/metastore2/restdocu/xml/";
  private final static String TEMP_DIR_4_SCHEMAS = TEMP_DIR_4_ALL + "schema/";
  private final static String TEMP_DIR_4_METADATA = TEMP_DIR_4_ALL + "metadata/";
  private final static String SCHEMA_V1 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
          + "        xmlns=\"http://www.example.org/schema/xsd/\"\n"
          + "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "        elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "\n"
          + "<xs:element name=\"metadata\">\n"
          + "  <xs:complexType>\n"
          + "    <xs:sequence>\n"
          + "      <xs:element name=\"title\" type=\"xs:string\"/>\n"
          + "    </xs:sequence>\n"
          + "  </xs:complexType>\n"
          + "</xs:element>\n"
          + "\n"
          + "</xs:schema>";
  private final static String SCHEMA_V2 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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
  private final static String SCHEMA_V3 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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

  private final static String ANOTHER_SCHEMA = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/example\"\n"
          + "        xmlns=\"http://www.example.org/schema/xsd/example\"\n"
          + "        xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "        elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
          + "\n"
          + "<xs:element name=\"metadata\">\n"
          + "  <xs:complexType>\n"
          + "    <xs:sequence>\n"
          + "      <xs:element name=\"description\" type=\"xs:string\"/>\n"
          + "    </xs:sequence>\n"
          + "  </xs:complexType>\n"
          + "</xs:element>\n"
          + "\n"
          + "</xs:schema>";

  private final static String DOCUMENT_V1 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<example:metadata xmlns:example=\"http://www.example.org/schema/xsd/\" >\n"
          + "  <example:title>My first XML document</example:title>\n"
          + "</example:metadata>";

  private final static String DOCUMENT_V2 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<example:metadata xmlns:example=\"http://www.example.org/schema/xsd/\" >\n"
          + "  <example:title>My second XML document</example:title>\n"
          + "  <example:date>2018-07-02</example:date>\n"
          + "</example:metadata>";

  private final static String DOCUMENT_V3 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<example:metadata xmlns:example=\"http://www.example.org/schema/xsd/\" >\n"
          + "  <example:title>My third XML document</example:title>\n"
          + "  <example:date>2018-07-02</example:date>\n"
          + "  <example:note>since version 3 notes are allowed</example:note>\n"
          + "</example:metadata>";
  private static final ResourceIdentifier RELATED_RESOURCE = ResourceIdentifier.factoryUrlResourceIdentifier("https://repo/anyResourceId");

  @Before
  public void setUp() throws JsonProcessingException {
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
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(springSecurity()) 
            .apply(documentationConfiguration(this.restDocumentation)
                    .uris().withPort(8040).and()
                    .operationPreprocessors()
                    .withRequestDefaults(prettyPrint())
                    .withResponseDefaults(Preprocessors.removeHeaders("X-Content-Type-Options", "X-XSS-Protection", "X-Frame-Options"), prettyPrint()))
            .build();
  }

  /**
   * Workflow for documentation: // 1. Registering metadata schema // 2. Getting
   * metadata Schema Record // 3. Getting metadata Schema document // 4. Update
   * to second version of schema // 5. Update to third version of schema // 6.
   * Registering another metadata schema // 7. List all schemas (only 1 Schema)
   * // 8. List all versions of a schema // 9. Getting current schema // 10.
   * Getting specific version of a schema // 11. Validate metadata document //
   * 11 a) Validate with version=1 // 11 b) Validate without version // 12.
   * Update metadata Schema Record // Metadata management // 1. Ingest Metadata
   * document // 2. Accessing metadata document // 3. Accessing metadata record
   * // 4. Update metadata record & document // 5. Update metadata record // 6.
   * List all Versions of a record // 7. Find a metadata record.
   *
   * @throws Exception
   */
  @Test
  public void documentSchemaRegistry() throws Exception {
    MetadataSchemaRecord schemaRecord = new MetadataSchemaRecord();
    String contextPath = "/metastore";
    String endpointSchema = contextPath + "/api/v1/schemas/";
    String endpointMetadata = contextPath + "/api/v1/metadata/";
    //  1. Registering metadata schema
    //**************************************************************************
    schemaRecord.setSchemaId(EXAMPLE_SCHEMA_ID);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    MockMultipartFile schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", SCHEMA_V1.getBytes());
    MockMultipartFile recordFile = new MockMultipartFile("record", "schema-record.json", "application/json", new ByteArrayInputStream(mapper.writeValueAsString(schemaRecord).getBytes()));

    //create resource and obtain location from response header
    String location = this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema).
            file(schemaFile).
            file(recordFile).
            contextPath(contextPath)).
            andDo(document("register-schema")).
            andExpect(status().isCreated()).
            andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);
    //  2. Getting metadata Schema Record
    //**************************************************************************
    // Get single metadata schema record
    String etag = this.mockMvc.perform(get(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            contextPath(contextPath).
            accept("application/vnd.datamanager.schema-record+json")).
            andDo(document("get-schema-record")).
            andExpect(status().isOk()).
            andReturn().getResponse().getHeader("ETag");

    //  3. Getting metadata Schema document
    //**************************************************************************
    // Get metadata schema
    this.mockMvc.perform(get(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            contextPath(contextPath)).
            andDo(document("get-schema-document")).
            andExpect(status().isOk()).
            andReturn().getResponse();
    //  4. Update to second version of schema
    //**************************************************************************
    //update schema document and create new version
    schemaFile = new MockMultipartFile("schema", "schema-v2.xsd", "application/xml", SCHEMA_V2.getBytes());
    etag = this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            file(schemaFile).
            contextPath(contextPath).
            header("If-Match", etag).with(putMultipart())).
            andDo(document("update-schema-v2")).
            andExpect(status().isOk()).
            andReturn().getResponse().getHeader("ETag");
    //  5. Update to third version of schema
    //**************************************************************************
    schemaFile = new MockMultipartFile("schema", "schema-v3.xsd", "application/xml", SCHEMA_V3.getBytes());
    etag = this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            file(schemaFile).
            contextPath(contextPath).
            header("If-Match", etag).with(putMultipart())).
            andDo(document("update-schema-v3")).
            andExpect(status().isOk()).
            andReturn().getResponse().getHeader("ETag");
    //  6. Registering another metadata schema
    //**************************************************************************
    schemaRecord.setSchemaId(ANOTHER_SCHEMA_ID);

    schemaFile = new MockMultipartFile("schema", "another-schema.xsd", "application/xml", ANOTHER_SCHEMA.getBytes());
    recordFile = new MockMultipartFile("record", "another-schema-record.json", "application/json", new ByteArrayInputStream(mapper.writeValueAsString(schemaRecord).getBytes()));

    location = this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema).
            file(schemaFile).
            file(recordFile).
            contextPath(contextPath)).
            andDo(document("register-another-schema")).
            andExpect(status().isCreated()).
            andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);
    //  7. List all schema records (only current schemas)
    //**************************************************************************
    this.mockMvc.perform(get(endpointSchema).
            contextPath(contextPath)).
            andDo(document("get-all-schemas")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    this.mockMvc.perform(get(endpointSchema).
            contextPath(contextPath).
            param("page", Integer.toString(0)).
            param("size", Integer.toString(20))).
            andDo(document("get-all-schemas-pagination")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    //  8. List all versions of a schema
    //**************************************************************************
    this.mockMvc.perform(get(endpointSchema).
            contextPath(contextPath).
            param("schemaId", EXAMPLE_SCHEMA_ID)).
            andDo(document("get-all-versions-of-a-schema")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    //  9. Getting current schema
    //**************************************************************************
    this.mockMvc.perform(get(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            contextPath(contextPath)).
            andDo(document("get-schema-v3")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    // 10. Getting specific version of a schema
    //**************************************************************************
    this.mockMvc.perform(get(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            contextPath(contextPath).
            param("version", "1")).
            andDo(document("get-schema-v1")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    // 11. Validate metadata document
    //**************************************************************************
    MockMultipartFile metadataFile_v3 = new MockMultipartFile("document", "metadata-v3.xml", "application/xml", DOCUMENT_V3.getBytes());
    // 11 a) Validate with version=1 --> invalid
    //**************************************************************************
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema + "/" + EXAMPLE_SCHEMA_ID + "/validate").
            file(metadataFile_v3).
            contextPath(contextPath).
            queryParam("version", "1")).
            andDo(document("validate-document-v1")).
            andExpect(status().isUnprocessableEntity()).
            andReturn().getResponse();
    // 11 b) Validate without version --> version 3 (should be valid)
    //**************************************************************************
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema + "/" + EXAMPLE_SCHEMA_ID + "/validate").
            file(metadataFile_v3).
            contextPath(contextPath)).
            andDo(document("validate-document-v3")).
            andExpect(status().isNoContent()).
            andReturn().getResponse();
    // 12. Update metadata Schema Record
    //**************************************************************************
    // Update metadata record to allow admin to edit schema as well.
    MvcResult result = this.mockMvc.perform(get(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            contextPath(contextPath).
            header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    schemaRecord = mapper.readValue(body, MetadataSchemaRecord.class);
    schemaRecord.getAcl().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));

    recordFile = new MockMultipartFile("record", "schema-record-v4.json", "application/json", mapper.writeValueAsString(schemaRecord).getBytes());
    this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointSchema + "/" + EXAMPLE_SCHEMA_ID).
            file(recordFile).
            contextPath(contextPath).
            header("If-Match", etag).with(putMultipart())).
            andDo(document("update-schema-record")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    //**************************************************************************
    // Metadata management
    //**************************************************************************
    // 1. Ingest metadata document
    //**************************************************************************
    // Create a metadata record.
    MetadataRecord metadataRecord = new MetadataRecord();
//    record.setId("my_id");
    metadataRecord.setRelatedResource(RELATED_RESOURCE);
    metadataRecord.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(EXAMPLE_SCHEMA_ID));
    metadataRecord.setSchemaVersion(1L);

    recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(metadataRecord).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", DOCUMENT_V1.getBytes());

    location = this.mockMvc.perform(MockMvcRequestBuilders.multipart(endpointMetadata).
            file(recordFile).
            file(metadataFile).
            contextPath(contextPath)).
            andDo(document("ingest-metadata-document")).
            andExpect(status().isCreated()).
            andExpect(redirectedUrlPattern("http://*:*/**/*?version=1")).
            andReturn().getResponse().getHeader("Location");
    // Get URL
    location = location.replace("/v2/", "/v1/");
    String newLocation = location.split("[?]")[0];

    // 2. Accessing metadata document
    //**************************************************************************
    this.mockMvc.perform(get(location).accept("application/xml").
            contextPath(contextPath)).
            andDo(document("get-metadata-document")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    // 3. Accessing metadata record
    //**************************************************************************
    this.mockMvc.perform(get(location).
            contextPath(contextPath).
            accept(MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(document("get-metadata-record")).
            andExpect(status().isOk()).
            andReturn().getResponse();

    // 4. Update metadata record & document
    //**************************************************************************
    result = this.mockMvc.perform(get(location).
            contextPath(contextPath).
            header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    MetadataRecord record = mapper.readValue(body, MetadataRecord.class);
    record.getAcl().add(new AclEntry("guest", PERMISSION.READ));
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(EXAMPLE_SCHEMA_ID));
    record.setSchemaVersion(2L);
    recordFile = new MockMultipartFile("record", "metadata-record-v2.json", "application/json", mapper.writeValueAsString(record).getBytes());
    metadataFile = new MockMultipartFile("document", "metadata-v2.xml", "application/xml", DOCUMENT_V2.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(newLocation).
            file(recordFile).
            file(metadataFile).
            contextPath(contextPath).
            header("If-Match", etag).with(putMultipart())).
            andDo(print()).
            andDo(document("update-metadata-record-v2")).
            andExpect(status().isOk()).
            andReturn();
    etag = result.getResponse().getHeader("ETag");
    location = result.getResponse().getHeader("Location");
    location = location.replace("/v2/", "/v1/");
    // 5. Update metadata record
    //**************************************************************************
    // update once more to newest version of schema
    // Get Etag
    this.mockMvc.perform(get(location).
            contextPath(contextPath).
            accept(MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(document("get-metadata-record-v2")).
            andExpect(status().isOk()).
            andReturn().getResponse();
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(EXAMPLE_SCHEMA_ID));
    record.setSchemaVersion(3L);
    recordFile = new MockMultipartFile("record", "metadata-record-v3.json", "application/json", mapper.writeValueAsString(record).getBytes());
    metadataFile = new MockMultipartFile("document", "metadata-v3.xml", "application/xml", DOCUMENT_V3.getBytes());

    result = this.mockMvc.perform(MockMvcRequestBuilders.multipart(newLocation).
            file(recordFile).
            file(metadataFile).
            contextPath(contextPath).
            header("If-Match", etag).with(putMultipart())).
            andDo(print()).
            andDo(document("update-metadata-record-v3")).
            andExpect(status().isOk()).
            andReturn();
    location = result.getResponse().getHeader("Location");
    location = location.replace("/v2/", "/v1/");
    this.mockMvc.perform(get(location).
            contextPath(contextPath)).
            andDo(document("get-metadata-document-v3")).
            andExpect(status().isOk()).
            andReturn().getResponse();
    // 6. List all versions of a record
    //**************************************************************************
    String resourceId = record.getId();
    this.mockMvc.perform(get(endpointMetadata).
            contextPath(contextPath).
            param("id", resourceId)).
            andDo(print()).
            andDo(document("list-all-versions-of-metadata-document")).
            andExpect(status().isOk()).
            andReturn();

    // 7. Find a metadata record.
    //**************************************************************************
    // find all metadata for a resource
    Instant oneHourBefore = Instant.now().minusSeconds(3600);
    Instant twoHoursBefore = Instant.now().minusSeconds(7200);
    this.mockMvc.perform(get(endpointMetadata).
            contextPath(contextPath).
            param("resoureId", RELATED_RESOURCE.getIdentifier())).
            andDo(print()).
            andDo(document("find-metadata-record-resource")).
            andExpect(status().isOk()).
            andReturn();

    this.mockMvc.perform(get(endpointMetadata).
            contextPath(contextPath).
            param("from", twoHoursBefore.toString())).
            andDo(print()).
            andDo(document("find-metadata-record-from")).
            andExpect(status().isOk()).
            andReturn();

    this.mockMvc.perform(get(endpointMetadata).
            contextPath(contextPath).
            param("from", twoHoursBefore.toString()).
            param("until", oneHourBefore.toString())).
            andDo(print()).
            andDo(document("find-metadata-record-from-to")).
            andExpect(status().isOk()).
            andReturn();

  }

  private static RequestPostProcessor putMultipart() { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setMethod("PUT");
      return request;
    };
  }

}
