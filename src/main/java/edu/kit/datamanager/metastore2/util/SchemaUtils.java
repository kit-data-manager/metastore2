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

/**
 *
 * @author jejkal
 */
public class SchemaUtils{

  private static final Pattern JSON_FIRST_BYTE = Pattern.compile("[{\\[].*");
  private static final Pattern XML_FIRST_BYTE = Pattern.compile("[\\<schema](.|\\s)*");

  public static MetadataSchemaRecord.SCHEMA_TYPE guessType(byte[] schema){
    Matcher m = JSON_FIRST_BYTE.matcher(new String(schema));
    if(m.matches()){
      return MetadataSchemaRecord.SCHEMA_TYPE.JSON;
    } else{
      m = XML_FIRST_BYTE.matcher(new String(schema));
      if(m.matches()){
        return MetadataSchemaRecord.SCHEMA_TYPE.XML;
      }
    }
    return null;
  }
}
