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
package edu.kit.datamanager.metastore2.runner;

import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicDocumentMetrics implements MeterBinder {
  /**
   * Prefix for metrics.
   */
  public static final String PREFIX_METRICS = "metastore.";
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
   * Number of schemas to be registered.
   */
  public static final int NO_OF_SCHEMAS = 10;
  /**
   * Label for metrics of documents per schema.
   */
  public static final String LABEL_DOCUMENTS_PER_SCHEMA = "documents_per_schema";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(DynamicDocumentMetrics.class);
  private final Set<String> registeredSchemas = ConcurrentHashMap.newKeySet();
  private MeterRegistry meterRegistry;

  @Override
  public void bindTo(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    Gauge.builder(PREFIX_METRICS + LABEL_METADATA_DOCUMENTS, this::countMetadataDocuments).register(meterRegistry);
    Gauge.builder(PREFIX_METRICS + LABEL_METADATA_SCHEMAS, this::countMetadataSchemas).register(meterRegistry);
    // Register the initial set of schemas
    updateMetrics();
  }

  /**
   * Register and update the metrics for the metastore.
   */
  @Scheduled(fixedRate = 360000) // 60 minutes
  public void updateMetrics() {
    Map<String, Long> documentsPerSchema = DataResourceRecordUtil.collectDocumentsPerSchema();
    LOG.trace("Documents per schema: {}", documentsPerSchema);
    documentsPerSchema.
            entrySet().
            stream().
            sorted(Map.Entry.<String, Long>comparingByValue().reversed()).
            limit(NO_OF_SCHEMAS).
            forEach(entry -> {
              String schemaId = entry.getKey();
              if (registeredSchemas.add(schemaId)) {
                Gauge.builder(PREFIX_METRICS + LABEL_DOCUMENTS_PER_SCHEMA,
                                this,
                                ddm -> ddm.getDocumentsPerSchema().
                                        getOrDefault(schemaId, 0L)).
                        tags(Tags.of(LABEL_SCHEMA_ID, schemaId)).
                        register(meterRegistry);
              }
            });
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
    return DataResourceRecordUtil.collectDocumentsPerSchema();
  }
}
