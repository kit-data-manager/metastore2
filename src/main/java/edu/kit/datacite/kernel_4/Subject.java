
package edu.kit.datacite.kernel_4;

import java.net.URI;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Subject {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("subject")
    @Expose
    private String subject;
    @SerializedName("subjectScheme")
    @Expose
    private String subjectScheme;
    @SerializedName("schemeURI")
    @Expose
    private URI schemeURI;
    @SerializedName("valueURI")
    @Expose
    private URI valueURI;
    @SerializedName("lang")
    @Expose
    private String lang;

    /**
     * 
     * (Required)
     * 
     */
    public String getSubject() {
        return subject;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubjectScheme() {
        return subjectScheme;
    }

    public void setSubjectScheme(String subjectScheme) {
        this.subjectScheme = subjectScheme;
    }

    public URI getSchemeURI() {
        return schemeURI;
    }

    public void setSchemeURI(URI schemeURI) {
        this.schemeURI = schemeURI;
    }

    public URI getValueURI() {
        return valueURI;
    }

    public void setValueURI(URI valueURI) {
        this.valueURI = valueURI;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Subject.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("subject");
        sb.append('=');
        sb.append(((this.subject == null)?"<null>":this.subject));
        sb.append(',');
        sb.append("subjectScheme");
        sb.append('=');
        sb.append(((this.subjectScheme == null)?"<null>":this.subjectScheme));
        sb.append(',');
        sb.append("schemeURI");
        sb.append('=');
        sb.append(((this.schemeURI == null)?"<null>":this.schemeURI));
        sb.append(',');
        sb.append("valueURI");
        sb.append('=');
        sb.append(((this.valueURI == null)?"<null>":this.valueURI));
        sb.append(',');
        sb.append("lang");
        sb.append('=');
        sb.append(((this.lang == null)?"<null>":this.lang));
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
        result = ((result* 31)+((this.valueURI == null)? 0 :this.valueURI.hashCode()));
        result = ((result* 31)+((this.lang == null)? 0 :this.lang.hashCode()));
        result = ((result* 31)+((this.schemeURI == null)? 0 :this.schemeURI.hashCode()));
        result = ((result* 31)+((this.subject == null)? 0 :this.subject.hashCode()));
        result = ((result* 31)+((this.subjectScheme == null)? 0 :this.subjectScheme.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Subject) == false) {
            return false;
        }
        Subject rhs = ((Subject) other);
        return ((((((this.valueURI == rhs.valueURI)||((this.valueURI!= null)&&this.valueURI.equals(rhs.valueURI)))&&((this.lang == rhs.lang)||((this.lang!= null)&&this.lang.equals(rhs.lang))))&&((this.schemeURI == rhs.schemeURI)||((this.schemeURI!= null)&&this.schemeURI.equals(rhs.schemeURI))))&&((this.subject == rhs.subject)||((this.subject!= null)&&this.subject.equals(rhs.subject))))&&((this.subjectScheme == rhs.subjectScheme)||((this.subjectScheme!= null)&&this.subjectScheme.equals(rhs.subjectScheme))));
    }

}
