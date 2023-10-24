/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.kit.datamanager.metastore2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Helper class for tabulator local pagination used by web frontend.
 *
 * @author jejkal
 */
@Getter
@Builder
@SuppressWarnings("java:S1068")
public class TabulatorLocalPagination {
    @JsonProperty("last_page")
    private int lastPage;
    
    private List<?> data;
}
