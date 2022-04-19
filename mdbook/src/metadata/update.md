# Update an existing metadata document
Sometimes there are some typos in the ingested metadata document or you want to 
extend it with **optional** fields of a newer version of a schema. Therefor its 
version will be incremented. Nevertheless the old version of the metadata document
is still available.

---

## Target Audience

- (Data) Scientists

---

## Ingredients

- (local) MetaStore instance
- Web Browser
- Registered schema
- Metadata document stored in MetaStore

---

## Work Steps (Summary)


### Update an existing schema document
 * Step 1: Update metadata record
 * Step 2: Update metadata document
---

***Note:*** 
- Right now record and document cannot be updated in one step if you want to use
an updated schema.

## Step 1: Update metadata record
Click on <img src="/images/EditEntry.png" alt="Edit Entry" style="max-height:15px;" />
to get a form with the metadata record. Change like shown above:

<div class="centerbox">
    <img src="/images/MetadataManagement_Step6.png" alt="List of Schema Records" style="max-height:50em;" />
</div>

Click on 'Show Metadata Document' and then click 'Update' to update metadata record.

***Note:*** The version still remains the same but 'Schema version' was updated.  



## Step 2: Update metadata document
Click once more on <img src="/images/EditEntry.png" alt="Edit Entry" style="max-height:15px;" />
to get a form with the metadata record. As the schema version was already changed 
you can directly click on 'Show Metadata Document'
The new form will be showed:

<div class="centerbox">
    <img src="/images/MetadataManagement_Step7.png" alt="List of Schema Records" style="max-height:50em;" />
</div>

Add some notes and 'Update'

***Congratulations, you successfully updated your metadata document!***
