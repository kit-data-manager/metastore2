/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dto;

import lombok.Getter;

/**
 * Helper class for tabulator items.
 *
 * @author sabrinechelbi
 */
@Getter
@SuppressWarnings("java:S1068")
public class TabulatorItems {

  /**
   * The title that will be displayed in the header for the column.
   */
  private String title;

  /**
   * The key for the column in the JSON resource.
   */
  private String field;

  private String editor;

  private String formatter;

  private TabulatorFormatterParam formatterParams;

}
