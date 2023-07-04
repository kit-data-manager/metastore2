/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dto;

import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.SchemaRecord;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Data transfer object for Web UI.
 *
 * @author sabrinechelbi
 */
@Builder
@Getter
@SuppressWarnings("java:S1068")
public class LandingPageModel {

  /**
   * Metadata record(s).
   */
//  private List<MetadataRecord> metadataRecords;

  /**
   * array, which includes the table’s column definitions.
   */
//  private List<SchemaRecord> items;
  private String information;
}
