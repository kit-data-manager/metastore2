/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Interface for metadata editor controller.
 *
 * @author sabrinechelbi
 */
public interface IMetadataEditorController {

  @RequestMapping("/schema-management")
  ModelAndView schemaManagement();

  @RequestMapping("/metadata-management")
  ModelAndView metadataManagement(Pageable pgbl,
                                  WebRequest wr,
                                  HttpServletResponse hsr,
                                  UriComponentsBuilder ucb);

  @RequestMapping(value = {"/dashboard", "/", ""})
  String dashboard();

}
