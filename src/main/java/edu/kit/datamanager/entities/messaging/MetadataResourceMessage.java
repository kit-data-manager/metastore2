/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.entities.messaging;

import edu.kit.datamanager.metastore2.domain.AclRecord;
import edu.kit.datamanager.metastore2.domain.MetadataRecord;
import edu.kit.datamanager.metastore2.util.DataResourceRecordUtil;
import edu.kit.datamanager.repo.domain.DataResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xerces.util.URI;

/**
 * Handler for creating messages for metadata.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MetadataResourceMessage extends DataResourceMessage {

  /**
   * Holds the schema type of the metadata document.
   */
  public static final String DOCUMENT_TYPE_PROPERTY = "documentType";
  public static final String RESOLVING_URL_PROPERTY = "resolvingUrl";

  /**
   * Create Message for create event.
   *
   * @param metadataRecord record holding all properties of document
   * @param caller caller of the event
   * @param sender sender of the event.
   * @return Message for create event.
   */
  public static MetadataResourceMessage factoryCreateMetadataMessage(MetadataRecord metadataRecord, String caller, String sender) {
    return createMessage(metadataRecord, ACTION.CREATE, SUB_CATEGORY.DATA, caller, sender);
  }

  /**
   * Create Message for update event.
   *
   * @param metadataRecord record holding all properties of document
   * @param caller caller of the event
   * @param sender sender of the event.
   * @return Message for update event.
   */
  public static MetadataResourceMessage factoryUpdateMetadataMessage(MetadataRecord metadataRecord, String caller, String sender) {
    return createMessage(metadataRecord, ACTION.UPDATE, SUB_CATEGORY.DATA, caller, sender);
  }

  /**
   * Create Message for delete event.
   *
   * @param metadataRecord record holding all properties of document
   * @param caller caller of the event
   * @param sender sender of the event.
   * @return Message for delete event.
   */
  public static MetadataResourceMessage factoryDeleteMetadataMessage(MetadataRecord metadataRecord, String caller, String sender) {
    return createMessage(metadataRecord, ACTION.DELETE, SUB_CATEGORY.DATA, caller, sender);
  }

  /**
   * Create Message for create event.
   *
   * @param metadataRecord record holding all properties of document
   * @param caller caller of the event
   * @param sender sender of the event.
   * @return Message for create event.
   */
  public static MetadataResourceMessage factoryCreateMetadataMessage(DataResource metadataRecord, String caller, String sender) {
    return createMessage(metadataRecord, ACTION.CREATE, SUB_CATEGORY.DATA, caller, sender);
  }

  /**
   * Create Message for create event.
   *
   * @param metadataRecord record holding all properties of document
   * @param action message was triggered by this action
   * @param subCategory the sub category of the message
   * @param principal who triggered this message
   * @param sender sender of the event.
   * @return Message for create event.
   */
  public static MetadataResourceMessage createMessage(MetadataRecord metadataRecord, ACTION action, SUB_CATEGORY subCategory, String principal, String sender) {
    MetadataResourceMessage msg = new MetadataResourceMessage();
    Map<String, String> properties = new HashMap<>();
    if (metadataRecord != null) {
      properties.put(RESOLVING_URL_PROPERTY, removeFilterFromUri(metadataRecord.getMetadataDocumentUri()));
      properties.put(DOCUMENT_TYPE_PROPERTY, metadataRecord.getSchema().getIdentifier());
      msg.setEntityId(metadataRecord.getId());
    }
    if (action != null) {
      msg.setAction(action.getValue());
    }
    if (subCategory != null) {
      msg.setSubCategory(subCategory.getValue());
    }
    msg.setPrincipal(principal);
    msg.setSender(sender);
    msg.setMetadata(properties);
    msg.setCurrentTimestamp();
    return msg;
  }

  /**
   * Create Message for update event.
   *
   * @param metadataRecord record holding all properties of document
   * @param caller caller of the event
   * @param sender sender of the event.
   * @return Message for update event.
   */
  public static MetadataResourceMessage factoryUpdateMetadataMessage(DataResource metadataRecord, String caller, String sender) {
    return createMessage(metadataRecord, ACTION.UPDATE, SUB_CATEGORY.DATA, caller, sender);
  }

  /**
   * Create Message for delete event.
   *
   * @param metadataRecord record holding all properties of document
   * @param caller caller of the event
   * @param sender sender of the event.
   * @return Message for delete event.
   */
  public static MetadataResourceMessage factoryDeleteMetadataMessage(DataResource metadataRecord, String caller, String sender) {
    return createMessage(metadataRecord, ACTION.DELETE, SUB_CATEGORY.DATA, caller, sender);
  }

  /**
   * Create Message for create event.
   *
   * @param dataResource record holding all properties of document
   * @param action message was triggered by this action
   * @param subCategory the sub category of the message
   * @param principal who triggered this message
   * @param sender sender of the event.
   * @return Message for create event.
   */
  public static MetadataResourceMessage createMessage(DataResource dataResource, ACTION action, SUB_CATEGORY subCategory, String principal, String sender) {
    MetadataResourceMessage msg = new MetadataResourceMessage();
    Map<String, String> properties = new HashMap<>();
    if (dataResource != null) {
      String metadataDocumentUri = DataResourceRecordUtil.getMetadataDocumentUri(dataResource.getId(), dataResource.getVersion()).toString();
      String schemaDocumentUri = DataResourceRecordUtil.getSchemaIdentifier(dataResource).getValue();
      String[] split = schemaDocumentUri.split(DataResourceRecordUtil.SCHEMA_VERSION_SEPARATOR, -1);
      String schemaId = removeFilterFromUri(split[split.length - 1]);

      properties.put(RESOLVING_URL_PROPERTY, removeFilterFromUri(metadataDocumentUri));
      properties.put(DOCUMENT_TYPE_PROPERTY, schemaId);
      msg.setEntityId(dataResource.getId());
    }
    if (action != null) {
      msg.setAction(action.getValue());
    }
    if (subCategory != null) {
      msg.setSubCategory(subCategory.getValue());
    }
    msg.setPrincipal(principal);
    msg.setSender(sender);
    msg.setMetadata(properties);
    msg.setCurrentTimestamp();
    return msg;
  }
  
  /**
   * Remove version and other stuff added to the URI.
   * @param uri URI of the object.
   * @return  URI without additional parameter.
   */
  public static String removeFilterFromUri(String uri) {
    String strippedUri = null;
    if (uri != null) {
      strippedUri = uri.split("\\?", -1)[0];
    }
    return strippedUri;
  }
  @Override
  public String getEntityName() {
    return "metadata";
  }
}
