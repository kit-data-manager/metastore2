/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web;

import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author sabrinechelbi
 */
public interface ISchemaRegistryControllerUI {

    public ModelAndView schemaManagement();

    public ModelAndView metadataSchemaManagement(String id, Pageable pgbl,
            WebRequest wr,
            HttpServletResponse hsr,
            UriComponentsBuilder ucb);
    
    public ModelAndView metadataManagement();

    public String dashboard();

    public String startPage();
    
}
