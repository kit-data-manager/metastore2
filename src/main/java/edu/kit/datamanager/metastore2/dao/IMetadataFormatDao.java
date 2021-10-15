/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dao;

import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 */
public interface IMetadataFormatDao extends JpaRepository<MetadataFormat, String>, JpaSpecificationExecutor<MetadataFormat> {
  @Query("select p.metadataPrefix from #{#entityName} p")
  List<String> getAllIds();

}
