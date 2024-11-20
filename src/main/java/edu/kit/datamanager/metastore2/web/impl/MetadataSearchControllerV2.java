/*
 * Copyright 2019 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.web.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.datamanager.configuration.SearchConfiguration;
import edu.kit.datamanager.util.ElasticSearchUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for search entities.
 */
@Controller
@RequestMapping(value = "/api/v2/metadata")
@Tag(name = "Metadata Repository")
@Schema(description = "Metadata Resource Management")
public class MetadataSearchControllerV2 {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataSearchControllerV2.class);
  
  private static final String SEARCH_PATH_POSTFIX = "/_search";

  @Autowired
  private final SearchConfiguration searchConfiguration;

  /**
   * Constructor with configuration.
   *
   * @param searchConfiguration Configuration for search.
   */
  public MetadataSearchControllerV2(SearchConfiguration searchConfiguration) {
    this.searchConfiguration = searchConfiguration;
    LOG.info("------------------------------------------------------");
    LOG.info("------{}", this.searchConfiguration);
    LOG.info("------------------------------------------------------");
  }

  @PostMapping("/{schemaId}/search")
  @Operation(summary = "Search for metadata document/records",
          description = "Search for metadata document/records using the configured Elastic backend. "
          + "This endpoint serves as direct proxy to the RESTful endpoint of Elastic. "
          + "In the body, a query document following the Elastic query format has to be provided. "
          + "Format errors are returned directly from Elastic. "
          + "This endpoint also supports authentication and authorization. "
          + "User information obtained via JWT is applied to the provided query as "
          + "post filter to restrict only to accessible resources. If a post filter "
          + "was already provided with the query it will be replaced. "
          + "Furthermore, this endpoint supports pagination. "
          + "'page' and 'size' query parameters are translated into the Elastic attributes "
          + "'from' and 'size' automatically, if not already provided within the query by the caller.",
          security = {
            @SecurityRequirement(name = "bearer-jwt")},
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the search result is returned.")
          })
  @ResponseBody
  @PageableAsQueryParam
  public ResponseEntity<?> proxy(@RequestBody JsonNode body,
          @Parameter(description = "Contains all schemaIds "
                  + "to which the records refer as comma-separated values. "
                  + "Regular expressions are also allowed. "
                  + "See https://www.elastic.co/guide/en/elasticsearch/reference/7.17/multi-index.html", required = true) @PathVariable(value = "schemaId") String schemaIds,
          ProxyExchange<JsonNode> proxy,
          @Parameter(hidden = true) final Pageable pgbl) throws Exception {

    // Prepare query with authorization
    prepareQuery(body, pgbl);
    LOG.trace("Redirect post to " + searchConfiguration.getUrl() + "/" + schemaIds + SEARCH_PATH_POSTFIX);

    return proxy.uri(searchConfiguration.getUrl() + "/" + schemaIds + SEARCH_PATH_POSTFIX).post();
  }

  @PostMapping("/search")
  @Operation(summary = "Search for metadata document/records",
          description = "Search for metadata document/records using the configured Elastic backend. "
          + "This endpoint serves as direct proxy to the RESTful endpoint of Elastic. "
          + "In the body, a query document following the Elastic query format has to be provided. "
          + "Format errors are returned directly from Elastic. "
          + "This endpoint also supports authentication and authorization. "
          + "User information obtained via JWT is applied to the provided query as "
          + "post filter to restrict only to accessible resources. If a post filter "
          + "was already provided with the query it will be replaced. "
          + "Furthermore, this endpoint supports pagination. "
          + "'page' and 'size' query parameters are translated into the Elastic attributes "
          + "'from' and 'size' automatically, if not already provided within the query by the caller.",
          security = {
            @SecurityRequirement(name = "bearer-jwt")},
          responses = {
            @ApiResponse(responseCode = "200", description = "OK and the search result is returned.")
          })
  @ResponseBody
  @PageableAsQueryParam
  public ResponseEntity<?> proxy(@RequestBody JsonNode body,
          ProxyExchange<JsonNode> proxy,
          @Parameter(hidden = true) final Pageable pgbl) throws Exception {

    // Prepare query with authorization
    prepareQuery(body, pgbl);
    LOG.trace("Redirect post to " + searchConfiguration.getUrl() + SEARCH_PATH_POSTFIX);

    return proxy.uri(searchConfiguration.getUrl() + SEARCH_PATH_POSTFIX).post();
  }

  /**
   * Prepare query for elasticsearch.
   *
   * @param body query
   * @param pgbl page information
   * @return Prepared query with post filter for authorization.
   */
  private ObjectNode prepareQuery(JsonNode body, Pageable pgbl) {
    LOG.trace("Provided Elastic query: '{}'", body.toString());

    // Set or replace post-filter
    ObjectNode on = (ObjectNode) body;
    ElasticSearchUtil.addPaginationInformation(on, pgbl.getPageNumber(), pgbl.getPageSize());
    ElasticSearchUtil.buildPostFilter(on);

    LOG.trace("Generated elastic query with post filter: '{}'", on.toPrettyString());
    return on;
  }
}
