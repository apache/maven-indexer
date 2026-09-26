<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# How Maven Indexer works

Maven Indexer builds a [Lucene](https://lucene.apache.org/) index of the artifacts in a Maven repository, and
publishes it in a compact transfer format that clients download to search the repository offline. Maven Central
publishes such an index at <https://repo.maven.apache.org/maven2/.index/>. IDEs use it to offer artifact search and
class-name lookup without querying a remote service.

This page explains the moving parts. For working code, see the
[basic example](https://github.com/apache/maven-indexer/tree/master/indexer-examples/indexer-examples-basic) and the
[Spring example](https://github.com/apache/maven-indexer/tree/master/indexer-examples/indexer-examples-spring).

## Modules

| Module | Use it to |
|---|---|
| `indexer-core` | Create, update, search and publish Lucene indexes. Most applications need only this. |
| `indexer-reader` | Read a published index file record by record, without Lucene. |
| `indexer-cli` | Index a repository directory and pack or unpack index files from the command line. |
| `search-api` and `search-backend-*` | Search through one API against Maven Central's search service (`smo`), a remote repository's directory listing (`remoterepository`) or a local `indexer-core` index (`indexer`). |

## Components

`indexer-core` is a set of JSR-330 components. Obtain them from a Sisu or Guice injector, or construct the default
implementations yourself:

- `Indexer` creates indexing contexts, searches them and adds or removes artifacts.
- `IndexUpdater` downloads a published index into a context.
- `IndexPacker` writes a context out in the published format.
- `Scanner` walks a repository directory and reports the artifacts it finds.
- `IndexCreator` implementations decide which fields are extracted from each artifact.

## Indexing contexts

Everything happens inside an `IndexingContext`, created with `Indexer.createIndexingContext`. A context has:

- an ID of its own and the ID of the repository it describes;
- the directory holding the Lucene index;
- optionally, the repository directory to scan, and the URL to download a published index from;
- the list of index creators applied when artifacts are added.

The index creators are identified by these IDs:

| ID | Adds |
|---|---|
| `min` | Coordinates, packaging, file size and date, SHA-1, name and description. `maven-plugin` and `maven-archetype` require it. |
| `jarContent` | The class names contained in the artifact. |
| `maven-plugin` | The goal prefix and goals of a Maven plugin. |
| `maven-archetype` | Recognizes archetype JARs by their descriptor and sets their packaging to `maven-archetype`. |
| `osgi-metadatas` | OSGi bundle headers, and a SHA-256 digest of bundles. |

The [indexer-core page](indexer-core/index.html) lists every field each creator adds.

Since 7.2.0, the file-system methods take and return `java.nio.file.Path`; the `java.io.File` variants are
deprecated.

## Filling an index

A context is filled in one of two ways.

**Download a published index.** Pass an `IndexUpdateRequest` to `IndexUpdater.fetchAndUpdateIndex`. The request
names the context and a `ResourceFetcher` that retrieves files from the remote `.index` directory. The first update
downloads the full index; later updates download only the incremental chunks published since, when they are
available.

**Scan a repository directory.** Pass a `ScanningRequest` to `Scanner.scan`. Each artifact found is turned into an
`ArtifactContext`, run through the context's index creators and added to the index. To add or remove single artifacts,
use `Indexer.addArtifactsToIndex` and `Indexer.deleteArtifactsFromIndex`.

## Searching

Build a query with `Indexer.constructQuery`, passing a field such as `MAVEN.GROUP_ID` and a search expression:

- `SourcedSearchExpression` matches the value exactly, for input from a program;
- `UserInputSearchExpression` normalizes the value and matches it as a prefix, for input typed by a person; results are
  scored rather than exact.

Combine queries with Lucene's `BooleanQuery.Builder`, then run them with one of these methods:

| Method | Returns |
|---|---|
| `searchFlat` | All hits as a set, for small result sizes. |
| `searchGrouped` | Hits grouped, for example by group and artifact ID. |
| `searchIterator` | Hits one at a time, filtered as they are read. |
| `identify` | The artifacts whose SHA-1 matches a given file. |

`searchIterator` returns an `IteratorSearchResponse` that holds index files open until it is closed. Always close
it, for example with try-with-resources:

```java
try (IteratorSearchResponse response = indexer.searchIterator(request)) {
    for (ArtifactInfo ai : response) {
        // ...
    }
}
```

## Publishing an index

`IndexPacker.packIndex` writes a context as `nexus-maven-repository-index.gz`, together with
`nexus-maven-repository-index.properties` that records its timestamp and chain of incremental chunks. With
incremental chunks enabled, each later run also writes a `nexus-maven-repository-index.N.gz` holding only the changes,
so clients can update without downloading the full index again. The [command-line tool](indexer-cli/index.html) wraps
scanning and packing for a repository directory.

## Reading an index without Lucene

`indexer-reader` reads the published files directly. `IndexReader` iterates over the chunks of an index, a
`ChunkReader` yields each record as a map of raw fields, and `RecordExpander` turns a raw record into a typed `Record`.
Use it to process every record of an index, for example to export it, without building a Lucene index first.
