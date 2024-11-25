# Introduction

In this documentation, the basics of the KIT Data Manager RESTful API of
the MetaStore Service are described. You will be guided through the
first steps of register an XML schema, update it. After that add an
appropriate metadata document to MetaStore which may be linked to a data
resource.

This documentation assumes, that you have an instance of the KIT Data
Manager MetaStore repository service installed locally. If the
repository is running on another host or port you should change hostname
and/or port accordingly. Furthermore, the examples assume that you are
using the repository without authentication and authorization, which is
provided by another service. If you plan to use this optional service,
please refer to its documentation first to see how the examples in this
documentation have to be modified in order to work with authentication.
Typically, this should be achieved by simple adding an additional header
entry.

The example structure is identical for all examples below. Each example
starts with a CURL command that can be run by copy&paste to your
console/terminal window. The second part shows the HTTP request sent to
the server including arguments and required headers. Finally, the third
block shows the response comming from the server. In between, special
characteristics of the calls are explained together with additional,
optional arguments or alternative responses.

For technical reasons, all metadata resources shown in the examples
contain all fields, e.g. also empty lists or fields with value 'null'.
You may ignore most of them as long as they are not needed. Some of them
will be assigned by the server, others remain empty or null as long as
you don’t assign any value to them. All fields mandatory at creation
time are explained in the resource creation example.

## Building the URL

The URL for accessing the MetaStore REST endpoints is constructed as
follows:

1.  Protocol (e.g.: http, https)

2.  Host name (e.g.: localhost, www.example.org)

3.  Port (e.g.: 8040)

4.  Context path (e.g.: /metastore)

5.  Endpoint (e.g.: /api/v2/metadata)

For example, to list all the schema records in your local MetaStore, you
need to run the following in your browser:
<http://localhost:8040/metastore/api/v2/schemas/>

In former versions (&lt; 1.3.0), no context path was provided by
default.

# XML (Schema)

## Schema Registration and Management

In this first section, the handling of schema resources is explained. It
all starts with creating your first xml schema resource. The model of a
datacite record looks like this:

    {
      "id" : "...",
      "identifier" : {
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "givenName" : "..."
      } ],
      "titles" : [ {
        "value" : "..."
      } ],
      "publisher" : "...",
      "publicationYear" : "...",
      "resourceType" : {
        "value" : "...",
        "typeGeneral" : "..."
      },
      "dates" : [ {
        "value" : "...",
        "type" : "..."
      } ],
      "relatedIdentifiers" : [ {
        "value" : "...",
        "identifierType" : "...",
        "relationType" : "..."
      }} ],
      "alternateIdentifiers" : [ {
        "value" : "...",
        "identifierType" : "..."
      } ],
      "version" : "...",
      "rights": [
        {
          "schemeId": "",
          "schemeUri": ""
        }
      ],
      "lastUpdate" : "...",
      "state" : "...",
      "acls" : [ {
        "sid" : "...",
        "permission" : "..."
      } ]
    }

At least the following elements are expected to be provided by the user:

-   id: A unique label for the schema.

-   title: Any title for the schema.

In addition, ACL may be useful to make schema readable/editable by
others. This will be of interest while accessing/updating an existing
schema.(if authorization is enabled)

License URI is optional. It’s new since 1.4.2.

## Registering a Metadata Schema Document

The following example shows the creation of the first XSD schema only
providing mandatory fields mentioned above:

    schema-record-v2.json:
    {
      "id": "my_first_xsd",
      "titles": [
        {
          "value": "Title for my_first_xsd",
        }
      ]
    }

    schema.xsd:
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                xmlns="http://www.example.org/schema/xsd/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                elementFormDefault="qualified" attributeFormDefault="unqualified">

      <xs:element name="metadata">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="title" type="xs:string"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
    </xs:schema>

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'schema=@schema.xsd;type=application/xml' \
        -F 'record=@schema-record.json;type=application/json'

You can see, that most of the sent datacite record is empty. Only
(schema)Id and title are provided by the user. HTTP-wise the call looks
as follows:

    POST /metastore/api/v2/schemas/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=schema.xsd
    Content-Type: application/xml

    <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
            xmlns="http://www.example.org/schema/xsd/"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            elementFormDefault="qualified" attributeFormDefault="unqualified">

    <xs:element name="metadata">
      <xs:complexType>
        <xs:sequence>
          <xs:element name="title" type="xs:string"/>
        </xs:sequence>
      </xs:complexType>
    </xs:element>

    </xs:schema>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=schema-record.json
    Content-Type: application/json

    {"id":"my_first_xsd","identifier":null,"creators":[],"titles":[{"id":null,"value":"Title for my_first_xsd","titleType":null,"lang":null}],"publisher":null,"publicationYear":null,"resourceType":null,"subjects":[],"contributors":[],"dates":[],"relatedIdentifiers":[],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[],"sizes":[],"formats":[],"version":null,"rights":[],"fundingReferences":[],"lastUpdate":null,"state":null,"embargoDate":null,"acls":[]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'multpart/form-data' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1
    ETag: "1432044929"
    Content-Type: application/json
    Content-Length: 801

    {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:32.58Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

What you see is, that the datacite record looks different from the
original document. Some of the elements received a value by the server.
Furthermore, you’ll find an ETag header with the current ETag of the
resource. This value is returned by POST, GET and PUT calls and must be
provided for all calls modifying the resource, e.g. POST, PUT and
DELETE, in order to avoid conflicts.

There are two possible values for DataResource: *XML\_Schema* and
*JSON\_Schema* which depends on the format of the given schema document.
(XML in our case.) As the schema document has a defined structure
"MODEL" is used as 'typeGeneral'.

### Getting a Datacite Schema Record

For obtaining one datacite record you have to provide the value of the
field 'Id'.

As 'Accept' field you have to provide
'application/vnd.datacite.org+json' otherwise you will get the landing
page of the digital object schema instead.

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd' -i -X GET \
        -H 'Accept: application/vnd.datacite.org+json'

In the actual HTTP request just access the path of the resource using
the base path and the 'schemaId'. Be aware that you also have to provide
the 'Accept' field.

    GET /metastore/api/v2/schemas/my_first_xsd HTTP/1.1
    Accept: application/vnd.datacite.org+json
    Host: localhost:8040

As a result, you receive the datacite record send before and again the
corresponding ETag in the HTTP response header.

    HTTP/1.1 200 OK
    ETag: "1432044929"
    Content-Type: application/vnd.datacite.org+json
    Content-Length: 801

    {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:32.58Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

### Getting a Metadata Schema Document

For obtaining accessible metadata schemas you also have to provide the
'schemaId'. For accessing schema document you have to provide
'application/xml' as 'Accept' header.

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd' -i -X GET \
        -H 'Accept: application/xml'

In the actual HTTP request there is nothing special. You just access the
path of the resource using the base path and the 'schemaId'.

    GET /metastore/api/v2/schemas/my_first_xsd HTTP/1.1
    Accept: application/xml
    Host: localhost:8040

As a result, you receive the XSD schema send before.

    HTTP/1.1 200 OK
    Content-Type: application/xml
    Content-Length: 591
    Accept-Ranges: bytes

    <?xml version="1.0" encoding="UTF-8"?>
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/" xmlns="http://www.example.org/schema/xsd/" xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" attributeFormDefault="unqualified">

        <xs:element name="metadata">

            <xs:complexType>

                <xs:sequence>

                    <xs:element name="title" type="xs:string"/>

                </xs:sequence>

            </xs:complexType>

        </xs:element>

    </xs:schema>

### Updating a Metadata Schema Document (add mandatory 'date' field)

Updating a metadata schema document will not break old metadata
documents. As every update results in a new version 'old' metadata
schema documents are still available.

For updating an existing metadata schema and/or datacite record a valid
ETag is needed. The actual ETag is available via the HTTP GET call of
the datacite record (see above). Just send an HTTP POST with the updated
metadata schema document and/or datacite record.

    schema-v2.xsd:
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                xmlns="http://www.example.org/schema/xsd/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                elementFormDefault="qualified" attributeFormDefault="unqualified">

      <xs:element name="metadata">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="title" type="xs:string"/>
            <xs:element name="date" type="xs:date"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
    </xs:schema>

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "1432044929"' \
        -F 'schema=@schema-v2.xsd;type=application/xml'

HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/schemas/my_first_xsd HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "1432044929"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=schema-v2.xsd
    Content-Type: application/xml

    <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
            xmlns="http://www.example.org/schema/xsd/"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            elementFormDefault="qualified" attributeFormDefault="unqualified">

    <xs:element name="metadata">
      <xs:complexType>
        <xs:sequence>
          <xs:element name="title" type="xs:string"/>
          <xs:element name="date" type="xs:date"/>
        </xs:sequence>
      </xs:complexType>
    </xs:element>

    </xs:schema>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive the updated datacite record of the schema
document and in the HTTP response header the new location URL and the
ETag.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=2
    ETag: "1398092788"
    Content-Type: application/json
    Content-Length: 1011

    {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:32.695Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

### Updating a Metadata Schema Document (add optional 'note' field)

For updating existing metadata schema document we have to provide the
new ETag. Just send an HTTP POST with the updated metadata schema
document and/or datacite record.

    schema-v3.xsd:
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
                xmlns="http://www.example.org/schema/xsd/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                elementFormDefault="qualified" attributeFormDefault="unqualified">

      <xs:element name="metadata">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="title" type="xs:string"/>
            <xs:element name="date" type="xs:date"/>
            <xs:element name="note" type="xs:string" minOccurs="0"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
    </xs:schema>

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "1398092788"' \
        -F 'schema=@schema-v3.xsd;type=application/xml'

HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/schemas/my_first_xsd HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "1398092788"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=schema-v3.xsd
    Content-Type: application/xml

    <xs:schema targetNamespace="http://www.example.org/schema/xsd/"
            xmlns="http://www.example.org/schema/xsd/"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            elementFormDefault="qualified" attributeFormDefault="unqualified">

    <xs:element name="metadata">
      <xs:complexType>
        <xs:sequence>
          <xs:element name="title" type="xs:string"/>
          <xs:element name="date" type="xs:date"/>
          <xs:element name="note" type="xs:string" minOccurs="0"/>
        </xs:sequence>
      </xs:complexType>
    </xs:element>

    </xs:schema>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive the updated datacite record of the schema
document and in the HTTP response header the new location URL and the
ETag.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=3
    ETag: "-393230376"
    Content-Type: application/json
    Content-Length: 1011

    {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:32.731Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

The updated datacite record contains three modified fields:
'schemaVersion', 'lastUpdate' and 'schemaDocumentUri'.

## Registering another Metadata Schema Document

The following example shows the creation of another XSD schema only
providing mandatory fields mentioned above:

    another-schema-record.json:
    {
      "id": "another_xsd",
      "titles": [
        {
          "value": "Title for another_xsd",
        }
      ]
    }

    another-schema.xsd:
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/example"
            xmlns="http://www.example.org/schema/xsd/example"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            elementFormDefault="qualified" attributeFormDefault="unqualified">
      <xs:element name="metadata">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="description" type="xs:string"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
    </xs:schema>

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'schema=@another-schema.xsd;type=application/xml' \
        -F 'record=@another-schema-record.json;type=application/json'

HTTP-wise the call looks as follows:

    POST /metastore/api/v2/schemas/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=another-schema.xsd
    Content-Type: application/xml

    <xs:schema targetNamespace="http://www.example.org/schema/xsd/example"
            xmlns="http://www.example.org/schema/xsd/example"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            elementFormDefault="qualified" attributeFormDefault="unqualified">

    <xs:element name="metadata">
      <xs:complexType>
        <xs:sequence>
          <xs:element name="description" type="xs:string"/>
        </xs:sequence>
      </xs:complexType>
    </xs:element>

    </xs:schema>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=another-schema-record.json
    Content-Type: application/json

    {"id":"another_xsd","identifier":null,"creators":[],"titles":[{"id":null,"value":"Title for another_xsd","titleType":null,"lang":null}],"publisher":null,"publicationYear":null,"resourceType":null,"subjects":[],"contributors":[],"dates":[],"relatedIdentifiers":[],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[],"sizes":[],"formats":[],"version":null,"rights":[],"fundingReferences":[],"lastUpdate":null,"state":null,"embargoDate":null,"acls":[]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'multpart/form-data' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8040/metastore/api/v2/schemas/another_xsd?version=1
    ETag: "1370518322"
    Content-Type: application/json
    Content-Length: 799

    {
      "id" : "another_xsd",
      "identifier" : {
        "id" : 2,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 2,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 2,
        "value" : "Title for another_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 2,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 2,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 2,
        "value" : "another_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:32.747Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

Now there are two schemaIds registered in the metadata schema registry.

### Getting a List of Metadata Schema Records

For getting all accessible datacite records of schema documents type:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/' -i -X GET

Same for HTTP request:

    GET /metastore/api/v2/schemas/ HTTP/1.1
    Host: localhost:8040

As a result, you receive a list of datacite records.

    HTTP/1.1 200 OK
    Content-Range: 0-1/2
    Content-Type: application/json
    Content-Length: 1816

    [ {
      "id" : "another_xsd",
      "identifier" : {
        "id" : 2,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 2,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 2,
        "value" : "Title for another_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 2,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 2,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 2,
        "value" : "another_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:32.747Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:32.731Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

Only the current version of each schemaId is listed.

The header contains the field 'Content-Range" which displays delivered
indices and the maximum number of available schema records. If there are
more than 20 schemas registered you have to provide page and/or size as
additional query parameters.

-   page: Number of the page you want to get **(starting with page 0)**

-   size: Number of entries per page.

The modified HTTP request with pagination looks like follows:

    GET /metastore/api/v2/schemas/?page=0&size=20 HTTP/1.1
    Host: localhost:8040

### Getting a List of all Schema Records for a Specific SchemaId

If you want to obtain all versions of a specific schema you may add the
schemaId as a filter parameter. This may look like this:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/?schemaId=my_first_xsd' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/schemas/?schemaId=my_first_xsd HTTP/1.1
    Host: localhost:8040

As a result, you receive a list of datacite records in descending order.
(current version first)

    HTTP/1.1 200 OK
    Content-Range: 0-2/3
    Content-Type: application/json
    Content-Length: 2831

    [ {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:32.731Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:32.695Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:32.58Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

### Getting current Version of Metadata Schema Document

To get the current version of the metadata schema document just send an
HTTP GET with the linked 'schemaId':

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd' -i -X GET \
        -H 'Accept: application/xml'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/schemas/my_first_xsd HTTP/1.1
    Accept: application/xml
    Host: localhost:8040

As a result, you receive the XSD schema document sent before:

    HTTP/1.1 200 OK
    Content-Type: application/xml
    Content-Length: 767
    Accept-Ranges: bytes

    <?xml version="1.0" encoding="UTF-8"?>
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/" xmlns="http://www.example.org/schema/xsd/" xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" attributeFormDefault="unqualified">

        <xs:element name="metadata">

            <xs:complexType>

                <xs:sequence>

                    <xs:element name="title" type="xs:string"/>

                    <xs:element name="date" type="xs:date"/>

                    <xs:element name="note" type="xs:string" minOccurs="0"/>

                </xs:sequence>

            </xs:complexType>

        </xs:element>

    </xs:schema>

For accessing schema document you have to provide 'application/xml' as
'Accept' header.

### Getting a specific Version of Metadata Schema Document

To get a specific version of the metadata schema document just send an
HTTP GET with the linked 'schemaId' and the version number you are
looking for as query parameter:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1' -i -X GET \
        -H 'Accept: application/xml'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/schemas/my_first_xsd?version=1 HTTP/1.1
    Accept: application/xml
    Host: localhost:8040

As a result, you receive the initial XSD schema document (version 1).

    HTTP/1.1 200 OK
    Content-Type: application/xml
    Content-Length: 591
    Accept-Ranges: bytes

    <?xml version="1.0" encoding="UTF-8"?>
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/" xmlns="http://www.example.org/schema/xsd/" xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" attributeFormDefault="unqualified">

        <xs:element name="metadata">

            <xs:complexType>

                <xs:sequence>

                    <xs:element name="title" type="xs:string"/>

                </xs:sequence>

            </xs:complexType>

        </xs:element>

    </xs:schema>

As before you have to provide 'application/XML' as 'Accept' header.

### Validating Metadata Document

Before an ingest of metadata is made the metadata should be successfully
validated. Otherwise the ingest may be rejected. Select the schema and
the schemaVersion to validate given document.

    metadata-v3.xml:
    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My third XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 3 notes are allowed</example:note>
    </example:metadata>

On a first step validation with the old schema will be done:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd/validate?version=1' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'document=@metadata-v3.xml;type=application/xml'

Same for the HTTP request. The schemaVersion number is set by a query
parameter.

    POST /metastore/api/v2/schemas/my_first_xsd/validate?version=1 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v3.xml
    Content-Type: application/xml

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My third XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 3 notes are allowed</example:note>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive 422 as HTTP status and an error message holding
some information about the error.

    HTTP/1.1 422 Unprocessable Entity
    Content-Type: application/problem+json
    Content-Length: 314

    {
      "type" : "about:blank",
      "title" : "Unprocessable Entity",
      "status" : 422,
      "detail" : "Validation error: cvc-complex-type.2.4.d: Invalid content was found starting with element 'example:date'. No child element is expected at this point.",
      "instance" : "/metastore/api/v2/schemas/my_first_xsd/validate"
    }

The document holds a mandatory and an optional field introduced in the
second and third version of schema. Let’s try to validate with third
version of schema. Only version number will be different. (if no query
parameter is available the current version will be selected)

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd/validate' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'document=@metadata-v3.xml;type=application/xml'

Same for the HTTP request.

    POST /metastore/api/v2/schemas/my_first_xsd/validate HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v3.xml
    Content-Type: application/xml

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My third XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 3 notes are allowed</example:note>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

Everything should be fine now. As a result, you receive 204 as HTTP
status and no further content.

    HTTP/1.1 204 No Content

### Update Metadata Schema Record

In case of authorization it may be neccessary to update datacite record
to be accessible by others. To do so an update has to be made. In this
example we introduce a user called 'admin' and give him all rights.

    schema-record-v4.json
    {
      "id": "my_first_xsd",
      [...]
      "acls": [
        {
          "id": 1,
          "sid": "SELF",
          "permission": "ADMINISTRATE"
        },
        {
          "sid": "admin",
          "permission": "ADMINISTRATE"
        }
      ]
    }

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_xsd' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "-393230376"' \
        -F 'record=@schema-record-v4.json;type=application/json'

Same for the HTTP request.

    PUT /metastore/api/v2/schemas/my_first_xsd HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "-393230376"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=schema-record-v4.json
    Content-Type: application/json

    {"id":"my_first_xsd","identifier":{"id":1,"value":"(:tba)","identifierType":"DOI"},"creators":[{"id":1,"familyName":null,"givenName":"SELF","affiliations":[]}],"titles":[{"id":1,"value":"Title for my_first_xsd","titleType":null,"lang":null}],"publisher":"SELF","publicationYear":"2024","resourceType":{"id":1,"value":"XML_Schema","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[{"id":1,"value":"2024-11-22T14:26:32Z","type":"CREATED"}],"relatedIdentifiers":[{"id":1,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=2","relationType":"IS_NEW_VERSION_OF","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[{"id":1,"value":"my_first_xsd","identifierType":"INTERNAL"}],"sizes":[],"formats":[],"version":"3","rights":[],"fundingReferences":[],"lastUpdate":"2024-11-22T14:26:32.731Z","state":"VOLATILE","embargoDate":null,"acls":[{"id":1,"sid":"SELF","permission":"ADMINISTRATE"},{"id":null,"sid":"admin","permission":"ADMINISTRATE"}]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive 200 as HTTP status, the updated datacite record
and the updated ETag and location in the HTTP response header.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=3
    ETag: "137544821"
    Content-Type: application/json
    Content-Length: 1087

    {
      "id" : "my_first_xsd",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_xsd"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "XML_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:32Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_xsd?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_xsd",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:33.003Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      }, {
        "id" : 3,
        "sid" : "admin",
        "permission" : "ADMINISTRATE"
      } ]
    }

After the update the following fields has changed:

-   version number increased by one.

-   lastUpdate to the date of the last update (set by server)

-   acls additional ACL entry (set during update)

## Metadata Management

After registration of a schema metadata may be added to MetaStore. In
this section, the handling of metadata resources is explained. It all
starts with creating your first metadata resource. The model of a
datacite record looks like this:

    {
      "id" : "...",
      "identifier" : {
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "givenName" : "..."
      } ],
      "titles" : [ {
        "value" : "..."
      } ],
      "publisher" : "...",
      "publicationYear" : "...",
      "resourceType" : {
        "value" : "...",
        "typeGeneral" : "..."
      },
      "dates" : [ {
        "value" : "...",
        "type" : "..."
      } ],
      "relatedIdentifiers" : [ {
        "value" : "...",
        "identifierType" : "...",
        "relationType" : "..."
      }} ],
      "alternateIdentifiers" : [ {
        "value" : "...",
        "identifierType" : "..."
      } ],
      "version" : "...",
      "rights": [
        {
          "schemeId": "",
          "schemeUri": ""
        }
      ],
      "lastUpdate" : "...",
      "state" : "...",
      "acls" : [ {
        "sid" : "...",
        "permission" : "..."
      } ]
    }

At least the following elements have to be provided by the user:

-   title: Any title for the metadata document

-   resourceType: *XML\_Metadata' or 'JSON\_Metadata' and type 'MODEL*.

-   relatedIdentifier/schema: Link to the related schema.
    (identifierType: INTERNAL and URL are supported, relationType:
    IS\_DERIVED\_FROM)

-   relatedIdentifier/data: Link to the (data) resource.
    (identifierType: any, relationType: IS\_METADATA\_FOR)

In addition, ACL may be useful to make metadata editable by others.
(This will be of interest while updating an existing metadata)

If linked schema is identified by its schemaId the INTERNAL type has to
be used. It’s then linked to the current schema version at creation
time.

License URI is optional. It’s new since 1.4.2.

### Register/Ingest a Datacite Record with Metadata Document

The following example shows the creation of the first metadata document
and its datacite record only providing mandatory fields mentioned above:

    metadata-record.json:
    {
      "titles": [
        {
          "value": "Title of first XML metadata document",
        }
      ],
      "resourceType": {
        "value": "XML_Metadata",
        "typeGeneral": "MODEL"
      },
      "relatedIdentifiers": [
        {
          "identifierType": "URL",
          "value": "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1",
          "relationType": "IS_DERIVED_FROM"
        },
        {
          "identifierType": "URL",
          "value": "https://repo/anyResourceId",
          "relationType": "IS_METADATA_FOR"
        }
      ]
    }

    metadata.xml:
    <?xml version='1.0' encoding='utf-8'?>
      <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
    </example:metadata>

The schemaId used while registering metadata schema has to be used to
link the metadata with the approbriate metadata schema.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'record=@metadata-record.json;type=application/json' \
        -F 'document=@metadata.xml;type=application/xml'

You can see, that most of the sent datacite record is empty. Only
schemaId and relatedResource are provided by the user. HTTP-wise the
call looks as follows:

    POST /metastore/api/v2/metadata/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record.json
    Content-Type: application/json

    {"id":null,"identifier":null,"creators":[],"titles":[{"id":null,"value":"Title of first XML metadata document","titleType":null,"lang":null}],"publisher":null,"publicationYear":null,"resourceType":{"id":null,"value":"XML_Metadata","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[],"relatedIdentifiers":[{"id":null,"identifierType":"URL","value":"https://repo/anyResourceId","relationType":"IS_METADATA_FOR","scheme":null,"relatedMetadataScheme":null},{"id":null,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1","relationType":"HAS_METADATA","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[],"sizes":[],"formats":[],"version":null,"rights":[],"fundingReferences":[],"lastUpdate":null,"state":null,"embargoDate":null,"acls":[]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata.xml
    Content-Type: application/xml

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'multpart/form-data' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1
    ETag: "-1820103729"
    Content-Type: application/json
    Content-Length: 1203

    {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:33.023Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

What you see is, that the datacite record looks different from the
original document. Some of the elements received a value by the server.
In the header you’ll find a location URL to access the ingested metadata
and an ETag with the current ETag of the resource. This value is
returned by POST, GET and PUT calls and must be provided for all calls
modifying the resource, e.g. POST, PUT and DELETE, in order to avoid
conflicts.

### Accessing Metadata Document

For accessing the metadata the location URL provided before may be used.
The URL is compiled by the id of the metadata and its version.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1' -i -X GET \
        -H 'Accept: application/xml'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1 HTTP/1.1
    Accept: application/xml
    Host: localhost:8040

The linked metadata will be returned. The result is sent back to the
user and will look that way:

    HTTP/1.1 200 OK
    Content-Length: 198
    Accept-Ranges: bytes
    Content-Type: application/xml

    <?xml version="1.0" encoding="UTF-8"?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/">

        <example:title>My first XML document</example:title>

    </example:metadata>

What you see is, that the metadata is untouched.

For accessing metadata document you have to provide 'application/xml' as
'Accept' header.

### Accessing Datacite Record of Metadata Document

For accessing the datacite record the same URL as before has to be used.
The only difference is the content type. It has to be set to
"application/vnd.datacite.org+json". Then the command line looks like
this:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1' -i -X GET \
        -H 'Accept: application/vnd.datacite.org+json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1 HTTP/1.1
    Accept: application/vnd.datacite.org+json
    Host: localhost:8040

The linked metadata will be returned. The result is sent back to the
user and will look that way:

    HTTP/1.1 200 OK
    ETag: "-1820103729"
    Location: http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1
    Content-Type: application/vnd.datacite.org+json
    Content-Length: 1203

    {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:33.023Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

You also get the datacite record seen before.

### Updating a Datacite Record (edit ACL entries) & Metadata Document

The following example shows the update of the datacite record and
metadata document to a newer version of the schema. As mentioned before
the ETag is needed:

    metadata-record-v2.json:
    {
      "id": "3efdd6fe-429d-40a6-acff-c7c40631d508",
      [...]
      "relatedIdentifiers": [
        [...]
        {
          "id": 1,
          "identifierType": "URL",
          "value": "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=2",
          "relationType": "IS_DERIVED_FROM"
        }
      ],
      [...]
      "acls": [
        [...]
        {
          "sid": "guest",
          "permission": "READ"
        }
      ]
    }

    metadata-v2.xml:
    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My second XML document</example:title>
      <example:date>2018-07-02</example:date>
    </example:metadata>

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "-1820103729"' \
        -F 'record=@metadata-record-v2.json;type=application/json' \
        -F 'document=@metadata-v2.xml;type=application/xml'

You can see, that the schema was set to version 2 (allowing additional
field for date) and the ACL entry for "guest" was added. All other
properties are still the same. HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "-1820103729"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record-v2.json
    Content-Type: application/json

    {"id":"db2379c7-1952-406d-ae69-5a9a9d39c9c9","identifier":{"id":3,"value":"(:tba)","identifierType":"DOI"},"creators":[{"id":3,"familyName":null,"givenName":"SELF","affiliations":[]}],"titles":[{"id":3,"value":"Title of first XML metadata document","titleType":null,"lang":null}],"publisher":"SELF","publicationYear":"2024","resourceType":{"id":3,"value":"XML_Metadata","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[{"id":3,"value":"2024-11-22T14:26:33Z","type":"CREATED"}],"relatedIdentifiers":[{"id":2,"identifierType":"URL","value":"https://repo/anyResourceId","relationType":"IS_METADATA_FOR","scheme":null,"relatedMetadataScheme":null},{"id":3,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=2","relationType":"HAS_METADATA","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[{"id":3,"value":"db2379c7-1952-406d-ae69-5a9a9d39c9c9","identifierType":"INTERNAL"}],"sizes":[],"formats":[],"version":"1","rights":[],"fundingReferences":[],"lastUpdate":"2024-11-22T14:26:33.023Z","state":"VOLATILE","embargoDate":null,"acls":[{"id":null,"sid":"guest","permission":"READ"},{"id":4,"sid":"SELF","permission":"ADMINISTRATE"}]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v2.xml
    Content-Type: application/xml

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My second XML document</example:title>
      <example:date>2018-07-02</example:date>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

The response provides the updated datacite record:

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2
    ETag: "1547105945"
    Content-Type: application/json
    Content-Length: 1475

    {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=2",
        "relationType" : "HAS_METADATA"
      }, {
        "id" : 4,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:33.105Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

You will get the updated datacite record with the following changes: -
new schema (version) - an additional ACL entry - 'version' of record was
incremented by one - 'lastUpdate' was also modified by the server.

### Updating Datacite Record of Metadata Document & Document

Repeat the last step and update to the current version. As mentioned
before the ETag is needed. As the ETag has changed in the meanwhile you
first have to get the new ETag.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2' -i -X GET \
        -H 'Accept: application/vnd.datacite.org+json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2 HTTP/1.1
    Accept: application/vnd.datacite.org+json
    Host: localhost:8040

You will get the new datacite record with the new ETag.

    HTTP/1.1 200 OK
    ETag: "1547105945"
    Location: http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2
    Content-Type: application/vnd.datacite.org+json
    Content-Length: 1475

    {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=2",
        "relationType" : "HAS_METADATA"
      }, {
        "id" : 4,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:33.105Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

Now you can update metadata due to new version of schema using the new
Etag.

    metadata-record-v3.json:
    {
      "id": "3efdd6fe-429d-40a6-acff-c7c40631d508",
      [...]
      "relatedIdentifiers": [
        [...]
        {
          "id": 1,
          "identifierType": "INTERNAL",
          "value": "my_first_xsd",
          "relationType": "IS_DERIVED_FROM"
        }
      ],
      [...]
    }

In contrast to the previous update, the INTERNAL identifier is used.
This always refers to the latest version of the schema (in our case
version 3).

    metadata-v3.xml:
    <?xml version='1.0' encoding='utf-8'?>
      <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My third XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 3 notes are allowed</example:note>
    </example:metadata>

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "1547105945"' \
        -F 'record=@metadata-record-v3.json;type=application/json' \
        -F 'document=@metadata-v3.xml;type=application/xml'

HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "1547105945"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record-v3.json
    Content-Type: application/json

    {"id":"db2379c7-1952-406d-ae69-5a9a9d39c9c9","identifier":{"id":3,"value":"(:tba)","identifierType":"DOI"},"creators":[{"id":3,"familyName":null,"givenName":"SELF","affiliations":[]}],"titles":[{"id":3,"value":"Title of first XML metadata document","titleType":null,"lang":null}],"publisher":"SELF","publicationYear":"2024","resourceType":{"id":3,"value":"XML_Metadata","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[{"id":3,"value":"2024-11-22T14:26:33Z","type":"CREATED"}],"relatedIdentifiers":[{"id":2,"identifierType":"URL","value":"https://repo/anyResourceId","relationType":"IS_METADATA_FOR","scheme":null,"relatedMetadataScheme":null},{"id":3,"identifierType":"INTERNAL","value":"my_first_xsd","relationType":"HAS_METADATA","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[{"id":3,"value":"db2379c7-1952-406d-ae69-5a9a9d39c9c9","identifierType":"INTERNAL"}],"sizes":[],"formats":[],"version":"1","rights":[],"fundingReferences":[],"lastUpdate":"2024-11-22T14:26:33.023Z","state":"VOLATILE","embargoDate":null,"acls":[{"id":null,"sid":"guest","permission":"READ"},{"id":4,"sid":"SELF","permission":"ADMINISTRATE"}]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v3.xml
    Content-Type: application/xml

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My third XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 3 notes are allowed</example:note>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

You will get the new datacite record.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=3
    ETag: "1996473594"
    Content-Type: application/json
    Content-Length: 1475

    {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:33.149Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

Now you can access the updated metadata via the URI in the HTTP response
header.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=3' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=3 HTTP/1.1
    Host: localhost:8040

You will get the updated metadata.

    HTTP/1.1 200 OK
    Content-Length: 323
    Accept-Ranges: bytes
    Content-Type: application/json

    <?xml version="1.0" encoding="UTF-8"?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/">

        <example:title>My third XML document</example:title>

        <example:date>2018-07-02</example:date>

        <example:note>since version 3 notes are allowed</example:note>

    </example:metadata>

### Find a Datacite Record of Metadata Document

Search will find all current datacite records. There are some filters
available which may be combined. All filters for the datacite records
are set via query parameters. The following filters are allowed:

-   id

-   resourceId

-   from

-   until

The header contains the field 'Content-Range" which displays delivered
indices and the maximum number of available schema records. If there are
more than 20 datacite records registered you have to provide page and/or
size as additional query parameters.

-   page: Number of the page you want to get **(starting with page 0)**

-   size: Number of entries per page.

### Getting a List of all Datacite Records for a Specific Metadata Document

If you want to obtain all versions of a specific resource you may add
'id' as a filter parameter. This may look like this:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?id=db2379c7-1952-406d-ae69-5a9a9d39c9c9' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?id=db2379c7-1952-406d-ae69-5a9a9d39c9c9 HTTP/1.1
    Host: localhost:8040

As a result, you receive a list of datacite records in descending order.
(current version first)

    HTTP/1.1 200 OK
    Content-Range: 0-2/3
    Content-Type: application/json
    Content-Length: 4161

    [ {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:33.149Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=2",
        "relationType" : "HAS_METADATA"
      }, {
        "id" : 4,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:33.105Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=1",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:33.023Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

#### Find by resourceId

If you want to find all records belonging to an external resource.
MetaStore may hold multiple metadata documents per resource.
(Nevertheless only one per registered schema)

Command line:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?resoureId=https%3A%2F%2Frepo%2FanyResourceId' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?resoureId=https%3A%2F%2Frepo%2FanyResourceId HTTP/1.1
    Host: localhost:8040

You will get the current version of the datacite record(s).

    HTTP/1.1 200 OK
    Content-Range: 0-0/1
    Content-Type: application/json
    Content-Length: 1479

    [ {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:33.149Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

#### Find after a specific date

If you want to find all datacite records updated after a specific date.

Command line:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A33.209222545Z' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A33.209222545Z HTTP/1.1
    Host: localhost:8040

You will get the current version datacite records updated ln the last 2
hours.

    HTTP/1.1 200 OK
    Content-Range: 0-0/1
    Content-Type: application/json
    Content-Length: 1479

    [ {
      "id" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first XML metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "XML_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:33Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/db2379c7-1952-406d-ae69-5a9a9d39c9c9?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_xsd?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "db2379c7-1952-406d-ae69-5a9a9d39c9c9",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:33.149Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

#### Find in a specific date range

If you want to find all datacite records updated in a specific date
range.

Command line:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A33.209222545Z&until=2024-11-22T13%3A26%3A33.209219312Z' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A33.209222545Z&until=2024-11-22T13%3A26%3A33.209219312Z HTTP/1.1
    Host: localhost:8040

You will get an empty array as no datacite record exists in the given
range:

    HTTP/1.1 200 OK
    Content-Range: */0
    Content-Type: application/json
    Content-Length: 3

    [ ]

# JSON (Schema)

## Schema Registration and Management

In this section, the handling of json schema resources is explained. It
all starts with creating your first json schema resource. The model of a
datacite record looks like this:

    {
      "schemaId" : "...",
      "schemaVersion" : 1,
      "mimeType" : "...",
      "type" : "...",
      "createdAt" : "...",
      "lastUpdate" : "...",
      "acl" : [ {
        "id" : 1,
        "sid" : "...",
        "permission" : "..."
      } ],
      "licenseUri" : "...",
      "schemaDocumentUri" : "...",
      "schemaHash" : "...",
      "locked" : false
    }

At least the following elements are expected to be provided by the user:

-   schemaId: A unique label for the schema.

-   mimeType: The resource type must be assigned by the user. For JSON
    schemas this should be *application/json*

-   type: XML or JSON. For JSON schemas this should be *JSON*

In addition, ACL may be useful to make schema editable by others. (This
will be of interest while updating an existing schema)

License URI is optional. It’s new since 1.5.0.

## Registering a Metadata Schema Document

The following example shows the creation of the first json schema only
providing mandatory fields mentioned above:

    schema-record4json.json:
    {
      "schemaId" : "my_first_json",
      "type" : "JSON"
    }

    schema.json:
    {
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "$id": "http://www.example.org/schema/json",
      "type": "object",
      "title": "Json schema for tests",
      "default": {},
      "required": [
          "title"
      ],
      "properties": {
        "title": {
          "type": "string",
          "title": "Title",
          "description": "Title of object."
        }
      },
      "additionalProperties": false
    }

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'schema=@schema.json;type=application/json' \
        -F 'record=@schema-record4json.json;type=application/json'

You can see, that most of the sent datacite record is empty. Only
schemaId, mimeType and type are provided by the user. HTTP-wise the call
looks as follows:

    POST /metastore/api/v2/schemas/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=schema.json
    Content-Type: application/json

    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json",
        "type": "object",
        "title": "Json schema for tests",
        "default": {},
        "required": [
            "title"
        ],
        "properties": {
            "title": {
                "type": "string",
                "title": "Title",
                "description": "Title of object."
            }
        },
        "additionalProperties": false
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=schema-record4json.json
    Content-Type: application/json

    {"id":"my_first_json","identifier":null,"creators":[],"titles":[{"id":null,"value":"Title for my_first_json","titleType":null,"lang":null}],"publisher":null,"publicationYear":null,"resourceType":null,"subjects":[],"contributors":[],"dates":[],"relatedIdentifiers":[],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[],"sizes":[],"formats":[],"version":null,"rights":[],"fundingReferences":[],"lastUpdate":null,"state":null,"embargoDate":null,"acls":[]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'multpart/form-data' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1
    ETag: "-759012511"
    Content-Type: application/json
    Content-Length: 806

    {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:26.664Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

What you see is, that the datacite record looks different from the
original document. Some of the elements received a value by the server.
Furthermore, you’ll find an ETag header with the current ETag of the
resource. This value is returned by POST, GET and PUT calls and must be
provided for all calls modifying the resource, e.g. POST, PUT and
DELETE, in order to avoid conflicts.

There are two possible values for DataResource: *XML\_Schema* and
*JSON\_Schema* which depends on the format of the given schema document.
(JSON in our case.) As the schema document has a defined structure
"MODEL" is used as 'typeGeneral'.

### Getting a Datacite Schema Record

For obtaining one datacite record you have to provide the value of the
field 'schemaId'.

As 'Accept' field you have to provide
'application/vnd.datacite.org+json' otherwise you will get the landing
page of the digital object instead.

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json' -i -X GET \
        -H 'Accept: application/vnd.datacite.org+json'

In the actual HTTP request just access the path of the resource using
the base path and the 'schemaId'. Be aware that you also have to provide
the 'Accept' field.

    GET /metastore/api/v2/schemas/my_first_json HTTP/1.1
    Accept: application/vnd.datacite.org+json
    Host: localhost:8040

As a result, you receive the datacite record send before and again the
corresponding ETag in the HTTP response header.

    HTTP/1.1 200 OK
    ETag: "-759012511"
    Content-Type: application/vnd.datacite.org+json
    Content-Length: 806

    {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:26.664Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

### Getting a Metadata Schema Document

For obtaining accessible metadata schemas you also have to provide the
'schemaId'. For accessing schema document you have to provide
'application/xml' as 'Accept' header.

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json' -i -X GET \
        -H 'Accept: application/json'

In the actual HTTP request there is nothing special. You just access the
path of the resource using the base path and the 'schemaId'.

    GET /metastore/api/v2/schemas/my_first_json HTTP/1.1
    Accept: application/json
    Host: localhost:8040

As a result, you receive the XSD schema send before.

    HTTP/1.1 200 OK
    Content-Type: text/plain
    Content-Length: 388
    Accept-Ranges: bytes

    {
      "$schema" : "https://json-schema.org/draft/2020-12/schema",
      "$id" : "http://www.example.org/schema/json",
      "type" : "object",
      "title" : "Json schema for tests",
      "default" : { },
      "required" : [ "title" ],
      "properties" : {
        "title" : {
          "type" : "string",
          "title" : "Title",
          "description" : "Title of object."
        }
      },
      "additionalProperties" : false
    }

### Updating a Metadata Schema Document (add mandatory 'date' field)

Updating a metadata schema document will not break old metadata
documents. As every update results in a new version 'old' metadata
schema documents are still available.

For updating an existing metadata schema (record) a valid ETag is
needed. The actual ETag is available via the HTTP GET call of the
datacite record (see above). Just send an HTTP POST with the updated
metadata schema document and/or datacite record.

    schema-v2.json:
    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json",
        "type": "object",
        "title": "Json schema for tests",
        "default": {},
        "required": [
            "title",
            "date"
        ],
        "properties": {
            "title": {
                "type": "string",
                "title": "Title",
                "description": "Title of object."
            },
            "date": {
                "type": "string",
                "format": "date",
                "title": "Date",
                "description": "Date of object"
            }
        },
        "additionalProperties": false
    }

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "-759012511"' \
        -F 'schema=@schema-v2.json;type=application/json'

HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/schemas/my_first_json HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "-759012511"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=schema-v2.json
    Content-Type: application/json

    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json",
        "type": "object",
        "title": "Json schema for tests",
        "default": {},
        "required": [
            "title",
            "date"
        ],
        "properties": {
            "title": {
                "type": "string",
                "title": "Title",
                "description": "Title of object."
            },
            "date": {
                "type": "string",
                "format": "date",
                "title": "Date",
                "description": "Date of object"
            }
        },
        "additionalProperties": false
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive the updated datacite record of the schema
document and in the HTTP response header the new location URL and the
ETag.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=2
    ETag: "575519683"
    Content-Type: application/json
    Content-Length: 1016

    {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:26.854Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

### Updating a Metadata Schema Document (add optional 'note' field)

For updating existing metadata schema document we have to provide the
new ETag. Just send an HTTP POST with the updated metadata schema
document and/or datacite record.

    schema-v3.json:
    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json",
        "type": "object",
        "title": "Json schema for tests",
        "default": {},
        "required": [
            "title",
            "date"
        ],
        "properties": {
            "title": {
                "type": "string",
                "title": "Title",
                "description": "Title of object."
            },
            "date": {
                "type": "string",
                "format": "date",
                "title": "Date",
                "description": "Date of object"
            },
            "note": {
                "type": "string",
                "title": "Note",
                "description": "Additonal information about object"
            }
        },
        "additionalProperties": false
    }

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "575519683"' \
        -F 'schema=@schema-v3.json;type=application/json'

HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/schemas/my_first_json HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "575519683"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=schema-v3.json
    Content-Type: application/json

    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json",
        "type": "object",
        "title": "Json schema for tests",
        "default": {},
        "required": [
            "title",
            "date"
        ],
        "properties": {
            "title": {
                "type": "string",
                "title": "Title",
                "description": "Title of object."
            },
            "date": {
                "type": "string",
                "format": "date",
                "title": "Date",
                "description": "Date of object"
            },
            "note": {
                "type": "string",
                "title": "Note",
                "description": "Additonal information about object"
            }
        },
        "additionalProperties": false
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive the updated datacite record of the schema
document and in the HTTP response header the new location URL and the
ETag.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3
    ETag: "254981415"
    Content-Type: application/json
    Content-Length: 1015

    {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:26.94Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

The updated datacite record contains three modified fields:
'schemaVersion', 'lastUpdate' and 'schemaDocumentUri'.

## Registering another Metadata Schema Document

The following example shows the creation of another json schema only
providing mandatory fields mentioned above:

    another-schema-record4json.json:
    {
      "schemaId" : "another_json",
      "type" : "JSON"
    }

    another-schema.json:
    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json/example",
        "type": "object",
        "title": "Another Json schema for tests",
        "default": {},
        "required": [
            "description"
        ],
        "properties": {
            "description": {
                "type": "string",
                "title": "Description",
                "description": "Any description."
            }
        },
        "additionalProperties": false
    }

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'schema=@another-schema.json;type=application/xml' \
        -F 'record=@another-schema-record.json;type=application/json'

HTTP-wise the call looks as follows:

    POST /metastore/api/v2/schemas/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema; filename=another-schema.json
    Content-Type: application/xml

    {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "http://www.example.org/schema/json/example",
        "type": "object",
        "title": "Another Json schema for tests",
        "default": {},
        "required": [
            "description"
        ],
        "properties": {
            "description": {
                "type": "string",
                "title": "Description",
                "description": "Any description."
            }
        },
        "additionalProperties": false
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=another-schema-record.json
    Content-Type: application/json

    {"id":"another_json","identifier":null,"creators":[],"titles":[{"id":null,"value":"Title for another_json","titleType":null,"lang":null}],"publisher":null,"publicationYear":null,"resourceType":null,"subjects":[],"contributors":[],"dates":[],"relatedIdentifiers":[],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[],"sizes":[],"formats":[],"version":null,"rights":[],"fundingReferences":[],"lastUpdate":null,"state":null,"embargoDate":null,"acls":[]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'multpart/form-data' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8040/metastore/api/v2/schemas/another_json?version=1
    ETag: "-1932760524"
    Content-Type: application/json
    Content-Length: 799

    {
      "id" : "another_json",
      "identifier" : {
        "id" : 2,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 2,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 2,
        "value" : "Title for another_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 2,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 2,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 2,
        "value" : "another_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:27Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

Now there are two schemaIds registered in the metadata schema registry.

### Getting a List of Metadata Schema Records

For getting all accessible datacite records of schema documents type:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/' -i -X GET

Same for HTTP request:

    GET /metastore/api/v2/schemas/ HTTP/1.1
    Host: localhost:8040

As a result, you receive a list of datacite records.

    HTTP/1.1 200 OK
    Content-Range: 0-1/2
    Content-Type: application/json
    Content-Length: 1820

    [ {
      "id" : "another_json",
      "identifier" : {
        "id" : 2,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 2,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 2,
        "value" : "Title for another_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 2,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 2,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 2,
        "value" : "another_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:27Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:26.94Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

Only the current version of each schemaId is listed.

The header contains the field 'Content-Range" which displays delivered
indices and the maximum number of available schema records. If there are
more than 20 schemas registered you have to provide page and/or size as
additional query parameters.

-   page: Number of the page you want to get **(starting with page 0)**

-   size: Number of entries per page.

The modified HTTP request with pagination looks like follows:

    GET /metastore/api/v2/schemas/?page=0&size=20 HTTP/1.1
    Host: localhost:8040

### Getting a List of all Schema Records for a Specific SchemaId

If you want to obtain all versions of a specific schema you may add the
schemaId as a filter parameter. This may look like this:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/?schemaId=my_first_json' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/schemas/?schemaId=my_first_json HTTP/1.1
    Host: localhost:8040

As a result, you receive a list of datacite records in descending order.
(current version first)

    HTTP/1.1 200 OK
    Content-Range: 0-2/3
    Content-Type: application/json
    Content-Length: 2845

    [ {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:26.94Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:26.854Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:26.664Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

### Getting current Version of Metadata Schema Document

To get the current version of the metadata schema document just send an
HTTP GET with the linked 'schemaId':

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json' -i -X GET \
        -H 'Accept: application/json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/schemas/my_first_json HTTP/1.1
    Accept: application/json
    Host: localhost:8040

As a result, you receive the XSD schema document sent before:

    HTTP/1.1 200 OK
    Content-Type: text/plain
    Content-Length: 661
    Accept-Ranges: bytes

    {
      "$schema" : "https://json-schema.org/draft/2020-12/schema",
      "$id" : "http://www.example.org/schema/json",
      "type" : "object",
      "title" : "Json schema for tests",
      "default" : { },
      "required" : [ "title", "date" ],
      "properties" : {
        "title" : {
          "type" : "string",
          "title" : "Title",
          "description" : "Title of object."
        },
        "date" : {
          "type" : "string",
          "format" : "date",
          "title" : "Date",
          "description" : "Date of object"
        },
        "note" : {
          "type" : "string",
          "title" : "Note",
          "description" : "Additonal information about object"
        }
      },
      "additionalProperties" : false
    }

For accessing schema document you have to provide 'application/json' as
'Accept' header.

### Getting a specific Version of Metadata Schema Document

To get a specific version of the metadata schema document just send an
HTTP GET with the linked 'schemaId' and the version number you are
looking for as query parameter:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1' -i -X GET \
        -H 'Accept: application/json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/schemas/my_first_json?version=1 HTTP/1.1
    Accept: application/json
    Host: localhost:8040

As a result, you receive the initial XSD schema document (version 1).

    HTTP/1.1 200 OK
    Content-Type: text/plain
    Content-Length: 388
    Accept-Ranges: bytes

    {
      "$schema" : "https://json-schema.org/draft/2020-12/schema",
      "$id" : "http://www.example.org/schema/json",
      "type" : "object",
      "title" : "Json schema for tests",
      "default" : { },
      "required" : [ "title" ],
      "properties" : {
        "title" : {
          "type" : "string",
          "title" : "Title",
          "description" : "Title of object."
        }
      },
      "additionalProperties" : false
    }

As before you have to provide 'application/json' as 'Accept' header.

### Validating Metadata Document

Before an ingest of metadata is made the metadata should be successfully
validated. Otherwise the ingest may be rejected. Select the schema and
the schemaVersion to validate given document.

    metadata-v3.json:
    {
    "title": "My third JSON document",
    "date": "2018-07-02",
    "note": "since version 3 notes are allowed"
    }

On a first step validation with the old schema will be done:

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json/validate?version=1' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'document=@metadata-v3.json;type=application/json'

Same for the HTTP request. The schemaVersion number is set by a query
parameter.

    POST /metastore/api/v2/schemas/my_first_json/validate?version=1 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v3.json
    Content-Type: application/json

    {
    "title": "My third JSON document",
    "date": "2018-07-02",
    "note": "since version 3 notes are allowed"
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive 422 as HTTP status and an error message holding
some information about the error.

    HTTP/1.1 422 Unprocessable Entity
    Content-Type: application/problem+json
    Content-Length: 432

    {
      "type" : "about:blank",
      "title" : "Unprocessable Entity",
      "status" : 422,
      "detail" : "400 BAD_REQUEST \"Error validating json!\n$: Eigenschaft 'date' ist im Schema nicht definiert und das Schema lässt keine zusätzlichen Eigenschaften zu\n$: Eigenschaft 'note' ist im Schema nicht definiert und das Schema lässt keine zusätzlichen Eigenschaften zu\"",
      "instance" : "/metastore/api/v2/schemas/my_first_json/validate"
    }

The document holds a mandatory and an optional field introduced in the
second and third version of schema. Let’s try to validate with third
version of schema. Only version number will be different. (if no query
parameter is available the current version will be selected)

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json/validate' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'document=@metadata-v3.json;type=application/json'

Same for the HTTP request.

    POST /metastore/api/v2/schemas/my_first_json/validate HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v3.json
    Content-Type: application/json

    {
    "title": "My third JSON document",
    "date": "2018-07-02",
    "note": "since version 3 notes are allowed"
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

Everything should be fine now. As a result, you receive 204 as HTTP
status and no further content.

    HTTP/1.1 204 No Content

### Update Metadata Schema Record

In case of authorization it may be neccessary to update datacite record
to be accessible by others. To do so an update has to be made. In this
example we introduce a user called 'admin' and give him all rights.

    schema-record4json-v4.json
    {
      "id": "my_first_json",
      [...]
      "acls": [
        {
          "id": 1,
          "sid": "SELF",
          "permission": "ADMINISTRATE"
        },
        {
          "sid": "admin",
          "permission": "ADMINISTRATE"
        }
      ]
    }

    $ curl 'http://localhost:8040/metastore/api/v2/schemas/my_first_json' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "254981415"' \
        -F 'record=@schema-record4json-v4.json;type=application/json'

Same for the HTTP request.

    PUT /metastore/api/v2/schemas/my_first_json HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "254981415"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=schema-record4json-v4.json
    Content-Type: application/json

    {"id":"my_first_json","identifier":{"id":1,"value":"(:tba)","identifierType":"DOI"},"creators":[{"id":1,"familyName":null,"givenName":"SELF","affiliations":[]}],"titles":[{"id":1,"value":"Title for my_first_json","titleType":null,"lang":null}],"publisher":"SELF","publicationYear":"2024","resourceType":{"id":1,"value":"JSON_Schema","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[{"id":1,"value":"2024-11-22T14:26:26Z","type":"CREATED"}],"relatedIdentifiers":[{"id":1,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=2","relationType":"IS_NEW_VERSION_OF","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[{"id":1,"value":"my_first_json","identifierType":"INTERNAL"}],"sizes":[],"formats":[],"version":"3","rights":[],"fundingReferences":[],"lastUpdate":"2024-11-22T14:26:26.94Z","state":"VOLATILE","embargoDate":null,"acls":[{"id":1,"sid":"SELF","permission":"ADMINISTRATE"},{"id":null,"sid":"admin","permission":"ADMINISTRATE"}]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive 200 as HTTP status, the updated datacite record
and the updated ETag and location in the HTTP response header.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3
    ETag: "395896196"
    Content-Type: application/json
    Content-Length: 1092

    {
      "id" : "my_first_json",
      "identifier" : {
        "id" : 1,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 1,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 1,
        "value" : "Title for my_first_json"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 1,
        "value" : "JSON_Schema",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 1,
        "value" : "2024-11-22T14:26:26Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 1,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/my_first_json?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 1,
        "value" : "my_first_json",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:27.409Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      }, {
        "id" : 3,
        "sid" : "admin",
        "permission" : "ADMINISTRATE"
      } ]
    }

After the update the following fields has changed:

-   version number increased by one.

-   lastUpdate to the date of the last update (set by server)

-   acls additional ACL entry (set during update)

## Metadata Management

After registration of a schema metadata may be added to MetaStore. In
this section, the handling of metadata resources is explained. It all
starts with creating your first metadata resource. The model of a
datacite record is similar to the record of the schema document:

    {
      "id" : "...",
      "identifier" : {
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "givenName" : "..."
      } ],
      "titles" : [ {
        "value" : "..."
      } ],
      "publisher" : "...",
      "publicationYear" : "...",
      "resourceType" : {
        "value" : "...",
        "typeGeneral" : "..."
      },
      "dates" : [ {
        "value" : "...",
        "type" : "..."
      } ],
      "relatedIdentifiers" : [ {
        "value" : "...",
        "identifierType" : "...",
        "relationType" : "..."
      }} ],
      "alternateIdentifiers" : [ {
        "value" : "...",
        "identifierType" : "..."
      } ],
      "version" : "...",
      "rights": [
        {
          "schemeId": "",
          "schemeUri": ""
        }
      ],
      "lastUpdate" : "...",
      "state" : "...",
      "acls" : [ {
        "sid" : "...",
        "permission" : "..."
      } ]
    }

At least the following elements have to be provided by the user:

-   title: Any title for the metadata document

-   resourceType: *XML\_Metadata' or 'JSON\_Metadata' and type 'MODEL*.

-   relatedIdentifier/schema: Link to the related schema.
    (identifierType: INTERNAL and URL are supported, relationType:
    IS\_DERIVED\_FROM)

-   relatedIdentifier/data: Link to the (data) resource.
    (identifierType: any, relationType: IS\_METADATA\_FOR)

In addition, ACL may be useful to make metadata editable by others.
(This will be of interest while updating an existing metadata)

If linked schema is identified by its schemaId the INTERNAL type has to
be used. It’s then linked to the current schema version at creation
time.

License URI is optional. It’s new since 1.4.2.

### Register/Ingest a Datacite Record with Metadata Document

The following example shows the creation of the first metadata document
and its datacite record only providing mandatory fields mentioned above:

    metadata-record4json.json:
    {
      "titles": [
        {
          "value": "Title of first metadata document",
        }
      ],
      "publisher": null,
      "publicationYear": null,
      "resourceType": {
        "value": "JSON_Metadata",
        "typeGeneral": "MODEL"
      },
      "relatedIdentifiers": [
        {
          "identifierType": "URL",
          "value": "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1",
          "relationType": "IS_DERIVED_FROM"
        },
        {
          "identifierType": "URL",
          "value": "https://repo/anyResourceId",
          "relationType": "IS_METADATA_FOR"
        }
      ]
    }

    metadata.json:
    {
      "title": "My first JSON document"
    }

The schemaId used while registering metadata schema has to be used to
link the metadata with the approbriate metadata schema.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'record=@metadata-record4json.json;type=application/json' \
        -F 'document=@metadata.json;type=application/json'

You can see, that most of the sent datacite record is empty. Only
schemaId and relatedResource are provided by the user. HTTP-wise the
call looks as follows:

    POST /metastore/api/v2/metadata/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record4json.json
    Content-Type: application/json

    {"id":null,"identifier":null,"creators":[],"titles":[{"id":null,"value":"Title of first JSON metadata document","titleType":null,"lang":null}],"publisher":null,"publicationYear":null,"resourceType":{"id":null,"value":"JSON_Metadata","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[],"relatedIdentifiers":[{"id":null,"identifierType":"URL","value":"https://repo/anyResourceId","relationType":"IS_METADATA_FOR","scheme":null,"relatedMetadataScheme":null},{"id":null,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1","relationType":"HAS_METADATA","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[],"sizes":[],"formats":[],"version":null,"rights":[],"fundingReferences":[],"lastUpdate":null,"state":null,"embargoDate":null,"acls":[]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata.json
    Content-Type: application/json

    {
    "title": "My first JSON document"
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'multpart/form-data' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1
    ETag: "536402562"
    Content-Type: application/json
    Content-Length: 1205

    {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:27.43Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

What you see is, that the datacite record looks different from the
original document. Some of the elements received a value by the server.
In the header you’ll find a location URL to access the ingested metadata
and an ETag with the current ETag of the resource. This value is
returned by POST, GET and PUT calls and must be provided for all calls
modifying the resource, e.g. POST, PUT and DELETE, in order to avoid
conflicts.

### Accessing Metadata Document

For accessing the metadata the location URL provided before may be used.
The URL is compiled by the id of the metadata and its version.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1' -i -X GET \
        -H 'Accept: application/json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1 HTTP/1.1
    Accept: application/json
    Host: localhost:8040

The linked metadata will be returned. The result is sent back to the
user and will look that way:

    HTTP/1.1 200 OK
    Content-Length: 40
    Accept-Ranges: bytes
    Content-Type: application/json

    {
      "title" : "My first JSON document"
    }

What you see is, that the metadata is untouched.

For accessing metadata document you have to provide *application/json*
as 'Accept' header.

### Accessing Datacite Record of Metadata Document

For accessing the datacite record the same URL as before has to be used.
The only difference is the content type. It has to be set to
"application/vnd.datacite.org+json". Then the command line looks like
this:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1' -i -X GET \
        -H 'Accept: application/vnd.datacite.org+json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1 HTTP/1.1
    Accept: application/vnd.datacite.org+json
    Host: localhost:8040

The linked metadata will be returned. The result is sent back to the
user and will look that way:

    HTTP/1.1 200 OK
    ETag: "536402562"
    Location: http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1
    Content-Type: application/vnd.datacite.org+json
    Content-Length: 1205

    {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:27.43Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

You also get the datacite record seen before.

### Updating a Datacite Record of Metadata Document (edit ACL entries)

The following example shows the update of the datacite record. As
mentioned before the ETag is needed:

    metadata-record4json-v2.json:
    {
      "id": "d5439ccf-af4c-4727-b45b-1aa8b949d60e",
      [...]
      "relatedIdentifiers": [
        [...]
        {
          "id": 1,
          "identifierType": "URL",
          "value": "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=2",
          "relationType": "IS_DERIVED_FROM"
        }
      ],
      [...]
      "acls": [
        [...]
        {
          "sid": "guest",
          "permission": "READ"
        }
      ]
    }

    metadata-v2.json:
    {
    "title": "My second JSON document",
    "date": "2018-07-02"
    }

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "536402562"' \
        -F 'record=@metadata-record4json-v2.json;type=application/json' \
        -F 'document=@metadata-v2.json;type=application/xml'

You can see, that the schema was set to version 2 (allowing additional
field for date) and the ACL entry for "guest" was added. All other
properties are still the same. HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "536402562"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record4json-v2.json
    Content-Type: application/json

    {"id":"a7484515-ba87-46e2-8a21-a1600039346d","identifier":{"id":3,"value":"(:tba)","identifierType":"DOI"},"creators":[{"id":3,"familyName":null,"givenName":"SELF","affiliations":[]}],"titles":[{"id":3,"value":"Title of first JSON metadata document","titleType":null,"lang":null}],"publisher":"SELF","publicationYear":"2024","resourceType":{"id":3,"value":"JSON_Metadata","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[{"id":3,"value":"2024-11-22T14:26:27Z","type":"CREATED"}],"relatedIdentifiers":[{"id":2,"identifierType":"URL","value":"https://repo/anyResourceId","relationType":"IS_METADATA_FOR","scheme":null,"relatedMetadataScheme":null},{"id":3,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=2","relationType":"HAS_METADATA","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[{"id":3,"value":"a7484515-ba87-46e2-8a21-a1600039346d","identifierType":"INTERNAL"}],"sizes":[],"formats":[],"version":"1","rights":[],"fundingReferences":[],"lastUpdate":"2024-11-22T14:26:27.43Z","state":"VOLATILE","embargoDate":null,"acls":[{"id":null,"sid":"guest","permission":"READ"},{"id":4,"sid":"SELF","permission":"ADMINISTRATE"}]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v2.json
    Content-Type: application/xml

    {
    "title": "My second JSON document",
    "date": "2018-07-02"
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

The response provides the updated datacite record:

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2
    ETag: "2057586597"
    Content-Type: application/json
    Content-Length: 1478

    {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 4,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=2",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:27.556Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

You will get the updated datacite record with the following changes: -
new schema (version) - an additional ACL entry - 'version' of record was
incremented by one - 'lastUpdate' was also modified by the server.

### Updating Datacite Record of Metadata Document & Document

Repeat the last step and update to the current version. As mentioned
before the ETag is needed. As the ETag has changed in the meanwhile you
first have to get the new ETag.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2' -i -X GET \
        -H 'Accept: application/vnd.datacite.org+json'

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2 HTTP/1.1
    Accept: application/vnd.datacite.org+json
    Host: localhost:8040

You will get the new datacite record with the new ETag.

    HTTP/1.1 200 OK
    ETag: "2057586597"
    Location: http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2
    Content-Type: application/vnd.datacite.org+json
    Content-Length: 1478

    {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 4,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=2",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:27.556Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

Now you can update metadata due to new version of schema using the new
Etag.

    metadata-record4json-v3.json:
    {
      "id": "d5439ccf-af4c-4727-b45b-1aa8b949d60e",
      [...]
      "relatedIdentifiers": [
        [...]
        {
          "id": 1,
          "identifierType": "INTERNAL",
          "value": "my_first_json",
          "relationType": "IS_DERIVED_FROM"
        }
      ],
      [...]
      "acls": [
        [...]
        {
          "sid": "guest",
          "permission": "READ"
        }
      ]
    }

In contrast to the previous update, the INTERNAL identifier is used.
This always refers to the latest version of the schema (in our case
version 3).

    metadata-v3.json:
    {
    "title": "My third JSON document",
    "date": "2018-07-02",
    "note": "since version 3 notes are allowed"
    }

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "2057586597"' \
        -F 'record=@metadata-record4json-v3.json;type=application/json' \
        -F 'document=@metadata-v3.json;type=application/json'

HTTP-wise the call looks as follows:

    PUT /metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "2057586597"
    Host: localhost:8040

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record4json-v3.json
    Content-Type: application/json

    {"id":"a7484515-ba87-46e2-8a21-a1600039346d","identifier":{"id":3,"value":"(:tba)","identifierType":"DOI"},"creators":[{"id":3,"familyName":null,"givenName":"SELF","affiliations":[]}],"titles":[{"id":3,"value":"Title of first JSON metadata document","titleType":null,"lang":null}],"publisher":"SELF","publicationYear":"2024","resourceType":{"id":3,"value":"JSON_Metadata","typeGeneral":"MODEL"},"subjects":[],"contributors":[],"dates":[{"id":3,"value":"2024-11-22T14:26:27Z","type":"CREATED"}],"relatedIdentifiers":[{"id":2,"identifierType":"URL","value":"https://repo/anyResourceId","relationType":"IS_METADATA_FOR","scheme":null,"relatedMetadataScheme":null},{"id":3,"identifierType":"URL","value":"http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3","relationType":"HAS_METADATA","scheme":null,"relatedMetadataScheme":null}],"descriptions":[],"geoLocations":[],"language":null,"alternateIdentifiers":[{"id":3,"value":"a7484515-ba87-46e2-8a21-a1600039346d","identifierType":"INTERNAL"}],"sizes":[],"formats":[],"version":"1","rights":[],"fundingReferences":[],"lastUpdate":"2024-11-22T14:26:27.43Z","state":"VOLATILE","embargoDate":null,"acls":[{"id":null,"sid":"guest","permission":"READ"},{"id":4,"sid":"SELF","permission":"ADMINISTRATE"}]}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document; filename=metadata-v3.json
    Content-Type: application/json

    {
    "title": "My third JSON document",
    "date": "2018-07-02",
    "note": "since version 3 notes are allowed"
    }
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

You will get the new datacite record.

    HTTP/1.1 200 OK
    Location: http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=3
    ETag: "1038379398"
    Content-Type: application/json
    Content-Length: 1478

    {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:27.626Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }

Now you can access the updated metadata via the URI in the HTTP response
header.

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=3' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=3 HTTP/1.1
    Host: localhost:8040

You will get the updated metadata.

    HTTP/1.1 200 OK
    Content-Length: 113
    Accept-Ranges: bytes
    Content-Type: application/json

    {
      "title" : "My third JSON document",
      "date" : "2018-07-02",
      "note" : "since version 3 notes are allowed"
    }

### Find a Datacite Record of Metadata Document

Search will find all current datacite records. There are some filters
available which may be combined. All filters for the datacite records
are set via query parameters. The following filters are allowed:

-   id

-   resourceId

-   from

-   until

The header contains the field 'Content-Range" which displays delivered
indices and the maximum number of available schema records. If there are
more than 20 datacite records registered you have to provide page and/or
size as additional query parameters.

-   page: Number of the page you want to get **(starting with page 0)**

-   size: Number of entries per page.

### Getting a List of all Datacite Records for a Specific Metadata Document

If you want to obtain all versions of a specific resource you may add
'id' as a filter parameter. This may look like this:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?id=a7484515-ba87-46e2-8a21-a1600039346d' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?id=a7484515-ba87-46e2-8a21-a1600039346d HTTP/1.1
    Host: localhost:8040

As a result, you receive a list of datacite records in descending order.
(current version first)

    HTTP/1.1 200 OK
    Content-Range: 0-2/3
    Content-Type: application/json
    Content-Length: 4169

    [ {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:27.626Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 4,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=1",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=2",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "2",
      "lastUpdate" : "2024-11-22T14:26:27.556Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    }, {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=1",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "1",
      "lastUpdate" : "2024-11-22T14:26:27.43Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

#### Find by resourceId

If you want to find all records belonging to an external resource.
MetaStore may hold multiple metadata documents per resource.
(Nevertheless only one per registered schema)

Command line:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?resoureId=https%3A%2F%2Frepo%2FanyResourceId' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?resoureId=https%3A%2F%2Frepo%2FanyResourceId HTTP/1.1
    Host: localhost:8040

You will get the current version datacite record.

    HTTP/1.1 200 OK
    Content-Range: 0-0/1
    Content-Type: application/json
    Content-Length: 1482

    [ {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:27.626Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

#### Find after a specific date

If you want to find all datacite records updated after a specific date.

Command line:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A27.730520539Z' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A27.730520539Z HTTP/1.1
    Host: localhost:8040

You will get the current version datacite records updated ln the last 2
hours.

    HTTP/1.1 200 OK
    Content-Range: 0-0/1
    Content-Type: application/json
    Content-Length: 1482

    [ {
      "id" : "a7484515-ba87-46e2-8a21-a1600039346d",
      "identifier" : {
        "id" : 3,
        "value" : "(:tba)",
        "identifierType" : "DOI"
      },
      "creators" : [ {
        "id" : 3,
        "givenName" : "SELF"
      } ],
      "titles" : [ {
        "id" : 3,
        "value" : "Title of first JSON metadata document"
      } ],
      "publisher" : "SELF",
      "publicationYear" : "2024",
      "resourceType" : {
        "id" : 3,
        "value" : "JSON_Metadata",
        "typeGeneral" : "MODEL"
      },
      "dates" : [ {
        "id" : 3,
        "value" : "2024-11-22T14:26:27Z",
        "type" : "CREATED"
      } ],
      "relatedIdentifiers" : [ {
        "id" : 2,
        "identifierType" : "URL",
        "value" : "https://repo/anyResourceId",
        "relationType" : "IS_METADATA_FOR"
      }, {
        "id" : 5,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/metadata/a7484515-ba87-46e2-8a21-a1600039346d?version=2",
        "relationType" : "IS_NEW_VERSION_OF"
      }, {
        "id" : 3,
        "identifierType" : "URL",
        "value" : "http://localhost:8040/metastore/api/v2/schemas/my_first_json?version=3",
        "relationType" : "HAS_METADATA"
      } ],
      "alternateIdentifiers" : [ {
        "id" : 3,
        "value" : "a7484515-ba87-46e2-8a21-a1600039346d",
        "identifierType" : "INTERNAL"
      } ],
      "version" : "3",
      "lastUpdate" : "2024-11-22T14:26:27.626Z",
      "state" : "VOLATILE",
      "acls" : [ {
        "id" : 6,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ]
    } ]

#### Find in a specific date range

If you want to find all datacite records updated in a specific date
range.

Command line:

    $ curl 'http://localhost:8040/metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A27.730520539Z&until=2024-11-22T13%3A26%3A27.730516191Z' -i -X GET

HTTP-wise the call looks as follows:

    GET /metastore/api/v2/metadata/?from=2024-11-22T12%3A26%3A27.730520539Z&until=2024-11-22T13%3A26%3A27.730516191Z HTTP/1.1
    Host: localhost:8040

You will get an empty array as no datacite record exists in the given
range:

    HTTP/1.1 200 OK
    Content-Range: */0
    Content-Type: application/json
    Content-Length: 3

    [ ]

# Remarks on Working with Versions

While working with versions you should keep some particularities in
mind. Access to version is only possible for single resources. There is
e.g. no way to obtain all resources in version 2 from the server. If a
specific version of a resource is returned, the obtained ETag also
relates to this specific version. Therefore, you should NOT use this
ETag for any update operation as the operation will fail with response
code 412 (PRECONDITION FAILED). Consequently, it is also NOT allowed to
modify a format version of a resource. If you want to rollback to a
previous version, you should obtain the resource and submit a PUT
request of the entire document which will result in a new version equal
to the previous state unless there were changes you are not allowed to
apply (anymore), e.g. if permissions have changed.
