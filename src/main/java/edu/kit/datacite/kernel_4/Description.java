
package edu.kit.datacite.kernel_4;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Description {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("description")
    @Expose
    private String description;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("descriptionType")
    @Expose
    private Description.DescriptionType descriptionType;
    @SerializedName("lang")
    @Expose
    private String lang;

    /**
     * 
     * (Required)
     * 
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Description.DescriptionType getDescriptionType() {
        return descriptionType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDescriptionType(Description.DescriptionType descriptionType) {
        this.descriptionType = descriptionType;
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
        sb.append(Description.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("description");
        sb.append('=');
        sb.append(((this.description == null)?"<null>":this.description));
        sb.append(',');
        sb.append("descriptionType");
        sb.append('=');
        sb.append(((this.descriptionType == null)?"<null>":this.descriptionType));
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
        result = ((result* 31)+((this.description == null)? 0 :this.description.hashCode()));
        result = ((result* 31)+((this.lang == null)? 0 :this.lang.hashCode()));
        result = ((result* 31)+((this.descriptionType == null)? 0 :this.descriptionType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Description) == false) {
            return false;
        }
        Description rhs = ((Description) other);
        return ((((this.description == rhs.description)||((this.description!= null)&&this.description.equals(rhs.description)))&&((this.lang == rhs.lang)||((this.lang!= null)&&this.lang.equals(rhs.lang))))&&((this.descriptionType == rhs.descriptionType)||((this.descriptionType!= null)&&this.descriptionType.equals(rhs.descriptionType))));
    }

    @Generated("jsonschema2pojo")
    public enum DescriptionType {

        @SerializedName("Abstract")
        ABSTRACT("Abstract"),
        @SerializedName("Methods")
        METHODS("Methods"),
        @SerializedName("SeriesInformation")
        SERIES_INFORMATION("SeriesInformation"),
        @SerializedName("TableOfContents")
        TABLE_OF_CONTENTS("TableOfContents"),
        @SerializedName("TechnicalInfo")
        TECHNICAL_INFO("TechnicalInfo"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, Description.DescriptionType> CONSTANTS = new HashMap<String, Description.DescriptionType>();

        static {
            for (Description.DescriptionType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        DescriptionType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Description.DescriptionType fromValue(String value) {
            Description.DescriptionType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
