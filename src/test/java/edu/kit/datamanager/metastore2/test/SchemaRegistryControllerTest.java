/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.entities.repo.AclEntry;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author Torridity
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {ServletTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class})
@ActiveProfiles("test")
public class SchemaRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IMetadataSchemaDao metadataSchemaDao;

    @Before
    public void setUp() throws JsonProcessingException {
        metadataSchemaDao.deleteAll();
    }

    @Test
    public void testGetSchemaRecordByIdWithoutVersion() throws Exception {
        createDcSchema();

        MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/dc")).andDo(print()).andExpect(status().isOk()).andReturn();
        ObjectMapper map = new ObjectMapper();
        MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("dc", result[0].getSchemaId());
        Assert.assertNotEquals("file:///tmp/dc.xsd", result[0].getSchemaDocumentUri());
    }

    @Test
    public void testGetSchemaRecordByIdWithVersion() throws Exception {
        createDcSchema();

        MvcResult res = this.mockMvc.perform(get("/api/v1/schemas/dc").param("version", "1")).andDo(print()).andExpect(status().isOk()).andReturn();
        ObjectMapper map = new ObjectMapper();
        MetadataSchemaRecord[] result = map.readValue(res.getResponse().getContentAsString(), MetadataSchemaRecord[].class);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.length);
        Assert.assertEquals("dc", result[0].getSchemaId());
        Assert.assertNotEquals("file:///tmp/dc.xsd", result[0].getSchemaDocumentUri());
    }

    @Test
    public void testGetSchemaRecordByIdWithInvalidVersion() throws Exception {
        createDcSchema();
        this.mockMvc.perform(get("/api/v1/schemas/dc").param("version", "2")).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    private void createDcSchema() {
        MetadataSchemaRecord record = new MetadataSchemaRecord();
        record.setCreatedAt(Instant.now());
        record.setLastUpdate(Instant.now());
        record.setSchemaId("dc");
        record.setSchemaVersion(1);
        record.setMimeType("application/xml");
        record.setType(MetadataSchemaRecord.SCHEMA_TYPE.XML);
        Set<AclEntry> acl = new HashSet<>();
        AclEntry entry = new AclEntry();
        entry.setSid("SELF");
        entry.setPermission(PERMISSION.WRITE);
        acl.add(entry);
        record.setAcl(acl);
        record.setSchemaDocumentUri("file:///tmp/dc.xsd");
        metadataSchemaDao.save(record);
    }

}
