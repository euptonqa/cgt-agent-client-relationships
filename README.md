# Capital Gains Agent Client Relationships microservice

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html) [![Build Status](https://travis-ci.org/hmrc/cgt-agent-client-relationships.svg)](https://travis-ci.org/hmrc/cgt-agent-client-relationships) [ ![Download](https://api.bintray.com/packages/hmrc/releases/cgt-agent-client-relationships/images/download.svg) ](https://bintray.com/hmrc/releases/cgt-agent-client-relationships/_latestVersion)

## Summary

This microservice provides RESTful endpoints for the creation of an Agent and Client relationship. It communicates with the Government Gateway microservice to link a Client to the Agent currently logged in.
 
There is a frontend microservice [CGT-Agent-Client-Relationships-Frontend](https://github.com/hmrc/cgt-agent-client-relationships-frontend) that provides the views and controllers which interact with this microservice.

### Run the application

To run the application execute

```
sbt 'run 9773'
```

### Test the application

To test the application execute

```
sbt test
```

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs a [JRE] to run.

## Dependencies

* Audit - Datastream
* Auth
* Government Gateway - GG

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")