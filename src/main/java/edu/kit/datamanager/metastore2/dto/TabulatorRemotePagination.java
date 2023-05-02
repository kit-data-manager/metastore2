/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Helper class for tabulator remote pagination used by web frontend.
 *
 * @author sabrinechelbi
 */
@Getter
@Builder
public class TabulatorRemotePagination {

  @JsonProperty("last_page")
  private int lastPage;

  private List<?> data;
}
