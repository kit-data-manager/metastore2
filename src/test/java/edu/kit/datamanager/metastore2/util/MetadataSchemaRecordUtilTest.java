/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author hartmann-v
 */
public class MetadataSchemaRecordUtilTest {

  public MetadataSchemaRecordUtilTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Constructor
   */
  @Test
  public void testConstructor() {
    assertNotNull(new MetadataSchemaRecordUtil());
  }

  /**
   * Test of migrateToDataResource method, of class MetadataSchemaRecordUtil.
   */
  @Test
  public void testMigrateToDataResource() {
    System.out.println("migrateToDataResource");
    System.out.println("Test moved to SchemaRegistryControllerTest");
    // due to mandatory application properties.
  }

  /**
   * Test of migrateToMetadataSchemaRecord method, of class
   * MetadataSchemaRecordUtil.
   */
  @Test
  public void testMigrateToMetadataSchemaRecord() {
    System.out.println("migrateToMetadataSchemaRecord");
    System.out.println("Test moved to SchemaRegistryControllerTest");
    // due to mandatory application properties.
  }

  /**
   * Test of mergeRecords method, of class MetadataSchemaRecordUtil.
   */
  @Test
  public void testMergeRecords() {
    System.out.println("mergeRecords");

    MetadataSchemaRecord managed = null;
    MetadataSchemaRecord provided = null;
    MetadataSchemaRecord expResult = null;
    MetadataSchemaRecord result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);

    managed = null;
    provided = createSchemaRecord(1, 3, 5, 7, 11, 12);
    expResult = createSchemaRecord(1, 3, 5, 7, 11, 12);
    result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);

    managed = createSchemaRecord(1, 3, 5, 7, 11, 12);
    provided = null;
    result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);

    managed = new MetadataSchemaRecord();
    provided = createSchemaRecord(1, 3, 5, 7, 11, 12);
    expResult = createSchemaRecord(1, 3, 5, 7, 11, 12);
    result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);

    managed = new MetadataSchemaRecord();
    provided = createSchemaRecord(1, 3, 5, 7, 11, 12);
    expResult = createSchemaRecord(1, 2, 3, 4, 5, 6, 7, 11, 12);
    result = MetadataSchemaRecordUtil.mergeRecords(provided, managed);
    assertEquals(expResult, result);

    managed = createSchemaRecord(1, 3, 5, 7, 11, 12);
    provided = createSchemaRecord(1, 3, 5, 7, 11, 12);
    expResult = createSchemaRecord(1, 3, 5, 7, 11, 12);
    result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);

    managed = createSchemaRecord(1, 3, 5, 7, 11, 12);
    provided = createSchemaRecord(1, 3, 5, 7, 11, 12);
    provided.setDoNotSync(Boolean.FALSE);
    expResult.setDoNotSync(Boolean.FALSE);
    result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
    assertEquals(expResult, result);
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testtestCreateMetadataSchemaRecordNull() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    String resourceId = "any";
    String eTag = "neverMind";
    MultipartFile recordDocument = null;
    MultipartFile schemaDocument = null;
    MetadataSchemaRecordUtil.createMetadataSchemaRecord(conf, recordDocument, schemaDocument, null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataSchemaRecordEmpty() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    String resourceId = "any";
    String eTag = "neverMind";
    MultipartFile recordDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MetadataSchemaRecordUtil.createMetadataSchemaRecord(conf, recordDocument, schemaDocument, null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testCreateMetadataSchemaRecordInvalid() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    String resourceId = "any";
    String eTag = "neverMind";
    MultipartFile recordDocument = new MockMultipartFile("schema", "schema.json", "application/json", "{something really strange}".getBytes());
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MetadataSchemaRecordUtil.createMetadataSchemaRecord(conf, recordDocument, schemaDocument, null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testtestUpdateMetadataSchemaRecordNull() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    String resourceId = "any";
    String eTag = "neverMind";
    MultipartFile recordDocument = null;
    MultipartFile schemaDocument = null;
    MetadataSchemaRecordUtil.updateMetadataSchemaRecord(conf, resourceId, eTag, recordDocument, schemaDocument, null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataSchemaRecordEmpty() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    String resourceId = "any";
    String eTag = "neverMind";
    MultipartFile recordDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MetadataSchemaRecordUtil.updateMetadataSchemaRecord(conf, resourceId, eTag, recordDocument, schemaDocument, null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testUpdateMetadataSchemaRecordInvalid() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    String resourceId = "any";
    String eTag = "neverMind";
    MultipartFile recordDocument = new MockMultipartFile("schema", "schema.json", "application/json", "{something really strange}".getBytes());
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MetadataSchemaRecordUtil.updateMetadataSchemaRecord(conf, resourceId, eTag, recordDocument, schemaDocument, null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateResourceIdentifierNull() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, null, (ResourceIdentifier) null, 1L);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateResourceIdentifierNull_2() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, null, (String) null, 1L);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateResourceIdentifierNoValue() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    ResourceIdentifier identifier = ResourceIdentifier.factoryInternalResourceIdentifier(null);
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, schemaDocument, identifier, 1L);
    fail("Don't reach this line!");
  }

  @Test(expected = NullPointerException.class)
  public void testValidateResourceIdentifierNoType() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    ResourceIdentifier identifier = ResourceIdentifier.factoryResourceIdentifier("any", null);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateMetadataDocumentNull() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaDocumentUri("any");
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, (MultipartFile) null, schemaRecord);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateMetadataDocumentEmpty() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaDocumentUri("any");
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "".getBytes());
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, schemaDocument, schemaRecord);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateMetadataDocumentSchemaRecordNull() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    SchemaRecord schemaRecord = null;
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "any content".getBytes());
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, schemaDocument, schemaRecord);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateMetadataDocumentSchemaRecordUriNull() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    SchemaRecord schemaRecord = new SchemaRecord();
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "any content".getBytes());
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, schemaDocument, schemaRecord);
    fail("Don't reach this line!");
  }

  @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
  public void testValidateMetadataDocumentSchemaRecordUriEmpty() {
    MetastoreConfiguration conf = new MetastoreConfiguration();
    SchemaRecord schemaRecord = new SchemaRecord();
    schemaRecord.setSchemaDocumentUri("   ");
    MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", "any content".getBytes());
    MetadataSchemaRecordUtil.validateMetadataDocument(conf, schemaDocument, schemaRecord);
    fail("Don't reach this line!");
  }

  @Test
  public void testIdentifierTypes() {
    MetadataSchemaRecord mr = new MetadataSchemaRecord();
    ResourceIdentifier.IdentifierType[] values = ResourceIdentifier.IdentifierType.values();
    for (ResourceIdentifier.IdentifierType item : values) {
      assertNotNull(item.value() + " is not defined in DataResource!", Identifier.IDENTIFIER_TYPE.valueOf(item.name()));
    }

  }

  private MetadataSchemaRecord buildMSR(Set<AclEntry> aclEntry, String comment, Instant creationDate, String definition,
          String eTag, String label, Instant update, boolean doNotSync, String mimetype, String pid, String schemaDocument,
          String schemaHash, String schemaId, Long version, SCHEMA_TYPE type) {
    MetadataSchemaRecord msr = new MetadataSchemaRecord();
    msr.setAcl(aclEntry);
    msr.setComment(comment);
    msr.setCreatedAt(creationDate);
    msr.setDefinition(definition);
    msr.setETag(eTag);
    msr.setLabel(label);
    msr.setLastUpdate(update);
    msr.setDoNotSync(doNotSync);
    msr.setMimeType(mimetype);
    msr.setPid(ResourceIdentifier.factoryUrlResourceIdentifier(pid));
    msr.setSchemaDocumentUri(schemaDocument);
    msr.setSchemaHash(schemaHash);
    msr.setSchemaId(schemaId);
    msr.setSchemaVersion(version);
    msr.setType(type);

    return msr;

  }

  public MetadataSchemaRecord createSchemaRecord(int... skipped) {
    Set<AclEntry> aclEntries = new HashSet<>();
    AclEntry entry = new AclEntry();
    entry.setId(1L);
    entry.setPermission(PERMISSION.WRITE);
    entry.setSid("write");
    aclEntries.add(createEntry(1L, PERMISSION.NONE, "none"));
    aclEntries.add(createEntry(2L, PERMISSION.READ, "read"));
    MetadataSchemaRecord msr = buildMSR(aclEntries, "comment", Instant.now().truncatedTo(ChronoUnit.SECONDS), "definition", "eTag", "label", Instant.MAX.truncatedTo(ChronoUnit.SECONDS), true, MediaType.APPLICATION_XML_VALUE, "pid", "schemadocument", "hash", "schemaId", 1L, SCHEMA_TYPE.XML);
    for (int remove : skipped) {
      switch (remove) {
        case 1:
          msr.setAcl(null);
          break;
        case 2:
          msr.setComment(null);
          break;
        case 3:
          msr.setCreatedAt(null);
          break;
        case 4:
          msr.setDefinition(null);
          break;
        case 5:
          msr.setETag(null);
          break;
        case 6:
          msr.setLabel(null);
          break;
        case 7:
          msr.setLastUpdate(null);
          break;
        case 8:
          msr.setDoNotSync(null);
          break;
        case 9:
          msr.setMimeType(null);
          break;
        case 10:
          msr.setPid(null);
          break;
        case 11:
          msr.setSchemaDocumentUri(null);
          break;
        case 12:
          msr.setSchemaHash(null);
          break;
        case 13:
          msr.setSchemaId(null);
          break;
        case 14:
          msr.setSchemaVersion(null);
          break;
        case 15:
          msr.setType(null);
          break;
      }
    }
    return msr;
  }

  private AclEntry createEntry(Long id, PERMISSION permission, String sid) {
    AclEntry entry = new AclEntry();
    entry.setId(id);
    entry.setPermission(permission);
    entry.setSid(sid);
    return entry;

  }
}
