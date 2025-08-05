/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.DataRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * DAO for the record of a metadata document.
 */
public interface IDataRecordDao extends JpaRepository<DataRecord, String>, JpaSpecificationExecutor<DataRecord>{
  Optional<DataRecord> findByMetadataIdAndVersion(String metadataId, Long version);
  Optional<DataRecord> findTopByMetadataIdOrderByVersionDesc(String metadataId);
  List<DataRecord>     findBySchemaId(String schemaId);
  List<DataRecord>     findBySchemaId(String schemaId, Pageable pageable);
  List<DataRecord>     findBySchemaIdAndLastUpdateAfter(String schemaId, Instant from);
  List<DataRecord>     findBySchemaIdAndLastUpdateBefore(String schemaId, Instant until);
  List<DataRecord>     findBySchemaIdInAndLastUpdateBetween(List<String> schemaId, Instant from, Instant until, Pageable pageable);
  List<DataRecord>     findBySchemaIdAndLastUpdateBetween(String schemaId, Instant from, Instant until, Pageable pageable);
  long                 countBySchemaIdInAndLastUpdateBetween(List<String> schemaId, Instant from, Instant until);
  long                 countBySchemaIdAndLastUpdateBetween(String schemaId, Instant from, Instant until);
}
