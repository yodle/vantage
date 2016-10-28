#Sample Data
This directory houses some sample data to populate Vantage so that you can explore its functionality without needing to load your own data.  The sample data contains four components.

* org.sample:transitively-included-library is a library with no dependencies itself.  In version 1.6.0, a critical issue was introduced.  The issue was fixed in version 1.6.1.
* org.sample:sample-library is a sample library which depends on org.sample:transitively-included-library.
* sample-webapp represents a web application versioned by commit hash.  It directly depends on org.sample:sample-library, and (fittingly) transitively depends on org.sample:transitively-included-library.  A prior version of the app depended on the bad version of org.sample:transitively-included-library
* another-webapp represents another webapp which still depends on the bad version of org.sample:transitively-included-library
  
##Loading data

1. Run Vantage from the root of the repository via `./gradlew bootRun -Pembedded`.  This will bring it up against an embedded neo4j database which will not be persisted to disk
2. Run `load-sample.sh` in the `/sample` directory.  This script contains a shell script which `curl`s  the sample data to Vantage.
3. Optionally, you can also load Vantage's dependencies by running `./gradlew vantagePublish` in the root of the repository.  This will publish dependencies via the [Vantage Gradle plugin](https://github.com/yodle/vantage-gradle).

Because the processing of version creation is queued, it may take several seconds for Vantage to finish loading all the data.  You can view Vantage's logs to see when it has finished processing all the data.  Once it has finished, you can view the loaded data by navigating to http://localhost:8080.

##Things to check out

For a more guided tour of Vantage's features, you can follow the following steps

* http://localhost:8080 - view a list of all known components and the most recent versions.  Click on the `sample-webapp` link to view all versions of that component.
* http://localhost:8080/vantage/components/sample-webapp - view all versions of sample-webapp.  The yellow warning icon indicates that version `13d00032` includes a dependency with a known issue.  The most recent version, `fd6e1697` is marked as active because it is the most recent version of the application.  Click on the `13d00032` link to view more details about that version.
* http://localhost:8080/vantage/components/sample-webapp/versions/13d00032 - view version `13d00032` of the sample webapp.  The dependency with a known issue is floated to the top of the application's dependencies wiht a warning icon.  Hovering over the warning icon provides a summary of the issue.  Click on the `org.sample:transitively-included-library:1.6.0` link to view details about version 1.6.0 of that library.
* http://localhost:8080/vantage/components/org.sample:transitively-included-library/versions/1.6.0 - A summary of the issue affecting this version is displayed at the top of the page.  All other active components that depend on this version are displayed, whether the dependency is direct or transitive.  Note that `another-webapp` still depends on this version despite it having a known issue.  This component happens to have no dependencies itself.  Click on the name of the issue to view the details of that issue
* http://localhost:8080/vantage/issues/transitively-included-library-issue - This page shows all details about the issue, including the earliest version affected and the fix version.  This page also allows editing most details about the issue.  Click on the `org.sample:transitively-included-library` link.
* http://localhost:8080/vantage/components/org.sample:transitively-included-library - This page shows all versions of the library, much like the earlier page displaying all versions of the sample-webapp.  The key difference here is that version 1.6.0 is affected directly by an issue and thus has a red warning icon.  Also, version 1.6.0 is marked as active because it is depended on by another active component.

A user who is interested in the state of the two webapps is able to determine that sample-webapp does not depend on any version with known issues, but another-webapp still does and should be upgraded.  A user who owns the org.sample:transitively-included-library can determine that version 1.6.0 is still active.  They can then further dig into what applications or libraries still depend on it so that they can notify those consumers that they should upgrade.  

If you want to get hands on, there are a couple things you may want to try out:

* PUT the version in `sample-webapp-13d00032.vantage.json` to vantage with the dryRun=true query parameter.  (I.e. curl -H "Content-Type: application/json" -XPUT localhost:8080/api/v1/components/sample-webapp/versions/13d00032?dryRun=true -d @sample-webapp-13d00032.vantage.json).  Rather than queue the version and permanently create it, it will synchronously perform a dry run of creating the version.  This will cause issues transitively affecting this version to be returned, allowing for this information to be displayed in your build processes and potentially even gate builds.
* Create a new issue in the Vantage UI.  Note that the fix version is optional and can be left off if there is not yet a fixed version of the library but you still want to notify consumers, or if you do not want any consumers using the library at all.
