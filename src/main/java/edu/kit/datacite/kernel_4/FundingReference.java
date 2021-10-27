
package edu.kit.datacite.kernel_4;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class FundingReference {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("funderName")
    @Expose
    private String funderName;
    @SerializedName("funderIdentifier")
    @Expose
    private String funderIdentifier;
    @SerializedName("funderIdentifierType")
    @Expose
    private FundingReference.FunderIdentifierType funderIdentifierType;
    @SerializedName("awardNumber")
    @Expose
    private String awardNumber;
    @SerializedName("awardURI")
    @Expose
    private URI awardURI;
    @SerializedName("awardTitle")
    @Expose
    private String awardTitle;

    /**
     * 
     * (Required)
     * 
     */
    public String getFunderName() {
        return funderName;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setFunderName(String funderName) {
        this.funderName = funderName;
    }

    public String getFunderIdentifier() {
        return funderIdentifier;
    }

    public void setFunderIdentifier(String funderIdentifier) {
        this.funderIdentifier = funderIdentifier;
    }

    public FundingReference.FunderIdentifierType getFunderIdentifierType() {
        return funderIdentifierType;
    }

    public void setFunderIdentifierType(FundingReference.FunderIdentifierType funderIdentifierType) {
        this.funderIdentifierType = funderIdentifierType;
    }

    public String getAwardNumber() {
        return awardNumber;
    }

    public void setAwardNumber(String awardNumber) {
        this.awardNumber = awardNumber;
    }

    public URI getAwardURI() {
        return awardURI;
    }

    public void setAwardURI(URI awardURI) {
        this.awardURI = awardURI;
    }

    public String getAwardTitle() {
        return awardTitle;
    }

    public void setAwardTitle(String awardTitle) {
        this.awardTitle = awardTitle;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FundingReference.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("funderName");
        sb.append('=');
        sb.append(((this.funderName == null)?"<null>":this.funderName));
        sb.append(',');
        sb.append("funderIdentifier");
        sb.append('=');
        sb.append(((this.funderIdentifier == null)?"<null>":this.funderIdentifier));
        sb.append(',');
        sb.append("funderIdentifierType");
        sb.append('=');
        sb.append(((this.funderIdentifierType == null)?"<null>":this.funderIdentifierType));
        sb.append(',');
        sb.append("awardNumber");
        sb.append('=');
        sb.append(((this.awardNumber == null)?"<null>":this.awardNumber));
        sb.append(',');
        sb.append("awardURI");
        sb.append('=');
        sb.append(((this.awardURI == null)?"<null>":this.awardURI));
        sb.append(',');
        sb.append("awardTitle");
        sb.append('=');
        sb.append(((this.awardTitle == null)?"<null>":this.awardTitle));
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
        result = ((result* 31)+((this.funderIdentifierType == null)? 0 :this.funderIdentifierType.hashCode()));
        result = ((result* 31)+((this.funderName == null)? 0 :this.funderName.hashCode()));
        result = ((result* 31)+((this.funderIdentifier == null)? 0 :this.funderIdentifier.hashCode()));
        result = ((result* 31)+((this.awardNumber == null)? 0 :this.awardNumber.hashCode()));
        result = ((result* 31)+((this.awardTitle == null)? 0 :this.awardTitle.hashCode()));
        result = ((result* 31)+((this.awardURI == null)? 0 :this.awardURI.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FundingReference) == false) {
            return false;
        }
        FundingReference rhs = ((FundingReference) other);
        return (((((((this.funderIdentifierType == rhs.funderIdentifierType)||((this.funderIdentifierType!= null)&&this.funderIdentifierType.equals(rhs.funderIdentifierType)))&&((this.funderName == rhs.funderName)||((this.funderName!= null)&&this.funderName.equals(rhs.funderName))))&&((this.funderIdentifier == rhs.funderIdentifier)||((this.funderIdentifier!= null)&&this.funderIdentifier.equals(rhs.funderIdentifier))))&&((this.awardNumber == rhs.awardNumber)||((this.awardNumber!= null)&&this.awardNumber.equals(rhs.awardNumber))))&&((this.awardTitle == rhs.awardTitle)||((this.awardTitle!= null)&&this.awardTitle.equals(rhs.awardTitle))))&&((this.awardURI == rhs.awardURI)||((this.awardURI!= null)&&this.awardURI.equals(rhs.awardURI))));
    }

    @Generated("jsonschema2pojo")
    public enum FunderIdentifierType {

        @SerializedName("ISNI")
        ISNI("ISNI"),
        @SerializedName("GRID")
        GRID("GRID"),
        @SerializedName("Crossref Funder ID")
        CROSSREF_FUNDER_ID("Crossref Funder ID"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, FundingReference.FunderIdentifierType> CONSTANTS = new HashMap<String, FundingReference.FunderIdentifierType>();

        static {
            for (FundingReference.FunderIdentifierType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        FunderIdentifierType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static FundingReference.FunderIdentifierType fromValue(String value) {
            FundingReference.FunderIdentifierType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
