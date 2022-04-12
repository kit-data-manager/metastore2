# Update an existing schema document
Sometimes there are some typos in the registered schema or you want to extend
it with **optional** fields the schema may be updated. Therefor its version will
be incremented. Nevertheless all metadata documents already linked to the old
version are still valid as the version is part of the reference. The metadata 
documents stayed untouched.

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

## Step 2: Update existing schema locally
Extend the existing schema by an optional field containing notes. The updated schema
will look like this:
This example results in the following schema using the first link given above:
```
{
    "$schema": "http://json-schema.org/draft-07/schema",
    "type": "object",
    "required": [
        "author",
        "title",
        "creation_date"
    ],
    "properties": {
        "author": {
            "type": "string"
        },
        "title": {
            "type": "string"
        },
        "creation_date": {
            "type": "string"
        },
        "notes": {
            "type": "string"
        }
    },
    "additionalProperties": false
 }
```
As the entry 'notes' is not defined in the 'required' array this entry may be 
optional.
Store the new schema in a file named 'my_first_json_schema_version_2.json'.

## Step 3: Upload updated schema
***Congratulations, you successfully updated your schema!***
