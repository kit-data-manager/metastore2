# Make Document public available

By default, unauthenticated users do not have access to a document. If you want to
want to make it publicly available, you must update the access rights. 

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
Click 'ADD' to add another entry to the ACL, insert 'anonymousUser' as SID and grant the
appropriate permission. 

This may look like this:

<div class="centerbox">
    <img src="/images/ACL_Step2.png" alt="Add ACL Entry" style="max-height:50em;" />
</div>

***Note:*** 
- You should **never** grant higher permissions than 'READ' to unauthenticated users.


Click on 'Update Schema' or in case of a metadata document click on 'Show Metadata Document' 
and afterwards 'Update' to update schema/metadata record.

***That's it! All users can now also access (dependent of their permissions)
the selected document.***

