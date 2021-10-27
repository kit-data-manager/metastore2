
package edu.kit.datacite.kernel_4;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Types {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("resourceType")
    @Expose
    private String resourceType;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("resourceTypeGeneral")
    @Expose
    private Types.ResourceTypeGeneral resourceTypeGeneral;

    /**
     * 
     * (Required)
     * 
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Types.ResourceTypeGeneral getResourceTypeGeneral() {
        return resourceTypeGeneral;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setResourceTypeGeneral(Types.ResourceTypeGeneral resourceTypeGeneral) {
        this.resourceTypeGeneral = resourceTypeGeneral;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Types.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("resourceType");
        sb.append('=');
        sb.append(((this.resourceType == null)?"<null>":this.resourceType));
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
        result = ((result* 31)+((this.resourceTypeGeneral == null)? 0 :this.resourceTypeGeneral.hashCode()));
        result = ((result* 31)+((this.resourceType == null)? 0 :this.resourceType.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Types) == false) {
            return false;
        }
        Types rhs = ((Types) other);
        return (((this.resourceTypeGeneral == rhs.resourceTypeGeneral)||((this.resourceTypeGeneral!= null)&&this.resourceTypeGeneral.equals(rhs.resourceTypeGeneral)))&&((this.resourceType == rhs.resourceType)||((this.resourceType!= null)&&this.resourceType.equals(rhs.resourceType))));
    }

    @Generated("jsonschema2pojo")
    public enum ResourceTypeGeneral {

        @SerializedName("Audiovisual")
        AUDIOVISUAL("Audiovisual"),
        @SerializedName("Collection")
        COLLECTION("Collection"),
        @SerializedName("DataPaper")
        DATA_PAPER("DataPaper"),
        @SerializedName("Dataset")
        DATASET("Dataset"),
        @SerializedName("Event")
        EVENT("Event"),
        @SerializedName("Image")
        IMAGE("Image"),
        @SerializedName("InteractiveResource")
        INTERACTIVE_RESOURCE("InteractiveResource"),
        @SerializedName("Model")
        MODEL("Model"),
        @SerializedName("PhysicalObject")
        PHYSICAL_OBJECT("PhysicalObject"),
        @SerializedName("Service")
        SERVICE("Service"),
        @SerializedName("Software")
        SOFTWARE("Software"),
        @SerializedName("Sound")
        SOUND("Sound"),
        @SerializedName("Text")
        TEXT("Text"),
        @SerializedName("Workflow")
        WORKFLOW("Workflow"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, Types.ResourceTypeGeneral> CONSTANTS = new HashMap<String, Types.ResourceTypeGeneral>();

        static {
            for (Types.ResourceTypeGeneral c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ResourceTypeGeneral(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Types.ResourceTypeGeneral fromValue(String value) {
            Types.ResourceTypeGeneral constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
