/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.DataRecord;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author Torridity
 */
public interface IDataRecordDao extends JpaRepository<DataRecord, String>, JpaSpecificationExecutor<DataRecord>{
  DataRecord findByMetadataId(String metadataId);
  List<DataRecord> findBySchemaId(String schemaId);
  List<DataRecord> findBySchemaIdAndLastUpdateAfter(String schemaId, Instant from);
  List<DataRecord> findBySchemaIdAndLastUpdateBefore(String schemaId, Instant until);
  List<DataRecord> findBySchemaIdAndLastUpdateAfterAndLastUpdateBefore(String schemaId, Instant from, Instant until);
}
