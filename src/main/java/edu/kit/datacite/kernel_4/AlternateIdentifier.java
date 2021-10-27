
package edu.kit.datacite.kernel_4;

import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class AlternateIdentifier {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("alternateIdentifier")
    @Expose
    private String alternateIdentifier;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("alternateIdentifierType")
    @Expose
    private String alternateIdentifierType;

    /**
     * 
     * (Required)
     * 
     */
    public String getAlternateIdentifier() {
        return alternateIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setAlternateIdentifier(String alternateIdentifier) {
        this.alternateIdentifier = alternateIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getAlternateIdentifierType() {
        return alternateIdentifierType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setAlternateIdentifierType(String alternateIdentifierType) {
        this.alternateIdentifierType = alternateIdentifierType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AlternateIdentifier.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("alternateIdentifier");
        sb.append('=');
        sb.append(((this.alternateIdentifier == null)?"<null>":this.alternateIdentifier));
        sb.append(',');
        sb.append("alternateIdentifierType");
        sb.append('=');
        sb.append(((this.alternateIdentifierType == null)?"<null>":this.alternateIdentifierType));
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
        result = ((result* 31)+((this.alternateIdentifier == null)? 0 :this.alternateIdentifier.hashCode()));
        result = ((result* 31)+((this.alternateIdentifierType == null)? 0 :this.alternateIdentifierType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AlternateIdentifier) == false) {
            return false;
        }
        AlternateIdentifier rhs = ((AlternateIdentifier) other);
        return (((this.alternateIdentifier == rhs.alternateIdentifier)||((this.alternateIdentifier!= null)&&this.alternateIdentifier.equals(rhs.alternateIdentifier)))&&((this.alternateIdentifierType == rhs.alternateIdentifierType)||((this.alternateIdentifierType!= null)&&this.alternateIdentifierType.equals(rhs.alternateIdentifierType))));
    }

}
