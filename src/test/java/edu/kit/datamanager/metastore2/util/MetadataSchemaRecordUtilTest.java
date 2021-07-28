/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord.SCHEMA_TYPE;
import edu.kit.datamanager.metastore2.domain.ResourceIdentifier;
import edu.kit.datamanager.repo.configuration.RepoBaseConfiguration;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
     * Test of migrateToDataResource method, of class MetadataSchemaRecordUtil.
     */
    @Test
    public void testMigrateToDataResource() {
        System.out.println("migrateToDataResource");
        RepoBaseConfiguration applicationProperties = null;
        MetadataSchemaRecord metadataSchemaRecord = null;
        DataResource expResult = null;
        DataResource result = MetadataSchemaRecordUtil.migrateToDataResource(applicationProperties, metadataSchemaRecord);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of migrateToMetadataSchemaRecord method, of class
     * MetadataSchemaRecordUtil.
     */
    @Test
    public void testMigrateToMetadataSchemaRecord() {
        System.out.println("migrateToMetadataSchemaRecord");
        RepoBaseConfiguration applicationProperties = null;
        DataResource dataResource = null;
        boolean provideETag = false;
        MetadataSchemaRecord expResult = null;
        MetadataSchemaRecord result = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(applicationProperties, dataResource, provideETag);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of mergeRecords method, of class MetadataSchemaRecordUtil.
     */
    @Test
    public void testMergeRecords() {
        System.out.println("mergeRecords");
        MetadataSchemaRecord managed = new MetadataSchemaRecord();
        MetadataSchemaRecord provided = createSchemaRecord(0);
        MetadataSchemaRecord expResult = createSchemaRecord();
        MetadataSchemaRecord result = MetadataSchemaRecordUtil.mergeRecords(managed, provided);
        assertEquals(expResult, result);
    }

    @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
    public void testtestUpdateMetadataSchemaRecordNull() {
        MetastoreConfiguration conf = new MetastoreConfiguration();
        String resourceId = "any";
        String eTag = "neverMind";
        MultipartFile recordDocument = null;
        MultipartFile schemaDocument = null;
        MetadataSchemaRecordUtil.updateMetadataSchemaRecord(conf, resourceId, eTag, recordDocument, schemaDocument, null);

    }

    @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
    public void testUpdateMetadataSchemaRecordEmpty() {
        MetastoreConfiguration conf = new MetastoreConfiguration();
        String resourceId = "any";
        String eTag = "neverMind";
        MultipartFile recordDocument = new MockMultipartFile("schema", "schema.json", "application/json", new String().getBytes());
        MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", new String().getBytes());
        MetadataSchemaRecordUtil.updateMetadataSchemaRecord(conf, resourceId, eTag, recordDocument, schemaDocument, null);
    }

    @Test(expected = edu.kit.datamanager.exceptions.BadArgumentException.class)
    public void testUpdateMetadataSchemaRecordInvalid() {
        MetastoreConfiguration conf = new MetastoreConfiguration();
        String resourceId = "any";
        String eTag = "neverMind";
        MultipartFile recordDocument = new MockMultipartFile("schema", "schema.json", "application/json", new String("{something really strange}").getBytes());
        MultipartFile schemaDocument = new MockMultipartFile("schema", "schema.json", "application/json", new String().getBytes());
        MetadataSchemaRecordUtil.updateMetadataSchemaRecord(conf, resourceId, eTag, recordDocument, schemaDocument, null);
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
        msr.setPid(ResourceIdentifier.factoryInternalResourceIdentifier(pid));
        msr.setSchemaDocumentUri(schemaDocument);
        msr.setSchemaHash(schemaHash);
        msr.setSchemaId(schemaId);
        msr.setSchemaVersion(version);
        msr.setType(type);

        return msr;

    }

    private MetadataSchemaRecord createSchemaRecord(int... skipped) {
        Set<AclEntry> aclEntries = new HashSet<>();
        AclEntry entry = new AclEntry();
        entry.setId(1l);
        entry.setPermission(PERMISSION.WRITE);
        entry.setSid("write");
        aclEntries.add(createEntry(1l, PERMISSION.NONE, "none"));
        aclEntries.add(createEntry(2l, PERMISSION.READ, "read"));
        MetadataSchemaRecord msr = buildMSR(aclEntries, "comment", Instant.now(), "definition", "eTag", "label", Instant.MAX, true, "mimetype", "pid", "schemadocument", "hash", "schemaId", 1l, SCHEMA_TYPE.XML);
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
