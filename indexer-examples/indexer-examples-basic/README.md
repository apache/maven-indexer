Maven Indexer Examples
======

This example covers simple use cases and is runnable as Java App or just using Maven "test" goal (as there is a Junit test simply executing the main() method, to not have to fuss with classpath etc.)

Try following steps:

```
$ cd indexer-examples
$ mvn clean test -Pexamples
  ... first run will take few minutes to download the index, and then will run showing some output
$ mvn test -Pexamples
  ... (no clean goal!) second run will finish quickly, as target folder will already contain an up-to-date index
```

Please, note that the tests in this module will only compile by default; the will not be executed, unless you activate the profile (-Ptests).

Have fun,  
~t~