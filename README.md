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


## How to run Specify locally

* Clone or download the following repositories
  * Specify7
  * DaSSCo-file-proxy
  * specify-adapter
  * DaSSCo-asset-service
* Copy the content from specify-adapter **docker-compose-specify.yaml** into specify7 docker-compose.yaml
* Copy the following docker compose files from DaSSCo-file-proxy into specify
  * **docker-compose-jaeger.yaml**
  * **docker-compose-rabbitmq.yaml**
  * **docker-compose-postgres.yaml**
  * **docker-compose-postgres.yaml**
  * **docker-compose-keycloak.yaml**
* In specify7 open **nginx.conf** and change **set $backend** url to `http://asset-server:80` for the asset server
* Follow the specify7 instructions for seeding the database (how to create the .sql file and how to apply it the MariaDb)
* Run the following in specify7
  * Run `docker compose up database keycloak rabbitmq jaeger app`
  * Run `docker compose up asset-server` when **app** is running
  * Run `docker compose up` when **asset-server** is running
* Run DaSSCo-asset-service to create the tables used in DaSSCo-file-proxy
* Go to localhost:80 in your browser -> the login is the one you made when making the .sql file for seeding


### Python code to generate a token
```
import time
import datetime
import hmac

key = 'test_attachment_key'
timestamp = str(int(time.time())*1000)
filename = 'dd7cb124-7170-421d-ab4e-4da34c9d4c8d.jpg'

mac = hmac.new(key.encode(), timestamp.encode() + filename.encode(), 'md5')
print(':'.join((mac.hexdigest(), timestamp)))
```