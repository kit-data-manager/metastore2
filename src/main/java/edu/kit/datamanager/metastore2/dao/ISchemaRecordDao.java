/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 *
 * @author Torridity
 */
public interface ISchemaRecordDao extends JpaRepository<SchemaRecord, String>, JpaSpecificationExecutor<SchemaRecord>{
  boolean existsSchemaRecordBySchemaIdAndVersion(String schemaId, Long version);
  SchemaRecord findBySchemaIdAndVersion(String schemaId, Long version);
  List<SchemaRecord> findBySchemaIdOrderByVersionDesc(String schemaId);
  SchemaRecord findTopBySchemaIdOrderByVersionDesc(String schemaId);
  SchemaRecord findFirstBySchemaIdOrderByVersionDesc(String schemaId);
}
