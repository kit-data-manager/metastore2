/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.metastore2.validation.impl;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import edu.kit.datamanager.metastore2.validation.IValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * Class for validating XML files. 
 */
@Component
public class XmlValidator implements IValidator{

  private static final Logger LOG = LoggerFactory.getLogger(XmlValidator.class);
  
  private String errorMessage;

  @Override
  public boolean supportsSchemaType(MetadataSchemaRecord.SCHEMA_TYPE type){
    return MetadataSchemaRecord.SCHEMA_TYPE.XML.equals(type);
  }

  @Override
  public boolean isSchemaValid(InputStream schemaStream){
    LOG.trace("Checking schema for validity.");
    boolean result = false;
    Source schemaSource = new StreamSource(schemaStream);
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    try{
      LOG.trace("Creating schema instance.");
      Schema schema = schemaFactory.newSchema(schemaSource);
      LOG.trace("Obtaining validator.");
      schema.newValidator();
      LOG.trace("Schema seems to be valid.");
      result = true;
    } catch(SAXException e){
      LOG.trace("Failed to validate schema.", e);
    }
    return result;
  }

  @Override
  public boolean validateMetadataDocument(File schemaFile, InputStream metadataDocumentStream){
    LOG.trace("Checking metdata document using schema at {}.", schemaFile);
    boolean valid = false;
    LOG.trace("Reading metadata document from stream.");
    Source xmlFile = new StreamSource(metadataDocumentStream);
    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    try{
      LOG.trace("Creating schema instance.");
      Schema schema = schemaFactory.newSchema(schemaFile);
      LOG.trace("Obtaining validator.");
      Validator validator = schema.newValidator();
      LOG.trace("Validating metadata file.");
      validator.validate(xmlFile);
      LOG.trace("Metadata document is valid according to schema.");
      valid = true;
    } catch(SAXException | IOException e){
      LOG.trace("Failed to validate metadata document.", e);
      errorMessage = new String("Validation error: " + e.getMessage());
    }
    return valid;
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }
}
