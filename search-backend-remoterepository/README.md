Indexer Search RemoteRepository Backend
=======================================

Search API backend implementation with limited capabilities using JSoup.

Requires Java 11 at runtime.

This is a limited client, for limited functionality only. In turn, it uses rock-solid
service that may provide needed answers for some very basic queries. It relies on 
Maven Repository Metadata served from remote repository and some HTTP "checks" to deduce
actual context.

It supports:
* "G" => list existing As of G (uses HTML parsing)
* "G AND A" => list existing versions of GA (uses Maven Metadata)
* "G AMD A AND V" => list existing artifacts for given GAV (uses HTML parsing)
* "G AND A AND V AND E..." => existence check
* "G AND A AND V AND E...+sha1" => existence and validity check

Note: this backend does NOT assume nor perform any kind of validation, so it is up to
caller to either ensure parameters are really G, A and V and to interpret results
correctly.

Some example use cases:
* "what is the latest version of GA?"
* "what classifier exists for GAV?"
* "is GAVCE accessible/synced to Maven Central"

It relies on following facts:
* GA directories have maven-metadata.xml listing all Vs.
* GAV directories, IF snapshot, have maven-metadata.xml otherwise have artifact files (subdirectories ignored).
* uses external checksums found in remote repository (SHA1, MD5) if needed.

Important notes:
* supports only "default" (aka Maven3) layout
* supports only release repositories currently
* does not support wildcards (so queries must contain full identifiers)

Examples:

```java
  RemoteRepositorySearchBackend backend = RemoteRepositorySearchBackendFactory.createDefault();
```
