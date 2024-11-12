package edu.kit.datamanager.metastore2.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    return ResourceIdentifier.class.getName() + '@' + Integer.toHexString(System.identityHashCode(this)) + '[' +
            "resourceIdentifier" +
            '=' +
            ((this.getIdentifier() == null) ? "<null>" : this.getIdentifier()) +
            ',' +
            "resourceIdentifierType" +
            '=' +
            ((this.getIdentifierType() == null) ? "<null>" : this.getIdentifierType()) +
            ']';
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
    boolean returnValue = true;
    if (other != this) {
      returnValue = false;
      if (other instanceof ResourceIdentifier rhs) {
        // check for id
        if ((((this.getId() == null) && (rhs.getId() == null))
                || ((this.getId() != null) && this.getId().equals(rhs.getId())))
                //check for identifier
                && (((this.getIdentifier() == null) && (rhs.getIdentifier() == null))
                || ((this.getIdentifier() != null) && this.getIdentifier().equals(rhs.getIdentifier())))
                // check for identifierType
                && (((this.getIdentifierType() == null) && (rhs.getIdentifierType() == null))
                || ((this.getIdentifierType() != null) && this.getIdentifierType().equals(rhs.getIdentifierType())))) {
          returnValue = true;
        }
      }
    }
    return returnValue;
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
    private static final Map<String, ResourceIdentifier.IdentifierType> CONSTANTS = new HashMap<>();

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

    /**
     * Return identifier value as string.
     *
     * @return identifier value as string.
     */
    public String value() {
      return this.value;
    }

    /**
     * Get Identifier from value
     *
     * @param value Value as string.
     * @return Identifier
     */
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
   * Get identifier.
   *
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Set identifier.
   *
   * @param identifier the identifier to set
   */
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  /**
   * Get identifier type.
   *
   * @return the identifierType
   */
  public IdentifierType getIdentifierType() {
    return identifierType;
  }

  /**
   * Set identifier type.
   *
   * @param identifierType the identifierType to set
   */
  public void setIdentifierType(IdentifierType identifierType) {
    if (identifierType == null) {
      throw new NullPointerException("Type of ResourceIdentifier has to be one of " + Arrays.toString(IdentifierType.values()));
    }
    this.identifierType = identifierType;
  }

  /**
   * Create ResourceIdentifier from URL.
   *
   * @param identifier Url as string.
   * @return Resource identifier.
   */
  public static ResourceIdentifier factoryUrlResourceIdentifier(String identifier) {
    return factoryResourceIdentifier(identifier, ResourceIdentifier.IdentifierType.URL);
  }

  /**
   * Create ResourceIdentifier from string.
   *
   * @param identifier Internal identifier as string.
   * @return Resource identifier.
   */
  public static ResourceIdentifier factoryInternalResourceIdentifier(String identifier) {
    return factoryResourceIdentifier(identifier, ResourceIdentifier.IdentifierType.INTERNAL);
  }

  /**
   * Create ResourceIdentifier from string and type.
   *
   * @param identifier Identifier as string.
   * @param type Identifier type of the resource identifier.
   * @return Resource identifier.
   */
  public static ResourceIdentifier factoryResourceIdentifier(String identifier, ResourceIdentifier.IdentifierType type) {
    ResourceIdentifier resourceIdentifier = new ResourceIdentifier();
    resourceIdentifier.setIdentifier(identifier);
    resourceIdentifier.setIdentifierType(type);
    return resourceIdentifier;
  }
}
