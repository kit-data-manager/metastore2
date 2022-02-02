/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 */
public class CreateSchemaUtil {

  public final static String KIT_SCHEMA = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
          + "<xs:schema targetNamespace=\"http://www.example.org/kit\"\n"
          + "           xmlns:kit=\"http://www.example.org/kit\"\n"
          + "           xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
          + "           elementFormDefault=\"qualified\">\n"
          + "  <xs:simpleType name=\"stringtype\">\n"
          + "    <xs:restriction base=\"xs:string\"></xs:restriction>\n"
          + "  </xs:simpleType>\n"
          + " \n"
          + "  <xs:simpleType name=\"employeeidtype\">\n"
          + "    <xs:restriction base=\"xs:string\">\n"
          + "      <xs:pattern value=\"[a-z]{2}[0-9]{4}\"></xs:pattern>\n"
          + "    </xs:restriction>\n"
          + "  </xs:simpleType>\n"
          + " \n"
          + "  <xs:simpleType name=\"shorttype\">\n"
          + "    <xs:restriction base=\"xs:string\">\n"
          + "      <xs:pattern value=\"[A-Z\\\\-]{3,8}\"></xs:pattern>\n"
          + "    </xs:restriction>\n"
          + "  </xs:simpleType>\n"
          + " \n"
          + "  <xs:complexType name=\"departmenttype\">\n"
          + "    <xs:sequence>\n"
          + "      <xs:element name=\"departmentname\" type=\"kit:stringtype\"></xs:element>\n"
          + "      <xs:element name=\"shortname\" type=\"kit:shorttype\"></xs:element>\n"
          + "    </xs:sequence>\n"
          + "  </xs:complexType>\n"
          + " \n"
          + "  <xs:complexType name=\"employeetype\">\n"
          + "    <xs:sequence>\n"
          + "      <xs:element name=\"name\" type=\"kit:stringtype\"></xs:element>\n"
          + "      <xs:element name=\"department\" type=\"kit:departmenttype\"></xs:element>\n"
          + "    </xs:sequence>\n"
          + "    <xs:attribute name=\"employeeid\" type=\"kit:employeeidtype\" use=\"required\"></xs:attribute>\n"
          + "  </xs:complexType>\n"
          + "\n"
          + "  <xs:element name=\"employee\" type=\"kit:employeetype\"></xs:element>\n"
          + "</xs:schema>";
  public final static String KIT_DOCUMENT = "<employee xmlns=\"http://www.example.org/kit\" employeeid=\"ab1234\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SCC</shortname>\n"
          + "  </department>\n"
          + "</employee>";
  public final static String KIT_DOCUMENT_VERSION_2 = "<employee xmlns=\"http://www.example.org/kit\" employeeid=\"ab1234\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SCC-DEM</shortname>\n"
          + "  </department>\n"
          + "</employee>";
  public final static String KIT_DOCUMENT_INVALID_1 = "<employee xmlns=\"http://www.example.org/kit\" employeeid=\"abcdefg\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SCC</shortname>\n"
          + "  </department>\n"
          + "</employee>";
  public final static String KIT_DOCUMENT_INVALID_2 = "<employee xmlns=\"http://www.example.org/kit\" employeeid=\"ab12345\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SCC</shortname>\n"
          + "  </department>\n"
          + "</employee>";
  public final static String KIT_DOCUMENT_INVALID_3 = "<employee xmlns=\"http://www.example.org/kit\" employeeid=\"ab1234\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SC</shortname>\n"
          + "  </department>\n"
          + "</employee>";
  public final static String KIT_DOCUMENT_INVALID_4 = "<employee xmlns=\"http://www.example.org/kit\" employeeid=\"ab1234\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SCC-TOLONG</shortname>\n"
          + "  </department>\n"
          + "</employee>";

  public final static String KIT_DOCUMENT_WRONG_NAMESPACE = "<employee xmlns=\"http://www.example.org/invalid/ns\" employeeid=\"ab1234\">\n"
          + "  <name>John Doe</name>\n"
          + "  <department>\n"
          + "  <departmentname>Steinbuch Centre for Computing</departmentname>\n"
          + "    <shortname>SCC</shortname>\n"
          + "  </department>\n"
          + "</employee>";
  public final static String XML_SCHEMA_V1 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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
  public final static String XML_SCHEMA_V2 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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
  public final static String XML_SCHEMA_V3 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
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
  public final static String XML_DOCUMENT_V1 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of first version</ex:title>\n"
          + "</ex:metadata>";
  public final static String XML_DOCUMENT_V2 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of second version</ex:title>\n"
          + "  <ex:date>2021-06-15</ex:date>\n"
          + "</ex:metadata>";
  public final static String XML_DOCUMENT_V3 = "<?xml version='1.0' encoding='utf-8'?>\n"
          + "<ex:metadata xmlns:ex=\"http://www.example.org/schema/xsd/\">\n"
          + "  <ex:title>Title of third version</ex:title>\n"
          + "  <ex:date>2021-06-16</ex:date>\n"
          + "  <ex:note>since version 3</ex:note>\n"
          + "</ex:metadata>";
  private static String userToken;

  private final static String otherUserPrincipal = "test_user";

  public static String ingestKitSchemaRecord(MockMvc mockMvc, String schemaId, String jwtSecret) throws Exception {
    return ingestSchemaRecord(mockMvc, schemaId, KIT_SCHEMA, jwtSecret);

  }

  public static String ingestSchemaRecord(MockMvc mockMvc, String schemaId, String schemaContent, String jwtSecret) throws Exception {
    String locationUri = null;
    jwtSecret = (jwtSecret == null) ? "jwtSecret" : jwtSecret;
    // Test if schema is already registered.
    MvcResult result = mockMvc.perform(get("/api/v1/schemas/" + schemaId).header("Accept", MetadataSchemaRecord.METADATA_SCHEMA_RECORD_MEDIA_TYPE)).andDo(print()).andReturn();
    if (result.getResponse().getStatus() != HttpStatus.OK.value()) {
      userToken = edu.kit.datamanager.util.JwtBuilder.createUserToken(otherUserPrincipal, RepoUserRole.USER).
              addSimpleClaim("email", "any@example.org").
              addSimpleClaim("orcid", "0000-0001-2345-6789").
              addSimpleClaim("loginFailures", 0).
              addSimpleClaim("active", true).
              addSimpleClaim("locked", false).getCompactToken(jwtSecret);
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

      result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/schemas").
              file(recordFile).
              file(schemaFile).
              header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)).
              andDo(print()).andExpect(status().isCreated()).andReturn();
      if (result.getResponse().getStatus() == HttpStatus.CREATED.value()) {
        locationUri = result.getResponse().getHeader("Location");
      }
    }
    return locationUri;
  }

}
