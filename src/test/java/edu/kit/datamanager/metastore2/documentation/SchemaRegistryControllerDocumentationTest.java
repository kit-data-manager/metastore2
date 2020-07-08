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
import edu.kit.datamanager.service.IAuditService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author jejkal
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
@TestPropertySource(properties = {"metastore.metadata.schemaRegistries=http://localhost:41405/api/v1/"})
@TestPropertySource(properties = {"server.port=41405"})
public class SchemaRegistryControllerDocumentationTest{

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
  private final static String TEMP_DIR_4_SCHEMAS = "/tmp/metastore2/";
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
  @Autowired
  private IAuditService<MetadataSchemaRecord> schemaAuditService;


  @Before
  public void setUp() throws JsonProcessingException{
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
  public void documentSchemaRegistry() throws Exception{
    MetadataSchemaRecord schemaRecord = new MetadataSchemaRecord();
    schemaRecord.setSchemaId(EXAMPLE_SCHEMA_ID);
    schemaRecord.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    schemaRecord.setMimeType(MediaType.APPLICATION_XML.toString());
    ObjectMapper mapper = new ObjectMapper();
     mapper.registerModule(new JavaTimeModule());
     System.out.println("xxxx"+ mapper.writeValueAsString(schemaRecord) + "xxxx");

    MockMultipartFile schemaFile = new MockMultipartFile("schema", EXAMPLE_SCHEMA.getBytes());
    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", new ByteArrayInputStream(mapper.writeValueAsString(schemaRecord).getBytes()));

      
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
    
    // Get single metadata schema record
    String etag = this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID).accept("application/vnd.datamanager.schema-record+json")).andExpect(status().isOk()).andDo(document("get-schema-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");
    
    // Get metadata schema
    this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID)).andExpect(status().isOk()).andDo(document("get-schema", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

      
    //update schema document and create new version
    schemaFile = new MockMultipartFile("schema", NEW_EXAMPLE_SCHEMA.getBytes());
    etag = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(schemaFile).
            file(recordFile).header("If-Match", etag)).
            andExpect(status().isCreated()).
            andDo(document("update-schema", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).
            andReturn().getResponse().getHeader("ETag");
    System.out.println(location);
    
    // Get metadata schema version 2
    this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID)).andExpect(status().isOk()).andDo(document("get-schemav2", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Get metadata schema version 1
    this.mockMvc.perform(get("/api/v1/schemas/" + EXAMPLE_SCHEMA_ID).param("version", "1")).andExpect(status().isOk()).andDo(document("get-schemav1", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Validate XML against schema version 1 (is invalid)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/"+ EXAMPLE_SCHEMA_ID + "/validate").file("document", DC_DOCUMENT_V2.getBytes()).queryParam("version", "1")).andDo(document("validate-documentv1", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Validate XML against schema version 2 (should be valid)
    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/"+ EXAMPLE_SCHEMA_ID + "/validate").file("document", DC_DOCUMENT_V2.getBytes())).andDo(document("validate-documentv2", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();

    // Update metadata record to allow admin to edit schema as well.
    MvcResult result = this.mockMvc.perform(get("/api/v1/schemas/"+ EXAMPLE_SCHEMA_ID).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andExpect(status().isOk()).andReturn();
    etag = result.getResponse().getHeader("ETag");
    String body = result.getResponse().getContentAsString();

    mapper = new ObjectMapper();
    schemaRecord = mapper.readValue(body, MetadataSchemaRecord.class);
    schemaRecord.getAcl().add(new AclEntry("admin", PERMISSION.ADMINISTRATE));

    this.mockMvc.perform(put("/api/v1/schemas/"+ EXAMPLE_SCHEMA_ID).header("If-Match", etag).contentType(MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE).content(mapper.writeValueAsString(schemaRecord))).andDo(document("update-schema-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();
    
    
    // Create a metadata record.
        MetadataRecord metadataRecord = new MetadataRecord();
//    record.setId("my_id");
    metadataRecord.setSchemaId(EXAMPLE_SCHEMA_ID);
    metadataRecord.setRelatedResource(RELATED_RESOURCE);
    
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(metadataRecord).getBytes());
    MockMultipartFile metadataFile = new MockMultipartFile("document", DC_DOCUMENT_V1.getBytes());

    this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/metadata/").
            file(recordFile).
            file(metadataFile)).andDo(document("create-metadata-record", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andExpect(status().isCreated()).andExpect(redirectedUrlPattern("/**/*?version=1")).andReturn();
    
//
//    //apply a simple patch to the resource
//    String patch = "[{\"op\": \"replace\",\"path\": \"/publicationYear\",\"value\": \"2017\"}]";
//    this.mockMvc.perform(patch("/api/v1/dataresources/" + resourceId).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent()).andDo(document("patch-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //perform a GET for the patched resource...the publicationYear should be modified
//    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-patched-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");
//
//    //do some more complex patch adding a new alternate identifier
//    patch = "[{\"op\": \"add\",\"path\": \"/alternateIdentifiers/1\",\"value\": {\"identifierType\":\"OTHER\", \"value\":\"resource-1-231118\"}}]";
//    this.mockMvc.perform(patch("/api/v1/dataresources/" + resourceId).header("If-Match", etag).contentType("application/json-patch+json").content(patch)).andDo(print()).andExpect(status().isNoContent()).andDo(document("patch-resource-complex", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //get the resource again together with the current ETag
//    MockHttpServletResponse response = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-patched-resource-complex", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse();
//
//    //perform PUT operation 
//    etag = response.getHeader("ETag");
//    String resourceString = response.getContentAsString();
//    DataResource resourceToPut = mapper.readValue(resourceString, DataResource.class);
//    resourceToPut.setPublisher("KIT Data Manager");
//    this.mockMvc.perform(put("/api/v1/dataresources/" + resourceId).header("If-Match", etag).contentType("application/json").content(mapper.writeValueAsString(resourceToPut))).andDo(print()).andExpect(status().isOk()).andDo(document("put-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-put-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");
//
//    //try to GET the resource using the alternate identifier added a second ago
//    this.mockMvc.perform(get("/api/v1/dataresources/" + "resource-1-231118")).andExpect(status().isSeeOther()).andDo(document("get-resource-by-alt-id", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //find by example
//    DataResource example = new DataResource();
//    example.setResourceType(ResourceType.createResourceType("testingSample"));
//    this.mockMvc.perform(post("/api/v1/dataresources/search").contentType("application/json").content(mapper.writeValueAsString(example))).
//            andExpect(status().isOk()).
//            andDo(document("find-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //upload random data file
//    Path temp = Files.createTempFile("randomFile", "test");
//
//    try(FileWriter w = new FileWriter(temp.toFile())){
//      w.write(RandomStringUtils.randomAlphanumeric(64));
//      w.flush();
//    }
//
//    MockMultipartFile fstmp = new MockMultipartFile("file", "randomFile.txt", "multipart/form-data", Files.newInputStream(temp));
//    this.mockMvc.perform(multipart("/api/v1/dataresources/" + resourceId + "/data/randomFile.txt").file(fstmp)).andDo(print()).andExpect(status().isCreated()).andDo(document("upload-file", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //upload random data file with metadata
//    ContentInformation cinfo = new ContentInformation();
//    Map<String, String> metadata = new HashMap<>();
//    metadata.put("test", "ok");
//    cinfo.setVersioningService("none");
//    cinfo.setMetadata(metadata);
//    fstmp = new MockMultipartFile("file", "randomFile2.txt", "multipart/form-data", Files.newInputStream(temp));
//    MockMultipartFile secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));
//    this.mockMvc.perform(multipart("/api/v1/dataresources/" + resourceId + "/data/randomFile2.txt").file(fstmp).file(secmp)).andDo(print()).andExpect(status().isCreated()).andDo(document("upload-file-with-metadata", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //upload referenced file
//    cinfo = new ContentInformation();
//    cinfo.setVersioningService("none");
//    cinfo.setContentUri("https://www.google.com");
//    secmp = new MockMultipartFile("metadata", "metadata.json", "application/json", mapper.writeValueAsBytes(cinfo));
//    this.mockMvc.perform(multipart("/api/v1/dataresources/" + resourceId + "/data/referencedContent").file(secmp)).andDo(print()).andExpect(status().isCreated()).andDo(document("upload-file-with-reference", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //obtain content metadata
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId + "/data/randomFile.txt").header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andExpect(status().isOk()).andDo(document("get-content-metadata", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //obtain content metadata as listing
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId + "/data/").header(HttpHeaders.ACCEPT, "application/vnd.datamanager.content-information+json")).andExpect(status().isOk()).andDo(document("get-content-listing", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //download file
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId + "/data/randomFile.txt")).andExpect(status().isOk()).andDo(document("download-file", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //get audit information
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).header(HttpHeaders.ACCEPT, "application/vnd.datamanager.audit+json")).andExpect(status().isOk()).andDo(document("get-audit-information", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //get particular version
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId).param("version", "2")).andExpect(status().isOk()).andDo(document("get-resource-version", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //get particular version
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-current-resource-version", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //perform a DELETE
//    this.mockMvc.perform(delete("/api/v1/dataresources/" + resourceId).header("If-Match", etag)).andExpect(status().isNoContent()).andDo(document("delete-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//    //perform another GET to show that resources are still accessible by the owner/admin
//    etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-deleted-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");
//
//    //perform a DELETE a second time
//    this.mockMvc.perform(delete("/api/v1/dataresources/" + resourceId).header("If-Match", etag)).andExpect(status().isNoContent()).andDo(document("delete-resource-twice", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
//
//    //perform a final GET to show that resources is no longer accessible if it is gone
//    this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isNotFound()).andDo(document("get-gone-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));

  }

}
