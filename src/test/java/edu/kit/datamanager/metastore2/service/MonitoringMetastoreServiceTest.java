/*
 * Copyright 2025 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.service;


import edu.kit.datamanager.metastore2.configuration.MonitoringMetastoreConfiguration;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.configuration.MonitoringConfiguration;
import edu.kit.datamanager.repo.util.MonitoringUtil;
import edu.kit.datamanager.metastore2.service.MonitoringMetastoreService;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MonitoringMetastoreServiceTest {
  private SimpleMeterRegistry meterRegistry;
  private MonitoringMetastoreService metrics;
  MonitoringMetastoreConfiguration monitoringMetastoreConfiguration;
  MonitoringConfiguration monitoringConfiguration;

  @Before
  public void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    monitoringMetastoreConfiguration = new MonitoringMetastoreConfiguration();
    monitoringConfiguration = new MonitoringConfiguration();
    monitoringConfiguration.setEnabled(true);
    MonitoringUtil.setMonitoringConfiguration(monitoringConfiguration);
    metrics = new MonitoringMetastoreService(monitoringConfiguration, monitoringMetastoreConfiguration);
  }

  @Test
  public void testUpdateMetricsWithInvalidSetup() {
    try (MockedStatic<DataResourceRecordUtil> dataResourceRecordUtilMockedStatic = Mockito.mockStatic(DataResourceRecordUtil.class)) {
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfMetadataDocuments).thenReturn(0L);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfSchemaDocuments).thenReturn(0L);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::collectDocumentsPerSchema).thenReturn(Map.ofEntries(
              Map.entry("schema1", 10L)
      ));
      MonitoringMetastoreConfiguration monitoringMetastoreConfiguration = new MonitoringMetastoreConfiguration();
      MonitoringConfiguration monitoringConfiguration = new MonitoringConfiguration();
      monitoringConfiguration.setEnabled(false);
      MonitoringMetastoreService metrics = new MonitoringMetastoreService(monitoringConfiguration, monitoringMetastoreConfiguration);
      metrics.updateMetrics();
      monitoringConfiguration.setEnabled(true);
      metrics.updateMetrics();
      metrics.bindTo(meterRegistry);
      metrics.updateMetrics();
    }

  }

  @Test
  public void testCountMetadataDocuments() {
    // Simulate the count of metadata documents
    long expectedCountMetadataDocuments = 50;
    long expectedCountSchemaDocuments = 5;
    try (MockedStatic<DataResourceRecordUtil> dataResourceRecordUtilMockedStatic = Mockito.mockStatic(DataResourceRecordUtil.class)) {
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfMetadataDocuments).thenReturn(expectedCountMetadataDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfSchemaDocuments).thenReturn(expectedCountSchemaDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::collectDocumentsPerSchema).thenReturn(Map.ofEntries(
              Map.entry("schema1", 10L),
              Map.entry("schema2", 20L),
              Map.entry("schema3", 30L),
              Map.entry("schema4", 40L),
              Map.entry("schema5", 50L),
              Map.entry("schema6", 60L),
              Map.entry("schema7", 70L),
              Map.entry("schema8", 80L),
              Map.entry("schema9", 90L),
              Map.entry("schema10", 100L),
              Map.entry("schema11", 110L)
      ));
      metrics.bindTo(meterRegistry);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_DOCUMENTS).gauge().value()).isEqualTo(expectedCountMetadataDocuments);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_SCHEMAS).gauge().value()).isEqualTo(expectedCountSchemaDocuments);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).tags(MonitoringMetastoreService.LABEL_SCHEMA_ID, "schema6").gauge().value()).isEqualTo(60);
      try {
        assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).tags(MonitoringMetastoreService.LABEL_SCHEMA_ID, "schema1").gauge().value()).isEqualTo(10);
        Assert.fail();
      } catch (MeterNotFoundException mnfe) {
        // This exception is expected because the schema1 is not registered
        // in the meter registry due to the limit of 10 schemas.
        Assert.assertTrue(true);
      } catch (Exception e) {
        // This exception is not expected
        Assert.fail();
      }
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).meters()).hasSize(monitoringMetastoreConfiguration.getNoOfSchemas());
    }
  }

  @Test
  public void testCountMetadataDocumentsWithAdditionalSchema() {
    // Simulate the count of metadata documents
    long expectedCountMetadataDocuments = 60;
    long expectedCountSchemaDocuments = 3;
    try (MockedStatic<DataResourceRecordUtil> dataResourceRecordUtilMockedStatic = Mockito.mockStatic(DataResourceRecordUtil.class)) {
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfMetadataDocuments).thenReturn(expectedCountMetadataDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfSchemaDocuments).thenReturn(expectedCountSchemaDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::collectDocumentsPerSchema).thenReturn(Map.ofEntries(
              Map.entry("schema1", 10L),
              Map.entry("schema2", 20L),
              Map.entry("schema3", 30L)
      ));
      metrics.bindTo(meterRegistry);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_DOCUMENTS).gauge().value()).isEqualTo(expectedCountMetadataDocuments);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_SCHEMAS).gauge().value()).isEqualTo(expectedCountSchemaDocuments);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).tags(MonitoringMetastoreService.LABEL_SCHEMA_ID, "schema2").gauge().value()).isEqualTo(20);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).meters()).hasSize(3);
      // read metrics with new values
      expectedCountMetadataDocuments = 500;
      expectedCountSchemaDocuments = 4;
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfMetadataDocuments).thenReturn(expectedCountMetadataDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfSchemaDocuments).thenReturn(expectedCountSchemaDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::collectDocumentsPerSchema).thenReturn(Map.ofEntries(
              Map.entry("schema1", 110L),
              Map.entry("schema2", 120L),
              Map.entry("schema3", 130L),
              Map.entry("schema4", 140L)
      ));
      metrics.bindTo(meterRegistry);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_DOCUMENTS).gauge().value()).isEqualTo(expectedCountMetadataDocuments);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_SCHEMAS).gauge().value()).isEqualTo(expectedCountSchemaDocuments);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).tags(MonitoringMetastoreService.LABEL_SCHEMA_ID, "schema2").gauge().value()).isEqualTo(120);
      assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).meters()).hasSize(4);
    }
  }

  @Test
  public void testWithoutMonitoring() {
    // Simulate the count of metadata documents
    long expectedCountMetadataDocuments = 50;
    long expectedCountSchemaDocuments = 5;
    try (MockedStatic<DataResourceRecordUtil> dataResourceRecordUtilMockedStatic = Mockito.mockStatic(DataResourceRecordUtil.class);
         MockedStatic<MonitoringUtil> monitoringUtilMockedStatic = Mockito.mockStatic(MonitoringUtil.class)) {
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfMetadataDocuments).thenReturn(expectedCountMetadataDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::getNoOfSchemaDocuments).thenReturn(expectedCountSchemaDocuments);
      dataResourceRecordUtilMockedStatic.when(DataResourceRecordUtil::collectDocumentsPerSchema).thenReturn(Map.ofEntries(
              Map.entry("schema1", 10L),
              Map.entry("schema2", 20L),
              Map.entry("schema3", 30L),
              Map.entry("schema4", 40L),
              Map.entry("schema5", 50L),
              Map.entry("schema6", 60L),
              Map.entry("schema7", 70L),
              Map.entry("schema8", 80L),
              Map.entry("schema9", 90L),
              Map.entry("schema10", 100L),
              Map.entry("schema11", 110L)
      ));
      monitoringUtilMockedStatic.when(MonitoringUtil::isMonitoringEnabled).thenReturn(false);
      metrics.bindTo(meterRegistry);
      try {
        assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_DOCUMENTS).gauge().value()).isEqualTo(expectedCountMetadataDocuments);
        assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_METADATA_SCHEMAS).gauge().value()).isEqualTo(expectedCountSchemaDocuments);
        assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).tags(MonitoringMetastoreService.LABEL_SCHEMA_ID, "schema6").gauge().value()).isEqualTo(60);
        assertThat(meterRegistry.get(MonitoringMetastoreService.PREFIX_METRICS + MonitoringMetastoreService.LABEL_DOCUMENTS_PER_SCHEMA).tags(MonitoringMetastoreService.LABEL_SCHEMA_ID, "schema1").gauge().value()).isEqualTo(10);
        Assert.fail();
      } catch (MeterNotFoundException mnfe) {
        // This exception is expected because the s monitoring is disabled.
        Assert.assertTrue(true);
      } catch (Exception e) {
        // This exception is not expected
        Assert.fail();
      }
    }
  }
}
