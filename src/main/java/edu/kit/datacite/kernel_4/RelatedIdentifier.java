
package edu.kit.datacite.kernel_4;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class RelatedIdentifier {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("relatedIdentifier")
    @Expose
    private String relatedIdentifier;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("relatedIdentifierType")
    @Expose
    private RelatedIdentifier.RelatedIdentifierType relatedIdentifierType;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("relationType")
    @Expose
    private RelatedIdentifier.RelationType relationType;
    @SerializedName("relatedMetadataScheme")
    @Expose
    private String relatedMetadataScheme;
    @SerializedName("schemeURI")
    @Expose
    private URI schemeURI;
    @SerializedName("schemeType")
    @Expose
    private String schemeType;
    @SerializedName("resourceTypeGeneral")
    @Expose
    private edu.kit.datacite.kernel_4.Types.ResourceTypeGeneral resourceTypeGeneral;

    /**
     * 
     * (Required)
     * 
     */
    public String getRelatedIdentifier() {
        return relatedIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setRelatedIdentifier(String relatedIdentifier) {
        this.relatedIdentifier = relatedIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    public RelatedIdentifier.RelatedIdentifierType getRelatedIdentifierType() {
        return relatedIdentifierType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setRelatedIdentifierType(RelatedIdentifier.RelatedIdentifierType relatedIdentifierType) {
        this.relatedIdentifierType = relatedIdentifierType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public RelatedIdentifier.RelationType getRelationType() {
        return relationType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setRelationType(RelatedIdentifier.RelationType relationType) {
        this.relationType = relationType;
    }

    public String getRelatedMetadataScheme() {
        return relatedMetadataScheme;
    }

    public void setRelatedMetadataScheme(String relatedMetadataScheme) {
        this.relatedMetadataScheme = relatedMetadataScheme;
    }

    public URI getSchemeURI() {
        return schemeURI;
    }

    public void setSchemeURI(URI schemeURI) {
        this.schemeURI = schemeURI;
    }

    public String getSchemeType() {
        return schemeType;
    }

    public void setSchemeType(String schemeType) {
        this.schemeType = schemeType;
    }

    public edu.kit.datacite.kernel_4.Types.ResourceTypeGeneral getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    public void setResourceTypeGeneral(edu.kit.datacite.kernel_4.Types.ResourceTypeGeneral resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RelatedIdentifier.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("relatedIdentifier");
        sb.append('=');
        sb.append(((this.relatedIdentifier == null)?"<null>":this.relatedIdentifier));
        sb.append(',');
        sb.append("relatedIdentifierType");
        sb.append('=');
        sb.append(((this.relatedIdentifierType == null)?"<null>":this.relatedIdentifierType));
        sb.append(',');
        sb.append("relationType");
        sb.append('=');
        sb.append(((this.relationType == null)?"<null>":this.relationType));
        sb.append(',');
        sb.append("relatedMetadataScheme");
        sb.append('=');
        sb.append(((this.relatedMetadataScheme == null)?"<null>":this.relatedMetadataScheme));
        sb.append(',');
        sb.append("schemeURI");
        sb.append('=');
        sb.append(((this.schemeURI == null)?"<null>":this.schemeURI));
        sb.append(',');
        sb.append("schemeType");
        sb.append('=');
        sb.append(((this.schemeType == null)?"<null>":this.schemeType));
        sb.append(',');
        sb.append("resourceTypeGeneral");
        sb.append('=');
        sb.append(((this.resourceTypeGeneral == null)?"<null>":this.resourceTypeGeneral));
        sb.append(',');
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
        result = ((result* 31)+((this.relationType == null)? 0 :this.relationType.hashCode()));
        result = ((result* 31)+((this.resourceTypeGeneral == null)? 0 :this.resourceTypeGeneral.hashCode()));
        result = ((result* 31)+((this.schemeType == null)? 0 :this.schemeType.hashCode()));
        result = ((result* 31)+((this.relatedIdentifier == null)? 0 :this.relatedIdentifier.hashCode()));
        result = ((result* 31)+((this.schemeURI == null)? 0 :this.schemeURI.hashCode()));
        result = ((result* 31)+((this.relatedIdentifierType == null)? 0 :this.relatedIdentifierType.hashCode()));
        result = ((result* 31)+((this.relatedMetadataScheme == null)? 0 :this.relatedMetadataScheme.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RelatedIdentifier) == false) {
            return false;
        }
        RelatedIdentifier rhs = ((RelatedIdentifier) other);
        return ((((((((this.relationType == rhs.relationType)||((this.relationType!= null)&&this.relationType.equals(rhs.relationType)))&&((this.resourceTypeGeneral == rhs.resourceTypeGeneral)||((this.resourceTypeGeneral!= null)&&this.resourceTypeGeneral.equals(rhs.resourceTypeGeneral))))&&((this.schemeType == rhs.schemeType)||((this.schemeType!= null)&&this.schemeType.equals(rhs.schemeType))))&&((this.relatedIdentifier == rhs.relatedIdentifier)||((this.relatedIdentifier!= null)&&this.relatedIdentifier.equals(rhs.relatedIdentifier))))&&((this.schemeURI == rhs.schemeURI)||((this.schemeURI!= null)&&this.schemeURI.equals(rhs.schemeURI))))&&((this.relatedIdentifierType == rhs.relatedIdentifierType)||((this.relatedIdentifierType!= null)&&this.relatedIdentifierType.equals(rhs.relatedIdentifierType))))&&((this.relatedMetadataScheme == rhs.relatedMetadataScheme)||((this.relatedMetadataScheme!= null)&&this.relatedMetadataScheme.equals(rhs.relatedMetadataScheme))));
    }

    @Generated("jsonschema2pojo")
    public enum RelatedIdentifierType {

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
        private final static Map<String, RelatedIdentifier.RelatedIdentifierType> CONSTANTS = new HashMap<String, RelatedIdentifier.RelatedIdentifierType>();

        static {
            for (RelatedIdentifier.RelatedIdentifierType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        RelatedIdentifierType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static RelatedIdentifier.RelatedIdentifierType fromValue(String value) {
            RelatedIdentifier.RelatedIdentifierType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum RelationType {

        @SerializedName("IsCitedBy")
        IS_CITED_BY("IsCitedBy"),
        @SerializedName("Cites")
        CITES("Cites"),
        @SerializedName("IsSupplementTo")
        IS_SUPPLEMENT_TO("IsSupplementTo"),
        @SerializedName("IsSupplementedBy")
        IS_SUPPLEMENTED_BY("IsSupplementedBy"),
        @SerializedName("IsContinuedBy")
        IS_CONTINUED_BY("IsContinuedBy"),
        @SerializedName("Continues")
        CONTINUES("Continues"),
        @SerializedName("IsDescribedBy")
        IS_DESCRIBED_BY("IsDescribedBy"),
        @SerializedName("Describes")
        DESCRIBES("Describes"),
        @SerializedName("HasMetadata")
        HAS_METADATA("HasMetadata"),
        @SerializedName("IsMetadataFor")
        IS_METADATA_FOR("IsMetadataFor"),
        @SerializedName("HasVersion")
        HAS_VERSION("HasVersion"),
        @SerializedName("IsVersionOf")
        IS_VERSION_OF("IsVersionOf"),
        @SerializedName("IsNewVersionOf")
        IS_NEW_VERSION_OF("IsNewVersionOf"),
        @SerializedName("IsPreviousVersionOf")
        IS_PREVIOUS_VERSION_OF("IsPreviousVersionOf"),
        @SerializedName("IsPartOf")
        IS_PART_OF("IsPartOf"),
        @SerializedName("HasPart")
        HAS_PART("HasPart"),
        @SerializedName("IsReferencedBy")
        IS_REFERENCED_BY("IsReferencedBy"),
        @SerializedName("References")
        REFERENCES("References"),
        @SerializedName("IsDocumentedBy")
        IS_DOCUMENTED_BY("IsDocumentedBy"),
        @SerializedName("Documents")
        DOCUMENTS("Documents"),
        @SerializedName("IsCompiledBy")
        IS_COMPILED_BY("IsCompiledBy"),
        @SerializedName("Compiles")
        COMPILES("Compiles"),
        @SerializedName("IsVariantFormOf")
        IS_VARIANT_FORM_OF("IsVariantFormOf"),
        @SerializedName("IsOriginalFormOf")
        IS_ORIGINAL_FORM_OF("IsOriginalFormOf"),
        @SerializedName("IsIdenticalTo")
        IS_IDENTICAL_TO("IsIdenticalTo"),
        @SerializedName("IsReviewedBy")
        IS_REVIEWED_BY("IsReviewedBy"),
        @SerializedName("Reviews")
        REVIEWS("Reviews"),
        @SerializedName("IsDerivedFrom")
        IS_DERIVED_FROM("IsDerivedFrom"),
        @SerializedName("IsSourceOf")
        IS_SOURCE_OF("IsSourceOf"),
        @SerializedName("IsRequiredBy")
        IS_REQUIRED_BY("IsRequiredBy"),
        @SerializedName("Requires")
        REQUIRES("Requires"),
        @SerializedName("IsObsoletedBy")
        IS_OBSOLETED_BY("IsObsoletedBy"),
        @SerializedName("Obsoletes")
        OBSOLETES("Obsoletes");
        private final String value;
        private final static Map<String, RelatedIdentifier.RelationType> CONSTANTS = new HashMap<String, RelatedIdentifier.RelationType>();

        static {
            for (RelatedIdentifier.RelationType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        RelationType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static RelatedIdentifier.RelationType fromValue(String value) {
            RelatedIdentifier.RelationType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
