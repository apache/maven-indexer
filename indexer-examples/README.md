Maven Indexer Example 01
======

This example covers simple use cases and is runnable as Java App or just using Maven "test" goal (as there is a Junit test simply executing the main() method, to not have to fuss with classpath etc.)

Try following steps:

```
$ cd indexer-example-01
$ mvn clean test
  ... first run will take few minutes to download the index, and then will run showing some output
$ mvn test
  ... (no clean goal!) second run will finish quickly, as target folder will already contain an up-to-date index
```


Have fun,  
~t~