
package edu.kit.datacite.kernel_4;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Title {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("titleType")
    @Expose
    private Title.TitleType titleType;
    @SerializedName("lang")
    @Expose
    private String lang;

    /**
     * 
     * (Required)
     * 
     */
    public String getTitle() {
        return title;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public Title.TitleType getTitleType() {
        return titleType;
    }

    public void setTitleType(Title.TitleType titleType) {
        this.titleType = titleType;
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
        sb.append(Title.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("title");
        sb.append('=');
        sb.append(((this.title == null)?"<null>":this.title));
        sb.append(',');
        sb.append("titleType");
        sb.append('=');
        sb.append(((this.titleType == null)?"<null>":this.titleType));
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
        result = ((result* 31)+((this.titleType == null)? 0 :this.titleType.hashCode()));
        result = ((result* 31)+((this.title == null)? 0 :this.title.hashCode()));
        result = ((result* 31)+((this.lang == null)? 0 :this.lang.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Title) == false) {
            return false;
        }
        Title rhs = ((Title) other);
        return ((((this.titleType == rhs.titleType)||((this.titleType!= null)&&this.titleType.equals(rhs.titleType)))&&((this.title == rhs.title)||((this.title!= null)&&this.title.equals(rhs.title))))&&((this.lang == rhs.lang)||((this.lang!= null)&&this.lang.equals(rhs.lang))));
    }

    @Generated("jsonschema2pojo")
    public enum TitleType {

        @SerializedName("AlternativeTitle")
        ALTERNATIVE_TITLE("AlternativeTitle"),
        @SerializedName("Subtitle")
        SUBTITLE("Subtitle"),
        @SerializedName("TranslatedTitle")
        TRANSLATED_TITLE("TranslatedTitle"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, Title.TitleType> CONSTANTS = new HashMap<String, Title.TitleType>();

        static {
            for (Title.TitleType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TitleType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Title.TitleType fromValue(String value) {
            Title.TitleType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
