# MetaStore
Metastore is a metadata repository for managing a large number of metadata documents. 
While registering metadata documents they are formally quality-controlled, 
persistently stored and then accessible via a unique identifier. 
In addition, Metastore allows versioning of metadata documents.
A schema formally defines the structure of a metadata document. The internal 
schema registry (providing XML and JSON) manages these schemas by registering, 
(persistent) storing and (if necessary) versioning them. All metadata documents 
are linked to a schema that is used for validation during ingest.
For content search of metadata documents an additional indexing service is 
available to transform the metadata documents to make them ready for elasticsearch.
It also provides an easy to use GUI for creating and editing documents. 

## When to use MetaStore
If at least first point is valid:
1. You want to manage XML/JSON documents build on a given XSD/JSON Schema.
2. You want to manage huge amounts (up to millions) of documents.
3. You want to share (some of) your (schema) documents with others.
4. You want to make (some of) your (schema) documents public available.
5. You want to make them referencable.
6. You want to update your (schema) documents with or without simple versioning.
7. You want to use your own (extended) schema.

## When NOT to use MetaStore
Don't use MetaStore if 
1. you want to use plain documents not based on a schema. 
2. you want use JSON-LD (take a look at Coscine provided by RTWH Aachen university).

## At a Glance
MetaStore is a general purpose metadata repository and schema registry service.

It allows you to 
- register an (XML/JSON) schema
- update an (XML/JSON) schema
- add metadata documents linked with a registered schema
- validate metadata documents against a registered schema
- update/add metadata documents online via a GUI

