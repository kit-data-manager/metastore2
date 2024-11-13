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
package edu.kit.datamanager.metastore2.oaipmh.service;

import edu.kit.datamanager.metastore2.configuration.MetastoreConfiguration;
import edu.kit.datamanager.metastore2.configuration.OaiPmhConfiguration;
import edu.kit.datamanager.metastore2.dao.IDataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataFormatDao;
import edu.kit.datamanager.metastore2.domain.DataRecord;
import edu.kit.datamanager.metastore2.domain.oaipmh.MetadataFormat;
import edu.kit.datamanager.metastore2.oaipmh.util.OAIPMHBuilder;
import edu.kit.datamanager.repo.util.DataResourceUtils;
import edu.kit.datamanager.util.xml.DataCiteMapper;
import edu.kit.datamanager.util.xml.DublinCoreMapper;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.datacite.schema.kernel_4.Resource;
import org.openarchives.oai._2.*;
import org.purl.dc.elements._1.ElementContainer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Simple OAI-PMH repository implementation taking its information from a KIT
 * Data Manager instance. Metadata formats are obtained from the metadata schema
 * table. The according metadata documents are the ones extracted during ingest
 * and stored in the metadata indexing table. By default, this implementation
 * supports the Dublin Core metadata format. If no metadata extraction is
 * performed during ingest and no metadata schemas are registered, the according
 * Dublin Core documents are created on the fly by this implementation. This
 * offers at least basic OAI-PMH support out of the box for each repository
 * based on KIT Data Manager.
 *
 * This OAI-PMH repository identifies itself with the following properties:
 *
 * <pre>
 * Name: DefaultRepository
 * RepositoryDescription: Empty (might be Re3Data Identification alter on)
 * DeletedRecordSupport: No
 * AdminEmail: DataManagerSettings.GENERAL_SYSTEM_MAIL_ADDRESS
 * EarliestDatestamp: 1970-01-01T00:00:00Z
 * Granularity: YYYY_MM_DD_THH_MM_SS_Z
 * BaseUrl: DataManagerSettings.GENERAL_BASE_URL_ID + /oaipmh
 * </pre>
 *
 * OAI-PMH sets are currently not supported. However, listing all sets returns a
 * single element 'default' and all records are defined to be in set 'default'.
 *
 * @author jejkal
 */
@Component
@SuppressWarnings("JavaUtilDate")
public class MetastoreOAIPMHRepository extends AbstractOAIPMHRepository {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MetastoreOAIPMHRepository.class);

  private static final MetadataFormatType DC_SCHEMA;
  private static final MetadataFormatType DATACITE_SCHEMA;

  private final OaiPmhConfiguration pluginConfiguration;

  @Autowired
  private IDataRecordDao dataRecordDao;
  @Autowired
  private IMetadataFormatDao metadataFormatDao;
  @Autowired
  private MetastoreConfiguration metadataConfig;
  
  static {
    DC_SCHEMA = new MetadataFormatType();
    DC_SCHEMA.setMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
    DC_SCHEMA.setSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
    DC_SCHEMA.setMetadataPrefix("oai_dc");

    DATACITE_SCHEMA = new MetadataFormatType();
    DATACITE_SCHEMA.setMetadataNamespace("http://datacite.org/schema/kernel-4");
    DATACITE_SCHEMA.setSchema("http://schema.datacite.org/meta/kernel-4.1/metadata.xsd");
    DATACITE_SCHEMA.setMetadataPrefix("datacite");
  }

  /**
   * Default constructor.
   * @param pluginConfiguration configuration for OAI-PMH.
   */
  @Autowired
  public MetastoreOAIPMHRepository(OaiPmhConfiguration pluginConfiguration) {
    this("MetastoreRepository", pluginConfiguration);
  }

  /**
   * Default constructor.
   *
   * @param name The repository name.
   * @param pluginConfiguration configuration for OAI-PMH.
   */
  private MetastoreOAIPMHRepository(String name, OaiPmhConfiguration pluginConfiguration) {
    super(name);
    this.pluginConfiguration = pluginConfiguration;
  }

  @Override
  public List<DescriptionType> getRepositoryDescription() {
    return new ArrayList<>();
  }

  @Override
  public DeletedRecordType getDeletedRecordSupport() {
    return DeletedRecordType.NO;
  }

  @Override
  public List<String> getAdminEmail() {
    return Collections.singletonList(pluginConfiguration.getAdminEmail());
  }

  @Override
  public String getEarliestDatestamp() {
    return getDateFormat().format(new Date(0L));
  }

  @Override
  public GranularityType getGranularity() {
    return GranularityType.YYYY_MM_DD_THH_MM_SS_Z;
  }

  @Override
  public String getBaseUrl() {
    return ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
  }

  @Override
  public boolean isPrefixSupported(String prefix) {
    boolean exists = DC_SCHEMA.getMetadataPrefix().equals(prefix) || DATACITE_SCHEMA.getMetadataPrefix().equals(prefix);
    LOGGER.trace(prefix + ": " + exists);
    if (!exists) {
      MetadataFormat metadataFormat = new MetadataFormat();
      metadataFormat.setMetadataPrefix(prefix);
      ExampleMatcher caseInsensitive = ExampleMatcher.matchingAll().withIgnoreCase();
      Example<MetadataFormat> example = Example.of(metadataFormat, caseInsensitive);
      Optional<MetadataFormat> findOne = metadataFormatDao.findOne(example);
      if (findOne.isPresent()) {
        MetadataFormat item = findOne.get();
        LOGGER.trace("Found at least one item with prefix: " + item.getMetadataPrefix());
        exists = true;
      }
    }
    return exists;
  }

  @Override
  public void identify(OAIPMHBuilder builder) {
    LOGGER.trace("Performing identify().");
    //should already been handled in builder...modification of response possible here
  }

  @Override
  public void listSets(OAIPMHBuilder builder) {
    LOGGER.trace("Performing listSets().");
    //@TODO support collections?
    builder.addError(OAIPMHerrorcodeType.NO_SET_HIERARCHY, "Sets are currently not supported.");
  }

  @Override
  public void listMetadataFormats(OAIPMHBuilder builder) {
    LOGGER.trace("Performing listMetadataFormats().");
    //@TODO extend by other formats
    builder.addMetadataFormat(DC_SCHEMA);
    builder.addMetadataFormat(DATACITE_SCHEMA);
    long noOfEntries = metadataFormatDao.count();
    long entriesPerPage = 50;
    long page = 0;
    // add also the schema registered in the schema registry
    do {
      List<MetadataFormat> allXmlSchemas = metadataFormatDao.findAll(PageRequest.of((int) page, (int) entriesPerPage)).getContent();
      for (MetadataFormat metadataFormat : allXmlSchemas) {
        MetadataFormatType item = new MetadataFormatType();
        item.setMetadataNamespace(metadataFormat.getMetadataNamespace());
        item.setMetadataPrefix(metadataFormat.getMetadataPrefix());
        item.setSchema(metadataFormat.getSchema());
        builder.addMetadataFormat(item);
      }
      page++;
    } while (page * entriesPerPage < noOfEntries);
  }

  @Override
  public void listIdentifiers(OAIPMHBuilder builder) {
    LOGGER.trace("Performing listIdentifiers().");
    List<DataRecord> results = getEntities(builder);
    if (results.isEmpty()) {
      if (!builder.isError()) {
        LOGGER.error("No results obtained. Returning OAI-PMH error NO_RECORDS_MATCH.");
        builder.addError(OAIPMHerrorcodeType.NO_RECORDS_MATCH, null);
      }
      return;
    }

    LOGGER.trace("Adding {} records to result.", results.size());
    results.stream().forEach(result -> {
      //TODO get proper date
      Date changeDate = new Date(0L);
      if (result.getLastUpdate() != null) {
        changeDate = Date.from(result.getLastUpdate());
      }

      builder.addRecord(result.getMetadataId(), changeDate, List.of("default"));
    });
  }

  @Override
  public void getRecord(OAIPMHBuilder builder) {
    //only get record entries for prefix
    LOGGER.trace("Performing getRecord().");
    DataRecord resource = getEntity(builder);

    if (resource != null) {
      LOGGER.trace("Adding single record to result.");
      addRecordEntry(resource, builder);
    } else {
      LOGGER.error("No result obtained. Returning OAI-PMH error ID_DOES_NOT_EXIST.");
      builder.addError(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "No object for identifier " + builder.getIdentifier() + " found.");
    }

  }

  @Override
  public void listRecords(OAIPMHBuilder builder) {
    LOGGER.trace("Performing listRecords().");
    List<DataRecord> results = getEntities(builder);
    if (results.isEmpty()) {
      if (!builder.isError()) {
        LOGGER.error("No results obtained. Returning OAI-PMH error NO_RECORDS_MATCH.");
        builder.addError(OAIPMHerrorcodeType.NO_RECORDS_MATCH, null);
      }
      return;
    }
    LOGGER.trace("Adding {} records to result.", results.size());
    results.stream().forEach(result -> addRecordEntry(result, builder));
  }

  /**
   * Get the metadata document for the provided object and schema id. The
   * metadata document is loaded from the URL read from the entry fitting the
   * object-schemaId-combination in the MetadataIndexingTask table. If no entry
   * was found, it is checked whether DublinCore metadata is requested by the
   * schemaId 'dc'. If this is the case, DublinCore metadata is generated on the
   * fly.
   *
   * Otherwise, null is returned and must be handled by the caller with an
   * according OAI-PMH error.
   *
   * @param object The object to obtain the metadata document for.
   * @param schemaId The id of the metadata schema.
   *
   * @return The metadata document or null.
   */
  private Document getMetadataDocument(DataRecord object, String schemaId) {
    LOGGER.trace("Obtaining metadata document for schema {} and resource identifier {}", schemaId, object.getId());
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    boolean wasError = true;
    if (DC_SCHEMA.getMetadataPrefix().equals(schemaId)) {
      //create DC document on the fly
      LOGGER.info("Creating Dublin Core document on the fly.", object.getId());
      //create DC metadata
      try {
        UnaryOperator<String> dummy;
        dummy = t -> "dummy" + t;
        edu.kit.datamanager.repo.domain.DataResource dr = DataResourceUtils.getResourceByIdentifierOrRedirect(metadataConfig, object.getMetadataId(), null, dummy);
        ElementContainer container = DublinCoreMapper.dataResourceToDublinCoreContainer(DataResourceUtils.migrateToDataResource(dr));
        JAXBContext jaxbContext = JAXBContext.newInstance(ElementContainer.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(container, bout);
        wasError = false;
      } catch (JAXBException ex) {
        LOGGER.error("Failed to build Dublin Core document.", ex);
      }
    } else if (DATACITE_SCHEMA.getMetadataPrefix().equals(schemaId)) {
      LOGGER.info("Creating Datacite document on the fly.", object.getId());
      try {
        UnaryOperator<String> dummy;
        dummy = t -> "dummy" + t;
        edu.kit.datamanager.repo.domain.DataResource dr = DataResourceUtils.getResourceByIdentifierOrRedirect(metadataConfig, object.getMetadataId(), null, dummy);
        // Todo check for internal related schema identifier switch to URL
        Resource resource = DataCiteMapper.dataResourceToDataciteResource(DataResourceUtils.migrateToDataResource(dr));
        JAXBContext jaxbContext = JAXBContext.newInstance(Resource.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(resource, bout);
        wasError = false;
      } catch (JAXBException ex) {
        LOGGER.error("Failed to build Datacite document.", ex);
      }
    } else if (object.getSchemaId().equals(schemaId)) {
      LOGGER.info("Return stored document of resource '{}'.", object.getMetadataId());
      try {
        URL url = new URI(object.getMetadataDocumentUri()).toURL();
        byte[] readFileToByteArray = FileUtils.readFileToByteArray(Paths.get(url.toURI()).toFile());
        try (InputStream inputStream = new ByteArrayInputStream(readFileToByteArray)) {
          IOUtils.copy(inputStream, bout);
          wasError = false;
        }
      } catch (URISyntaxException | IOException ex) {
        LOGGER.error("Error while reading document", ex);
      }
    } else {
      LOGGER.error("No valid schema '{}' found for resource '{}'", schemaId, object.getMetadataId());
    }

    Document doc = null;
    if (!wasError) {
      try {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        DocumentBuilder docBuilder = fac.newDocumentBuilder();
        doc = docBuilder.parse(new ByteArrayInputStream(bout.toByteArray()));
      } catch (SAXException | IOException | ParserConfigurationException ex) {
        LOGGER.error("Failed to create w3c document from serialized metadata of schema " + schemaId + ".", ex);
      }
    }
    return doc;
  }

  /**
   * Add a record entry for the provided digital object to the provided builder.
   * This call tries to obtain the metadata document for the provided object and
   * is this succeeds, an according record is added to the builder.
   *
   * If no metadata document can be obtained, an according OAI-PMH error is
   * added.
   *
   * @param result The digital object to add a record for.
   * @param builder The OAIPMHBuilder.
   */
  private void addRecordEntry(DataRecord result, OAIPMHBuilder builder) {
    LOGGER.trace("Adding record for object identifier {} to response.", result.getId());
    Document doc = getMetadataDocument(result, builder.getMetadataPrefix());
    if (doc != null) {
      LOGGER.trace("Adding record using obtained metadata document.");
      Date resourceDate = new Date(0L);
      if (result.getLastUpdate() != null) {
        resourceDate = Date.from(result.getLastUpdate());
      }
      builder.addRecord(result.getMetadataId(), resourceDate, List.of("default"), doc.getDocumentElement());
    } else {
      LOGGER.error("No metadata document found for prefix {} and object identifier {}. Returning OAI-PMH error CANNOT_DISSEMINATE_FORMAT.", builder.getMetadataPrefix(), result.getId());
      builder.addError(OAIPMHerrorcodeType.CANNOT_DISSEMINATE_FORMAT, null);
    }
  }

  /**
   * Get a single entity according to the arguments set at the provided
   * OAIPMHBuilder.
   *
   * @param builder The OAIPMHBuilder.
   *
   * @return A entity which might be null.
   */
  private DataRecord getEntity(OAIPMHBuilder builder) {
    LOGGER.trace("Performing getEntity().");
    DataRecord entity = null;

    Optional<DataRecord> findEntity = dataRecordDao.findTopByMetadataIdOrderByVersionDesc(builder.getIdentifier());
    if (findEntity.isPresent()) {
      entity = findEntity.get();
    }
    return entity;

  }

  /**
   * Get all digital objects according to the arguments set at the provided
   * OAIPMHBuilder.
   *
   * Depending of the values ot 'from', 'until' and 'metadataPrefix' set at the
   * OAIPMHBuilder the result list may contain all or a reduced list of objects.
   * The list might also be empty. In that case a proper OAI-PMH error must be
   * created by the caller.
   *
   * @param builder The OAIPMHBuilder.
   *
   * @return A list of entities which might be empty.
   */
  @SuppressWarnings("StringSplitter")
  private List<DataRecord> getEntities(OAIPMHBuilder builder) {
    List<DataRecord> results;

    String prefix = builder.getMetadataPrefix();
    LOGGER.trace("Getting entities for metadata prefix {} from repository.", prefix);
    LOGGER.trace("Checking request for resumption token");
    String resumptionToken = builder.getResumptionToken();
    int currentCursor = 0;
    long overallCount;
    Instant from = builder.getFromDate() != null ? builder.getFromDate().toInstant() : Instant.now().minus(36500, ChronoUnit.DAYS);
    Instant until = builder.getUntilDate() != null ? builder.getUntilDate().toInstant() : Instant.now().plus(1, ChronoUnit.SECONDS);
    //check resumption token
    if (resumptionToken != null) {
      String tokenValue = new String(Base64.decodeBase64(URLDecoder.decode(resumptionToken, StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      LOGGER.trace("Found token with value {}", tokenValue);
      String[] elements = tokenValue.split("/");
      if (elements.length != 2) {
        LOGGER.error("Invalid resumption token. Returning OAI-PMH error BAD_RESUMPTION_TOKEN.");
        builder.addError(OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, null);
        return new ArrayList<>();
      }
      try {
        LOGGER.trace("Parsing token values.");
        currentCursor = Integer.parseInt(elements[0]);
        overallCount = Integer.parseInt(elements[1]);
        LOGGER.trace("Obtained {} as current cursor from token. Overall element count is {}.", currentCursor, overallCount);
      } catch (NumberFormatException ex) {
        //log error
        builder.addError(OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, null);
        return new ArrayList<>();
      }
    } else {
      LOGGER.trace("No resumption token found.");
    }

    int maxElementsPerList = pluginConfiguration.getMaxElementsPerList();

    int page = currentCursor / maxElementsPerList;
    boolean predefinedPrefix = DC_SCHEMA.getMetadataPrefix().equals(prefix) || DATACITE_SCHEMA.getMetadataPrefix().equals(prefix);
    if (predefinedPrefix) {
      List<String> findMetadataPrefix = metadataFormatDao.getAllIds();
      if (LOGGER.isTraceEnabled()) {
        for (String item : findMetadataPrefix) {
          LOGGER.trace("SchemaID: " + item);
        }
      }
      LOGGER.trace("findBySchemaIdAndLastUpdateBetween({},{},{}, Page({},{}))", findMetadataPrefix, from, until, page, maxElementsPerList);
      overallCount = dataRecordDao.countBySchemaIdInAndLastUpdateBetween(findMetadataPrefix, from, until);
      results = dataRecordDao.findBySchemaIdInAndLastUpdateBetween(findMetadataPrefix, from, until, PageRequest.of(page, maxElementsPerList));
      LOGGER.trace("Found '" + results.size() + "' elements of '" + dataRecordDao.count() + "' elements in total!");
    } else {
      LOGGER.trace("findBySchemaIdAndLastUpdateBetween({},{},{}, Page({},{}))", prefix, from, until, page, maxElementsPerList);
      overallCount = dataRecordDao.countBySchemaIdAndLastUpdateBetween(prefix, from, until);
      results = dataRecordDao.findBySchemaIdAndLastUpdateBetween(prefix, from, until, PageRequest.of(page, maxElementsPerList));
      LOGGER.trace("Found '" + results.size() + "' elements of '" + dataRecordDao.count() + "' elements in total!");
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("List top 100 of all items:");
      List<DataRecord> findAll = dataRecordDao.findAll(PageRequest.of(0, 100)).getContent();
      for (DataRecord item : findAll) {
        LOGGER.trace("-> " + item);
      }
    }
    LOGGER.trace("Setting next resumption token.");
    int cursor = currentCursor + results.size();

    if (cursor == overallCount) {
      LOGGER.debug("New cursor {} exceeds element count {}, no more elements available. Setting resumption token to 'null'.", (currentCursor + maxElementsPerList), overallCount);
      //lsit complete, add no resumptiontoken
      builder.setResumptionToken(null);
    } else {
      ResumptionTokenType token = new ResumptionTokenType();
      //set list size
      token.setCompleteListSize(BigInteger.valueOf(overallCount));
      //set current cursor
      token.setCursor(BigInteger.valueOf(currentCursor + results.size()));
      LOGGER.trace("Setting new resumption token with cursor at position {}.", token.getCursor());
      //we set no expiration as the token never expires
      String value = token.getCursor().intValue() + "/" + token.getCompleteListSize().intValue();
      LOGGER.trace("Setting resumption token value to {}.", value);
      token.setValue(URLEncoder.encode(Base64.encodeBase64String(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
      builder.setResumptionToken(token);
    }

    return results;
  }
}
