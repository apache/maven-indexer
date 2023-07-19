Indexer Search RemoteRepository Backend
=======================================

Search API SMO (https://search.maven.org/) backend implementation using https://jsoup.org/.

This is a limited client, for limited functionality only. In turn, it uses rock-solid
service that may provide needed answers for some very basic queries. It relies on 
combination of Maven Repository Metadata and JSoup and some HTTP "checks" to deduce
actual context.

It supports:
* "G AND A" => list existing versions of GA
* "G AMD A AND V" => list existing artifacts for given GAV

Note: this backend does NOT assume nor perform any kind of validation, so it is up to
caller to either ensure parameters are really G, A and V and to interpret results
correctly.

Use cases:
* "what is the latest version of GA?"
* "what classifier exists for GAV?"

Note: this client is tested only against Maven Central, and may not work for other 
services, as what basically it does is uses JSoup to parse returned HTML pages. Other
MRMs may produce different HTML output.

Implementation notes:
It relies on following facts:
* G subdirectories "are probably" As (not always true!).
* GA directories have maven-metadata.xml listing all Vs.
* GAV directories, IF snapshot, have maven-metadata.xml otherwise have artifact files (subdirectories ignored).
* uses external checksums found in remote repository (SHA1, MD5) if needed.

Examples:

```java
  RemoteRepositorySearchBackendImpl backend = new RemoteRepositorySearchBackendImpl("https://repo.maven.apache.org/maven2");
```
