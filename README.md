# Specify Adapter

## Endpoints:

- /specify/push/{assetGuid}
  - Get Asset metadata from Asset Service. Get Institution, Collection and Specimen Barcode.
  - Logs into Specify. Receives a CSRF Token + Collection list with Name (Key) and ID (Value).
  - Maps the Asset Collection and Institution to Specify Collections and Institutions.
  - Logs into a collection. Takes a username and password for Specify (need to be set up in the application-local.properties file) and a Collection ID. Returns cookies: CSRF Token, Session ID, and Collection ID.
  - Gets the Collection Object in Specify related to the Specimen for the Asset. 
  - Gets the List of Files for the Asset from the File Proxy. 
  - Gets Upload Params for the files (takes filenames and cookies, returns an attachment location and attachment token).
  - A call is made to get Collection Information, needed to get some variables.
  - Gets the file from ERDA, and uploads it to the Asset Server using the attachment location and token from before. 
  - Creates an AttachmentResource to append to the Collection Object.
  - Adds the AttachmentResource to the Collection Object with attached files.
  - Puts the updated Collection Object in Specify.
  - Logs out the user.

