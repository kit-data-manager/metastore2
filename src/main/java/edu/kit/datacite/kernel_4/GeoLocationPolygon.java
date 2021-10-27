
package edu.kit.datacite.kernel_4;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class GeoLocationPolygon {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("polygonPoints")
    @Expose
    private List<GeoLocationPoint> polygonPoints = new ArrayList<GeoLocationPoint>();
    @SerializedName("inPolygonPoint")
    @Expose
    private GeoLocationPoint inPolygonPoint;

    /**
     * 
     * (Required)
     * 
     */
    public List<GeoLocationPoint> getPolygonPoints() {
        return polygonPoints;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setPolygonPoints(List<GeoLocationPoint> polygonPoints) {
        this.polygonPoints = polygonPoints;
    }

    public GeoLocationPoint getInPolygonPoint() {
        return inPolygonPoint;
    }

    public void setInPolygonPoint(GeoLocationPoint inPolygonPoint) {
        this.inPolygonPoint = inPolygonPoint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GeoLocationPolygon.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("polygonPoints");
        sb.append('=');
        sb.append(((this.polygonPoints == null)?"<null>":this.polygonPoints));
        sb.append(',');
        sb.append("inPolygonPoint");
        sb.append('=');
        sb.append(((this.inPolygonPoint == null)?"<null>":this.inPolygonPoint));
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
        result = ((result* 31)+((this.polygonPoints == null)? 0 :this.polygonPoints.hashCode()));
        result = ((result* 31)+((this.inPolygonPoint == null)? 0 :this.inPolygonPoint.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GeoLocationPolygon) == false) {
            return false;
        }
        GeoLocationPolygon rhs = ((GeoLocationPolygon) other);
        return (((this.polygonPoints == rhs.polygonPoints)||((this.polygonPoints!= null)&&this.polygonPoints.equals(rhs.polygonPoints)))&&((this.inPolygonPoint == rhs.inPolygonPoint)||((this.inPolygonPoint!= null)&&this.inPolygonPoint.equals(rhs.inPolygonPoint))));
    }

}
