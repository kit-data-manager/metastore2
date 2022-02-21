/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.dto.EditorRequestSchema;
import edu.kit.datamanager.metastore2.dto.TabulatorItems;
import edu.kit.datamanager.metastore2.dto.EditorRequestMetadata;
import edu.kit.datamanager.metastore2.dto.TabulatorRemotePagination;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryControllerUI;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author sabrinechelbi
 */
@Controller
@Tag(name = "REST endpoints 4 GUI")
public class SchemaRegistryControllerUIImpl implements ISchemaRegistryControllerUI {

    private final static String DATAMODELSCHEMA = "/static/jsonSchemas/schemaRecord.json";
    private final static String UIFORMSCHEMA = "/static/jsonSchemas/uiFormSchemaRecord.json";
    private final static String ITEMSSCHEMA = "/static/jsonSchemas/itemsSchemaRecord.json";

    private final static String DATAMODELMETADATA = "/static/jsonSchemas/metadataRecord.json";
    private final static String UIFORMMETADATA = "/static/jsonSchemas/uiFormMetadataRecord.json";
    private final static String ITEMSMETADATA = "/static/jsonSchemas/itemsMetadataRecord.json";

    @Autowired
    private MetadataControllerImpl metadtaControllerImpl;

    @Autowired
    private SchemaRegistryControllerImpl schemaControllerImpl;

    @RequestMapping("/api/v1/ui/schemas")
    @ResponseBody
    @Override
    public ResponseEntity<TabulatorRemotePagination> getSchemaRecordsForUi(Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb) {

        Pageable pageable = PageRequest.of(pgbl.getPageNumber() - 1, pgbl.getPageSize(), Sort.by("id").ascending());

        ResponseEntity<List<MetadataSchemaRecord>> responseEntity4schemaRecords = schemaControllerImpl.getRecords(null, null, null, null, pageable, wr, hsr, ucb);
        List<MetadataSchemaRecord> schemaRecords = responseEntity4schemaRecords.getBody();

        String pageSize = responseEntity4schemaRecords.getHeaders().getFirst("Content-Range");

        TabulatorRemotePagination tabulatorRemotePagination = TabulatorRemotePagination.builder()
                .lastPage(tabulatorLastPage(pageSize, pageable))
                .data(schemaRecords)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(tabulatorRemotePagination);

    }

    @RequestMapping("/api/v1/ui/metadata")
    @ResponseBody
    @Override
    public ResponseEntity<TabulatorRemotePagination> getMetadataRecordsForUi(@RequestParam(value = "id", required = false) String id, Pageable pgbl, WebRequest wr, HttpServletResponse hsr, UriComponentsBuilder ucb) {

        Pageable pageable = PageRequest.of(pgbl.getPageNumber() - 1, pgbl.getPageSize(), Sort.by("id").ascending());
        List<String> schemaIds = id == null ? null : Arrays.asList(id);
        ResponseEntity< List<MetadataRecord>> responseEntity4metadataRecords = metadtaControllerImpl.getRecords(null, null, schemaIds, null, null, pageable, wr, hsr, ucb);
        List<MetadataRecord> metadataRecords = responseEntity4metadataRecords.getBody();

        String pageSize = responseEntity4metadataRecords.getHeaders().getFirst("Content-Range");

        TabulatorRemotePagination tabulatorRemotePagination = TabulatorRemotePagination.builder()
                .lastPage(tabulatorLastPage(pageSize, pageable))
                .data(metadataRecords)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(tabulatorRemotePagination);

    }

    @RequestMapping("/schema-management")
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

    @RequestMapping("/metadata-management")
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

    @RequestMapping(value = {"/dashboard", "/", ""})
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
                    new InputStreamReader(resource.getInputStream(), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
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
            Logger.getLogger(SchemaRegistryControllerUIImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return items;
    }

    /**
     * computes the total number of the available pages.
     *
     * @param pageSize pagination size
     * @param pageable page object
     * @return the total number of the available pages
     */
    private int tabulatorLastPage(String pageSize, Pageable pageable) {
        if ((Integer.parseInt(pageSize.split("/")[1]) % pageable.getPageSize()) == 0) {
            return Integer.parseInt(pageSize.split("/")[1]) / pageable.getPageSize();
        }
        return Integer.parseInt(pageSize.split("/")[1]) / pageable.getPageSize() + 1;
    }

}
