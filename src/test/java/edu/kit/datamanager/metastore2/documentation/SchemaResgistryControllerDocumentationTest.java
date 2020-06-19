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
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.acl.AclEntry;
import edu.kit.datamanager.service.IAuditService;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 *
 * @author jejkal
 */
//@ActiveProfiles("doc")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class,
  TransactionalTestExecutionListener.class,
  WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
public class SchemaResgistryControllerDocumentationTest{

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
  private final static String TEMP_DIR_4_SCHEMAS = "/tmp/metastore2/";
  private static final String INVALID_SCHEMA = "invalid_dc";
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

  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private IAuditService<MetadataSchemaRecord> schemaAuditService;


  @Before
  public void setUp() throws JsonProcessingException{
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .addFilters(springSecurityFilterChain)
            .apply(documentationConfiguration(this.restDocumentation))
            .build();
  }

  @Test
  public void documentBasicAccess() throws Exception{
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("my_dc");
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry("test",PERMISSION.READ));
    aclEntries.add(new AclEntry("SELF",PERMISSION.ADMINISTRATE));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();
     mapper.registerModule(new JavaTimeModule());

    MockMultipartFile recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    MockMultipartFile schemaFile = new MockMultipartFile("schema", DC_SCHEMA.getBytes());

      
    //create resource and obtain location from response header
    String location = this.mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
            file(recordFile).
            file(schemaFile)).
            andExpect(status().isCreated()).
            andDo(document("create-schema", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).
            andReturn().getResponse().getHeader("Location");

    Assert.assertNotNull(location);

    //extract resourceId from response header and use it to issue a GET to obtain the current ETag
    String resourceId = location.substring(location.lastIndexOf("/") + 1);
//    String etag = this.mockMvc.perform(get("/api/v1/dataresources/" + resourceId)).andExpect(status().isOk()).andDo(document("get-resource", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()))).andReturn().getResponse().getHeader("ETag");
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
