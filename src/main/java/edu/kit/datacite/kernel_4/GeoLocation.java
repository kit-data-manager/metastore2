
package edu.kit.datacite.kernel_4;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class GeoLocation {

    @SerializedName("geoLocationPlace")
    @Expose
    private String geoLocationPlace;
    @SerializedName("geoLocationPoint")
    @Expose
    private GeoLocationPoint geoLocationPoint;
    @SerializedName("geoLocationBox")
    @Expose
    private GeoLocationBox geoLocationBox;
    @SerializedName("geoLocationPolygons")
    @Expose
    private Set<GeoLocationPolygon> geoLocationPolygons = new LinkedHashSet<GeoLocationPolygon>();

    public String getGeoLocationPlace() {
        return geoLocationPlace;
    }

    public void setGeoLocationPlace(String geoLocationPlace) {
        this.geoLocationPlace = geoLocationPlace;
    }

    public GeoLocationPoint getGeoLocationPoint() {
        return geoLocationPoint;
    }

    public void setGeoLocationPoint(GeoLocationPoint geoLocationPoint) {
        this.geoLocationPoint = geoLocationPoint;
    }

    public GeoLocationBox getGeoLocationBox() {
        return geoLocationBox;
    }

    public void setGeoLocationBox(GeoLocationBox geoLocationBox) {
        this.geoLocationBox = geoLocationBox;
    }

    public Set<GeoLocationPolygon> getGeoLocationPolygons() {
        return geoLocationPolygons;
    }

    public void setGeoLocationPolygons(Set<GeoLocationPolygon> geoLocationPolygons) {
        this.geoLocationPolygons = geoLocationPolygons;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(GeoLocation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("geoLocationPlace");
        sb.append('=');
        sb.append(((this.geoLocationPlace == null)?"<null>":this.geoLocationPlace));
        sb.append(',');
        sb.append("geoLocationPoint");
        sb.append('=');
        sb.append(((this.geoLocationPoint == null)?"<null>":this.geoLocationPoint));
        sb.append(',');
        sb.append("geoLocationBox");
        sb.append('=');
        sb.append(((this.geoLocationBox == null)?"<null>":this.geoLocationBox));
        sb.append(',');
        sb.append("geoLocationPolygons");
        sb.append('=');
        sb.append(((this.geoLocationPolygons == null)?"<null>":this.geoLocationPolygons));
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
        result = ((result* 31)+((this.geoLocationPlace == null)? 0 :this.geoLocationPlace.hashCode()));
        result = ((result* 31)+((this.geoLocationBox == null)? 0 :this.geoLocationBox.hashCode()));
        result = ((result* 31)+((this.geoLocationPoint == null)? 0 :this.geoLocationPoint.hashCode()));
        result = ((result* 31)+((this.geoLocationPolygons == null)? 0 :this.geoLocationPolygons.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof GeoLocation) == false) {
            return false;
        }
        GeoLocation rhs = ((GeoLocation) other);
        return (((((this.geoLocationPlace == rhs.geoLocationPlace)||((this.geoLocationPlace!= null)&&this.geoLocationPlace.equals(rhs.geoLocationPlace)))&&((this.geoLocationBox == rhs.geoLocationBox)||((this.geoLocationBox!= null)&&this.geoLocationBox.equals(rhs.geoLocationBox))))&&((this.geoLocationPoint == rhs.geoLocationPoint)||((this.geoLocationPoint!= null)&&this.geoLocationPoint.equals(rhs.geoLocationPoint))))&&((this.geoLocationPolygons == rhs.geoLocationPolygons)||((this.geoLocationPolygons!= null)&&this.geoLocationPolygons.equals(rhs.geoLocationPolygons))));
    }

}
