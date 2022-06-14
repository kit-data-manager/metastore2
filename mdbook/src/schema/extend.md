# Extend an existing schema document
In case a suitable (JSON/XML) schema has been found, but some important information is 
missing, it is strongly recommended not to extend this schema by means of an update.
Since old schemas are then no longer compatible with the new version, in such a 
case a new schema should be registered based on the existing one.

---

## Target Audience

- Data Scientists

---

## Ingredients

- (local) MetaStore instance
- Web Browser

---

## Work Steps (Summary)

### Extend an existing schema document
* Step 1: Fetch selected schema
* Step 2: Extend schema
* Step 3: Register new schema

---

## Step 1: Fetch selected schema
First of all the choosen schema has to be stored locally.
If the schema is already registered in the MetaStore please refer to 
[Step 1: Download schema](./update.md#step-1-download-schema).

## Step 2: Extend schema
Add the missing fields to the schema and mark them as 'required'.
Extend previous schema with a mandatory 'abstract' entry this would look like this:
```
{{#include ./my_second_json_schema.json}}
```
## Step 2: Create new schema
Register new schema.

<div class="centerbox">
    <img src="/images/SchemaManagement_Step5.png" alt="Schema Management List" style="max-height:50em;" />
</div>

Click on 'Register new Schema' to register a new schema. Fill the form with the metadata for the
schema document. The following values are mandatory:
- SchemaId
- Mime Type: application/json or application/xml
- Type: JSON or XML 
- Choose schema file via file chooser dialog. 

Afterwards the form should look like this:

<div class="centerbox">
    <img src="/images/SchemaManagement_Step7.png" alt="Schema Record Form" style="max-height:50em;" />
</div>

***Note:*** If no authentication is made, all users are registered as 'SELF'. 
Otherwise the one who registers the schema gets the administration rights. 

After clicking 'CREATE' the new schema will be listed inside the table:

<div class="centerbox">
    <img src="/images/SchemaManagement_Step8.png" alt="List of Schemas" style="max-height:50em;" />
</div>
