**Test Reporty Thing*** (**trt**) is a web application for recording automated test executions, viewing reports on the results, and diagnosing failures.

Documentation wiki: https://github.com/thetestpeople/trt/wiki

Development
-----------

Requirements:

* Java 7+
* Bower (http://bower.io/)
* Play Framework (https://www.playframework.com/)

Setup after clone:

    bower install
    activator run
    
View website (http://localhost:9000/)

Run tests:

    sbt test

Exclude all the slow integration tests:

    sbt "test-only * -- -l com.thetestpeople.trt.tags.SlowTest"
