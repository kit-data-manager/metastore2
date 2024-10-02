/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web.impl;

import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.dto.TabulatorLocalPagination;
import edu.kit.datamanager.metastore2.dto.TabulatorRemotePagination;
import edu.kit.datamanager.metastore2.web.IFrontendController;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Controller used by web frontends.
 *
 * @author sabrinechelbi
 */
@Controller
@RequestMapping(value = "/api/v1/ui")
@Hidden
@Tag(name = "REST endpoints 4 GUI")
public class FrontendControllerImpl implements IFrontendController {

  private static final Logger LOG = LoggerFactory.getLogger(FrontendControllerImpl.class);

  private static final String SHOW_PAGE = "Pageable: '{}'";
  private static final String CONTENT_RANGE = "Content-Range";

  @Autowired
  private MetadataControllerImpl metadtaControllerImpl;

  @Autowired
  private SchemaRegistryControllerImpl schemaControllerImpl;

  @Override
  public ResponseEntity<TabulatorLocalPagination> findAllSchemasForTabulator(
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb) {

    LOG.trace("Performing findAllSchemasForTabulator( pgbl='{}').", pgbl);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
    if (pgbl != null) {
      pageable = PageRequest.of(pgbl.getPageNumber() < 1 ? 0 : pgbl.getPageNumber() - 1, pgbl.getPageSize(), Sort.by("id").ascending());
    }
    LOG.trace(SHOW_PAGE, pageable);

    ResponseEntity<List<MetadataSchemaRecord>> responseEntity4schemaRecords = schemaControllerImpl.getRecords(null, null, null, null, pageable, wr, hsr, ucb);
    List<MetadataSchemaRecord> schemaRecords = responseEntity4schemaRecords.getBody();
    String pageSize = responseEntity4schemaRecords.getHeaders().getFirst(CONTENT_RANGE);

    TabulatorLocalPagination tabulatorLocalPagination = TabulatorLocalPagination.builder()
            .lastPage(tabulatorLastPage(pageSize, pageable))
            .data(schemaRecords)
            .build();
    return ResponseEntity.status(HttpStatus.OK).body(tabulatorLocalPagination);
  }

  @Override
  public ResponseEntity<TabulatorLocalPagination> findAllMetadataForTabulator(
          @RequestParam(value = "id", required = false) String id,
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb) {
    LOG.trace("Performing findAllMetadataForTabulator( id='{}', pgbl='{}').", id, pgbl);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
    if (pgbl != null) {
      pageable = PageRequest.of(pgbl.getPageNumber() < 1 ? 0 : pgbl.getPageNumber() - 1, pgbl.getPageSize(), Sort.by("id").ascending());
    }
    LOG.trace(SHOW_PAGE, pageable);
    List<String> metadataDocumentId = id == null ? null : List.of(id);
    ResponseEntity< List<MetadataRecord>> responseEntity4metadataRecords = metadtaControllerImpl.getRecords(null, null, metadataDocumentId, null, null, pageable, wr, hsr, ucb);
    List<MetadataRecord> metadataRecords = responseEntity4metadataRecords.getBody();

    String pageSize = responseEntity4metadataRecords.getHeaders().getFirst(CONTENT_RANGE);

    TabulatorLocalPagination tabulatorLocalPagination = TabulatorLocalPagination.builder()
            .lastPage(tabulatorLastPage(pageSize, pageable))
            .data(metadataRecords)
            .build();
    return ResponseEntity.status(HttpStatus.OK).body(tabulatorLocalPagination);
  }

  @Override
  public ResponseEntity<TabulatorRemotePagination> getSchemaRecordsForUi(
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb) {
    LOG.trace("Performing getSchemaRecordsForUi( pgbl='{}').", pgbl);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
    if (pgbl != null) {
      pageable = PageRequest.of(pgbl.getPageNumber() < 1 ? 0 : pgbl.getPageNumber() - 1, pgbl.getPageSize(), Sort.by("id").ascending());
    }
    LOG.trace(SHOW_PAGE, pageable);

    ResponseEntity<List<MetadataSchemaRecord>> responseEntity4schemaRecords = schemaControllerImpl.getRecords(null, null, null, null, pageable, wr, hsr, ucb);
    List<MetadataSchemaRecord> schemaRecords = responseEntity4schemaRecords.getBody();

    String pageSize = responseEntity4schemaRecords.getHeaders().getFirst(CONTENT_RANGE);

    TabulatorRemotePagination tabulatorRemotePagination = TabulatorRemotePagination.builder()
            .lastPage(tabulatorLastPage(pageSize, pageable))
            .data(schemaRecords)
            .build();

    return ResponseEntity.status(HttpStatus.OK).body(tabulatorRemotePagination);

  }

  @Override
  public ResponseEntity<TabulatorRemotePagination> getMetadataRecordsForUi(
          @RequestParam(value = "id", required = false) String id,
          @Parameter(hidden = true) final Pageable pgbl,
          final WebRequest wr,
          final HttpServletResponse hsr,
          final UriComponentsBuilder ucb) {
    LOG.trace("Performing getMetadataRecordsForUi( id='{}', pgbl='{}').", id, pgbl);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
    if (pgbl != null) {
      pageable = PageRequest.of(pgbl.getPageNumber() < 1 ? 0 : pgbl.getPageNumber() - 1, pgbl.getPageSize(), Sort.by("id").ascending());
    }
    LOG.trace(SHOW_PAGE, pageable);

    List<String> schemaIds = id == null ? null : List.of(id);
    ResponseEntity< List<MetadataRecord>> responseEntity4metadataRecords;
    List<MetadataRecord> metadataRecords = null;
    String pageSize = null;
    try {
      responseEntity4metadataRecords = metadtaControllerImpl.getRecords(null, null, schemaIds, null, null, pageable, wr, hsr, ucb);
      metadataRecords = responseEntity4metadataRecords.getBody();
      pageSize = responseEntity4metadataRecords.getHeaders().getFirst(CONTENT_RANGE);
    } catch (Exception ex) {
      // Test for document id instead of schema id.
      if (schemaIds != null) {
        responseEntity4metadataRecords = metadtaControllerImpl.getRecords(schemaIds.get(0), null, null, null, null, pageable, wr, hsr, ucb);
        metadataRecords = responseEntity4metadataRecords.getBody();
        pageSize = responseEntity4metadataRecords.getHeaders().getFirst(CONTENT_RANGE);
      }
    }

    TabulatorRemotePagination tabulatorRemotePagination = TabulatorRemotePagination.builder()
            .lastPage(tabulatorLastPage(pageSize, pageable))
            .data(metadataRecords)
            .build();

    return ResponseEntity.status(HttpStatus.OK).body(tabulatorRemotePagination);

  }

  /**
   * computes the total number of the available pages.
   *
   * @param pageSize pagination size
   * @param pageable page object
   * @return the total number of the available pages
   */
  @SuppressWarnings("StringSplitter")
  private int tabulatorLastPage(String pageSize, Pageable pageable) {
    if ((Integer.parseInt(pageSize.split("/")[1]) % pageable.getPageSize()) == 0) {
      return Integer.parseInt(pageSize.split("/")[1]) / pageable.getPageSize();
    }
    return Integer.parseInt(pageSize.split("/")[1]) / pageable.getPageSize() + 1;
  }

}
