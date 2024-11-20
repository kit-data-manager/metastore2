/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.validation.impl;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.validation.IValidator;
import org.apache.xerces.impl.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class for validating XML files.
 */
@Component
public class XmlValidator implements IValidator {

  private static final Logger LOG = LoggerFactory.getLogger(XmlValidator.class);

  private String errorMessage;

  @Override
  public boolean supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE type) {
    return MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(type);
  }

  @Override
  public boolean supportsMimetype(String type) {
    return MediaType.APPLICATION_XML_VALUE.equals(type);
  }

  @Override
  public IValidator getInstance() {
    return new XmlValidator();
  }

  @Override
  public boolean isSchemaValid(InputStream schemaStream) {
    boolean result = false;
    LOG.trace("Checking schema for validity.");
    try {
      SAXParser saxParser = getSaxParser();
      DefaultHandler errorHandler = new DefaultHandler();
      saxParser.parse(schemaStream, errorHandler);

      LOG.trace("Schema seems to be valid.");
      result = true;
    } catch (ParserConfigurationException | SAXException | IOException e) {
      LOG.error("Failed to validate schema.", e);
      errorMessage = "Validation error: " + e.getMessage();
    }
    return result;
  }

  @Override
  public boolean validateMetadataDocument(File schemaFile, InputStream metadataDocumentStream) {
    boolean valid = false;
    LOG.trace("Checking metdata document using schema at {}.", schemaFile);
    LOG.trace("Reading metadata document from stream.");
    try {
      SchemaFactory schemaFactory = getSchemaFactory();

      LOG.trace("Creating schema instance.");
      Schema schema;
      schema = schemaFactory.newSchema(schemaFile);

      LOG.trace("Obtaining validator.");
      Validator validator = schema.newValidator();

      LOG.trace("Validating metadata file.");
      Source xmlFile = new StreamSource(metadataDocumentStream);
      validator.validate(xmlFile);

      LOG.trace("Metadata document is valid according to schema.");
      valid = true;
    } catch (SAXException | IOException e) {
      LOG.error("Failed to validate metadata document.", e);
      errorMessage = "Validation error: " + e.getMessage();
    }
    return valid;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Get schema factory with disabled DTD parsing due to XXE vulnerabilty.
   *
   * @return schema factory
   */
  private SchemaFactory getSchemaFactory() {
    SchemaFactory schemaFactory;
    schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    return schemaFactory;
  }

  private SAXParser getSaxParser() throws ParserConfigurationException, SAXException {
    SAXParser parser;

    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setValidating(true);
    spf.setNamespaceAware(true);
    spf.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE, true);
    spf.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE, true);
    spf.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE, false);
    spf.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
    spf.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
    spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    parser = spf.newSAXParser();

    return parser;
  }
}
