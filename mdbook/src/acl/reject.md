# Make Document public available

By default, unauthenticated users do not have access to a document. If you want to
want to make it publicly available, you must update the access rights. 
If you already granted unauthenticated users access to a document you have to
forbid any access to them by setting the permission to 'NONE'.

---

## Target Audience

- (Data) Scientists

---

## Ingredients

- (local) MetaStore instance with at least one registered schema and related metadata document.
- Web Browser
---

## Work Steps (Summary)

### Make Document public available
 * Step 1: Update record for Digital Object

---

## Step 1: Update record for Digital Object
Click on <img src="/images/EditEntry.png" alt="Edit Entry" style="max-height:15px;" />
to get a form with the schema/metadata record. 
Set permission for 'anonymousUser' to 'NONE'. 

This may look like this:

<div class="centerbox">
    <img src="/images/ACL_Step3.png" alt="Add ACL Entry" style="max-height:50em;" />
</div>


Click on 'Update Schema' or in case of a metadata document click on 'Show Metadata Document' 
and afterwards 'Update' to update schema/metadata record.

***That's it! Unauthenticated users are no longer allowed to access the selected document.***

