#Vantage

Vantage is an application designed to give you perspective on the state of dependencies across your entire estate.  It is intended to help solve the dependency management problems that arise when you may have hundreds of applications, a pool of hundreds of possible dependencies, spread across dozens of independent teams.  Specific types of questions you may be interested in:

1. What versions of a given library are being actively used by applications or other libraries?
2. Which other applications or libraries depend on a given library?
3. Does my application depend on a version of a library with a known issue?

Note that while Vantage is currently in a usable state, it should be considered beta software.  Please view the issues associated with Vantage for known bugs and incoming functionality.  While we will attempt to maintain backwards compatibility where reasonable, at this time APIs, functionality, and database schemas are all subject to change.  

##Running Vantage
###Prerequisites
You must have the following prerequisites to build and run Vantage locally against an embedded neo4j instance:

* Java 8
* bower - You can install bower install via `npm install bower`.  If you do not have npm installed, follow the standard installation procedure for npm for your system.

You must additionally have the following prerequisites to run Vantage against a standalone neo4j instance

* A running neo4j instance of at least version 2.1.2

###Running Vantage Locally
The easiest way to start up vantage locally is by running `./gradlew bootRun -Pembedded`.  This will spin up vantage against an embedded transient version of neo4j.  You can then visit Vantage at `localhost:8080`.  You can view the swagger documentation by visiting `localhost:8080/api-browser/index.html`.  Updates to the database in this mode will not persist across runs of the application.  You can access the embedded neo4j server's browser as a normal neo4j browser.  Typically the embedded server will listen on 7474 if available, but may choose a different port.  The actual port chosen will appear in Vantage's logs.

If you wish to run locally against an external version of neo4j, simply remove the `-Pembedded` flag and provide the appropriate neo4j connection properties noted below:

* neo4j.server - the hostname of the neo4j server. (Default: localhost)
* neo4j.port - the port the neo4j server is listening on.  (Default: 7474)
* neo4j.user - the user to use when connecting to the neo4j server.  (Default: neo4j)
* neo4j.password - the password to use when connecting to the neo4j server.  (Default: password)

```
./gradlew bootRun -Dneo4j.server=my-neo4j-server.my-company.com -Dneo4j.user=my-neo4j-user -Dneo4j.password=my-neo4j-password
```

Vantage is a standard spring boot web application, so you may additionally override any other standard spring boot properties like server.port if desired.  

###Configuration options
In addition to the neo4j and spring boot configuration options, vantage has the following options

* vantage.require-dry-run-lock - If true, dry-run creates will lock the front of the create queue, meaning only one real or dry-run create can run at a time.  If false, dry-run creates will not lock the queue, meaning that any number of dry-runs can occur concurrently (along with one real create).  Multiple concurrent dry-run creates should be able to run wihtout deadlock, but this option exists as a safety valve.  (Default: false)


###Running Vantage In Production

For an actual production deployment, you can build it by running `./gradlew build` which will create an executable jar at `build/libs/vantage-<current version>.jar`.  You can run this jar by copying it to the server you want it to run on and invoking

```
java -jar /path/to/jar/vantage-<current version>.jar -Dneo4j.server=my-neo4j-server.my-company.com -Dneo4j.user=my-neo4j-user -Dneo4j.password=my-neo4j-password
```

Alternatively, you can create an `application.properties` file containing the properties you want to override and running the following command.

```
java -jar /path/to/jar/vantage-<current version>.jar --spring.config.location=/path/to/properties/file
```

###Example Commands

To run Vantage against a specific neo4j server:

```
./gradlew build
java -jar build/libs/vantage-<current version>.jar -Dneo4j.server=my-neo4j-server.my-company.com -Dneo4j.user=my-user -Dneo4j.password=my-password
```


##Using Vantage

Vantage relies on libraries and applications pushing their dependencies to Vantage.  In order to start using it, you will need to add a step to your deploy process to publish those dependencies to vantage.  If you are utilizing a continuous delivery model, you may instead want to publish your dependencies on a build of your application's master branch.  You probably do not want to publish those dependencies on branch builds as Vantage will assume that those are real active versions.  The format of the request to publish dependencies is shown in the REST Api Section below.

###REST Api

Vantage has three main resources.  A `component` is some unit of code that has dependencies.  For example, a web application and a library are both components.  (Vantage is unopinionated as to what exactly a component represents and so does not explicitly distinguish between different types of components).  A `version` is a subresource of a component which represents a specific version of the application or library.  This may be a standard versioning scheme such as `1.0` or something more freeform like a git commit hash. (See the Versioning section below for details on how Vantage interprets versions).   An `issue` is used to indicate that one or more versions of a component should not be used for some reason.  Issues may indicate anything from security holes to known bugs to business, environmental, or organizational requirements.

The REST api is documented below with sample request bodies and responses.  Additionally, you can view the api documentation in a running instance of Vantage by clicking on the 'View API Docs' link in the navbar

GET /api/v1/components - Gets a list of all components

Response:

```
[
  {
    "name" : "com.yodle:some-library",
    "description" : "This is a freeform description of this component",
    "mostRecentVersion" : "1.1.0"
  }
]
```

PUT /api/v1/components/{component} - Creates or updates a component named {component}

Request Body:

```
{
  "name" : "com.yodle:some-library",
  "description" : "This is a freeform description of this component"
}
```

GET /api/v1/components/{component} - Gets details for the component named {component}

Response:

```
{
  "name" : "com.yodle:some-library",
  "description" : "This is a freeform description of this component",
  "mostRecentVersion" : "1.1.0"
}
```

GET /api/v1/components/{component}/versions - Gets a summary of all versions of a component, sorted by the precedence described in the Versioning section

Response:

```
[
  {
    "component" : "com.yodle:some-library",
    "version" : "1.1.0",
    "active" : true,
    "directIssues" : [  //issues that affect this particular version of the component
      {
        "id" : "unique-issue-id",
        "level" : "MINOR",
        "message" : "Some message describing the issue",
        "affectsVersion" : {
          "component" : "com.yodle:some-library",
          "version" : "1.0.0" 
        },
        "fixVersion" : {
          "component" : "com.yodle:some-library",
          "version" : "1.2.0" 
        }
      }
    ],
    "transitiveIssues" : [ //issues that affect dependencies of this version
      {
        "id" : "some-other-issue-id",
        "level" : "CRITICAL",
        "message" : "Some message describing the issue",
        "affectsVersion" : {
          "component" : "com.yodle:some-dependency",
          "version" : "1.0.0" 
        } //Issues do not always have fix versions
      }
    ]
  }
]
```

GET /api/v1/components/{component}/versions/{version} - Gets details for a specific version of a component

Response:

```
{
  "component" : "com.yodle:some-library",
  "version" : "1.1.0",
  "active" : true,
  "resolvedDependencies" : [ //Versions of dependencies this component actually builds/runs against, including transitive dependencies.  If present, this should generally be used over requestedDepencencies as it is more reflective of reality
    {
      "profiles" : [
        "compile",
        "test"
      ],
      "version" : {
        "component" : "com.yodle:some-dependency",
        "version" : "1.2.0"
      }
    }
  ],
  "requestedDependencies" : [ //Versions of dependencies that literally appear directly in the build script.  These may be different than the actual versions used, e.g. due to version conflict resolution
    {
      "profiles" : [
        "compile",
        "test"
      ],
      "version" : {
        "component" : "com.yodle:some-dependency",
        "version" : "1.1.0"
      }
    }
  ],
  "dependents" : [ //Versions of other components that depend on this component
    {
      "profiles" : [
        "compile",
        "test"
      ],
      "version" : {
        "component" : "com.yodle:some-webapp",
        "version" : "d035e56b"
      }
    }
  ]
  "directIssues" : [
    {
      "id" : "unique-issue-id",
      "level" : "MINOR",
      "message" : "Some message describing the issue",
      "affectsVersion" : {
        "component" : "com.yodle:some-library",
        "version" : "1.0.0" 
      },
      "fixVersion" : {
        "component" : "com.yodle:some-library",
        "version" : "1.2.0" 
      }
    }
  ],
  "transitiveIssues" : [
    {
      "id" : "some-other-issue-id",
      "level" : "CRITICAL",
      "message" : "Some message describing the issue",
      "affectsVersion" : {
        "component" : "com.yodle:some-dependency",
        "version" : "1.0.0" 
      }
    }
  ]
}
```

PUT /api/v1/components/{component}/versions/{version}?dryRun=[true| **false** ] - Creates or updates a version for a component.  Dependencies are always added to existing dependencies, not overwritten.  DryRun defaults to false and will queue up the version creation, returning status code 202.  if dryRun=true, you will get a response equivalent to creating the version and calling GET /api/v1/components/{component}/versions/{version}, only the version will not be persisted in the database.

Response (dryRun=true only)

```
{
  "component" : "com.yodle:some-library",
  "version" : "1.1.0",
  "resolvedDependencies" : [
    {
      "profiles" : [
        "compile",
        "test"
      ],
      "version" : {
        "component" : "com.yodle:some-dependency",
        "version" : "1.2.0",
        "requestedDependencies" : [  //You can also provide second level dependencies.  This is primarily to capture some information about components that do not themselves report to Vantage.
          "profiles" : [
            "default"
          ],
          "version" : {
            "component" : "com.yodle:some-second-level-dependency",
            "version" : "2.0.0"
          }
        ]
      }
    }
  ],
  "requestedDependencies" : [
    {
      "profiles" : [
        "compile",
        "test"
      ],
      "version" : {
        "component" : "com.yodle:some-dependency",
        "version" : "1.1.0"
      }
    }
  ]
}
```


GET /api/v1/issues - Retrieve all issues
Response:

```
[
  {
    "id" : "some-issue-id",
    "level" : "CRITICAL",
    "message" : "Some message describing the issue",
    "affectsVersion" : {
      "component" : "com.yodle:some-dependency",
      "version" : "1.0.0" 
    },
    "fixVersion" : {
      "component" : "com.yodle:some-dependency",
      "version" : "1.2.0" 
    }
  }
]
```

GET /api/v1/issues/{issueId} - Retrieve an issue by id

Response:

```
{
  "id" : "some-issue-id",
  "level" : "CRITICAL",
  "message" : "Some message describing the issue",
  "affectsVersion" : {
    "component" : "com.yodle:some-dependency",
    "version" : "1.0.0" 
  },
  "fixVersion" : {
    "component" : "com.yodle:some-dependency",
    "version" : "1.2.0" 
  }
}
```

PUT /api/v1/issues/{issueId} - Create or update an issue with the given issueId.  This endpoint also supports partial updates.  

Request Body:

```
{
  "id" : "some-issue-id",
  "level" : "CRITICAL",
  "message" : "Some message describing the issue",
  "affectsVersion" : {
    "component" : "com.yodle:some-dependency",
    "version" : "1.0.0" 
  },
  "fixVersion" : {
    "component" : "com.yodle:some-dependency",
    "version" : "1.2.0" 
  }
}
```

###Versioning

Vantage supports two versioning schemes, commit hashes and maven versions.  The versioning scheme is used to determine if a version is newer or older than another version, which in turn is used to determine if that version is affected by a known issue.  The versioning scheme used is inferred from the known versions of a component.  If all versions consist purely of alphanumeric strings, Vantage assumes that the versions are commit hashes.  Commit hashes are assumed to be ordered by creation timestamp of the component.  Otherwise, the versioning scheme is assumed to be the standard maven convention.  This means that, for example, if you insert version 1.0, then 2.0, then 1.5, Vantage will act as if 1.5 comes between 1.0 and 2.0.  In other words, if an issue affects 1.0 and is fixed in 2.0, then Vantage will report that it also affects 1.5.

##Technical Details

Vantage is a Java 8 spring-boot web application with an angular frontend driven by a REST backend, using Neo4j as a data store.  It requires Neo4j of at least version 2.1.2.

##Initial Contributors

Vantage's commit history was wiped when it was initially made public.  The following individuals contributed to Vantage prior to the intial publish:

* David Kesler <dkesler@yodle.com>
* Michael Pearson <michael.pearson@yodle.com>
* Dan Hasday <dhasday@yodle.com>
* Andrew Rapport <andrew.rapport@yodle.com>
* Avidan Ackerson <avidan.ackerson@gmail.com>
