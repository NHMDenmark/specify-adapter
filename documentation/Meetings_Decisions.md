
# Introduction 
This document will hold discussions and decision including weekly progress in Specify Adapter Work Package

##  ***NT status meeting 19 June 2025*** 

Status : Can move files to Specify locally

New release: Friday 20th June 2025

Next version - tasks : 
- Optimisation
- Handle edge cases
- add unit tests

### Discussion and Decisions

1. Multispecimen Object discussion: 2 Specimen - one sheet

   - Discision: Same asset(attachment)+ mapping(metadata) added to both collection objects(specimens) in Specify
   - Mapping of Specimen pid - decision postponed for later
   - Mapping of Asset pid -  location of the file(for now)

2. Multiple Object Specimen discussion: severals assets - one specimen
   
   - Discision: Same asset(attachment)+ mapping(metadata) added to both collection objects(specimens) in Specify
   - Mapping of Specimen pid - decision postponed for later
   - Mapping of Asset pid -  location of the file(for now)
   
3. When deleting an attachment from Specify _ Workpackage -Specify -> ARS Integration
    - Specify marks it as delete
      - disappears from Specify
      - remains in ARS/ERDA, marked as to be deleted using the mark as to be deleted end point for manual decision 
      - adds a tombstone file in Specify with some meta information
  
4. When deleting asset in a ARS which was attachment of a collection object in Specify
   -  Generate a tombstone file
   -  add it as attachment instead to collection object in Specify
   
5. When deleting assets
   - sync an empty attachment list with specify
   - delete specific attachments in Specify and their mapping(metadata)   
   - if there are multple atachment - postponed until webasset server is replaced

6. When deleting attachments in Specify as part of testing
   - using the delete end point delete the attachment
   - if there are multple atachment - postponed until webasset server is replaced
    
7. ARS has presedence for all metadata fields for now
   - When syncing metadata, the values of metadata is overwritten to Specify, that means the values in ARS are persisted in Specify in all cases
    
##  ***NT status meeting 26 June 2025*** 

Status :
Tombstone
update the existing attachment
Bug fix -> wrong ID for creating attachment

New release: 27th June 2025

### Discussion and Decisions

1. Tombstone file

   - json named asset guid.json
   - asset guid with complete metadata
     
2. Handling edge cases
  - example, what if the asset is not put on the queue for creating attachment, etc
  - create tests amd see which other edge cases appear

3. Issue with delete - not working 
  -  found bug - not able to delete attachments 

4. How delete should work with exisitng collection object with existing attachments vs Newly created attachment/one attachment

  - it should only delete the relevant attachment and keep the rest of the previously existing attachments
  - to be revisited when WebAssetServer is replaces ARS - only need to delete the link and update ERDA

5. default.conf for All institutions
   -  there will be a default conf in the folder hierarchy applied to all institutions unless there is a conf file for institution
   -   Similarly the institution conf will be default for the whole institution unless there is a collection conf file

6. New Specify Installations(Instutions) can be added to specify bridge
   - by calling an end point to Specify adapter, new specify installations can be added

     **Depolyment**
June 27th, relased a new version of DaSSCo
Can now push updates to specify
Can add tombstones to specify
The prefix and collection_id bugs have been fixed

The following still needs to be implemented:
Delete entire attachment (the test delete functionality)
Multi specimens     


##  ***NT status meeting 2nd July 2025*** 

Status:
Multispecimen - MSO - multiple specimen on same asset implemented
MOS - nothing special to do, same flow as ordinary flow - metadata field mos_id should be mapped to container in specify -potential discussion for next time 
Some bug fixes
Mapping list - which make sense to be mapped: to be put in https://github.com/NHMDenmark/specify-adapter/edit/main/documentation/

Request for adding release notes for each deployment and include #issue no which have been solved in the release notes
Request for Documentation on all the tables the Sprecify Bridge is manulating and all the Specify API endpoints being used
Demo of the functionality until now
Meeting for discussing moderated access on 9th July

### Discussion and Decisions

1. Delete issue - picking up from last time, Lars created a issue with Specify Team and learned
 - there is a bug in SPecify API so delete has been put on hold for now 
 - for further reference and follow up https://discourse.specifysoftware.org/t/deleting-collectionobjectattachment-via-api-endpoints/2670
 - Another aspect of delete - for MSO just unlink and not do not delete

2. Found a bug
 - when removing a specimen from an asset in ARs will break the link to collection object in Specify
 - solution recommended -  search and delete in Specify

3. Add new metadata field - mimetype in ARS
 - updated mapping - mimetype field in SPecify should be mapped to new field mime_type field in ARS   

4. Keeping conf file transparent
  - there should not be any invisible processing happing through conf files - for example converting infering mime type from file formats behind the scenes
  - contactination and splitting should also be transparent and visible in conf files - for example origfilename=${asset_guid}.${file_format}

5. external publisher put on hold
  - current status - external publisher is an object which can contain URL and name but no support in SPecify so , put on hold
  - keeping potential for this in mind - need to work up a list of specific fields in specific tables and relevant that should be kept open for dynamic mapping

**Deployment**
4th July
