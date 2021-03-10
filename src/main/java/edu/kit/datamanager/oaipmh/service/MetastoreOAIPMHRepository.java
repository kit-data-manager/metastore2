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
package edu.kit.datamanager.oaipmh.service;

import edu.kit.datamanager.entities.repo.DataResource;
import edu.kit.datamanager.metastore2.configuration.ApplicationProperties;
import edu.kit.datamanager.metastore2.dao.IMetadataRecordDao;
import edu.kit.datamanager.metastore2.dao.IMetadataSchemaDao;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.oaipmh.configuration.OaiPmhConfiguration;
import edu.kit.datamanager.oaipmh.util.OAIPMHBuilder;
import edu.kit.datamanager.util.xml.DataCiteMapper;
import edu.kit.datamanager.util.xml.DublinCoreMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.datacite.schema.kernel_4.Resource;
import org.openarchives.oai._2.DeletedRecordType;
import org.openarchives.oai._2.DescriptionType;
import org.openarchives.oai._2.GranularityType;
import org.openarchives.oai._2.MetadataFormatType;
import org.openarchives.oai._2.OAIPMHerrorcodeType;
import org.openarchives.oai._2.ResumptionTokenType;
import org.purl.dc.elements._1.ElementContainer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
public class MetastoreOAIPMHRepository extends AbstractOAIPMHRepository{

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MetastoreOAIPMHRepository.class);

  private MetadataFormatType DC_SCHEMA;
  private MetadataFormatType DATACITE_SCHEMA;

  private OaiPmhConfiguration pluginConfiguration;

  @Autowired
  private ApplicationProperties metastoreProperties;
  @Autowired
  private IMetadataRecordDao metadataRecordDao;
  @Autowired
  private IMetadataSchemaDao metadataSchemaDao;
  @Autowired
  private OaiPmhConfiguration oaiConfigProperties;

  /**
   * Default constructor.
   */
  @Autowired
  public MetastoreOAIPMHRepository(OaiPmhConfiguration pluginConfiguration){
    this("MetastoreRepository", pluginConfiguration);
  }

  /**
   * Default constructor.
   *
   * @param name The repository name.
   */
  private MetastoreOAIPMHRepository(String name, OaiPmhConfiguration pluginConfiguration){
    super(name);
    this.pluginConfiguration = pluginConfiguration;
  }

  @Override
  public List<DescriptionType> getRepositoryDescription(){
    return new ArrayList<>();
  }

  @Override
  public DeletedRecordType getDeletedRecordSupport(){
    return DeletedRecordType.NO;
  }

  @Override
  public List<String> getAdminEmail(){
    return Arrays.asList(pluginConfiguration.getAdminEmail());
  }

  @Override
  public String getEarliestDatestamp(){
    return getDateFormat().format(new Date(0l));
  }

  @Override
  public GranularityType getGranularity(){
    return GranularityType.YYYY_MM_DD_THH_MM_SS_Z;
  }

  @Override
  public String getBaseUrl(){
    return ServletUriComponentsBuilder.fromCurrentRequest().toUriString();
  }

  @Override
  public boolean isPrefixSupported(String prefix){
      boolean exists = false;
      List<MetadataSchemaRecord> findAll = metadataSchemaDao.findAll();
    for (MetadataSchemaRecord item: findAll) {
        System.out.println(".");
        if (prefix.equalsIgnoreCase(item.getSchemaId())) {
            exists = true;
            break;
        }
    }
    return DC_SCHEMA.getMetadataPrefix().equals(prefix) || DATACITE_SCHEMA.getMetadataPrefix().equals(prefix);
  }

  @Override
  public void identify(OAIPMHBuilder builder){
    LOGGER.trace("Performing identify().");
    //should already been handled in builder...modification of response possible here
  }

  @Override
  public void listSets(OAIPMHBuilder builder){
    LOGGER.trace("Performing listSets().");
    //@TODO support collections?
    //builder.addSet("default", "default");
    builder.addError(OAIPMHerrorcodeType.NO_SET_HIERARCHY, "Sets are currently not supported.");
  }

  @Override
  public void listMetadataFormats(OAIPMHBuilder builder){
    LOGGER.trace("Performing listMetadataFormats().");
    //@TODO extend by other formats
    builder.addMetadataFormat(DC_SCHEMA);
    builder.addMetadataFormat(DATACITE_SCHEMA);
  }

  @Override
  public void listIdentifiers(OAIPMHBuilder builder){
    LOGGER.trace("Performing listIdentifiers().");
    List<DataResource> results = getEntities(builder);
    if(results.isEmpty()){
      if(!builder.isError()){
        LOGGER.error("No results obtained. Returning OAI-PMH error NO_RECORDS_MATCH.");
        builder.addError(OAIPMHerrorcodeType.NO_RECORDS_MATCH, null);
      }
      return;
    }

    LOGGER.trace("Adding {} records to result.", results.size());
    results.stream().forEach((result) -> {
      //TODO get proper date
      Date changeDate = new Date(0l);
      if(result.getLastUpdate() != null){
        changeDate = Date.from(result.getLastUpdate());
      }

      builder.addRecord(result.getId(), changeDate, Arrays.asList("default"));
    });
  }

  @Override
  public void getRecord(OAIPMHBuilder builder){
    //only get record entries for prefix
    LOGGER.trace("Performing getRecord().");
    DataResource resource = getEntity(builder);

    if(resource != null){
      LOGGER.trace("Adding single record to result.");
      addRecordEntry(resource, builder);
    } else{
      LOGGER.error("No result obtained. Returning OAI-PMH error ID_DOES_NOT_EXIST.");
      builder.addError(OAIPMHerrorcodeType.ID_DOES_NOT_EXIST, "No object for identifier " + builder.getIdentifier() + " found.");
    }

  }

  @Override
  public void listRecords(OAIPMHBuilder builder){
    LOGGER.trace("Performing listRecords().");
    List<DataResource> results = getEntities(builder);
    if(results.isEmpty()){
      if(!builder.isError()){
        LOGGER.error("No results obtained. Returning OAI-PMH error NO_RECORDS_MATCH.");
        builder.addError(OAIPMHerrorcodeType.NO_RECORDS_MATCH, null);
      }
      return;
    }
    LOGGER.trace("Adding {} records to result.", results.size());
    results.stream().forEach((result) -> {
      addRecordEntry(result, builder);
    });
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
  private Document getMetadataDocument(DataResource object, String schemaId){
    LOGGER.trace("Obtaining metadata document for schema {} and resource identifier {}", schemaId, object.getId());
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    boolean wasError = false;
    if(DC_SCHEMA.getMetadataPrefix().equals(schemaId)){
      //create DC document on the fly
      LOGGER.info("Creating Dublin Core document on the fly.", object.getId());
      //create DC metadata
      try{
        ElementContainer container = DublinCoreMapper.dataResourceToDublinCoreContainer(object);
        JAXBContext jaxbContext = JAXBContext.newInstance(ElementContainer.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(container, bout);
      } catch(JAXBException ex){
        LOGGER.error("Failed to build Dublin Core document.", ex);
        wasError = true;
      }
    } else if(DATACITE_SCHEMA.getMetadataPrefix().equals(schemaId)){
      LOGGER.info("Creating Datacite document on the fly.", object.getId());
      try{
        Resource resource = DataCiteMapper.dataResourceToDataciteResource(object);
        JAXBContext jaxbContext = JAXBContext.newInstance(Resource.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(resource, bout);
      } catch(JAXBException ex){
        LOGGER.error("Failed to build Datacite document.", ex);
        wasError = true;
      }
    }

    Document doc = null;
    if(!wasError){
      try{
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        DocumentBuilder docBuilder = fac.newDocumentBuilder();
        doc = docBuilder.parse(new ByteArrayInputStream(bout.toByteArray()));
      } catch(SAXException | IOException | ParserConfigurationException ex){
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
  private void addRecordEntry(DataResource result, OAIPMHBuilder builder){
    LOGGER.trace("Adding record for object identifier {} to response.", result.getId());
    Document doc = getMetadataDocument(result, builder.getMetadataPrefix());
    if(doc != null){
      LOGGER.trace("Adding record using obtained metadata document.");
      Date resourceDate = new Date(0l);
      if(result.getLastUpdate() != null){
        resourceDate = Date.from(result.getLastUpdate());
      }
      builder.addRecord(result.getId(), resourceDate, Arrays.asList("default"), doc.getDocumentElement());
    } else{
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
  private DataResource getEntity(OAIPMHBuilder builder){
    LOGGER.trace("Performing getEntity().");

    RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    HttpEntity<String> entity = new HttpEntity<>(headers);

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.
            fromHttpUrl(pluginConfiguration.getRepositoryBaseUrl() + builder.getIdentifier());

    LOGGER.trace("Sending GET request to URI {}.", uriBuilder.toUriString());

    //get all metadata resources
    ResponseEntity<DataResource> restResponse = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, entity, DataResource.class);
    LOGGER.trace("Returning response body.");
    return restResponse.getBody();

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
  private List<DataResource> getEntities(OAIPMHBuilder builder){
    List<DataResource> results = new ArrayList<>();

    String prefix = builder.getMetadataPrefix();
    LOGGER.trace("Getting entities for metadata prefix {} from repository.", prefix);
    LOGGER.trace("Checking request for resumption token");
    String resumptionToken = builder.getResumptionToken();
    int currentCursor = 0;
    int overallCount;
    //check resumption token

    if(resumptionToken != null){
      try{
        String tokenValue = new String(Base64.decodeBase64(URLDecoder.decode(resumptionToken, "UTF-8")));
        LOGGER.trace("Found token with value {}", tokenValue);
        String[] elements = tokenValue.split("/");
        if(elements.length != 2){
          LOGGER.error("Invalid resumption token. Returning OAI-PMH error BAD_RESUMPTION_TOKEN.");
          builder.addError(OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, null);
          return new ArrayList<>();
        }
        try{
          LOGGER.trace("Parsing token values.");
          currentCursor = Integer.parseInt(elements[0]);
          overallCount = Integer.parseInt(elements[1]);
          LOGGER.trace("Obtained {} as current cursor from token. Overall element count is {}.", currentCursor, overallCount);
        } catch(NumberFormatException ex){
          //log error
          builder.addError(OAIPMHerrorcodeType.BAD_RESUMPTION_TOKEN, null);
          return new ArrayList<>();
        }
      } catch(UnsupportedEncodingException ex){
        LOGGER.error("Failed to get results from repository. Returning empty list.", ex);
      }
    } else{
      LOGGER.trace("No resumption token found.");
    }

    RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    HttpEntity<String> entity = new HttpEntity<>(headers);

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.
            fromHttpUrl(pluginConfiguration.getRepositoryBaseUrl()).
            queryParam("from", (builder.getFromDate() == null) ? new Date(0l).toInstant() : builder.getFromDate().toInstant());
    if(builder.getUntilDate() != null){
      uriBuilder = uriBuilder.queryParam("until", builder.getUntilDate().toInstant());
    }
    int maxElementsPerList = pluginConfiguration.getMaxElementsPerList();

    int page = currentCursor / maxElementsPerList;
    uriBuilder = uriBuilder.queryParam("page", page).queryParam("size", maxElementsPerList);

    //get all metadata resources
    ResponseEntity<DataResource[]> restResponse = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, entity, DataResource[].class);

    List<String> contentRange = restResponse.getHeaders().get("Content-Range");
    if(contentRange != null && contentRange.size() >= 1){
      overallCount = Integer.parseInt(contentRange.get(0).substring(contentRange.get(0).indexOf("/") + 1));
    } else{
      overallCount = currentCursor + maxElementsPerList;
      LOGGER.warn("No Content-Range header found. Unable to determine resource count. Using current element with value {}.", overallCount);
    }

    List<DataResource> res = Arrays.asList(restResponse.getBody());
    LOGGER.trace("Setting next resumption token.");
    if(currentCursor + maxElementsPerList > overallCount){
      LOGGER.debug("New cursor {} exceeds element count {}, no more elements available. Setting resumption token to 'null'.", (currentCursor + maxElementsPerList), overallCount);
      //lsit complete, add no resumptiontoken
      builder.setResumptionToken(null);
    } else{
      ResumptionTokenType token = new ResumptionTokenType();
      //set list size
      token.setCompleteListSize(BigInteger.valueOf(overallCount));
      //set current cursor
      token.setCursor(BigInteger.valueOf(currentCursor + res.size()));
      LOGGER.trace("Setting new resumption token with cursor at position {}.", token.getCursor());
      //we set no expiration as the token never expires
      String value = token.getCursor().intValue() + "/" + token.getCompleteListSize().intValue();
      LOGGER.trace("Setting resumption token value to {}.", value);
      try{
        token.setValue(URLEncoder.encode(Base64.encodeBase64String(value.getBytes()), "UTF-8"));
        builder.setResumptionToken(token);
      } catch(UnsupportedEncodingException ex){
        LOGGER.error("Failed to get results from repository. Returning empty list.", ex);
      }
    }

    return res;
  }
}
