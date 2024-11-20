/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dto;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import lombok.Builder;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Data transfer object for Web UI.
 *
 * @author sabrinechelbi
 */
@Builder
@Getter
@SuppressWarnings("java:S1068")
public class EditorRequestSchema {

  /**
   * JSON schema, which describes the structure of the data model.
   */
  private JSONObject dataModel;

  /**
   * JSON user interface form, which describes the structure of the form layout.
   */
  private JSONObject uiForm;

  /**
   * array of schema records.
   */
  private List<MetadataSchemaRecord> schemaRecords;

  /**
   * array, which includes the table’s column definitions.
   */
  private TabulatorItems[] items;

}
