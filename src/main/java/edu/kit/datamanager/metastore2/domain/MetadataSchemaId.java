/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.domain;

import java.io.Serializable;
import lombok.Data;

/**
 *
 * @author Torridity
 */
@Data
public class MetadataSchemaId implements Serializable{

  private String schemaId;
  private Integer schemaVersion;

  public MetadataSchemaId(){
  }

  // default constructor
  public MetadataSchemaId(String schemaId, Integer schemaVersion){
    this.schemaId = schemaId;
    this.schemaVersion = schemaVersion;
  }

}
