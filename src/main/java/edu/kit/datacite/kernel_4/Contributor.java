
package edu.kit.datacite.kernel_4;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Contributor {

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("contributorType")
    @Expose
    private Contributor.ContributorType contributorType;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("nameType")
    @Expose
    private edu.kit.datacite.kernel_4.Creator.NameType nameType;
    @SerializedName("givenName")
    @Expose
    private String givenName;
    @SerializedName("familyName")
    @Expose
    private String familyName;
    @SerializedName("nameIdentifiers")
    @Expose
    private Set<NameIdentifier> nameIdentifiers = new LinkedHashSet<NameIdentifier>();
    @SerializedName("affiliations")
    @Expose
    private Set<Affiliation> affiliations = new LinkedHashSet<Affiliation>();
    @SerializedName("lang")
    @Expose
    private String lang;

    /**
     * 
     * (Required)
     * 
     */
    public Contributor.ContributorType getContributorType() {
        return contributorType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setContributorType(Contributor.ContributorType contributorType) {
        this.contributorType = contributorType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    public edu.kit.datacite.kernel_4.Creator.NameType getNameType() {
        return nameType;
    }

    public void setNameType(edu.kit.datacite.kernel_4.Creator.NameType nameType) {
        this.nameType = nameType;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public Set<NameIdentifier> getNameIdentifiers() {
        return nameIdentifiers;
    }

    public void setNameIdentifiers(Set<NameIdentifier> nameIdentifiers) {
        this.nameIdentifiers = nameIdentifiers;
    }

    public Set<Affiliation> getAffiliations() {
        return affiliations;
    }

    public void setAffiliations(Set<Affiliation> affiliations) {
        this.affiliations = affiliations;
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
        sb.append(Contributor.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("contributorType");
        sb.append('=');
        sb.append(((this.contributorType == null)?"<null>":this.contributorType));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("nameType");
        sb.append('=');
        sb.append(((this.nameType == null)?"<null>":this.nameType));
        sb.append(',');
        sb.append("givenName");
        sb.append('=');
        sb.append(((this.givenName == null)?"<null>":this.givenName));
        sb.append(',');
        sb.append("familyName");
        sb.append('=');
        sb.append(((this.familyName == null)?"<null>":this.familyName));
        sb.append(',');
        sb.append("nameIdentifiers");
        sb.append('=');
        sb.append(((this.nameIdentifiers == null)?"<null>":this.nameIdentifiers));
        sb.append(',');
        sb.append("affiliations");
        sb.append('=');
        sb.append(((this.affiliations == null)?"<null>":this.affiliations));
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
        result = ((result* 31)+((this.nameType == null)? 0 :this.nameType.hashCode()));
        result = ((result* 31)+((this.givenName == null)? 0 :this.givenName.hashCode()));
        result = ((result* 31)+((this.familyName == null)? 0 :this.familyName.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.nameIdentifiers == null)? 0 :this.nameIdentifiers.hashCode()));
        result = ((result* 31)+((this.affiliations == null)? 0 :this.affiliations.hashCode()));
        result = ((result* 31)+((this.contributorType == null)? 0 :this.contributorType.hashCode()));
        result = ((result* 31)+((this.lang == null)? 0 :this.lang.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Contributor) == false) {
            return false;
        }
        Contributor rhs = ((Contributor) other);
        return (((((((((this.nameType == rhs.nameType)||((this.nameType!= null)&&this.nameType.equals(rhs.nameType)))&&((this.givenName == rhs.givenName)||((this.givenName!= null)&&this.givenName.equals(rhs.givenName))))&&((this.familyName == rhs.familyName)||((this.familyName!= null)&&this.familyName.equals(rhs.familyName))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.nameIdentifiers == rhs.nameIdentifiers)||((this.nameIdentifiers!= null)&&this.nameIdentifiers.equals(rhs.nameIdentifiers))))&&((this.affiliations == rhs.affiliations)||((this.affiliations!= null)&&this.affiliations.equals(rhs.affiliations))))&&((this.contributorType == rhs.contributorType)||((this.contributorType!= null)&&this.contributorType.equals(rhs.contributorType))))&&((this.lang == rhs.lang)||((this.lang!= null)&&this.lang.equals(rhs.lang))));
    }

    @Generated("jsonschema2pojo")
    public enum ContributorType {

        @SerializedName("ContactPerson")
        CONTACT_PERSON("ContactPerson"),
        @SerializedName("DataCollector")
        DATA_COLLECTOR("DataCollector"),
        @SerializedName("DataCurator")
        DATA_CURATOR("DataCurator"),
        @SerializedName("DataManager")
        DATA_MANAGER("DataManager"),
        @SerializedName("Distributor")
        DISTRIBUTOR("Distributor"),
        @SerializedName("Editor")
        EDITOR("Editor"),
        @SerializedName("HostingInstitution")
        HOSTING_INSTITUTION("HostingInstitution"),
        @SerializedName("Producer")
        PRODUCER("Producer"),
        @SerializedName("ProjectLeader")
        PROJECT_LEADER("ProjectLeader"),
        @SerializedName("ProjectManager")
        PROJECT_MANAGER("ProjectManager"),
        @SerializedName("ProjectMember")
        PROJECT_MEMBER("ProjectMember"),
        @SerializedName("RegistrationAgency")
        REGISTRATION_AGENCY("RegistrationAgency"),
        @SerializedName("RegistrationAuthority")
        REGISTRATION_AUTHORITY("RegistrationAuthority"),
        @SerializedName("RelatedPerson")
        RELATED_PERSON("RelatedPerson"),
        @SerializedName("Researcher")
        RESEARCHER("Researcher"),
        @SerializedName("ResearchGroup")
        RESEARCH_GROUP("ResearchGroup"),
        @SerializedName("RightsHolder")
        RIGHTS_HOLDER("RightsHolder"),
        @SerializedName("Sponsor")
        SPONSOR("Sponsor"),
        @SerializedName("Supervisor")
        SUPERVISOR("Supervisor"),
        @SerializedName("WorkPackageLeader")
        WORK_PACKAGE_LEADER("WorkPackageLeader"),
        @SerializedName("Other")
        OTHER("Other");
        private final String value;
        private final static Map<String, Contributor.ContributorType> CONSTANTS = new HashMap<String, Contributor.ContributorType>();

        static {
            for (Contributor.ContributorType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ContributorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        public String value() {
            return this.value;
        }

        public static Contributor.ContributorType fromValue(String value) {
            Contributor.ContributorType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
