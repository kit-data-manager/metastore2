/*
 * Copyright 2021 Karlsruhe Institute of Technology.
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.kit.datacite.kernel_4.Datacite43Schema;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dona.doip.DoipConstants;
import net.dona.doip.client.DigitalObject;
import net.dona.doip.client.DoipException;
import net.dona.doip.client.Element;
import net.dona.doip.server.DoipServerRequest;
import net.dona.doip.util.GsonUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DoipUtils{
  /**
   * Label of the attribute containing datacite (in JSON format) metadata about the DO.
   */
  public static final String ATTR_DATACITE = "datacite";
  public static final String ID_SCHEMA = "schema";
  public static final String ID_METADATA = "metadata";
  public static final String ID_APPLICATION_PROFILE = "application_profile";
  /**
   * Label for the general DO type.
   */
  public static final String TYPE_DO = "0.TYPE/DO";

  private static final Logger LOGGER = LoggerFactory.getLogger(DoipUtils.class);

  public static DigitalObject ofDataResource(Datacite43Schema resource) throws DoipException{
    net.dona.doip.client.DigitalObject digitalObject = new DigitalObject();
    digitalObject.id = resource.getIdentifiers().iterator().next().getIdentifier();
    digitalObject.type = "0.TYPE/DO";
    digitalObject.attributes = new JsonObject();
    JsonElement dobjJson = GsonUtility.getGson().toJsonTree(resource);
    digitalObject.attributes.add(ATTR_DATACITE, dobjJson);
    // if (co.acl != null) digitalObject.attributes.add("acl", GsonUtility.getGson().toJsonTree(co.acl));
    // if (co.metadata != null) digitalObject.attributes.add("metadata", GsonUtility.getGson().toJsonTree(co.metadata));
//        if (co.userMetadata != null) {
//            for (Map.Entry<String, JsonElement> attribute :  co.userMetadata.entrySet()) {
//                digitalObject.attributes.add(doipAttributeOfUserMetadataKey(attribute.getKey()), attribute.getValue());
//            }
//        }
//    digitalObject.elements = new ArrayList<>();
//    if(content != null){
//      for(ContentInformation payload : content){
//        Element el = new Element();
//        el.id = payload.getRelativePath();
//        if(payload.getSize() >= 0){
//          el.length = payload.getSize();
//        }
//        el.type = payload.getMediaType();
//        el.attributes = new JsonObject();
//        if(payload.getFilename() != null){
//          el.attributes.addProperty("filename", payload.getFilename());
//        }
//        if(includeInputStreams){
//          if(payload.getContentStream() != null){
//            el.in = payload.getContentStream();
//          } else{
//            @SuppressWarnings("resource")
//            URI contentUri = URI.create(payload.getContentUri());
//            try{
//              InputStream in = contentUri.toURL().openStream();
//              if(in != null){
//                el.in = in;
//              }
//            } catch(IOException ex){
//              LOGGER.error("Failed to obtain input stream for content URI " + payload.getContentUri() + ".", ex);
//              throw new DoipException(DoipConstants.STATUS_ERROR, "Unable to include input streams.");
//            }
//          }
//        }
//        digitalObject.elements.add(el);
//      }
//    }
    return digitalObject;
  }

  public static Datacite43Schema toDatacite(DigitalObject digitalObject, boolean includeInputStreams){
    JsonElement metadata = digitalObject.attributes.get(ATTR_DATACITE);
    Datacite43Schema resource = GsonUtility.getGson().fromJson(metadata, Datacite43Schema.class);

//    List<ContentInformation> contentInformation = new ArrayList<>();
//    if(digitalObject.elements != null && digitalObject.elements.size() > 0){
//      for(Element el : digitalObject.elements){
//        ContentInformation contentInfo = new ContentInformation();
//        contentInfo.setRelativePath(el.id);
//        contentInfo.setMediaType(el.type);
//        if(el.length != null){
//          contentInfo.setSize(el.length);
//        }
//        if(includeInputStreams){
//          if(el.in != null){
//            contentInfo.setContentStream(el.in);
//          }
//        }
//        contentInformation.add(contentInfo);
//      }
//    }
//
//    resource.setAssociatedContentInformation(contentInformation);
    return resource;
  }

  public static boolean getBooleanAttributeFromRequest(DoipServerRequest req, String att){
    JsonElement el = req.getAttribute(att);
    if(el == null || !el.isJsonPrimitive()){
      return false;
    }

    JsonPrimitive priv = el.getAsJsonPrimitive();
    if(priv.isBoolean()){
      return priv.getAsBoolean();
    }
    if(priv.isString()){
      return "true".equalsIgnoreCase(priv.getAsString());
    }
    return false;
  }

//  public static List<SortField> getSortFieldsFromString(String sortFields){
//    if(sortFields == null || "".equals(sortFields)){
//      return null;
//    } else{
//      List<SortField> result = new ArrayList<>();
//      List<String> sortFieldStrings = Arrays.asList(sortFields.split(","));
//      sortFieldStrings.forEach((sortFieldString) -> {
//        result.add(getSortFieldFromString(sortFieldString));
//      });
//      return result;
//    }
//  }
//
//  public static SortField getSortFieldFromString(String sortFieldString){
//    String[] terms = sortFieldString.split(" ");
//    SortField.DIR dir = SortField.DIR.ASC;
//
//    if(terms.length > 1){
//      String direction = terms[1];
//      if("DESC".equalsIgnoreCase(direction)){
//        dir = SortField.DIR.DESC;
//      }
//    }
//    String fieldName = terms[0];
//    return new SortField(fieldName, dir);
//  }

//    public static final String DOIP_PRIVATE_KEY_FILE = "doipPrivateKey";
//    public static final String DOIP_PUBLIC_KEY_FILE = "doipPublicKey";
//    public static final String DOIP_CERTIFICATE_FILE = "doipCertificate.pem";
//
//    private PrivateKey doipPrivateKey;
//    private PublicKey doipPublicKey;
//    private X509Certificate[] doipCertificateChain;
//    private void initializeKeysAndCertChain(PrivateKey privateKey) throws Exception {
//        doipPrivateKey = privateKey;
//        doipPublicKey = (PublicKey) servletContext.getAttribute("net.cnri.cordra.startup.publicKey");
//        String cordraDataString = System.getProperty(Constants.CORDRA_DATA);
//        if (cordraDataString == null) throw new Exception("cordra.data is null");
//        Path cordraDataPath = Paths.get(cordraDataString);
//        File doipPrivKeyFile = new File(cordraDataPath.toFile(), DOIP_PRIVATE_KEY_FILE);
//        if (doipPrivKeyFile.exists()) {
//            doipPrivateKey = Util.getPrivateKeyFromFileWithPassphrase(doipPrivKeyFile, null);
//            File doipPubKeyFile = new File(cordraDataPath.toFile(), DOIP_PUBLIC_KEY_FILE);
//            if (doipPubKeyFile.exists()) {
//                doipPublicKey = Util.getPublicKeyFromFile(doipPubKeyFile.getAbsolutePath());
//            }
//            File doipCertFile = new File(cordraDataPath.toFile(), DOIP_CERTIFICATE_FILE);
//            if (doipCertFile.exists()) {
//                doipCertificateChain = readCertChainFromFile(doipCertFile);
//            }
//        }
//    }
//
//    public PublicKey getPublicKey() {
//        return doipPublicKey;
//    }
//
//    public PrivateKey getPrivateKey() {
//        return doipPrivateKey;
//    }
//
//    public X509Certificate[] getCertChain() {
//        return doipCertificateChain;
//    }
//
//    private static X509Certificate[] readCertChainFromFile(File certFile) throws Exception {
//        CertificateFactory cf = CertificateFactory.getInstance("X.509");
//        try (FileInputStream fis = new FileInputStream(certFile)) {
//            return cf.generateCertificates(fis).stream().toArray(X509Certificate[]::new);
//        }
//    }
//
//    public void createAndSaveKeysIfNecessary() {
//        if (doipPrivateKey == null && doipPublicKey == null) {
//            LOGGER.info("No handle keys found; minting new keypair");
//        } else {
//            return;
//        }
//        try {
//            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//            kpg.initialize(2048);
//            KeyPair keys = kpg.generateKeyPair();
//            doipPublicKey = keys.getPublic();
//            doipPrivateKey = keys.getPrivate();
//            String cordraDataString = System.getProperty(Constants.CORDRA_DATA);
//            if (cordraDataString == null) throw new Exception("cordra.data is null");
//            Path cordraDataPath = Paths.get(cordraDataString);
//            byte[] privateKeyBytes = Util.encrypt(Util.getBytesFromPrivateKey(doipPrivateKey), null, Common.ENCRYPT_NONE);
//            Path privateKeyPath = cordraDataPath.resolve(DOIP_PRIVATE_KEY_FILE);
//            Files.write(privateKeyPath, privateKeyBytes, StandardOpenOption.CREATE_NEW);
//            byte[] publicKeyBytes = Util.getBytesFromPublicKey(doipPublicKey);
//            Path publicKeyPath = cordraDataPath.resolve(DOIP_PUBLIC_KEY_FILE);
//            Files.write(publicKeyPath, publicKeyBytes, StandardOpenOption.CREATE_NEW);
//        } catch (Exception e) {
//            LOGGER.error("Unable to store newly-minted DOIP keys", e);
//            System.out.println("Unable to store newly-minted DOIP keys (see error.log for details)");
//        }
//    }
}
