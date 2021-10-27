
package edu.kit.datacite.kernel_4;

import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class GeoLocationBox {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("westBoundLongitude")
    @Expose
    private Double westBoundLongitude;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("eastBoundLongitude")
    @Expose
    private Double eastBoundLongitude;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("southBoundLatitude")
    @Expose
    private Double southBoundLatitude;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("northBoundLatitude")
    @Expose
    private Double northBoundLatitude;

    /**
     * 
     * (Required)
     * 
     */
    public Double getWestBoundLongitude() {
        return westBoundLongitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setWestBoundLongitude(Double westBoundLongitude) {
        this.westBoundLongitude = westBoundLongitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getEastBoundLongitude() {
        return eastBoundLongitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setEastBoundLongitude(Double eastBoundLongitude) {
        this.eastBoundLongitude = eastBoundLongitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getSouthBoundLatitude() {
        return southBoundLatitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setSouthBoundLatitude(Double southBoundLatitude) {
        this.southBoundLatitude = southBoundLatitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Double getNorthBoundLatitude() {
        return northBoundLatitude;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setNorthBoundLatitude(Double northBoundLatitude) {
        this.northBoundLatitude = northBoundLatitude;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GeoLocationBox.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("westBoundLongitude");
        sb.append('=');
        sb.append(((this.westBoundLongitude == null)?"<null>":this.westBoundLongitude));
        sb.append(',');
        sb.append("eastBoundLongitude");
        sb.append('=');
        sb.append(((this.eastBoundLongitude == null)?"<null>":this.eastBoundLongitude));
        sb.append(',');
        sb.append("southBoundLatitude");
        sb.append('=');
        sb.append(((this.southBoundLatitude == null)?"<null>":this.southBoundLatitude));
        sb.append(',');
        sb.append("northBoundLatitude");
        sb.append('=');
        sb.append(((this.northBoundLatitude == null)?"<null>":this.northBoundLatitude));
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
        result = ((result* 31)+((this.northBoundLatitude == null)? 0 :this.northBoundLatitude.hashCode()));
        result = ((result* 31)+((this.southBoundLatitude == null)? 0 :this.southBoundLatitude.hashCode()));
        result = ((result* 31)+((this.westBoundLongitude == null)? 0 :this.westBoundLongitude.hashCode()));
        result = ((result* 31)+((this.eastBoundLongitude == null)? 0 :this.eastBoundLongitude.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GeoLocationBox) == false) {
            return false;
        }
        GeoLocationBox rhs = ((GeoLocationBox) other);
        return (((((this.northBoundLatitude == rhs.northBoundLatitude)||((this.northBoundLatitude!= null)&&this.northBoundLatitude.equals(rhs.northBoundLatitude)))&&((this.southBoundLatitude == rhs.southBoundLatitude)||((this.southBoundLatitude!= null)&&this.southBoundLatitude.equals(rhs.southBoundLatitude))))&&((this.westBoundLongitude == rhs.westBoundLongitude)||((this.westBoundLongitude!= null)&&this.westBoundLongitude.equals(rhs.westBoundLongitude))))&&((this.eastBoundLongitude == rhs.eastBoundLongitude)||((this.eastBoundLongitude!= null)&&this.eastBoundLongitude.equals(rhs.eastBoundLongitude))));
    }

}
