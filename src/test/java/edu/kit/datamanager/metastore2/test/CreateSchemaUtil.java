/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 */
public class CreateSchemaUtil {

  public final static String KIT_SCHEMA = """
          <?xml version="1.0" encoding="UTF-8" ?>
          <xs:schema targetNamespace="http://www.example.org/kit"
                     xmlns:kit="http://www.example.org/kit"
                     xmlns:xs="http://www.w3.org/2001/XMLSchema"
                     elementFormDefault="qualified">
            <xs:simpleType name="stringtype">
              <xs:restriction base="xs:string"></xs:restriction>
            </xs:simpleType>
          \s
            <xs:simpleType name="employeeidtype">
              <xs:restriction base="xs:string">
                <xs:pattern value="[a-z]{2}[0-9]{4}"></xs:pattern>
              </xs:restriction>
            </xs:simpleType>
          \s
            <xs:simpleType name="shorttype">
              <xs:restriction base="xs:string">
                <xs:pattern value="[A-Z\\\\-]{3,8}"></xs:pattern>
              </xs:restriction>
            </xs:simpleType>
          \s
            <xs:complexType name="departmenttype">
              <xs:sequence>
                <xs:element name="departmentname" type="kit:stringtype"></xs:element>
                <xs:element name="shortname" type="kit:shorttype"></xs:element>
              </xs:sequence>
            </xs:complexType>
          \s
            <xs:complexType name="employeetype">
              <xs:sequence>
                <xs:element name="name" type="kit:stringtype"></xs:element>
                <xs:element name="department" type="kit:departmenttype"></xs:element>
              </xs:sequence>
              <xs:attribute name="employeeid" type="kit:employeeidtype" use="required"></xs:attribute>
            </xs:complexType>
          
            <xs:element name="employee" type="kit:employeetype"></xs:element>
          </xs:schema>""";
  public final static String KIT_DOCUMENT = """
          <employee xmlns="http://www.example.org/kit" employeeid="ab1234">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SCC</shortname>
            </department>
          </employee>""";
  public final static String KIT_DOCUMENT_VERSION_2 = """
          <employee xmlns="http://www.example.org/kit" employeeid="ab1234">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SCC-DEM</shortname>
            </department>
          </employee>""";
  public final static String KIT_DOCUMENT_SMALL_CHANGE = """
          <employee xmlns="http://www.example.org/kit" employeeid="ab1234">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>DEM</shortname>
            </department>
          </employee>""";
  public final static String KIT_DOCUMENT_INVALID_1 = """
          <employee xmlns="http://www.example.org/kit" employeeid="abcdefg">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SCC</shortname>
            </department>
          </employee>""";
  public final static String KIT_DOCUMENT_INVALID_2 = """
          <employee xmlns="http://www.example.org/kit" employeeid="ab12345">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SCC</shortname>
            </department>
          </employee>""";
  public final static String KIT_DOCUMENT_INVALID_3 = """
          <employee xmlns="http://www.example.org/kit" employeeid="ab1234">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SC</shortname>
            </department>
          </employee>""";
  public final static String KIT_DOCUMENT_INVALID_4 = """
          <employee xmlns="http://www.example.org/kit" employeeid="ab1234">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SCC-TOLONG</shortname>
            </department>
          </employee>""";

  public final static String KIT_DOCUMENT_WRONG_NAMESPACE = """
          <employee xmlns="http://www.example.org/invalid/ns" employeeid="ab1234">
            <name>John Doe</name>
            <department>
            <departmentname>Scientific Computing Center</departmentname>
              <shortname>SCC</shortname>
            </department>
          </employee>""";
  public final static String XML_SCHEMA_V1 = """
          <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                          xmlns="http://www.example.org/schema/xsd/"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema"
                          elementFormDefault="qualified" attributeFormDefault="unqualified">
                <xs:element name="metadata">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="title" type="xs:string"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:element>
              </xs:schema>""";
  public final static String XML_SCHEMA_V1_TYPO = """
          <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                          xmlns="http://www.example.org/schema/xsd/"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema"
                          elementFormDefault="qualified" attributeFormDefault="unqualified">
                <xs:element name="metadata">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="titel" type="xs:string"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:element>
              </xs:schema>""";
  public final static String XML_SCHEMA_V2 = """
          <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                          xmlns="http://www.example.org/schema/xsd/"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema"
                          elementFormDefault="qualified" attributeFormDefault="unqualified">
                <xs:element name="metadata">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="title" type="xs:string"/>
                      <xs:element name="date" type="xs:date"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:element>
              </xs:schema>""";
  public final static String XML_SCHEMA_V3 = """
          <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                          xmlns="http://www.example.org/schema/xsd/"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema"
                          elementFormDefault="qualified" attributeFormDefault="unqualified">
                <xs:element name="metadata">
                  <xs:complexType>
                    <xs:sequence>
                      <xs:element name="title" type="xs:string"/>
                      <xs:element name="date" type="xs:date"/>
                      <xs:element name="note" type="xs:string"/>
                    </xs:sequence>
                  </xs:complexType>
                </xs:element>
              </xs:schema>""";
  public final static String XML_DOCUMENT_V1 = """
          <?xml version='1.0' encoding='utf-8'?>
          <ex:metadata xmlns:ex="http://www.example.org/schema/xsd/">
            <ex:title>Title of first version</ex:title>
          </ex:metadata>""";
  public final static String XML_DOCUMENT_V2 = """
          <?xml version='1.0' encoding='utf-8'?>
          <ex:metadata xmlns:ex="http://www.example.org/schema/xsd/">
            <ex:title>Title of second version</ex:title>
            <ex:date>2021-06-15</ex:date>
          </ex:metadata>""";
  public final static String XML_DOCUMENT_V3 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of third version</ex:title>\n"
          + "  <ex:date>2021-06-16</ex:date>\n"
          + "  <ex:note>since version 3</ex:note>\n"
          + "</ex:metadata>";
  private static String userToken;

  private final static String otherUserPrincipal = "test_user";

  public static String ingestKitSchemaRecord(MockMvc mockMvc, String schemaId, String jwtSecret) throws Exception {
    return ingestXmlSchemaRecord(mockMvc, schemaId, KIT_SCHEMA, jwtSecret);

  }

  /**
   * Ingest schema in MetaStore as user 'test_user' If schema already exists
   * update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @return
   * @throws Exception
   */
  public static String ingestXmlSchemaRecord(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret) throws Exception {
    return ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, schemaContent, jwtSecret, false, status().isCreated());
  }

  /**
   * Update schema in MetaStore as user 'test_user'. If schema already exists
   * and noUpdate is false update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @param noUpdate      Only ingest or do update also
   * @return
   * @throws Exception
   */
  public static String ingestOrUpdateXmlSchemaRecord(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    String locationUri = null;
    jwtSecret = (jwtSecret == null) ? "jwtSecret" : jwtSecret;
    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "any@example.org").
            addSimpleClaim("orcid", "0000-0001-2345-6789").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(jwtSecret);
    return ingestOrUpdateXmlSchemaRecord(mockMvc, schemaId, schemaContent, update, userToken, expectedStatus);
  }

  /**
   * Update schema in MetaStore as user 'test_user'. If schema already exists
   * and noUpdate is false update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @param noUpdate      Only ingest or do update also
   * @return
   * @throws Exception
   */
  public static String ingestOrUpdateXmlSchemaRecord(MockMvc mockMvc, String schemaId, String schemaContent, boolean update, String userToken, ResultMatcher expectedStatus) throws Exception {
    String locationUri = null;
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId(schemaId);
    record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
    record.setMimeType(MediaType.APPLICATION_XML.toString());
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));
    record.setAcl(aclEntries);

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile;
    MockMultipartFile schemaFile;
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", schemaContent.getBytes());
    // Test if schema is already registered.
    MvcResult result = mockMvc.perform(get("/api/v1/schemas/" + schemaId).
                    header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andReturn();
    if (result.getResponse().getStatus() != HttpStatus.OK.value()) {

      result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas/").
                      file(recordFile).
                      file(schemaFile).
                      header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
              andDo(print()).andExpect(expectedStatus).andReturn();
      if (result.getResponse().getStatus() == HttpStatus.CREATED.value()) {
        locationUri = result.getResponse().getHeader("Location");
      }
    } else {
      if (update) {
        String etag = result.getResponse().getHeader("ETag");
        String body = result.getResponse().getContentAsString();
        record = mapper.readValue(body, MetadataSchemaRecord.class);
        recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
        // Update metadata document
        MockHttpServletRequestBuilder header = MockMvcRequestBuilders.
                multipart("/api/v1/schemas/" + schemaId).
                file(recordFile).
                file(schemaFile).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
                header("If-Match", etag).
                with(putMultipart());
        mockMvc.perform(header).
                andDo(print()).
                andExpect(expectedStatus).
                andReturn();
      }

    }
    return locationUri;
  }

  public static MvcResult ingestXmlMetadataDocument(MockMvc mockMvc, String schemaId, Long version, String metadataId, String metadataDocument, String jwtSecret) throws Exception {
    return ingestOrUpdateXmlMetadataDocument(mockMvc, schemaId, version, metadataId, metadataDocument, jwtSecret, false, status().isCreated());
  }

  public static MvcResult ingestOrUpdateXmlMetadataDocument(MockMvc mockMvc, String schemaId, Long version, String metadataId, String metadataDocument, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    jwtSecret = (jwtSecret == null) ? "jwtSecret" : jwtSecret;
    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "any@example.org").
            addSimpleClaim("orcid", "0000-0001-2345-6789").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(jwtSecret);
    return ingestOrUpdateXmlMetadataDocument(mockMvc, schemaId, version, metadataId, metadataDocument, update, userToken, expectedStatus);
  }

  public static MvcResult ingestOrUpdateXmlMetadataDocument(MockMvc mockMvc, String schemaId, Long version, String metadataId, String metadataDocument, boolean update, String userToken, ResultMatcher expectedStatus) throws Exception {
    // Test if metadataId is already registered.
    MvcResult result = null;
    MetadataRecord record = new MetadataRecord();
    record.setId(metadataId);
    record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
    if (version != null) {
      record.setSchemaVersion(version);
    }
    record.setRelatedResource(ResourceIdentifier.factoryInternalResourceIdentifier("any"));
    Set<AclEntry> aclEntries = new HashSet<>();
    aclEntries.add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));
    record.setAcl(aclEntries);
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile;
    MockMultipartFile metadataFile = null;
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    if (metadataDocument != null) {
      metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", metadataDocument.getBytes());
    }
    result = mockMvc.perform(get("/api/v1/metadata/" + metadataId).
                    header("Accept", MetadataRecord.METADATA_RECORD_MEDIA_TYPE)).
            andDo(print()).
            andReturn();
    if (result.getResponse().getStatus() != HttpStatus.OK.value()) {
      // Create metadata document
      MockMultipartHttpServletRequestBuilder file = MockMvcRequestBuilders.multipart("/api/v1/metadata/").file(recordFile);
      if (metadataFile != null) {
        file = file.file(metadataFile);
      }
      MockHttpServletRequestBuilder header = file.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken);
      result = mockMvc.perform(header).
              andDo(print()).
              andExpect(expectedStatus).
              andReturn();
    } else {
      if (update) {
        String etag = result.getResponse().getHeader("ETag");
        String body = result.getResponse().getContentAsString();
        record = mapper.readValue(body, MetadataRecord.class);
        record.setSchema(ResourceIdentifier.factoryInternalResourceIdentifier(schemaId));
        if (version != null) {
          record.setSchemaVersion(version);
        }
        recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
        // Update metadata document
        MockMultipartHttpServletRequestBuilder file = MockMvcRequestBuilders.multipart("/api/v1/metadata/" + metadataId).file(recordFile);
        if (metadataFile != null) {
          file = file.file(metadataFile);
        }
        MockHttpServletRequestBuilder header = file.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
                header("If-Match", etag).
                with(putMultipart());
        result = mockMvc.perform(header).
                andDo(print()).
                andExpect(expectedStatus).
                andReturn();
      }

    }
    return result;
  }

  public static String ingestKitSchemaRecordV2(MockMvc mockMvc, String schemaId, String jwtSecret) throws Exception {
    return ingestXmlSchemaRecordV2(mockMvc, schemaId, KIT_SCHEMA, jwtSecret);

  }

  /**
   * Ingest schema in MetaStore as user 'test_user' If schema already exists
   * update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @return
   * @throws Exception
   */
  public static String ingestXmlSchemaRecordV2(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret) throws Exception {
    return ingestOrUpdateXmlSchemaRecordV2(mockMvc, schemaId, schemaContent, jwtSecret, false, status().isCreated());
  }

  /**
   * Update schema in MetaStore as user 'test_user'. If schema already exists
   * and noUpdate is false update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @param noUpdate      Only ingest or do update also
   * @return
   * @throws Exception
   */
  public static String ingestOrUpdateXmlSchemaRecordV2(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    return ingestOrUpdateSchemaRecordV2(mockMvc, MediaType.APPLICATION_XML, schemaId, schemaContent, jwtSecret, update, expectedStatus);
  }

  /**
   * Update schema in MetaStore as user 'test_user'. If schema already exists
   * and noUpdate is false update schema.
   *
   * @param mockMvc
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @param noUpdate      Only ingest or do update also
   * @return
   * @throws Exception
   */
  public static String ingestOrUpdateJsonSchemaRecordV2(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    return ingestOrUpdateSchemaRecordV2(mockMvc, MediaType.APPLICATION_JSON, schemaId, schemaContent, jwtSecret, update, expectedStatus);
  }

  /**
   * Update schema in MetaStore as user 'test_user'. If schema already exists
   * and noUpdate is false update schema.
   *
   * @param mockMvc
   * @param mediaType
   * @param schemaId
   * @param schemaContent
   * @param jwtSecret
   * @param noUpdate      Only ingest or do update also
   * @return
   * @throws Exception
   */
  public static String ingestOrUpdateSchemaRecordV2(MockMvc mockMvc, MediaType mediaType, String schemaId, String schemaContent, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    String locationUri = null;
    jwtSecret = (jwtSecret == null) ? "jwtSecret" : jwtSecret;
    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "any@example.org").
            addSimpleClaim("orcid", "0000-0001-2345-6789").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(jwtSecret);
    return ingestOrUpdateSchemaRecordV2(mockMvc, mediaType, schemaId, schemaContent, update, userToken, expectedStatus);
  }

    /**
     * Update schema in MetaStore as user 'test_user'. If schema already exists
     * and noUpdate is false update schema.
     *
     * @param mockMvc
     * @param mediaType
     * @param schemaId
     * @param schemaContent
     * @param jwtSecret
     * @param noUpdate      Only ingest or do update also
     * @return
     * @throws Exception
     */
    public static String ingestOrUpdateSchemaRecordV2(MockMvc mockMvc, MediaType mediaType, String schemaId, String schemaContent, boolean update, String userToken, ResultMatcher expectedStatus) throws Exception {
      String locationUri = null;
    DataResource record;
    if (mediaType.toString().contains("xml")) {
      record = SchemaRegistryControllerTestV2.createDataResource4XmlSchema(schemaId);
    } else {
      record = SchemaRegistryControllerTestV2.createDataResource4JsonSchema(schemaId);
    }
    record.getAcls().add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));

    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile;
    MockMultipartFile schemaFile;
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    schemaFile = new MockMultipartFile("schema", "schema.xsd", "application/xml", schemaContent.getBytes());
    // Test if schema is already registered.
    MvcResult result = mockMvc.perform(get("/api/v2/schemas/" + schemaId).
                    accept(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andReturn();
    if (result.getResponse().getStatus() != HttpStatus.OK.value()) {

      result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v2/schemas/").
                      file(recordFile).
                      file(schemaFile).
                      header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
              andDo(print()).andExpect(expectedStatus).andReturn();
      if (result.getResponse().getStatus() == HttpStatus.CREATED.value()) {
        locationUri = result.getResponse().getHeader("Location");
      }
    } else {
      if (update) {
        String etag = result.getResponse().getHeader("ETag");
        String body = result.getResponse().getContentAsString();
        record = mapper.readValue(body, DataResource.class);
        recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
        // Update metadata document
        MockHttpServletRequestBuilder header = MockMvcRequestBuilders.
                multipart("/api/v2/schemas/" + schemaId).
                file(recordFile).
                file(schemaFile).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
                header("If-Match", etag).
                with(putMultipart());
        mockMvc.perform(header).
                andDo(print()).
                andExpect(expectedStatus).
                andReturn();
      }

    }
    return locationUri;
  }

  public static MvcResult ingestXmlMetadataDocumentV2(MockMvc mockMvc, String schemaId, Long version, String metadataId, String metadataDocument, String jwtSecret) throws Exception {
    return ingestOrUpdateXmlMetadataDocumentV2(mockMvc, schemaId, version, metadataId, metadataDocument, jwtSecret, false, status().isCreated());
  }

  public static MvcResult ingestOrUpdateXmlMetadataDocumentV2(MockMvc mockMvc, String schemaId, Long version, String metadataId, String metadataDocument, String jwtSecret, boolean update, ResultMatcher expectedStatus) throws Exception {
    jwtSecret = (jwtSecret == null) ? "jwtSecret" : jwtSecret;
    userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
            addSimpleClaim("email", "any@example.org").
            addSimpleClaim("orcid", "0000-0001-2345-6789").
            addSimpleClaim("loginFailures", 0).
            addSimpleClaim("active", true).
            addSimpleClaim("locked", false).getCompactToken(jwtSecret);
    return ingestOrUpdateXmlMetadataDocumentV2(mockMvc, schemaId, version, metadataId, metadataDocument, update, userToken, expectedStatus);

  }

    public static MvcResult ingestOrUpdateXmlMetadataDocumentV2(MockMvc mockMvc, String schemaId, Long version, String metadataId, String metadataDocument, boolean update, String userToken, ResultMatcher expectedStatus) throws Exception {

    // Test if metadataId is already registered.
    MvcResult result = null;
    String versionAsString = null;
    if (version != null) {
      versionAsString = version.toString();
    }

    DataResource record = SchemaRegistryControllerTestV2.createDataResource4Document(metadataId, schemaId, versionAsString);
    if (versionAsString != null) {
      record.setVersion(versionAsString);
    }
    RelatedIdentifier relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_DATA_RESOURCE_TYPE);
    relatedIdentifier.setValue("any");
    relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.INTERNAL);
    record.getAcls().add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));
    ObjectMapper mapper = new ObjectMapper();

    MockMultipartFile recordFile;
    MockMultipartFile metadataFile = null;
    recordFile = new MockMultipartFile("record", "record.json", "application/json", mapper.writeValueAsString(record).getBytes());
    if (metadataDocument != null) {
      metadataFile = new MockMultipartFile("document", "metadata.xml", "application/xml", metadataDocument.getBytes());
    }
    result = mockMvc.perform(get("/api/v2/metadata/" + metadataId).
                    accept(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE)).
            andDo(print()).
            andReturn();
    if (result.getResponse().getStatus() != HttpStatus.OK.value()) {
      // Create metadata document
      MockMultipartHttpServletRequestBuilder file = MockMvcRequestBuilders.multipart("/api/v2/metadata/").file(recordFile);
      if (metadataFile != null) {
        file = file.file(metadataFile);
      }
      MockHttpServletRequestBuilder header = file.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
              accept(DataResourceRecordUtil.DATA_RESOURCE_MEDIA_TYPE);
      result = mockMvc.perform(header).
              andDo(print()).
              andExpect(expectedStatus).
              andReturn();
    } else {
      if (update) {
        String etag = result.getResponse().getHeader("ETag");
        String body = result.getResponse().getContentAsString();
        record = mapper.readValue(body, DataResource.class);
        relatedIdentifier = DataResourceRecordUtil.getRelatedIdentifier(record, DataResourceRecordUtil.RELATED_SCHEMA_TYPE);
        if ((schemaId != null) && schemaId.startsWith("http")) {
          relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.URL);
        } else {
          relatedIdentifier.setIdentifierType(Identifier.IDENTIFIER_TYPE.INTERNAL);
          if (versionAsString != null) {
            relatedIdentifier.setValue(schemaId + DataResourceRecordUtil.SCHEMA_VERSION_SEPARATOR + versionAsString);
          } else {
            relatedIdentifier.setValue(schemaId);
          }
        }
        if (versionAsString != null) {
          record.setVersion(versionAsString);
        }
        recordFile = new MockMultipartFile("record", "metadata-record.json", "application/json", mapper.writeValueAsString(record).getBytes());
        // Update metadata document
        MockMultipartHttpServletRequestBuilder file = MockMvcRequestBuilders.multipart("/api/v2/metadata/" + metadataId).file(recordFile);
        if (metadataFile != null) {
          file = file.file(metadataFile);
        }
        MockHttpServletRequestBuilder header = file.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken).
                header("If-Match", etag).
                with(putMultipart());
        result = mockMvc.perform(header).
                andDo(print()).
                andExpect(expectedStatus).
                andReturn();
      }

    }
    return result;
  }

  private static RequestPostProcessor putMultipart() { // it's nice to extract into a helper
    return (MockHttpServletRequest request) -> {
      request.setMethod("PUT");
      return request;
    };
  }

}
