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
import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitoringMetastoreService implements MeterBinder {
  /**
   * Prefix for metrics.
   */
  public static String PREFIX_METRICS = "metastore.";
  /**
   * Label for metrics of metadata documents.
   */
  public static final String LABEL_METADATA_DOCUMENTS = "metadata_documents";
  /**
   * Label for metrics of metadata schemas.
   */
  public static final String LABEL_METADATA_SCHEMAS = "metadata_schemas";
  /**
   * Label for metrics of schema id.
   */
  public static final String LABEL_SCHEMA_ID = "schema_id";
  /**
   * Label for metrics of documents per schema.
   */
  public static final String LABEL_DOCUMENTS_PER_SCHEMA = "documents_per_schema";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(MonitoringMetastoreService.class);
  private final Set<String> registeredSchemas = ConcurrentHashMap.newKeySet();
  private Map<String, Long> documentsPerSchema = null;
  private MeterRegistry meterRegistry;

  private final MonitoringMetastoreConfiguration monitoringMetastoreConfiguration;
  private final MonitoringConfiguration monitoringConfiguration;

  /**
   * Constructor.
   *
   * @param monitoringConfiguration Configuration for monitoring.
   */
  @Autowired
  public MonitoringMetastoreService(MonitoringConfiguration monitoringConfiguration, @org.springframework.lang.NonNull MonitoringMetastoreConfiguration monitoringMetastoreConfiguration) {
    this.monitoringConfiguration = monitoringConfiguration;
    this.monitoringMetastoreConfiguration = monitoringMetastoreConfiguration;
    PREFIX_METRICS = monitoringConfiguration.getServiceName() + ".";
  }

  @Override
  public void bindTo(@NonNull MeterRegistry meterRegistry) {
    if (monitoringConfiguration.isEnabled()) {
      this.meterRegistry = meterRegistry;
      Gauge.builder(PREFIX_METRICS + LABEL_METADATA_DOCUMENTS, this::countMetadataDocuments).register(meterRegistry);
      Gauge.builder(PREFIX_METRICS + LABEL_METADATA_SCHEMAS, this::countMetadataSchemas).register(meterRegistry);
      getDocumentsPerSchema();
      // Register the initial set of schemas
      updateMetrics();
    } else {
      LOG.info("Monitoring is disabled. Skipping metric registration.");
    }
  }

  /**
   * Initialize and update the metrics for the metastore in regular manner.
   */
  public void updateMetrics() {
    if (monitoringConfiguration.isEnabled() && meterRegistry != null) {
      LOG.info("Updating metrics for the metastore");

      Map<String, Long> documentsPerSchema = getDocumentsPerSchema();
      LOG.trace("Documents per schema: {}", documentsPerSchema);
      documentsPerSchema.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(monitoringMetastoreConfiguration.getNoOfSchemas()).forEach(entry -> {
        String schemaId = entry.getKey();
        if (registeredSchemas.add(schemaId)) {
          Gauge.builder(PREFIX_METRICS + LABEL_DOCUMENTS_PER_SCHEMA, this, ddm -> ddm.getDocumentsPerSchema(schemaId)).tags(Tags.of(LABEL_SCHEMA_ID, schemaId)).register(meterRegistry);
        }
      });
    } else {
      LOG.info("Monitoring is disabled. Skipping metric update.");
    }
  }

  /**
   * Count the number of metadata schemas in the repository.
   *
   * @return The number of metadata schemas.
   */
  int countMetadataSchemas() {
    return (int) DataResourceRecordUtil.getNoOfSchemaDocuments();
  }

  /**
   * Count the number of metadata documents in the repository.
   *
   * @return The number of metadata documents.
   */
  int countMetadataDocuments() {
    return (int) DataResourceRecordUtil.getNoOfMetadataDocuments();
  }

  /**
   * Get the number of documents per schema.
   *
   * @return The number of documents per schema.
   */
  private Map<String, Long> getDocumentsPerSchema() {
    documentsPerSchema = DataResourceRecordUtil.collectDocumentsPerSchema();
    return documentsPerSchema;
  }

  /**
   * Get the number of documents per schema.
   *
   * @param schemaId The schema ID for which to get the number of documents.
   * @return The number of documents per schema.
   */
  private Long getDocumentsPerSchema(String schemaId) {
    return documentsPerSchema.getOrDefault(schemaId, 0L);
  }
}
