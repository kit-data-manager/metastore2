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
package edu.kit.datamanager.metastore2.oaipmh.web;

import edu.kit.datamanager.metastore2.oaipmh.service.AbstractOAIPMHRepository;
import edu.kit.datamanager.metastore2.oaipmh.util.OAIPMHBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.apache.http.HttpStatus;
import org.openarchives.oai._2.OAIPMHerrorcodeType;
import org.openarchives.oai._2.OAIPMHtype;
import org.openarchives.oai._2.VerbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

/**
 * Controller for OAI-PMH protocol.
 *
 * @author jejkal
 */
@Controller
@RequestMapping(value = "/oaipmh")
public class OaiPmhController {

  private static final Logger LOGGER = LoggerFactory.getLogger(OaiPmhController.class);

  @Autowired
  private AbstractOAIPMHRepository repository;

  @RequestMapping(path = "", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity processRequest(
          @Parameter(description = "OAI-PMH supported verb, e.g. GET_RECORD, IDENTIFY, LIST_IDENTIFIERS, LIST_METADATA_FORMATS, LIST_RECORDS, LIST_SETS.") @RequestParam(value = "verb", required = true) String verb,
          @Parameter(description = "OAI-PMH from date in a data format depending on the repository, e.g. yyyy-MM-dd'T'HH:mm:ss'Z'.", example = "2017-05-10T10:41:00Z") @RequestParam(value = "from", required = false) String from,
          @Parameter(description = "OAI-PMH until date in a data format depending on the repository, e.g. yyyy-MM-dd'T'HH:mm:ss'Z'.", example = "2017-05-10T10:41:00Z") @RequestParam(value = "until", required = false) String until,
          @Parameter(description = "OAI-PMH document set to harvest. Only available if sets are supported.") @RequestParam(value = "set", required = false) String set,
          @Parameter(description = "OAI-PMH metadata document identifier.") @RequestParam(value = "identifier", required = false) String identifier,
          @Parameter(description = "OAI-PMH metadata format prefix.") @RequestParam(value = "metadataPrefix", required = false) String metadataPrefix,
          @Parameter(description = "OAI-PMH resumption token for pagination.") @RequestParam(value = "resumptionToken", required = false) String resumptionToken) {
    VerbType verbType = null;

    try {
      LOGGER.trace("Checking provided OAI-PMH verb {}", verb);
      verbType = VerbType.fromValue(verb);
      LOGGER.trace("Verb {} is valid.", verb);
    } catch (IllegalArgumentException ex) {
      //wrong verb...OAIPMHBuilder will handle this
      LOGGER.warn("Verb '" + verb + "' is invalid. OAI-PMH error will be returned.", ex);
    }

    Date fromDate = null;
    Date untilDate = null;
    boolean wrongDateFormat = false;
    try {
      LOGGER.trace("Checking 'from' and 'until' dates.");
      if (from != null) {
        from = from.replaceAll("[\r\n]","");
        LOGGER.trace("Checking 'from' date {}.", from);
        fromDate = repository.getDateFormat().parse(from);
        LOGGER.trace("Successfully parsed 'from' date.");
      }
      if (until != null) {
        until = until.replaceAll("[\r\n]","");
        LOGGER.trace("Checking 'until' date {}.", until);
        untilDate = repository.getDateFormat().parse(until);
        LOGGER.trace("Successfully parsed 'until' date.");

      }
    } catch (ParseException ex) {
      LOGGER.warn("'from' and/or 'until' date are in an invalid format.  OAI-PMH error will be returned.", ex);
      wrongDateFormat = true;
    }

    try {
      OAIPMHBuilder builder = OAIPMHBuilder.init(repository, verbType, metadataPrefix, identifier, fromDate, untilDate, resumptionToken);
      if (set != null) {
        LOGGER.trace("'Set' request param provided, but sets are not supported. Returning OAI-PMH error.");
        builder.addError(OAIPMHerrorcodeType.NO_SET_HIERARCHY, "Sets are currently not supported.");
      }

      if (!builder.isError()) {
        if (wrongDateFormat) {
          LOGGER.debug("Returning BAD_ARGUMENT error due to wrong date format.");
          //date format is wrong
          builder.addError(OAIPMHerrorcodeType.BAD_ARGUMENT, "Either from and/or until date are in the wrong format.");
        } else if (!(metadataPrefix == null || repository.isPrefixSupported(metadataPrefix))) {
          //prefix is not null and not supported
          LOGGER.debug("Returning CANNOT_DISSEMINATE_FORMAT error due to unsupported metadata prefix '{}'.", metadataPrefix);
          builder.addError(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, "Metadata prefix " + metadataPrefix + " not supported by repository.");
        } else {
          LOGGER.trace("Handling request by repository implementation.");
          //if request is wrong, error is set already at this point...if no error, continue
          repository.handleRequest(builder);
        }
      }

      //build the result and return it.
      LOGGER.trace("Building and returning OAI-PMH response.");
      JAXBContext jaxbContext = JAXBContext.newInstance(OAIPMHtype.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      marshaller.marshal(builder.build(), bout);

      return ResponseEntity.ok(bout.toString(StandardCharsets.UTF_8));
    } catch (Exception e) {
      LOGGER.error("Failed to serialize OAIPMHtype.", e);
      return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body("Unable to serialize XML response.");
    }
  }
}
