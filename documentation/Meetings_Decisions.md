
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
    
