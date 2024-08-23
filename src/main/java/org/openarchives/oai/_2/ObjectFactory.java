//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2016.08.23 um 12:48:44 PM CEST 
//


package org.openarchives.oai._2;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.openarchives.oai._2 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _OAIPMH_QNAME = new QName("http://www.openarchives.org/OAI/2.0/", "OAI-PMH");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.openarchives.oai._2
     * 
     */
    public ObjectFactory() {
      // Generated constructor.
    }

    /**
     * Create an instance of {@link OAIPMHtype }
      * @return type of ...
    * 
     */
    public OAIPMHtype createOAIPMHtype() {
        return new OAIPMHtype();
    }

    /**
     * Create an instance of {@link AboutType }
     * @return type of ...
     * 
     */
    public AboutType createAboutType() {
        return new AboutType();
    }

    /**
     * Create an instance of {@link MetadataFormatType }
     * @return type of ...
     * 
     */
    public MetadataFormatType createMetadataFormatType() {
        return new MetadataFormatType();
    }

    /**
     * Create an instance of {@link ListRecordsType }
     * @return type of ...
     * 
     */
    public ListRecordsType createListRecordsType() {
        return new ListRecordsType();
    }

    /**
     * Create an instance of {@link ListMetadataFormatsType }
     * @return type of ...
     */
    public ListMetadataFormatsType createListMetadataFormatsType() {
        return new ListMetadataFormatsType();
    }

    /**
     * Create an instance of {@link DescriptionType }
     * @return type of ...
     * 
     */
    public DescriptionType createDescriptionType() {
        return new DescriptionType();
    }

    /**
     * Create an instance of {@link ResumptionTokenType }
     * @return type of ...
     * 
     */
    public ResumptionTokenType createResumptionTokenType() {
        return new ResumptionTokenType();
    }

    /**
     * Create an instance of {@link ListSetsType }
     * @return type of ...
     * 
     */
    public ListSetsType createListSetsType() {
        return new ListSetsType();
    }

    /**
     * Create an instance of {@link ListIdentifiersType }
     * @return type of ...
     * 
     */
    public ListIdentifiersType createListIdentifiersType() {
        return new ListIdentifiersType();
    }

    /**
     * Create an instance of {@link RequestType }
     * @return type of ...
     * 
     */
    public RequestType createRequestType() {
        return new RequestType();
    }

    /**
     * Create an instance of {@link RecordType }
     * @return type of ...
     * 
     */
    public RecordType createRecordType() {
        return new RecordType();
    }

    /**
     * Create an instance of {@link GetRecordType }
     * @return type of ...
     * 
     */
    public GetRecordType createGetRecordType() {
        return new GetRecordType();
    }

    /**
     * Create an instance of {@link SetType }
     * @return type of ...
     * 
     */
    public SetType createSetType() {
        return new SetType();
    }

    /**
     * Create an instance of {@link IdentifyType }
     * @return type of ...
     * 
     */
    public IdentifyType createIdentifyType() {
        return new IdentifyType();
    }

    /**
     * Create an instance of {@link OAIPMHerrorType }
     * @return type of ...
     * 
     */
    public OAIPMHerrorType createOAIPMHerrorType() {
        return new OAIPMHerrorType();
    }

    /**
     * Create an instance of {@link HeaderType }
      * @return type of ...
    * 
     */
    public HeaderType createHeaderType() {
        return new HeaderType();
    }

    /**
     * Create an instance of {@link MetadataType }
     * @return type of ...
     * 
     */
    public MetadataType createMetadataType() {
        return new MetadataType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link OAIPMHtype }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.openarchives.org/OAI/2.0/", name = "OAI-PMH")
    public JAXBElement<OAIPMHtype> createOAIPMH(OAIPMHtype value) {
        return new JAXBElement<>(_OAIPMH_QNAME, OAIPMHtype.class, null, value);
    }

}
