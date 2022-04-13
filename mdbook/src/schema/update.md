# Update an existing schema document
Sometimes there are some typos in the registered schema or you want to extend
it with **optional** fields the schema may be updated. Therefor its version will
be incremented. Nevertheless all metadata documents already linked to the old
version are still valid as the version is part of the reference. The metadata 
documents and their metadata stayed untouched.

---

## Target Audience

- Data Scientists

---

## Ingredients

- (local) MetaStore instance
- Web Browser
- Updated schema stored on local disc

---

## Work Steps (Summary)


### Update an existing schema document
 * Step 1: Download existing schema
 * Step 2: Update existing schema locally
 * Step 3: Upload updated schema
---

## Step 1: Download schema
Right now editing schemas online is not supported. If the schema is no longer
locally available it has to be downloaded from MetaStore. To do so you have to
do the following steps:
1. Press the 'View' icon of the wanted schema in the schema list.
2. Copy 'Schema Document Uri' to the address bar of your browser.
3. Right click page and select 'Save page as...' in the context menu. 

<div class="centerbox">
    <img src="/images/SchemaManagement_Step3.png" alt="List of Schema Records" style="max-height:50em;" />
</div>



## Step 2: Update existing schema locally
Extend the existing schema by an optional field containing notes. The updated schema
will look like this:
This example results in the following schema using the first link given above:
```
{{#include ./my_first_json_schema_version_2.json}}
```
As the entry 'notes' is not defined in the 'required' array this entry may be 
optional.
Store the new schema in a file named 'my_first_json_schema_version_2.json'.

## Step 3: Upload updated schema
Click the pencil of the appropriate schema and a form will open

<div class="centerbox">
    <img src="/images/SchemaManagement_Step4.png" alt="Update Schema Record Form" style="max-height:50em;" />
</div>

After selecting updated schema press 'Update Schema'. 
Now the version of the schema was increased.

<div class="centerbox">
    <img src="/images/SchemaManagement_Step5.png" alt="Updated Schema List" style="max-height:50em;" />
</div>

The new form for the metadata documents now looks like this:

<div class="centerbox">
    <img src="/images/SchemaManagement_Step6.png" alt="New Schema Record Form" style="max-height:50em;" />
</div>

***Congratulations, you successfully updated your schema!***
