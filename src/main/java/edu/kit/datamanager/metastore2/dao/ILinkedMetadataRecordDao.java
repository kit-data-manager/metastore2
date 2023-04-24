/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.LinkedMetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * DAO for linked metadata resources.
 */
public interface ILinkedMetadataRecordDao extends JpaRepository<LinkedMetadataRecord, String>, JpaSpecificationExecutor<MetadataRecord> {

  boolean existsMetadataRecordByRelatedResourceAndSchemaId(String relatedResource, String schemaId);

}
