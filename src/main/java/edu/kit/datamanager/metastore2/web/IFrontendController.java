/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web;

import edu.kit.datamanager.metastore2.dto.TabulatorLocalPagination;
import edu.kit.datamanager.metastore2.dto.TabulatorRemotePagination;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Endpoints needed by frontends using table as UI.
 */
public interface IFrontendController {

  @RequestMapping(value = "/schemas", method = RequestMethod.GET, produces = {"application/tabulator+json"})
  @ResponseBody
  @PageableAsQueryParam
  ResponseEntity<TabulatorLocalPagination> findAllSchemasForTabulator(
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb);

  @RequestMapping(value = "/metadata", method = RequestMethod.GET, produces = {"application/tabulator+json"})
  @ResponseBody
  @PageableAsQueryParam
  ResponseEntity<TabulatorLocalPagination> findAllMetadataForTabulator(
          @RequestParam(value = "id", required = false) String id,
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb);

  @RequestMapping(value = "/schemas", method = RequestMethod.GET, produces = {"application/json"})
  @ResponseBody
  ResponseEntity<TabulatorRemotePagination> getSchemaRecordsForUi(Pageable pgbl,
                                                                  WebRequest wr,
                                                                  HttpServletResponse hsr,
                                                                  UriComponentsBuilder ucb);

  @RequestMapping(value = "/metadata", method = RequestMethod.GET, produces = {"application/json"})
  @ResponseBody
  ResponseEntity<TabulatorRemotePagination> getMetadataRecordsForUi(@RequestParam(value = "id", required = false) String id,
                                                                    Pageable pgbl,
                                                                    WebRequest wr,
                                                                    HttpServletResponse hsr,
                                                                    UriComponentsBuilder ucb);

}
