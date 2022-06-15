# Make Document available for other Users

By default, only creators have access to your recorded schemas/documents. To allow
other users to access your documents, you need to update the access rights. 

---

## Target Audience

- (Data) Scientists

---

## Ingredients

- (local) MetaStore instance with at least one registered schema and related metadata document.
- Web Browser
- SID of all users you want to grant access.
---

## Work Steps (Summary)

### Make Document available for other Users
 * Step 1: Check for SID of user(s)
 * Step 2: Update record for Digital Object

---

## Step 1: Check for SID of user(s)
First of all, the users who are to be granted access to your document must be asked 
for their SID. 


## Step 2: Update record for Digital Object
Click on <img src="/metastore2/images/EditEntry.png" alt="Edit Entry" style="max-height:15px;" />
to get a form with the schema/metadata record. 
Click 'ADD' to add another entry to the ACL, insert the correct SID and grant the
appropriate permission. Repeat this for all users you want to add.

This may look like this:

<div class="centerbox">
    <img src="/metastore2/images/ACL_Step1.png" alt="Add ACL Entry" style="max-height:50em;" />
</div>


Click on 'Update Schema' or in case of a metadata document click on 'Show Metadata Document' 
and afterwards 'Update' to update schema/metadata record.

***That's it! The users you add can now also access (dependent of their permissions)
the selected document.***

