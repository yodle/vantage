#!/bin/sh

#Three versions of a library.  Note that because it is using maven-style versioning, they can be inserted in any order
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/org.sample:transitively-included-library/versions/1.5.0 -d @org.sample:transitively-included-library-1.5.0.vantage.json
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/org.sample:transitively-included-library/versions/1.6.1 -d @org.sample:transitively-included-library-1.6.1.vantage.json
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/org.sample:transitively-included-library/versions/1.6.0 -d @org.sample:transitively-included-library-1.6.0.vantage.json

#Three versions of another library.  This library includes the previous library
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/org.sample:sample-library/versions/1.0.0 -d @org.sample:sample-library-1.0.0.vantage.json
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/org.sample:sample-library/versions/1.1.0 -d @org.sample:sample-library-1.1.0.vantage.json
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/org.sample:sample-library/versions/1.1.1 -d @org.sample:sample-library-1.1.1.vantage.json

#A few versions of a sample webapp.  Because they are using commit hashes as versions, the order they are inserted is the order they are assumed to have been created in
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/sample-webapp/versions/3467f7a8 -d @sample-webapp-3467f7a8.vantage.json
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/sample-webapp/versions/13d00032 -d @sample-webapp-13d00032.vantage.json
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/sample-webapp/versions/fd6e1697 -d @sample-webapp-fd6e1697.vantage.json

#An example issue affecting version 1.6.0 of the org.sample:transitively-included-library
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/issues/transitively-included-library-issue -d @transitively-included-library-issue.json

#Another webapp which depends on the bad version of org.sample:transitively-included-library
curl -XPUT -H "Content-Type: application/json" localhost:8080/api/v1/components/another-webapp/versions/b51a0e61 -d @another-webapp-b51a0e61.vantage.json 

