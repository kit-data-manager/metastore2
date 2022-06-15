# Register a new schema document
Once a schema is choosen it may be registered in MetaStore (if not already done).

---

## Target Audience

- Data Scientists

---

## Ingredients

- (local) MetaStore instance
- Web Browser
- Selected schema from the previous step stored on local disc

---

## Work Steps (Summary)

### Register a new schema document
 * Step 1: Check if schema already exists?
 * Step 2: Create new schema

---

## Step 1: Check if schema already exists?
Right now it is difficult to find and/or compare schemas. It is strongly recommended
to use the standard labels of the schema when registering them. (E.g.: if you select
a schema from the Metadata Standards Catalog use the short name or abbreviation of 
the schema as identifier (e.g.: Dublin Core -> schemaID = DublinCore).


## Step 2: Create new schema
Register schema if it not already exists.

<div class="centerbox">
    <img src="/metastore2/images/SchemaManagement_Step1.png" alt="Schema Management GUI" style="max-height:50em;" />
</div>

Click on 'Register new Schema' to register a new schema. Fill the form with the metadata for the
schema document. The following values are mandatory:
- SchemaId
- Mime Type: application/json or application/xml
- Type: JSON or XML 
- Choose schema file via file chooser dialog. 

Afterwards the form should look like this:

<div class="centerbox">
    <img src="/metastore2/images/SchemaManagement_Step2.png" alt="Schema Record Form" style="max-height:50em;" />
</div>

***Note:*** If no authentication is made, all users are registered as 'SELF'. 
Otherwise the one who registers the schema gets the administration rights. 

After clicking 'CREATE' the new schema will be listed inside the table:

<div class="centerbox">
    <img src="/metastore2/images/SchemaManagement_Step3.png" alt="List of Schemas" style="max-height:50em;" />
</div>


***Congratulations, you successfully registered your first schema!***
