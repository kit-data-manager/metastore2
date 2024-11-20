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

import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * DAO for the record of a schema document.
 *
 * @author Torridity
 */
public interface ISchemaRecordDao extends JpaRepository<SchemaRecord, String>, JpaSpecificationExecutor<SchemaRecord> {

  boolean existsSchemaRecordBySchemaIdAndVersion(String schemaId, Long version);
  boolean existsSchemaRecordBySchemaIdStartsWithAndVersion(String schemaId, Long version);

  SchemaRecord findBySchemaId(String schemaIdWithVersion);

  SchemaRecord findByAlternateId(String alternateId);

  List<SchemaRecord> findBySchemaIdStartsWithOrderByVersionDesc(String schemaId);

  SchemaRecord findFirstBySchemaIdStartsWithOrderByVersionDesc(String schemaId);
}
