
package edu.kit.datamanager.metastore2.domain;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResourceIdentifier {

    /**
     * 
     * (Required)
     * 
     */
    @Expose
    private String identifier;
    /**
     * 
     * (Required)
     * 
     */
    @Expose
    private IdentifierType identifierType;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ResourceIdentifier.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("resourceIdentifier");
        sb.append('=');
        sb.append(((this.getIdentifier() == null)?"<null>":this.getIdentifier()));
        sb.append(',');
        sb.append("resourceIdentifierType");
        sb.append('=');
        sb.append(((this.getIdentifierType() == null)?"<null>":this.getIdentifierType()));
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.getIdentifier() == null)? 0 :this.getIdentifier().hashCode()));
        result = ((result* 31)+((this.getIdentifierType() == null)? 0 :this.getIdentifierType().hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ResourceIdentifier) == false) {
            return false;
        }
        ResourceIdentifier rhs = ((ResourceIdentifier) other);
        return ((((this.getIdentifier() == rhs.getIdentifier())||((this.getIdentifier()!= null)&&this.getIdentifier().equals(rhs.getIdentifier()))))&&((this.getIdentifierType() == rhs.getIdentifierType())||((this.getIdentifierType()!= null)&&this.getIdentifierType().equals(rhs.getIdentifierType()))));
    }

    public enum IdentifierType {

        @SerializedName("ARK")
        ARK("ARK"),
        @SerializedName("arXiv")
        AR_XIV("arXiv"),
        @SerializedName("bibcode")
        BIBCODE("bibcode"),
        @SerializedName("DOI")
        DOI("DOI"),
        @SerializedName("EAN13")
        EAN_13("EAN13"),
        @SerializedName("EISSN")
        EISSN("EISSN"),
        @SerializedName("Handle")
        HANDLE("Handle"),
        @SerializedName("IGSN")
        IGSN("IGSN"),
        @SerializedName("ISBN")
        ISBN("ISBN"),
        @SerializedName("ISSN")
        ISSN("ISSN"),
        @SerializedName("ISTC")
        ISTC("ISTC"),
        @SerializedName("LISSN")
        LISSN("LISSN"),
        @SerializedName("LSID")
        LSID("LSID"),
        @SerializedName("PMID")
        PMID("PMID"),
        @SerializedName("PURL")
        PURL("PURL"),
        @SerializedName("UPC")
        UPC("UPC"),
        @SerializedName("URL")
        URL("URL"),
        @SerializedName("URN")
        URN("URN"),
        @SerializedName("w3id")
        W_3_ID("w3id");
        private final String value;
        private final static Map<String, ResourceIdentifier.IdentifierType> CONSTANTS = new HashMap<String, ResourceIdentifier.IdentifierType>();

        static {
            for (ResourceIdentifier.IdentifierType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        IdentifierType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static ResourceIdentifier.IdentifierType fromValue(String value) {
            ResourceIdentifier.IdentifierType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

  /**
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @param identifier the identifier to set
   */
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  /**
   * @return the identifierType
   */
  public IdentifierType getIdentifierType() {
    return identifierType;
  }

  /**
   * @param identifierType the identifierType to set
   */
  public void setIdentifierType(IdentifierType identifierType) {
    this.identifierType = identifierType;
  }
  public static ResourceIdentifier factoryUrlResourceIdentifier(String identifier) {
    ResourceIdentifier resourceIdentifier = new ResourceIdentifier();
    resourceIdentifier.setIdentifier(identifier);
    resourceIdentifier.setIdentifierType(ResourceIdentifier.IdentifierType.URL);
    return resourceIdentifier;
  }
}
