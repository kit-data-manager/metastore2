
package edu.kit.datacite.kernel_4;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Datacite43Schema {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("types")
    @Expose
    private Types types;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("identifiers")
    @Expose
    private Set<Identifier> identifiers = new LinkedHashSet<Identifier>();
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("creators")
    @Expose
    private Set<Creator> creators = new LinkedHashSet<Creator>();
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("titles")
    @Expose
    private Set<Title> titles = new LinkedHashSet<Title>();
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("publisher")
    @Expose
    private String publisher;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("publicationYear")
    @Expose
    private String publicationYear;
    @SerializedName("subjects")
    @Expose
    private Set<Subject> subjects = new LinkedHashSet<Subject>();
    @SerializedName("contributors")
    @Expose
    private Set<Contributor> contributors = new LinkedHashSet<Contributor>();
    @SerializedName("dates")
    @Expose
    private Set<Date> dates = new LinkedHashSet<Date>();
    /**
     * Primary language of the resource. Allowed values are taken from  IETF BCP 47, ISO 639-1 language codes.
     * 
     */
    @SerializedName("language")
    @Expose
    private String language;
    @SerializedName("alternateIdentifiers")
    @Expose
    private Set<AlternateIdentifier> alternateIdentifiers = new LinkedHashSet<AlternateIdentifier>();
    @SerializedName("relatedIdentifiers")
    @Expose
    private Set<RelatedIdentifier> relatedIdentifiers = new LinkedHashSet<RelatedIdentifier>();
    @SerializedName("sizes")
    @Expose
    private Set<String> sizes = new LinkedHashSet<String>();
    @SerializedName("formats")
    @Expose
    private Set<String> formats = new LinkedHashSet<String>();
    @SerializedName("version")
    @Expose
    private String version;
    @SerializedName("rightsList")
    @Expose
    private Set<Rights> rightsList = new LinkedHashSet<Rights>();
    @SerializedName("descriptions")
    @Expose
    private Set<Description> descriptions = new LinkedHashSet<Description>();
    @SerializedName("geoLocations")
    @Expose
    private Set<GeoLocation> geoLocations = new LinkedHashSet<GeoLocation>();
    @SerializedName("fundingReferences")
    @Expose
    private Set<FundingReference> fundingReferences = new LinkedHashSet<FundingReference>();
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("schemaVersion")
    @Expose
    private String schemaVersion;

    /**
     * 
     * (Required)
     * 
     */
    public Types getTypes() {
        return types;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTypes(Types types) {
        this.types = types;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Set<Identifier> getIdentifiers() {
        return identifiers;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setIdentifiers(Set<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Set<Creator> getCreators() {
        return creators;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setCreators(Set<Creator> creators) {
        this.creators = creators;
    }

    /**
     * 
     * (Required)
     * 
     */
    public Set<Title> getTitles() {
        return titles;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setTitles(Set<Title> titles) {
        this.titles = titles;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getPublicationYear() {
        return publicationYear;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public Set<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(Set<Subject> subjects) {
        this.subjects = subjects;
    }

    public Set<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(Set<Contributor> contributors) {
        this.contributors = contributors;
    }

    public Set<Date> getDates() {
        return dates;
    }

    public void setDates(Set<Date> dates) {
        this.dates = dates;
    }

    /**
     * Primary language of the resource. Allowed values are taken from  IETF BCP 47, ISO 639-1 language codes.
     * 
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Primary language of the resource. Allowed values are taken from  IETF BCP 47, ISO 639-1 language codes.
     * 
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<AlternateIdentifier> getAlternateIdentifiers() {
        return alternateIdentifiers;
    }

    public void setAlternateIdentifiers(Set<AlternateIdentifier> alternateIdentifiers) {
        this.alternateIdentifiers = alternateIdentifiers;
    }

    public Set<RelatedIdentifier> getRelatedIdentifiers() {
        return relatedIdentifiers;
    }

    public void setRelatedIdentifiers(Set<RelatedIdentifier> relatedIdentifiers) {
        this.relatedIdentifiers = relatedIdentifiers;
    }

    public Set<String> getSizes() {
        return sizes;
    }

    public void setSizes(Set<String> sizes) {
        this.sizes = sizes;
    }

    public Set<String> getFormats() {
        return formats;
    }

    public void setFormats(Set<String> formats) {
        this.formats = formats;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<Rights> getRightsList() {
        return rightsList;
    }

    public void setRightsList(Set<Rights> rightsList) {
        this.rightsList = rightsList;
    }

    public Set<Description> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Set<Description> descriptions) {
        this.descriptions = descriptions;
    }

    public Set<GeoLocation> getGeoLocations() {
        return geoLocations;
    }

    public void setGeoLocations(Set<GeoLocation> geoLocations) {
        this.geoLocations = geoLocations;
    }

    public Set<FundingReference> getFundingReferences() {
        return fundingReferences;
    }

    public void setFundingReferences(Set<FundingReference> fundingReferences) {
        this.fundingReferences = fundingReferences;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Datacite43Schema.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("types");
        sb.append('=');
        sb.append(((this.types == null)?"<null>":this.types));
        sb.append(',');
        sb.append("identifiers");
        sb.append('=');
        sb.append(((this.identifiers == null)?"<null>":this.identifiers));
        sb.append(',');
        sb.append("creators");
        sb.append('=');
        sb.append(((this.creators == null)?"<null>":this.creators));
        sb.append(',');
        sb.append("titles");
        sb.append('=');
        sb.append(((this.titles == null)?"<null>":this.titles));
        sb.append(',');
        sb.append("publisher");
        sb.append('=');
        sb.append(((this.publisher == null)?"<null>":this.publisher));
        sb.append(',');
        sb.append("publicationYear");
        sb.append('=');
        sb.append(((this.publicationYear == null)?"<null>":this.publicationYear));
        sb.append(',');
        sb.append("subjects");
        sb.append('=');
        sb.append(((this.subjects == null)?"<null>":this.subjects));
        sb.append(',');
        sb.append("contributors");
        sb.append('=');
        sb.append(((this.contributors == null)?"<null>":this.contributors));
        sb.append(',');
        sb.append("dates");
        sb.append('=');
        sb.append(((this.dates == null)?"<null>":this.dates));
        sb.append(',');
        sb.append("language");
        sb.append('=');
        sb.append(((this.language == null)?"<null>":this.language));
        sb.append(',');
        sb.append("alternateIdentifiers");
        sb.append('=');
        sb.append(((this.alternateIdentifiers == null)?"<null>":this.alternateIdentifiers));
        sb.append(',');
        sb.append("relatedIdentifiers");
        sb.append('=');
        sb.append(((this.relatedIdentifiers == null)?"<null>":this.relatedIdentifiers));
        sb.append(',');
        sb.append("sizes");
        sb.append('=');
        sb.append(((this.sizes == null)?"<null>":this.sizes));
        sb.append(',');
        sb.append("formats");
        sb.append('=');
        sb.append(((this.formats == null)?"<null>":this.formats));
        sb.append(',');
        sb.append("version");
        sb.append('=');
        sb.append(((this.version == null)?"<null>":this.version));
        sb.append(',');
        sb.append("rightsList");
        sb.append('=');
        sb.append(((this.rightsList == null)?"<null>":this.rightsList));
        sb.append(',');
        sb.append("descriptions");
        sb.append('=');
        sb.append(((this.descriptions == null)?"<null>":this.descriptions));
        sb.append(',');
        sb.append("geoLocations");
        sb.append('=');
        sb.append(((this.geoLocations == null)?"<null>":this.geoLocations));
        sb.append(',');
        sb.append("fundingReferences");
        sb.append('=');
        sb.append(((this.fundingReferences == null)?"<null>":this.fundingReferences));
        sb.append(',');
        sb.append("schemaVersion");
        sb.append('=');
        sb.append(((this.schemaVersion == null)?"<null>":this.schemaVersion));
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
        result = ((result* 31)+((this.types == null)? 0 :this.types.hashCode()));
        result = ((result* 31)+((this.formats == null)? 0 :this.formats.hashCode()));
        result = ((result* 31)+((this.schemaVersion == null)? 0 :this.schemaVersion.hashCode()));
        result = ((result* 31)+((this.rightsList == null)? 0 :this.rightsList.hashCode()));
        result = ((result* 31)+((this.identifiers == null)? 0 :this.identifiers.hashCode()));
        result = ((result* 31)+((this.creators == null)? 0 :this.creators.hashCode()));
        result = ((result* 31)+((this.subjects == null)? 0 :this.subjects.hashCode()));
        result = ((result* 31)+((this.dates == null)? 0 :this.dates.hashCode()));
        result = ((result* 31)+((this.language == null)? 0 :this.language.hashCode()));
        result = ((result* 31)+((this.titles == null)? 0 :this.titles.hashCode()));
        result = ((result* 31)+((this.relatedIdentifiers == null)? 0 :this.relatedIdentifiers.hashCode()));
        result = ((result* 31)+((this.version == null)? 0 :this.version.hashCode()));
        result = ((result* 31)+((this.descriptions == null)? 0 :this.descriptions.hashCode()));
        result = ((result* 31)+((this.geoLocations == null)? 0 :this.geoLocations.hashCode()));
        result = ((result* 31)+((this.alternateIdentifiers == null)? 0 :this.alternateIdentifiers.hashCode()));
        result = ((result* 31)+((this.sizes == null)? 0 :this.sizes.hashCode()));
        result = ((result* 31)+((this.publisher == null)? 0 :this.publisher.hashCode()));
        result = ((result* 31)+((this.publicationYear == null)? 0 :this.publicationYear.hashCode()));
        result = ((result* 31)+((this.contributors == null)? 0 :this.contributors.hashCode()));
        result = ((result* 31)+((this.fundingReferences == null)? 0 :this.fundingReferences.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Datacite43Schema) == false) {
            return false;
        }
        Datacite43Schema rhs = ((Datacite43Schema) other);
        return (((((((((((((((((((((this.types == rhs.types)||((this.types!= null)&&this.types.equals(rhs.types)))&&((this.formats == rhs.formats)||((this.formats!= null)&&this.formats.equals(rhs.formats))))&&((this.schemaVersion == rhs.schemaVersion)||((this.schemaVersion!= null)&&this.schemaVersion.equals(rhs.schemaVersion))))&&((this.rightsList == rhs.rightsList)||((this.rightsList!= null)&&this.rightsList.equals(rhs.rightsList))))&&((this.identifiers == rhs.identifiers)||((this.identifiers!= null)&&this.identifiers.equals(rhs.identifiers))))&&((this.creators == rhs.creators)||((this.creators!= null)&&this.creators.equals(rhs.creators))))&&((this.subjects == rhs.subjects)||((this.subjects!= null)&&this.subjects.equals(rhs.subjects))))&&((this.dates == rhs.dates)||((this.dates!= null)&&this.dates.equals(rhs.dates))))&&((this.language == rhs.language)||((this.language!= null)&&this.language.equals(rhs.language))))&&((this.titles == rhs.titles)||((this.titles!= null)&&this.titles.equals(rhs.titles))))&&((this.relatedIdentifiers == rhs.relatedIdentifiers)||((this.relatedIdentifiers!= null)&&this.relatedIdentifiers.equals(rhs.relatedIdentifiers))))&&((this.version == rhs.version)||((this.version!= null)&&this.version.equals(rhs.version))))&&((this.descriptions == rhs.descriptions)||((this.descriptions!= null)&&this.descriptions.equals(rhs.descriptions))))&&((this.geoLocations == rhs.geoLocations)||((this.geoLocations!= null)&&this.geoLocations.equals(rhs.geoLocations))))&&((this.alternateIdentifiers == rhs.alternateIdentifiers)||((this.alternateIdentifiers!= null)&&this.alternateIdentifiers.equals(rhs.alternateIdentifiers))))&&((this.sizes == rhs.sizes)||((this.sizes!= null)&&this.sizes.equals(rhs.sizes))))&&((this.publisher == rhs.publisher)||((this.publisher!= null)&&this.publisher.equals(rhs.publisher))))&&((this.publicationYear == rhs.publicationYear)||((this.publicationYear!= null)&&this.publicationYear.equals(rhs.publicationYear))))&&((this.contributors == rhs.contributors)||((this.contributors!= null)&&this.contributors.equals(rhs.contributors))))&&((this.fundingReferences == rhs.fundingReferences)||((this.fundingReferences!= null)&&this.fundingReferences.equals(rhs.fundingReferences))));
    }

}
