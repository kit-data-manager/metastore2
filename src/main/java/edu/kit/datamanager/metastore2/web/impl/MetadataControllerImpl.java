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

import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.web.IMetadataController;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/api/v1/metadata")
@Api(value = "Metadata Repository")
public class MetadataControllerImpl implements IMetadataController{

  @Override
  public ResponseEntity createRecord(
          @RequestPart(name = "record") final MetadataRecord record,
          @RequestPart(name = "document") final MultipartFile document,
          WebRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder){
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity<MetadataRecord> getRecordById(
          @PathVariable(value = "id") String id,
          @RequestParam(value = "version", required = false) Long version,
          WebRequest wr,
          HttpServletResponse hsr){
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity getMetadataDocumentById(
          String id,
          Long version,
          WebRequest wr,
          HttpServletResponse hsr){
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity<List<MetadataRecord>> getRecords(
          @RequestParam(value = "resoureId", required = false) List<String> relatedIds,
          @RequestParam(value = "schemaId", required = false) List<String> schemaIds,
          @RequestParam(name = "from", required = false) Instant updateFrom,
          @RequestParam(name = "until", required = false) Instant updateUntil,
          Pageable pgbl,
          WebRequest wr,
          HttpServletResponse hsr,
          UriComponentsBuilder ucb){
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity<MetadataRecord> updateRecord(
          @PathVariable("id") String id,
          @RequestPart(name = "record", required = false) final MetadataRecord record,
          @RequestPart(name = "document", required = false) final MultipartFile document,
          WebRequest request,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder){
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ResponseEntity deleteRecord(
          @PathVariable(value = "id") String id,
          WebRequest wr,
          HttpServletResponse hsr){
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
