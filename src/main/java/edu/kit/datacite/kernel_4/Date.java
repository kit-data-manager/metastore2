
package edu.kit.datacite.kernel_4;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Date {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("date")
    @Expose
    private String date;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("dateType")
    @Expose
    private Date.DateType dateType;
    @SerializedName("dateInformation")
    @Expose
    private String dateInformation;

    /**
     * 
     * (Required)
     * 
     */
    public String getDate() {
        return date;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Date.DateType getDateType() {
        return dateType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setDateType(Date.DateType dateType) {
        this.dateType = dateType;
    }

    public String getDateInformation() {
        return dateInformation;
    }

    public void setDateInformation(String dateInformation) {
        this.dateInformation = dateInformation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Date.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("date");
        sb.append('=');
        sb.append(((this.date == null)?"<null>":this.date));
        sb.append(',');
        sb.append("dateType");
        sb.append('=');
        sb.append(((this.dateType == null)?"<null>":this.dateType));
        sb.append(',');
        sb.append("dateInformation");
        sb.append('=');
        sb.append(((this.dateInformation == null)?"<null>":this.dateInformation));
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
        result = ((result* 31)+((this.date == null)? 0 :this.date.hashCode()));
        result = ((result* 31)+((this.dateInformation == null)? 0 :this.dateInformation.hashCode()));
        result = ((result* 31)+((this.dateType == null)? 0 :this.dateType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Date) == false) {
            return false;
        }
        Date rhs = ((Date) other);
        return ((((this.date == rhs.date)||((this.date!= null)&&this.date.equals(rhs.date)))&&((this.dateInformation == rhs.dateInformation)||((this.dateInformation!= null)&&this.dateInformation.equals(rhs.dateInformation))))&&((this.dateType == rhs.dateType)||((this.dateType!= null)&&this.dateType.equals(rhs.dateType))));
    }

    @Generated("jsonschema2pojo")
    public enum DateType {

        @SerializedName("Accepted")
        ACCEPTED("Accepted"),
        @SerializedName("Available")
        AVAILABLE("Available"),
        @SerializedName("Copyrighted")
        COPYRIGHTED("Copyrighted"),
        @SerializedName("Collected")
        COLLECTED("Collected"),
        @SerializedName("Created")
        CREATED("Created"),
        @SerializedName("Issued")
        ISSUED("Issued"),
        @SerializedName("Submitted")
        SUBMITTED("Submitted"),
        @SerializedName("Updated")
        UPDATED("Updated"),
        @SerializedName("Valid")
        VALID("Valid"),
        @SerializedName("Withdrawn")
        WITHDRAWN("Withdrawn"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, Date.DateType> CONSTANTS = new HashMap<String, Date.DateType>();

        static {
            for (Date.DateType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        DateType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Date.DateType fromValue(String value) {
            Date.DateType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
