Introduction
============

In this documentation, the basics of the KIT Data Manager RESTful API of
the Metastore2 Service are described. You will be guided through the
first steps of register an XML schema, update it. After that add an
appropriate metadata document to metastore2 which may be linked to a
data resource.

This documentation assumes, that you have an instance of the KIT Data
Manager Metastore2 repository service installed locally. If the
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

Schema Registration and Management
==================================

In this first section, the handling of schema resources is explained. It
all starts with creating your first xml schema resource. The model of a
metadata schema record looks like this:

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
      "schemaDocumentUri" : "...",
      "schemaHash" : "...",
      "locked" : false
    }

At least the following elements are expected to be provided by the user:

-   schemaId: A unique label for the schema.

-   mimeType: The resource type must be assigned by the user. For XSD
    schemas this should be 'application/xml'

-   type: XML or JSON. For XSD schemas this should be 'XML'

In addition, ACL may be useful to make schema editable by others. (This
will be of interest while updating an existing schema)

Registering a Metadata Schema
-----------------------------

The following example shows the creation of the first xsd schema only
providing mandatory fields mentioned above:

    schema-record.json:
    {
      "schemaId" : "my_first_xsd",
      "mimeType" : "application/xml",
      "type" : "XML"
    }

    $ curl 'http://localhost:8080/api/v1/schemas/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'schema=<xs:schema targetNamespace="http://www.example.org/schema/xsd/"
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

    </xs:schema>' \
        -F 'record=@schema-record.json;type=application/json'

You can see, that most of the sent metadata schema record is empty. Only
schemaId, mimeType and type are provided by the user. HTTP-wise the call
looks as follows:

    POST /api/v1/schemas/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema

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
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=schema-record.json
    Content-Type: application/json

    {"schemaId":"my_first_xsd","schemaVersion":null,"label":null,"definition":null,"comment":null,"mimeType":"application/xml","type":"XML","createdAt":null,"lastUpdate":null,"acl":[],"schemaDocumentUri":null,"schemaHash":null,"locked":false}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'application/json' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8080/api/v1/schemas/my_first_xsd?version=1
    ETag: "605314824"
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 482

    {
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 1,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:53.9967342Z",
      "acl" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=1",
      "schemaHash" : "sha1:c227b2bf612264da33fe5a695b5450101ce9d766",
      "locked" : false
    }

What you see is, that the metadata schema record looks different from
the original document. All remaining elements received a value by the
server. Furthermore, you’ll find an ETag header with the current ETag of
the resource. This value is returned by POST, GET and PUT calls and must
be provided for all calls modifying the resource, e.g. POST, PUT and
DELETE, in order to avoid conflicts.

Getting a List of Metadata Schema Records
-----------------------------------------

Obtaining all accessible metadata schema records.

    $ curl 'http://localhost:8080/api/v1/schemas/' -i -X GET

In the actual HTTP request there is nothing special. You just access the
path of the resource using the base path.

    GET /api/v1/schemas/ HTTP/1.1
    Host: localhost:8080

As a result, you receive a list of metadata schema records.

    HTTP/1.1 200 OK
    Content-Range: 0-19/1
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 486

    [ {
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 1,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:53.9967342Z",
      "acl" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=1",
      "schemaHash" : "sha1:c227b2bf612264da33fe5a695b5450101ce9d766",
      "locked" : false
    } ]

The header contains the field 'Content-Range" which displays delivered
indices and the maximum number of available schema records. If there are
more than 20 schemata registered you have to provide page and/or size as
additional query parameters.

-   page: Number of the page you want to get **(starting with page 0)**

-   size: Number of entries per page.

The modified HTTP request with pagination looks like follows:

    GET /api/v1/schemas/?page=0&size=20 HTTP/1.1
    Host: localhost:8080

Getting a Metadata Schema Record
--------------------------------

For obtaining one metadata schema record you have to provide the value
of the field 'schemaId'.

As 'Accept' field you have to provide
'application/vnd.datamanager.schema-record+json' otherwise you will get
the metadata schema instead.

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd' -i -X GET \
        -H 'Accept: application/vnd.datamanager.schema-record+json'

In the actual HTTP request just access the path of the resource using
the base path and the 'schemaid'. Be aware that you also have to provide
the 'Accept' field.

    GET /api/v1/schemas/my_first_xsd HTTP/1.1
    Accept: application/vnd.datamanager.schema-record+json
    Host: localhost:8080

As a result, you receive the metadata schema record send before and
again the corresponding ETag in the HTTP response header.

    HTTP/1.1 200 OK
    ETag: "605314824"
    Content-Type: application/vnd.datamanager.schema-record+json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 482

    {
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 1,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:53.9967342Z",
      "acl" : [ {
        "id" : 1,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=1",
      "schemaHash" : "sha1:c227b2bf612264da33fe5a695b5450101ce9d766",
      "locked" : false
    }

Getting a Metadata Schema
-------------------------

For obtaining accessible metadata schemas you have multiple options:
list all resources, access a single resource using a known identifier or
search by example. The following example shows how to obtain a single
resource. You may also use the location URL provided during the
creation.

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd' -i -X GET

In the actual HTTP request there is nothing special. You just access the
path of the resource using the base path and the 'schemaId'.

    GET /api/v1/schemas/my_first_xsd HTTP/1.1
    Host: localhost:8080

As a result, you receive the XSD schema send before.

    HTTP/1.1 200 OK
    Content-Type: application/xml
    Content-Length: 691
    Accept-Ranges: bytes
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

    <?xml version="1.0" encoding="UTF-8"?>
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/" xmlns="http://www.example.org/schema/xsd/" xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" attributeFormDefault="unqualified">

        <xs:element name="metadata">

            <xs:complexType>

                <xs:sequence>

                    <xs:element name="title" type="xs:string"/>

                    <xs:element name="date" type="xs:date"/>

                </xs:sequence>

            </xs:complexType>

        </xs:element>

    </xs:schema>

Updating a Metadata Schema
--------------------------

This should be used with extreme caution. The new schema should only add
optional elements otherwise it will break old metadata records. Therefor
it’s not accessible from remote.

For updating an existing metadata schema (record) a valid ETag is
needed. The actual ETag is available via the HTTP GET call of the
metadata schema record. (see above) Just send an HTTP POST with the
updated metadata schema and/or metadata schema record.

    schema-record.json
    {
      "schemaId":"my_first_xsd",
      "mimeType":"application/xml",
      "type":"XML"
    }

    $ curl 'http://localhost:8080/api/v1/schemas/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "605314824"' \
        -F 'schema=<xs:schema targetNamespace="http://www.example.org/schema/xsd/"
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

    </xs:schema>' \
        -F 'record=@schema-record.json;type=application/json'

In the actual HTTP request there is nothing special. You just access the
path of the resource using the base path and the resource identifier.

    POST /api/v1/schemas/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "605314824"
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=schema

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
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=schema-record.json
    Content-Type: application/json

    {"schemaId":"my_first_xsd","schemaVersion":null,"label":null,"definition":null,"comment":null,"mimeType":"application/xml","type":"XML","createdAt":null,"lastUpdate":null,"acl":[],"schemaDocumentUri":null,"schemaHash":null,"locked":false}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive the XSD schema send before, schema record and
in the HTTP response header the new location URL and the ETag.

    HTTP/1.1 201 Created
    Location: http://localhost:8080/api/v1/schemas/my_first_xsd?version=2
    ETag: "-1476742390"
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 482

    {
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 2,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:54.7727347Z",
      "acl" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=2",
      "schemaHash" : "sha1:1baea3a07d95faea70707fcf46d114315613b970",
      "locked" : false
    }

Getting new Version of Metadata Schema
--------------------------------------

To get the new version of the metadata schema just send an HTTP GET with
the linked 'schemaId':

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd' -i -X GET

In the actual HTTP request there is nothing special. You just access the
path of the resource using the base path and the 'schemaId'.

    GET /api/v1/schemas/my_first_xsd HTTP/1.1
    Host: localhost:8080

As a result, you receive the XSD schema send before.

    HTTP/1.1 200 OK
    Content-Type: application/xml
    Content-Length: 789
    Accept-Ranges: bytes
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

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

Getting a specific Version of Metadata Schema
---------------------------------------------

To get a specific version of the metadata schema just send an HTTP GET
with the linked 'schemaId' and the version number you are looking for as
query parameter:

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd?version=1' -i -X GET

In the actual HTTP request there is also nothing special. You just
access the path of the resource using the base path and the 'schemaId'
with the version number as query parameter.

    GET /api/v1/schemas/my_first_xsd?version=1 HTTP/1.1
    Host: localhost:8080

As a result, you receive the initial XSD schema (version 1).

    HTTP/1.1 200 OK
    Content-Type: application/xml
    Content-Length: 691
    Accept-Ranges: bytes
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

    <?xml version="1.0" encoding="UTF-8"?>
    <xs:schema targetNamespace="http://www.example.org/schema/xsd/" xmlns="http://www.example.org/schema/xsd/" xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified" attributeFormDefault="unqualified">

        <xs:element name="metadata">

            <xs:complexType>

                <xs:sequence>

                    <xs:element name="title" type="xs:string"/>

                    <xs:element name="date" type="xs:date"/>

                </xs:sequence>

            </xs:complexType>

        </xs:element>

    </xs:schema>

Validating Metadata Document
----------------------------

Before an ingest of metadata is made the metadata should be successfully
validated. Otherwise the ingest may be rejected. Select the schema and
the version to validate given document. On a first step validation with
the old schema will be done:

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd/validate?version=1' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'document=<?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 2 notes are allowed</example:note>
    </example:metadata>' \
        -F 'version=1'

Same for the HTTP request. The version number is set by a query
parameter.

    POST /api/v1/schemas/my_first_xsd/validate?version=1 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=version

    1
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 2 notes are allowed</example:note>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As a result, you receive 422 as HTTP status and an error message holding
some information about the error.

    HTTP/1.1 422 Unprocessable Entity
    Content-Type: text/plain;charset=UTF-8
    Content-Length: 149
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

    Validation error: cvc-complex-type.2.4.d: Invalid content was found starting with element 'example:note'. No child element is expected at this point.

The document holds an optional field introduced in the second version of
schema. Let’s try to validate with second version of schema. Only
version number will be different. (if no query parameter is available
the current version will be selected)

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd/validate' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'document=<?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 2 notes are allowed</example:note>
    </example:metadata>'

Same for the HTTP request.

    POST /api/v1/schemas/my_first_xsd/validate HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 2 notes are allowed</example:note>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

Everything should be fine now. As a result, you receive 204 as HTTP
status and no further content.

    HTTP/1.1 204 No Content
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

Update Metadata Schema Record
-----------------------------

In case of authorization it may be neccessary to update metadata record
to be accessible by others. To do so an update has to be made. In this
example we introduce a user called 'admin' and give him all rights.

    schema-record-acl.json
    {
      "schemaId":"my_first_xsd",
      "mimeType":"application/xml",
      "type":"XML",
      "acl":[
        {
          "id":33,
          "sid":"SELF",
          "permission":"ADMINISTRATE"
        },
        {
          "id":null,"
           sid":"admin",
             "admin":"ADMINISTRATE"
        }
      ]
    }

    $ curl 'http://localhost:8080/api/v1/schemas/my_first_xsd' -i -X PUT \
        -H 'Content-Type: application/vnd.datamanager.schema-record+json' \
        -H 'If-Match: "-1476742390"' \
        -d '{
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 2,
      "label" : null,
      "definition" : null,
      "comment" : null,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:54.7727347Z",
      "acl" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      }, {
        "id" : null,
        "sid" : "admin",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=2",
      "schemaHash" : "sha1:1baea3a07d95faea70707fcf46d114315613b970",
      "locked" : false
    }'

Same for the HTTP request.

    PUT /api/v1/schemas/my_first_xsd HTTP/1.1
    Content-Type: application/vnd.datamanager.schema-record+json
    If-Match: "-1476742390"
    Content-Length: 629
    Host: localhost:8080

    {
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 2,
      "label" : null,
      "definition" : null,
      "comment" : null,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:54.7727347Z",
      "acl" : [ {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      }, {
        "id" : null,
        "sid" : "admin",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=2",
      "schemaHash" : "sha1:1baea3a07d95faea70707fcf46d114315613b970",
      "locked" : false
    }

As a result, you receive 200 as HTTP status, the updated metadata schema
record and the updated ETag and location in the HTTP response header.

    HTTP/1.1 200 OK
    Location: http://localhost:8080/api/v1/schemas/my_first_xsd?version=2
    ETag: "-2094009168"
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 562

    {
      "schemaId" : "my_first_xsd",
      "schemaVersion" : 2,
      "mimeType" : "application/xml",
      "type" : "XML",
      "createdAt" : "2020-08-13T11:12:53.9967342Z",
      "lastUpdate" : "2020-08-13T11:12:55.2057344Z",
      "acl" : [ {
        "id" : 3,
        "sid" : "admin",
        "permission" : "ADMINISTRATE"
      }, {
        "id" : 2,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "schemaDocumentUri" : "http://localhost:8080/api/v1/schemas/my_first_xsd?version=2",
      "schemaHash" : "sha1:1baea3a07d95faea70707fcf46d114315613b970",
      "locked" : false
    }

After the update the following fields has changed:

-   lastUpdate to the date of the last update (set by server)

-   acl additional ACL entry (set during update)

Version number will be only updated while changing metadata schema.

Metadata Management
===================

After registration of a schema metadata may be added to metastore. In
this section, the handling of metadata resources is explained. It all
starts with creating your first metadata resource. The model of a
metadata record looks like this:

    {
      "id" : "...",
      "relatedResource" : "...",
      "createdAt" : "...",
      "lastUpdate" : "...",
      "schemaId" : "...",
      "recordVersion" : 1,
      "acl" : [ {
        "id" : 1,
        "sid" : "...",
        "permission" : "..."
      } ],
      "metadataDocumentUri" : "...",
      "documentHash" : "..."
    }

At least the following elements are expected to be provided by the user:

-   schemaId: A unique label of the schema.

-   relatedResource: The link to the resource.

In addition, ACL may be useful to make metadata editable by others.
(This will be of interest while updating an existing metadata)

Creating a Metadata Record
--------------------------

The following example shows the creation of the first metadata record
and its metadata only providing mandatory fields mentioned above:

    metadata-record.json:
    {
        "relatedResource": "anyResourceId",
        "schemaId": "my_first_xsd"
    }

The schemaId used while registering metadata schema has to be used to
link the metadata with the approbriate metadata schema.

    $ curl 'http://localhost:8080/api/v1/metadata/' -i -X POST \
        -H 'Content-Type: multipart/form-data' \
        -F 'record=@metadata-record.json;type=application/json' \
        -F 'document=<?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
    </example:metadata>'

You can see, that most of the sent metadata schema record is empty. Only
schemaId and relatedResource are provided by the user. HTTP-wise the
call looks as follows:

    POST /api/v1/metadata/ HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record.json
    Content-Type: application/json

    {"id":null,"pid":null,"relatedResource":"anyResourceId","createdAt":null,"lastUpdate":null,"schemaId":"my_first_xsd","recordVersion":null,"acl":[],"metadataDocumentUri":null,"documentHash":null}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

As Content-Type only 'application/json' is supported and should be
provided. The other headers are typically set by the HTTP client. After
validating the provided document, adding missing information where
possible and persisting the created resource, the result is sent back to
the user and will look that way:

    HTTP/1.1 201 Created
    Location: http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1
    ETag: "-1899982640"
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 526

    {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.6257353Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 1,
      "acl" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1",
      "documentHash" : "sha1:63ddd73b983ee265caee93dbe72c1995e813dc9e"
    }

What you see is, that the metadata record looks different from the
original document. All remaining elements received a value by the
server. In the header you’ll find a location URL to access the ingested
metadata and an ETag with the current ETag of the resource. This value
is returned by POST, GET and PUT calls and must be provided for all
calls modifying the resource, e.g. POST, PUT and DELETE, in order to
avoid conflicts.

Accessing Metadata
------------------

For accessing the metadata the location URL provided before may be used.
The URL is compiled by the id of the metadata and its version.

    $ curl 'http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1' -i -X GET \
        -H 'Accept: application/xml'

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1 HTTP/1.1
    Accept: application/xml
    Host: localhost:8080

The linked metadata will be returned. The result is sent back to the
user and will look that way:

    HTTP/1.1 200 OK
    Content-Length: 257
    Accept-Ranges: bytes
    Content-Type: application/xml
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

    <?xml version="1.0" encoding="UTF-8"?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/">

        <example:title>My first XML document</example:title>

        <example:date>2018-07-02</example:date>

    </example:metadata>

What you see is, that the metadata is untouched.

Accessing Metadata Record
-------------------------

For accessing the metadata record the same URL as before has to be used.
The only difference is the content type. It has to be set to
"application/vnd.datamanager.metadata-record+json". Then the command
line looks like this:

    $ curl 'http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1' -i -X GET \
        -H 'Accept: application/vnd.datamanager.metadata-record+json'

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1 HTTP/1.1
    Accept: application/vnd.datamanager.metadata-record+json
    Host: localhost:8080

The linked metadata will be returned. The result is sent back to the
user and will look that way:

    HTTP/1.1 200 OK
    ETag: "-1899982640"
    Content-Type: application/vnd.datamanager.metadata-record+json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 526

    {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.6257353Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 1,
      "acl" : [ {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1",
      "documentHash" : "sha1:63ddd73b983ee265caee93dbe72c1995e813dc9e"
    }

You also get the metadata record seen before.

Updating a Metadata Record (edit ACL entries)
---------------------------------------------

The following example shows the update of the metadata record. As
mentioned before the ETag is needed:

    metadata-record-acl.json:
    {
        "relatedResource": "anyResourceId",
        "schemaId": "my_first_xsd",
        "acl": [ {
          "id":null,
          "sid":"guest",
          "permission":"READ"
        } ]
    }

    $ curl 'http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "-1899982640"' \
        -F 'record=@metadata-record-acl.json;type=application/json' \
        -F 'version=1'

You can see, that only the ACL entry for "guest" was added. All other
properties are still the same. HTTP-wise the call looks as follows:

    PUT /api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "-1899982640"
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=version

    1
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=record; filename=metadata-record-acl.json
    Content-Type: application/json

    {"id":"50090769-8da0-4793-8a2a-e71e094d6151","pid":null,"relatedResource":"anyResourceId","createdAt":"2020-08-13T11:12:55.6257353Z","lastUpdate":"2020-08-13T11:12:55.6257353Z","schemaId":"my_first_xsd","recordVersion":1,"acl":[{"id":null,"sid":"guest","permission":"READ"},{"id":4,"sid":"SELF","permission":"ADMINISTRATE"}],"metadataDocumentUri":"http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1","documentHash":"sha1:63ddd73b983ee265caee93dbe72c1995e813dc9e"}
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

You will get the new metadata record with the additional ACL entry. The
'lastUpdate' and the 'recordVersion' are modified by the server.

    HTTP/1.1 200 OK
    Location: http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=2
    ETag: "1803841369"
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 598

    {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.8347347Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 2,
      "acl" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=2",
      "documentHash" : "sha1:63ddd73b983ee265caee93dbe72c1995e813dc9e"
    }

What you see is, that the metadata record looks different from the
original document.

Version number will be only updated while changing metadata. ETag is
also untouched.

Updating Metadata
-----------------

The following example shows the update of the metadata. As mentioned
before the ETag is needed. As the ETag has changed in the meanwhile you
first have to get the new ETag.

    $ curl 'http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151' -i -X GET \
        -H 'Accept: application/vnd.datamanager.metadata-record+json'

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151 HTTP/1.1
    Accept: application/vnd.datamanager.metadata-record+json
    Host: localhost:8080

You will get the new metadata record with the new ETag.

    HTTP/1.1 200 OK
    ETag: "10139261"
    Content-Type: application/vnd.datamanager.metadata-record+json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 598

    {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.8347347Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 2,
      "acl" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=2",
      "documentHash" : "sha1:63ddd73b983ee265caee93dbe72c1995e813dc9e"
    }

Now you can update metadata due to new version of schema using the new
Etag.

    $ curl 'http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1' -i -X PUT \
        -H 'Content-Type: multipart/form-data' \
        -H 'If-Match: "10139261"' \
        -F 'document=<?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 2 notes are allowed</example:note>
    </example:metadata>' \
        -F 'version=1'

HTTP-wise the call looks as follows:

    PUT /api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=1 HTTP/1.1
    Content-Type: multipart/form-data; boundary=6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    If-Match: "10139261"
    Host: localhost:8080

    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=version

    1
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm
    Content-Disposition: form-data; name=document

    <?xml version='1.0' encoding='utf-8'?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/" >
      <example:title>My first XML document</example:title>
      <example:date>2018-07-02</example:date>
      <example:note>since version 2 notes are allowed</example:note>
    </example:metadata>
    --6o2knFse3p53ty9dmcQvWAIx1zInP11uCfbm--

You will get the new metadata record.

    HTTP/1.1 200 OK
    Location: http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=3
    ETag: "369913205"
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 598

    {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.9177353Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 3,
      "acl" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=3",
      "documentHash" : "sha1:d9851c90e6092c42cf003d8e70da90833882cba5"
    }

Now you can access the updated metadata via the URI in the HTTP response
header.

    $ curl 'http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=3' -i -X GET

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=3 HTTP/1.1
    Host: localhost:8080

You will get the updated metadata.

    HTTP/1.1 200 OK
    Content-Length: 333
    Accept-Ranges: bytes
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY

    <?xml version="1.0" encoding="UTF-8"?>
    <example:metadata xmlns:example="http://www.example.org/schema/xsd/">

        <example:title>My first XML document</example:title>

        <example:date>2018-07-02</example:date>

        <example:note>since version 2 notes are allowed</example:note>

    </example:metadata>

Find a Metadata Record
----------------------

Search will find all current metadata records. There are some filters
available which may be combined. All filters for the metadata records
are set via query parameters. The following filters are allowed:

-   resourceId

-   from

-   until

The header contains the field 'Content-Range" which displays delivered
indices and the maximum number of available schema records. If there are
more than 20 schemata registered you have to provide page and/or size as
additional query parameters.

-   page: Number of the page you want to get **(starting with page 0)**

-   size: Number of entries per page.

### Find by resourceId

If you want to find all records belonging to an external resource.
Metastore may hold multiple metadata documents per resource.
(Nevertheless only one per registered schema)

Command line:

    $ curl 'http://localhost:8080/api/v1/metadata/?resoureId=anyResourceId' -i -X GET

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/?resoureId=anyResourceId HTTP/1.1
    Host: localhost:8080

You will get the current version metadata record.

    HTTP/1.1 200 OK
    Content-Range: 0-19/1
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 602

    [ {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.9177353Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 3,
      "acl" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=3",
      "documentHash" : "sha1:d9851c90e6092c42cf003d8e70da90833882cba5"
    } ]

### Find after a specific date

If you want to find all metadata records updated after a specific date.

Command line:

    $ curl 'http://localhost:8080/api/v1/metadata/?from=2020-08-13T09%3A12%3A56.063733100Z' -i -X GET

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/?from=2020-08-13T09%3A12%3A56.063733100Z HTTP/1.1
    Host: localhost:8080

You will get the current version metadata records updated ln the last 2
hours.

    HTTP/1.1 200 OK
    Content-Range: 0-19/1
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 602

    [ {
      "id" : "50090769-8da0-4793-8a2a-e71e094d6151",
      "relatedResource" : "anyResourceId",
      "createdAt" : "2020-08-13T11:12:55.6257353Z",
      "lastUpdate" : "2020-08-13T11:12:55.9177353Z",
      "schemaId" : "my_first_xsd",
      "recordVersion" : 3,
      "acl" : [ {
        "id" : 5,
        "sid" : "guest",
        "permission" : "READ"
      }, {
        "id" : 4,
        "sid" : "SELF",
        "permission" : "ADMINISTRATE"
      } ],
      "metadataDocumentUri" : "http://localhost:8080/api/v1/metadata/50090769-8da0-4793-8a2a-e71e094d6151?version=3",
      "documentHash" : "sha1:d9851c90e6092c42cf003d8e70da90833882cba5"
    } ]

### Find in a specific date range

If you want to find all metadata records updated in a specific date
range.

Command line:

    $ curl 'http://localhost:8080/api/v1/metadata/?from=2020-08-13T09%3A12%3A56.063733100Z&until=2020-08-13T10%3A12%3A56.063733100Z' -i -X GET

HTTP-wise the call looks as follows:

    GET /api/v1/metadata/?from=2020-08-13T09%3A12%3A56.063733100Z&until=2020-08-13T10%3A12%3A56.063733100Z HTTP/1.1
    Host: localhost:8080

You will get an empty array as no metadata record exists in the given
range:

    HTTP/1.1 200 OK
    Content-Range: 0-19/0
    Content-Type: application/json
    X-Content-Type-Options: nosniff
    X-XSS-Protection: 1; mode=block
    X-Frame-Options: DENY
    Content-Length: 3

    [ ]

Remarks on Working with Versions
================================

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
