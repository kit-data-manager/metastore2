/*
 * Copyright 2020 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.test;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.service.MetadataSchemaAuditService;
import edu.kit.datamanager.service.IAuditService;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 *
 * @author jejkal
 */
@RunWith(SpringRunner.class)
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@PowerMockIgnore({"javax.crypto.*", "javax.management.*"})
@PrepareForTest(AuthenticationHelper.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  TransactionalTestExecutionListener.class
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MetadataSchemaAuditServiceTest{

  @Autowired
  private IAuditService<MetadataSchemaRecord> service;

  @Test
  public void testCaptureAndReadAuditInformation(){
    MetadataSchemaRecord record = new MetadataSchemaRecord();
    record.setSchemaId("test1");
    record.setLabel("MySchema");
    record.setSchemaVersion(1l);

    service.captureAuditInformation(record, "TEST");

    Optional<String> auditInfo = service.getAuditInformationAsJson(record.getSchemaId(), 1, 1);
    Assert.assertTrue(auditInfo.isPresent());

    Assert.assertEquals(1l, service.getCurrentVersion(record.getSchemaId()));
    Optional<MetadataSchemaRecord> recordFromVersioning = service.getResourceByVersion(record.getSchemaId(), 1l);
    Assert.assertTrue(recordFromVersioning.isPresent());
    Assert.assertNull(recordFromVersioning.get().getComment());

    record.setComment("Comment added.");
    service.captureAuditInformation(record, "TEST");
    recordFromVersioning = service.getResourceByVersion(record.getSchemaId(), 2l);
    Assert.assertTrue(recordFromVersioning.isPresent());
    Assert.assertEquals("Comment added.", recordFromVersioning.get().getComment());

    recordFromVersioning = service.getResourceByVersion(record.getSchemaId(), 3l);
    Assert.assertTrue(!recordFromVersioning.isPresent());

    service.deleteAuditInformation("TEST", record);
  }

  @Test
  public void testGetVersionForInvalidObject(){
    Assert.assertEquals(0l, service.getCurrentVersion("notExist"));
  }

}
