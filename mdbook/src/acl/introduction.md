# Access Rights Management

The MetaStore can be used with or without authentication. If authentication is enabled,
the creator of he documents is automatically granted administration rights.In the 
other case, everyone(SELF) who has access also has administration rights.
Each user has a SID (unique identifier) which identifies you. If no credentials 
are available, access is automatically granted as an anonymous user (SID: *anonymousUser*).

---
There are four levels of access rights:

| ACCESS LEVEL | Description |
|------------|-----------|
| ***NONE*** | No access is allowed. (This is the standard level for all SIDs not mentioned in the access control lists (ACLs).) |
| ***READ*** | User is allowed to read records and documents but not to change them. |
| ***WRITE*** | Extend the 'READ' access level. The user can additionally make changes except changing the access rights. |
| ***ADMINISTRATE*** | Extend the 'WRITE' access level. The user may also change access rights. |

---

## Ingredients

- (local) MetaStore instance with at least one registered schema and related metadata document.
- Web Browser


## Recipes

The following steps will be explained in detail on the next pages:

<nestednumerationlist>

1. [Make Document available for other Users](./accessible.md)
2. [Make Document public available](./publish.md)
3. [Disable public access for a single Document](./reject.md)
   
</nestednumerationlist>
