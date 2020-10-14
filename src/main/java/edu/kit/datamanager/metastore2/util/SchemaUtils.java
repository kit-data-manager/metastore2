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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jejkal
 */
public class SchemaUtils {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaUtils.class);
  
  private static final int MAX_LENGTH_OF_HEADER = 100;

  private static final Pattern JSON_FIRST_BYTE = Pattern.compile("(\\R\\s)*\\s*\\{\\s*\"\\$(.|\\s)*");//^\\s{\\s*\".*");
  private static final Pattern XML_FIRST_BYTE = Pattern.compile("((.|\\s)*<\\?xml[^<]*)?\\s*<\\s*(\\w{2,3}:)?schema(.|\\s)*", Pattern.MULTILINE);

  public static MetadataSchemaRecord.SCHEMA_TYPE guessType(byte[] schema) {
    // Cut schema to a maximum of MAX_LENGTH_OF_HEADER characters.
    int length = schema.length > MAX_LENGTH_OF_HEADER?MAX_LENGTH_OF_HEADER:schema.length;
    String schemaAsString = new String(schema, 0, length);
    LOG.trace("Guess type for '{}'",schemaAsString);

    Matcher m = JSON_FIRST_BYTE.matcher(schemaAsString);
    if (m.matches()) {
      return MetadataSchemaRecord.SCHEMA_TYPE.JSON;
    } else {
      m = XML_FIRST_BYTE.matcher(schemaAsString);
      if (m.matches()) {
        return MetadataSchemaRecord.SCHEMA_TYPE.XML;
      }
    }
    return null;
  }
}
