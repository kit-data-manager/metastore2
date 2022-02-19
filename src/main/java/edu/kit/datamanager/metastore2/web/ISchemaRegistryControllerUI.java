/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web;

import edu.kit.datamanager.metastore2.dto.TabulatorRemotePagination;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author sabrinechelbi
 */
public interface ISchemaRegistryControllerUI {
    
    public ResponseEntity<TabulatorRemotePagination> getSchemaRecordsForUi(Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb);
    
    public ResponseEntity<TabulatorRemotePagination> getMetadataRecordsForUi(@RequestParam(value = "id", required = false) String id, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb);

    public ModelAndView schemaManagement();

    public ModelAndView metadataManagement(Pageable pgbl,
            WebRequest wr,
            HttpServletResponse hsr,
            UriComponentsBuilder ucb);
    
    public String dashboard();
    
}
