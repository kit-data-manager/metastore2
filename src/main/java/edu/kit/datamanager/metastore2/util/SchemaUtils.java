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
package edu.kit.datamanager.metastore2.util;

import edu.kit.datamanager.metastore2.domain.MetadataSchemaRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for (XML) schema documents.
 *
 * @author jejkal
 */
public class SchemaUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaUtils.class);

  private static final int MAX_LENGTH_OF_HEADER = 100;

  private static final Pattern JSON_FIRST_BYTE = Pattern.compile("(\\R\\s)*\\s*\\{\\s*\"\\$(.|\\s)*");//
  private static final Pattern XML_FIRST_BYTE = Pattern.compile("((.|\\s)*<\\?xml[^<]*)?\\s*<\\s*(\\w{2,3}:)?schema(.|\\s)*", Pattern.MULTILINE);

  SchemaUtils() {
    //Utility class
  }

  /**
   * Guess type of schema document.
   *
   * @param schema schema document.
   * @return Schema type of document.
   */
  public static MetadataSchemaRecord.SCHEMA_TYPE guessType(byte[] schema) {
    // Cut schema to a maximum of MAX_LENGTH_OF_HEADER characters.
    if (schema != null) {
      int length = schema.length > MAX_LENGTH_OF_HEADER ? MAX_LENGTH_OF_HEADER : schema.length;
      String schemaAsString = new String(schema, 0, length, StandardCharsets.UTF_8);
      LOG.trace("Guess type for '{}'", schemaAsString);

      Matcher m = JSON_FIRST_BYTE.matcher(schemaAsString);
      if (schemaAsString.contains("{")) {
        if (m.matches()) {
          return MetadataSchemaRecord.SCHEMA_TYPE.JSON;
        }
      } else {
        if (schemaAsString.contains("<")) {
          m = XML_FIRST_BYTE.matcher(schemaAsString);
          if (m.matches()) {
            return MetadataSchemaRecord.SCHEMA_TYPE.XML;
          }
        }
      }
    }
    return null;
  }

  /**
   * Guess type of schema document.
   *
   * @param schema schema document.
   * @return Mimetype of document.
   */
  public static String guessMimetype(byte[] schema) {
    // Cut schema to a maximum of MAX_LENGTH_OF_HEADER characters.
    if (schema != null) {
      int length = schema.length > MAX_LENGTH_OF_HEADER ? MAX_LENGTH_OF_HEADER : schema.length;
      String schemaAsString = new String(schema, 0, length, StandardCharsets.UTF_8);
      LOG.trace("Guess type for '{}'", schemaAsString);

      Matcher m = JSON_FIRST_BYTE.matcher(schemaAsString);
      if (schemaAsString.contains("{") && m.matches()) {
          return MediaType.APPLICATION_JSON_VALUE;
      } else {
        if (schemaAsString.contains("<")) {
          m = XML_FIRST_BYTE.matcher(schemaAsString);
          if (m.matches()) {
            return MediaType.APPLICATION_XML_VALUE;
          }
        }
      }
    }
    return null;
  }

  /**
   * Determine target namespace from schema.
   * @param schema Schema document.
   * @return Namespace.
   */    
   public static String getTargetNamespaceFromSchema(byte[] schema) {
    String namespace = null;
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      // Disable DTD due to XXE vulnerabiltiy
      documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.parse(new ByteArrayInputStream(schema));
      NamedNodeMap map = document.getDocumentElement().getAttributes();
      namespace = map.getNamedItem("targetNamespace").getNodeValue();
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      java.util.logging.Logger.getLogger(SchemaUtils.class.getName()).log(Level.SEVERE, null, ex);
    }
    return namespace;

  }
}
