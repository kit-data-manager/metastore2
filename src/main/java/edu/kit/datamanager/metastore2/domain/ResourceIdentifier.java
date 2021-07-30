package edu.kit.datamanager.metastore2.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ResourceIdentifier implements Serializable {

    @Id
    @NotBlank(message = "The unqiue identifier of the resource identifier.")
    private Long id;

    @NotBlank(message = "A globally unique identifier pointing to this record, e.g. DOI, Handle, PURL.")
    private String identifier;

    @NotBlank(message = "The type of the unique identifier, e.g. DOI, Handle, PURL,...,internal.")
    private IdentifierType identifierType;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ResourceIdentifier.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("resourceIdentifier");
        sb.append('=');
        sb.append(((this.getIdentifier() == null) ? "<null>" : this.getIdentifier()));
        sb.append(',');
        sb.append("resourceIdentifierType");
        sb.append('=');
        sb.append(((this.getIdentifierType() == null) ? "<null>" : this.getIdentifierType()));
        sb.append(']');
 
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.getIdentifier() == null) ? 0 : this.getIdentifier().hashCode()));
        result = ((result * 31) + ((this.getIdentifierType() == null) ? 0 : this.getIdentifierType().hashCode()));
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
        return ((((this.getIdentifier() == rhs.getIdentifier()) || ((this.getIdentifier() != null) && this.getIdentifier().equals(rhs.getIdentifier())))) && ((this.getIdentifierType() == rhs.getIdentifierType()) || ((this.getIdentifierType() != null) && this.getIdentifierType().equals(rhs.getIdentifierType()))));
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
        W_3_ID("w3id"),
        // additional type for internal identifiers (not part of the official datacite schema)
        @SerializedName("INTERNAL")
        INTERNAL("INTERNAL");
        private final String value;
        private final static Map<String, ResourceIdentifier.IdentifierType> CONSTANTS = new HashMap<String, ResourceIdentifier.IdentifierType>();

        static {
            for (ResourceIdentifier.IdentifierType c : values()) {
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
        return factoryResourceIdentifier(identifier, ResourceIdentifier.IdentifierType.URL);
    }

    public static ResourceIdentifier factoryInternalResourceIdentifier(String identifier) {
        return factoryResourceIdentifier(identifier, ResourceIdentifier.IdentifierType.INTERNAL);
    }

    public static ResourceIdentifier factoryResourceIdentifier(String identifier, ResourceIdentifier.IdentifierType type) {
        ResourceIdentifier resourceIdentifier = new ResourceIdentifier();
        resourceIdentifier.setIdentifier(identifier);
        resourceIdentifier.setIdentifierType(type);
        return resourceIdentifier;
    }
}
