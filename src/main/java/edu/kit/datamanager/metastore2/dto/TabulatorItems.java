/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.dto;

import lombok.Getter;

/**
 *
 * @author sabrinechelbi
 */
@Getter

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
    
}
