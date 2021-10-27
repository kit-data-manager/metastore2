
package edu.kit.datacite.kernel_4;

import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class GeoLocationPoint {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("pointLongitude")
    @Expose
    private Double pointLongitude;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("pointLatitude")
    @Expose
    private Double pointLatitude;

    /**
     * 
     * (Required)
     * 
     */
    public Double getPointLongitude() {
        return pointLongitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setPointLongitude(Double pointLongitude) {
        this.pointLongitude = pointLongitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getPointLatitude() {
        return pointLatitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setPointLatitude(Double pointLatitude) {
        this.pointLatitude = pointLatitude;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GeoLocationPoint.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("pointLongitude");
        sb.append('=');
        sb.append(((this.pointLongitude == null)?"<null>":this.pointLongitude));
        sb.append(',');
        sb.append("pointLatitude");
        sb.append('=');
        sb.append(((this.pointLatitude == null)?"<null>":this.pointLatitude));
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
        result = ((result* 31)+((this.pointLongitude == null)? 0 :this.pointLongitude.hashCode()));
        result = ((result* 31)+((this.pointLatitude == null)? 0 :this.pointLatitude.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GeoLocationPoint) == false) {
            return false;
        }
        GeoLocationPoint rhs = ((GeoLocationPoint) other);
        return (((this.pointLongitude == rhs.pointLongitude)||((this.pointLongitude!= null)&&this.pointLongitude.equals(rhs.pointLongitude)))&&((this.pointLatitude == rhs.pointLatitude)||((this.pointLatitude!= null)&&this.pointLatitude.equals(rhs.pointLatitude))));
    }

}
