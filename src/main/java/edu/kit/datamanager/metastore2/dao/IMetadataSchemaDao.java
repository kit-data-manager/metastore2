/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * DAO for metadata schema record.
 *
 * @author Torridity
 */
public interface IMetadataSchemaDao extends JpaRepository<MetadataSchemaRecord, String>, JpaSpecificationExecutor<MetadataSchemaRecord> {

}
