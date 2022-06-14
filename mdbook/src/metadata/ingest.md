# Ingest a metadata document
MetaStore will not provide plain text files. As only JSON and XML are supported 
the metadata documents have to fulfill a given (JSON/XML) schema.
The GUI has full support of JSON documents. 

---

## Target Audience

- (Data) Scientists

---

## Ingredients

- (local) MetaStore instance
- Web Browser
- Already registered schema ([see 'Schema Management'](../schema/introduction.md))
- Data related to the metadata

---

## Work Steps (Summary)

### Ingest a metadata document
 * Step 1: Check if schema is already registered?
 * Step 2: Select registered schema
 * Step 3: Create metadata document

---

## Step 1: Check if schema is already registered?
If the schema your metadata document should use is not already registered please
have a look at chapter ['Select/create a Schema'](../schema/select.md). 


## Step 2: Select registered schema
Note the 'Schema Record Identifier' of selected schema or its URL.

## Step3: Create metadata document
Fortunately the GUI of MetaStore supports users to input metadata document. 
1. Select 'Metadata Management'
2. Click on 'Register new metadata document'
3. Fill form like seen below


<div class="centerbox">
    <img src="/images/MetadataManagement_Step1.png" alt="Metadata Management Form" style="max-height:50em;" />
</div>

***Notes:*** 
- If no authentication is made, all users are registered as 'SELF'. Otherwise 
the one who registers the schema gets the administration rights. 
- Identifier is optional and if given it has to be unique. (If no identifier is 
specified it will be set to a UUID.)
- It's recommended to use a URL as identifier for the related resource.
- It's recommended to use the previously noted 'Schema Record Identifier' as identifier.
'INTERNAL' must then be selected as 'Identifier Type'.
- In this step the first version of the schema will be selected. Therefore no notes are possible.

After clicking 'Show Input Form' the GUI creates an input form which queries all
entries. The filled form may look like this:

<div class="centerbox">
    <img src="/images/MetadataManagement_Step2.png" alt="Input Form for metadata document" style="max-height:50em;" />
</div>
Click on 'Register Metadata Document' to register the new metadata document.

The new metadata document will be listed inside the table:

<div class="centerbox">
    <img src="/images/MetadataManagement_Step3.png" alt="List of Metadata Documents" style="max-height:50em;" />
</div>


***Congratulations, you successfully registered your first metadata document!***
