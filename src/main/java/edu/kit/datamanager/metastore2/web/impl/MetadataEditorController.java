/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.metastore2.dto.EditorRequestMetadata;
import edu.kit.datamanager.metastore2.dto.EditorRequestSchema;
import edu.kit.datamanager.metastore2.dto.TabulatorItems;
import edu.kit.datamanager.metastore2.web.IMetadataEditorController;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Controller for the metadata editor web frontend.
 *
 * @author sabrinechelbi
 */
@Controller
@RequestMapping(value = "")
@Hidden
@Tag(name = "REST endpoints 4 MetadataEditor")
public class MetadataEditorController implements IMetadataEditorController {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataEditorController.class);

  private static final String DATAMODELSCHEMA = "/static/jsonSchemas/schemaRecord.json";
  private static final String UIFORMSCHEMA = "/static/jsonSchemas/uiFormSchemaRecord.json";
  private static final String ITEMSSCHEMA = "/static/jsonSchemas/itemsSchemaRecord.json";
 
  private static final String DATAMODELMETADATA = "/static/jsonSchemas/metadataRecord.json";
  private static final String UIFORMMETADATA = "/static/jsonSchemas/uiFormMetadataRecord.json";
  private static final String ITEMSMETADATA = "/static/jsonSchemas/itemsMetadataRecord.json";

  @Override
  public ModelAndView schemaManagement() {
    EditorRequestSchema request = EditorRequestSchema.builder()
            .dataModel(getJsonObject(DATAMODELSCHEMA))
            .uiForm(getJsonObject(UIFORMSCHEMA))
            .items(getJsonArrayOfItems(ITEMSSCHEMA)).build();

    ModelAndView model = new ModelAndView("schema-management");
    model.addObject("request", request);
    return model;
  }

  @Override
  public ModelAndView metadataManagement(
          Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb) {

    EditorRequestMetadata request = EditorRequestMetadata.builder()
            .dataModel(getJsonObject(DATAMODELMETADATA))
            .uiForm(getJsonObject(UIFORMMETADATA))
            .items(getJsonArrayOfItems(ITEMSMETADATA)).build();

    ModelAndView model = new ModelAndView("metadata-management");
    model.addObject("request", request);
    return model;
  }

  @Override
  public String dashboard() {
    return "dashboard";
  }

  /**
   * gets a JSON object from a file.
   *
   * @param path path of the file.
   * @return JSON object.
   */
  private JSONObject getJsonObject(String path) {
    Resource resource = new ClassPathResource(path);
    JSONParser parser = new JSONParser();
    JSONObject obj = null;
    try {
      obj = (JSONObject) parser.parse(
              new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
    } catch (IOException | ParseException e) {
      LOG.error("Error parsing JSON object!", e);
    }
    return obj;
  }

  /**
   * gets an array of TabulatorItems from a file.
   *
   * @param path path of a file.
   * @return array of TabulatorItems.
   */
  private TabulatorItems[] getJsonArrayOfItems(String path) {
    ObjectMapper mapper = new ObjectMapper();
    TabulatorItems[] items = null;
    Resource resource = new ClassPathResource(path);
    try {
      items = mapper.readValue(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), TabulatorItems[].class);
    } catch (IOException ex) {
      LOG.error(null, ex);
    }
    return items;
  }

}
