/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.metastore2.oaipmh.util;

import edu.kit.datamanager.metastore2.oaipmh.service.AbstractOAIPMHRepository;
import org.openarchives.oai._2.*;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Helper class for collecting request parameters and building OAI-PMH response.
 */
public class OAIPMHBuilder {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OAIPMHBuilder.class);

  private final OAIPMHtype response;
  private VerbType verb = null;
  private AbstractOAIPMHRepository repository;
  private String resumptionToken = null;
  private RequestType requestType;
  private IdentifyType identifyType;
  private ListMetadataFormatsType listMetadataFormatsType;
  private ListSetsType listSetsType;
  private GetRecordType getRecordType;
  private ListIdentifiersType listIdentifiers;
  private ListRecordsType listRecordsType;

  public OAIPMHBuilder() {
    response = new OAIPMHtype();
  }

  public static OAIPMHBuilder init(AbstractOAIPMHRepository repository, VerbType verb, String metadataPrefix, String identifier, Date from, Date until, String resumptionToken) {
    return new OAIPMHBuilder().initRequest(repository, verb, metadataPrefix, identifier, from, until, resumptionToken);
  }

  OAIPMHBuilder initRequest(AbstractOAIPMHRepository repository, VerbType verb, String metadataPrefix, String identifier, Date from, Date until, String resumptionToken) {
    this.repository = repository;
    requestType = new RequestType();

    if (from != null) {
      requestType.setFrom(repository.getDateFormat().format(from));
    }
    if (until != null) {
      requestType.setUntil(repository.getDateFormat().format(until));
    }
    requestType.setIdentifier(identifier);
    requestType.setMetadataPrefix(metadataPrefix);
    requestType.setValue(repository.getBaseUrl());
    requestType.setVerb(verb);
    if (verb == null) {
      addError(OAIPMHerrorcodeType.BAD_VERB, "Invalid verb type provided.");
      return this;
    }
    this.verb = verb;
    switch (verb) {
      case IDENTIFY:
        //no mandatory arguments
        identifyType = new IdentifyType();
        identifyType.setBaseURL(repository.getBaseUrl());
        identifyType.setGranularity(repository.getGranularity());
        identifyType.setEarliestDatestamp(repository.getEarliestDatestamp());
        identifyType.setDeletedRecord(repository.getDeletedRecordSupport());
        identifyType.setRepositoryName(repository.getRepositoryName());
        identifyType.setProtocolVersion(repository.getProtocolVersion());
        identifyType.getAdminEmail().addAll(repository.getAdminEmail());
        identifyType.getDescription().addAll(repository.getRepositoryDescription());
        break;
      case LIST_METADATA_FORMATS:
        //no mandatory identifier
        listMetadataFormatsType = new ListMetadataFormatsType();
        break;
      case LIST_SETS:
        this.resumptionToken = resumptionToken;
        listSetsType = new ListSetsType();
        break;
      case GET_RECORD:
        //identifier and metadata prefix are mandatory
        if (identifier == null || metadataPrefix == null) {
          addError(OAIPMHerrorcodeType.BAD_ARGUMENT, "Arguments identifier and metadataPrefix must not be null while using verb " + verb + ".");
        }

        getRecordType = new GetRecordType();
        break;
      case LIST_IDENTIFIERS:
        if (metadataPrefix == null) {
          addError(OAIPMHerrorcodeType.BAD_ARGUMENT, "Argument metadataPrefix must not be null while using verb " + verb + ".");
        }
        this.resumptionToken = resumptionToken;
        listIdentifiers = new ListIdentifiersType();
        break;
      case LIST_RECORDS:
        if (metadataPrefix == null) {
          addError(OAIPMHerrorcodeType.BAD_ARGUMENT, "Argument metadataPrefix must not be null while using verb " + verb + ".");
        }
        this.resumptionToken = resumptionToken;
        listRecordsType = new ListRecordsType();
        break;
    }
    return this;
  }

  public Date getFromDate() {
    String date = requestType.getFrom();
    try {
      if (date != null) {
        return repository.getDateFormat().parse(date);
      }
    } catch (ParseException ex) {
      //wrong date
    }
    return null;
  }

  public Date getUntilDate() {
    String date = requestType.getUntil();
    try {
      if (date != null) {
        return repository.getDateFormat().parse(date);
      }
    } catch (ParseException ex) {
      //wrong date
    }
    return null;
  }

  public VerbType getVerb() {
    return requestType.getVerb();
  }

  public String getMetadataPrefix() {
    return requestType.getMetadataPrefix();
  }

  public String getIdentifier() {
    return requestType.getIdentifier();
  }

  public String getResumptionToken() {
    return resumptionToken;
  }

  public OAIPMHBuilder addMetadataFormat(MetadataFormatType format) {
    if (isError()) {
      return this;
    }
    switch (verb) {
      case LIST_METADATA_FORMATS:
        listMetadataFormatsType.getMetadataFormat().add(format);
        break;
      default:
        // no action required
    }
    return this;
  }

  public OAIPMHBuilder addSet(String name, String spec) {
    if (isError()) {
      return this;
    }
    return addSet(name, spec, null);
  }

  public OAIPMHBuilder addSet(String name, String spec, Object description) {
    if (isError()) {
      return this;
    }
    switch (verb) {
      case LIST_SETS:
        SetType set = new SetType();
        set.setSetName(name);
        set.setSetSpec(spec);
        if (description != null) {
          DescriptionType descriptionType = new DescriptionType();
          descriptionType.setAny(description);
          set.getSetDescription().add(descriptionType);
        }
        listSetsType.getSet().add(set);
        break;
      default:
        // no action required
    }
    return this;
  }

  public OAIPMHBuilder addRecord(String identifier, Date recordDatestamp, List<String> setSpecs) {
    if (isError()) {
      return this;
    }
    return addRecord(identifier, recordDatestamp, setSpecs, null, null);
  }

  public OAIPMHBuilder addRecord(String identifier, Date recordDatestamp, List<String> setSpecs, Object metadata) {
    if (isError()) {
      return this;
    }
    return addRecord(identifier, recordDatestamp, setSpecs, metadata, null);
  }

  public OAIPMHBuilder addRecord(String identifier, Date recordDatestamp, List<String> setSpecs, Object metadata, Object about) {
    if (isError()) {
      return this;
    }
    switch (verb) {
      case GET_RECORD:
      case LIST_RECORDS: 
        getOrListRecords(identifier, recordDatestamp, setSpecs, metadata, about);
        break;
      case LIST_IDENTIFIERS: {
        HeaderType header = new HeaderType();
        if (identifier == null || recordDatestamp == null) {
          throw new IllegalArgumentException("Arguments identifier and recordDatestamp must not be null.");
        }
        header.setIdentifier(identifier);
        header.setDatestamp(repository.getDateFormat().format(recordDatestamp));
        // header.setStatus(StatusType.DELETED); --> not supported yet
        header.getSetSpec().addAll(setSpecs);
        listIdentifiers.getHeader().add(header);
        break;
      }
      default: 
        // no action required
    }
    return this;
  }
  
  private void getOrListRecords(String identifier, Date recordDatestamp, List<String> setSpecs, Object metadata, Object about) {
        RecordType recordType = new RecordType();
        HeaderType header = new HeaderType();
        if (identifier == null || recordDatestamp == null) {
          throw new IllegalArgumentException("Arguments identifier and recordDatestamp must not be null.");
        }
        header.setIdentifier(identifier);
        header.setDatestamp(repository.getDateFormat().format(recordDatestamp));
        if (setSpecs != null) {
          header.getSetSpec().addAll(setSpecs);
        }
        recordType.setHeader(header);
        if (metadata != null) {
          //record metadata
          MetadataType md = new MetadataType();
          md.setAny(metadata);
          recordType.setMetadata(md);
        }
        if (about != null) {
          //about info defined by community
          AboutType aboutType = new AboutType();
          aboutType.setAny(about);
          recordType.getAbout().add(aboutType);
        }

        if (VerbType.GET_RECORD.equals(verb)) {
          getRecordType.setRecord(recordType);
        } else {
          listRecordsType.getRecord().add(recordType);
        }
  }

  public OAIPMHBuilder addError(OAIPMHerrorcodeType code, String message) {
    OAIPMHerrorType error = new OAIPMHerrorType();
    error.setCode(code);
    if (message != null) {
      error.setValue(message);
    }
    response.getError().add(error);
    return this;
  }

  public OAIPMHBuilder setResumptionToken(ResumptionTokenType token) {
    if (isError()) {
      return this;
    }
    switch (verb) {
      case LIST_IDENTIFIERS:
        listIdentifiers.setResumptionToken(token);
        break;
      case LIST_SETS:
        listSetsType.setResumptionToken(token);
        break;
      case LIST_RECORDS:
        listRecordsType.setResumptionToken(token);
        break;
      default:
        // no token necessary
    }
    return this;
  }

  public boolean isError() {
    return !response.getError().isEmpty();
  }

  /**
   * Build type for OAI-PMH.
   *
   * @return OAI-PMH type.
   */
  @SuppressWarnings("JavaUtilDate")
  public OAIPMHtype build() {
    response.setRequest(requestType);
    GregorianCalendar cal = new GregorianCalendar();
    cal.setGregorianChange(new Date());
    try {
      response.setResponseDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
    } catch (DatatypeConfigurationException ex) {
      LOGGER.error("Failed to set response date to OAI-PMH response.", ex);
    }

    if (!isError()) {
      switch (verb) {
        case IDENTIFY:
          response.setIdentify(identifyType);
          break;
        case LIST_METADATA_FORMATS:
          response.setListMetadataFormats(listMetadataFormatsType);
          break;
        case LIST_SETS:
          response.setListSets(listSetsType);
          break;
        case GET_RECORD:
          response.setGetRecord(getRecordType);
          break;
        case LIST_IDENTIFIERS:
          response.setListIdentifiers(listIdentifiers);
          break;
        case LIST_RECORDS:
          response.setListRecords(listRecordsType);
          break;
      }
    }

    return response;
  }

}
