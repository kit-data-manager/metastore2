
package edu.kit.datacite.kernel_4;

import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Affiliation {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("affiliation")
    @Expose
    private String affiliation;

    /**
     * 
     * (Required)
     * 
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Affiliation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("affiliation");
        sb.append('=');
        sb.append(((this.affiliation == null)?"<null>":this.affiliation));
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
        result = ((result* 31)+((this.affiliation == null)? 0 :this.affiliation.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Affiliation) == false) {
            return false;
        }
        Affiliation rhs = ((Affiliation) other);
        return ((this.affiliation == rhs.affiliation)||((this.affiliation!= null)&&this.affiliation.equals(rhs.affiliation)));
    }

}
