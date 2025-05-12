package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.metastore2.dao.*;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringUtil {
    @Autowired
    private static MeterRegistry meterRegistry;

    public void registerMetrics() {
        Gauge.builder("metastore.data_records", this::countDataRecords).register(meterRegistry);
        Gauge.builder("metastore.linked_data_resources", this::countLinkedDataRecords).register(meterRegistry);
        Gauge.builder("metastore.linked_metadata_records", this::countLinkedMetadataRecords).register(meterRegistry);
        Gauge.builder("metastore.metadata_formats", this::countMetadataFormats).register(meterRegistry);
        Gauge.builder("metastore.metadata_schemas", this::countMetadataSchemas).register(meterRegistry);
        Gauge.builder("metastore.schema_records", this::countSchemaRecords).register(meterRegistry);
    }

    int countDataRecords() {
        return 0;
    }

    int countLinkedDataRecords() {
        return 0;
    }

    int countLinkedMetadataRecords() {
        return 0;
    }

    int countMetadataFormats() {
        return 0;
    }

    int countMetadataSchemas() {
        return 0;
    }

    int countSchemaRecords() {
        return 0;
    }
}
