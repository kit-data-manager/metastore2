
package edu.kit.datacite.kernel_4;

import java.net.URI;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class NameIdentifier {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("nameIdentifier")
    @Expose
    private String nameIdentifier;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("nameIdentifierScheme")
    @Expose
    private String nameIdentifierScheme;
    @SerializedName("schemeURI")
    @Expose
    private URI schemeURI;

    /**
     * 
     * (Required)
     * 
     */
    public String getNameIdentifier() {
        return nameIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setNameIdentifier(String nameIdentifier) {
        this.nameIdentifier = nameIdentifier;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getNameIdentifierScheme() {
        return nameIdentifierScheme;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setNameIdentifierScheme(String nameIdentifierScheme) {
        this.nameIdentifierScheme = nameIdentifierScheme;
    }

    public URI getSchemeURI() {
        return schemeURI;
    }

    public void setSchemeURI(URI schemeURI) {
        this.schemeURI = schemeURI;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(NameIdentifier.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("nameIdentifier");
        sb.append('=');
        sb.append(((this.nameIdentifier == null)?"<null>":this.nameIdentifier));
        sb.append(',');
        sb.append("nameIdentifierScheme");
        sb.append('=');
        sb.append(((this.nameIdentifierScheme == null)?"<null>":this.nameIdentifierScheme));
        sb.append(',');
        sb.append("schemeURI");
        sb.append('=');
        sb.append(((this.schemeURI == null)?"<null>":this.schemeURI));
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
        result = ((result* 31)+((this.nameIdentifierScheme == null)? 0 :this.nameIdentifierScheme.hashCode()));
        result = ((result* 31)+((this.schemeURI == null)? 0 :this.schemeURI.hashCode()));
        result = ((result* 31)+((this.nameIdentifier == null)? 0 :this.nameIdentifier.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof NameIdentifier) == false) {
            return false;
        }
        NameIdentifier rhs = ((NameIdentifier) other);
        return ((((this.nameIdentifierScheme == rhs.nameIdentifierScheme)||((this.nameIdentifierScheme!= null)&&this.nameIdentifierScheme.equals(rhs.nameIdentifierScheme)))&&((this.schemeURI == rhs.schemeURI)||((this.schemeURI!= null)&&this.schemeURI.equals(rhs.schemeURI))))&&((this.nameIdentifier == rhs.nameIdentifier)||((this.nameIdentifier!= null)&&this.nameIdentifier.equals(rhs.nameIdentifier))));
    }

}
