/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.web.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.dto.EditorRequestSchema;
import edu.kit.datamanager.metastore2.dto.TabulatorItems;
import edu.kit.datamanager.metastore2.dto.EditorRequestMetadata;
import edu.kit.datamanager.metastore2.util.MetadataSchemaRecordUtil;
import edu.kit.datamanager.metastore2.web.ISchemaRegistryControllerUI;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import edu.kit.datamanager.repo.dao.spec.dataresource.ResourceTypeSpec;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.ResourceType;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author sabrinechelbi
 */
@Controller
public class SchemaRegistryControllerUIImpl implements ISchemaRegistryControllerUI {

    private final static String DATAMODELSCHEMA = "/static/jsonSchemas/schemaRecord.json";
    private final static String UIFORMSCHEMA = "/static/jsonSchemas/uiFormSchemaRecord.json";
    private final static String ITEMSSCHEMA = "/static/jsonSchemas/itemsSchemaRecord.json";
    
            private final static String DATAMODELMETADATA = "/static/jsonSchemas/metadataRecord.json";
    private final static String UIFORMMETADATA = "/static/jsonSchemas/uiFormMetadataRecord.json";
    private final static String ITEMSMETADATA = "/static/jsonSchemas/itemsMetadataRecord.json";

    @Autowired
    private IDataResourceDao schemaRecordDao;

    @Autowired
    private MetastoreConfiguration schemaConfig;
    
        @Autowired
    private MetadataControllerImpl metadtaControllerImpl;

    @RequestMapping("/schema-management")
    @Override
    public ModelAndView schemaManagement() {
        // Search for resource type of MetadataSchemaRecord
        Specification<DataResource> spec = ResourceTypeSpec.toSpecification(ResourceType.createResourceType(MetadataSchemaRecord.RESOURCE_TYPE));

        List<DataResource> records = null;
        records = schemaRecordDao.findAll(spec);

        List<MetadataSchemaRecord> schemaRecords = new ArrayList<>();
        records.forEach((record) -> {
            MetadataSchemaRecord item = MetadataSchemaRecordUtil.migrateToMetadataSchemaRecord(schemaConfig, record, false);
            fixSchemaDocumentUri(item);
            schemaRecords.add(item);
        });

        EditorRequestSchema request = EditorRequestSchema.builder()
                .dataModel(getJsonObject(DATAMODELSCHEMA))
                .uiForm(getJsonObject(UIFORMSCHEMA))
                .schemaRecords(schemaRecords)
                .items(getJsonArrayOfItems(ITEMSSCHEMA)).build();

        ModelAndView model = new ModelAndView("schema-management");
        model.addObject("request", request);
        return model;
    }
    
    @RequestMapping("/schema-management/{id}")
    @Override
    public ModelAndView metadataManagement(@PathVariable(value = "id", required = true) String id,
            Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb) {

        ResponseEntity< List<MetadataRecord>> metadataRecords = metadtaControllerImpl.getRecords(null, null, Arrays.asList(id),null,null, pgbl, wr, hsr, ucb);
        EditorRequestMetadata request = EditorRequestMetadata.builder()
                .dataModel(getJsonObject(DATAMODELMETADATA))
                .uiForm(getJsonObject(UIFORMMETADATA))
                .metadataRecords(metadataRecords.getBody())
                .items(getJsonArrayOfItems(ITEMSMETADATA)).build();
        
        ModelAndView model = new ModelAndView("metadata-management");
         model.addObject("request", request);
        return model;
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
            items = mapper.readValue(Files.newBufferedReader(Paths.get(resource.getURI()), StandardCharsets.UTF_8), TabulatorItems[].class);
        } catch (IOException ex) {
            Logger.getLogger(SchemaRegistryControllerUIImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return items;
    }

    private void fixSchemaDocumentUri(MetadataSchemaRecord record) {
        record.setSchemaDocumentUri(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(SchemaRegistryControllerImpl.class).getSchemaDocumentById(record.getSchemaId(), record.getSchemaVersion(), null, null)).toUri().toString());
    }

}
