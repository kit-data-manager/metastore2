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
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.repo.domain.DataResource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class MonitoringUtil {
    /** Prefix for metrics. */
    private static final String PREFIX_METRICS = "metastore.";
    /** Number of schemas to be registered. */
    private static final int NO_OF_SCHEMAS = 10;

    @Autowired
    private static MeterRegistry meterRegistry;

    /**
     * Register the metrics for the metastore.
     */
    public void registerMetrics() {
        Gauge.builder(PREFIX_METRICS + "metadata_documents", this::countMetadataDocuments).register(meterRegistry);
        Gauge.builder(PREFIX_METRICS + "metadata_schemas", this::countMetadataSchemas).register(meterRegistry);
        registerDocumentsPerSchema(NO_OF_SCHEMAS);
    }

    /**
     * Count the number of metadata schemas in the repository.
     * @return The number of metadata schemas.
     */
    int countMetadataSchemas() {
        return (int)DataResourceRecordUtil.getNoOfSchemaDocuments();
    }
    /**
     * Count the number of metadata documents in the repository.
     * @return The number of metadata documents.
     */
    int countMetadataDocuments() {
        return (int)DataResourceRecordUtil.getNoOfMetadataDocuments();
    }
    /**
     * Register the top 'noOfSchemas' with their no of documents available.
     * @param noOfSchemas Number of schemas to be registered. (< 0 -> all)
     */
    private void registerDocumentsPerSchema(int noOfSchemas) {
        Map<String, Long> documentsPerSchema = DataResourceRecordUtil.collectDocumentsPerSchema();
        if (noOfSchemas < 0) {
            noOfSchemas = documentsPerSchema.size();
        }
        documentsPerSchema.
                entrySet().
                stream().
                sorted(Map.Entry.<String, Long>comparingByValue().reversed()).
                limit(noOfSchemas).
                forEach(entry -> Gauge.builder(PREFIX_METRICS + "documents_per_schema." + entry.getKey(), entry::getValue).register(meterRegistry));
    }

}
