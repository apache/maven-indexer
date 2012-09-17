Indexer CLI Notes
=================

This module produces the Indexer CLI tool, usable to index and publish indexes of a repository.

Example session:

```
$ java -jar indexer-cli-5.0.0.jar 

Use either unpack ("u") or index ("i" and "r") options, but none has been found!


usage: nexus-indexer [options]

Options:
 -q,--quiet          Quiet output - only show errors
 -D,--define         Define a system property
 -X,--debug          Produce execution debug output
 -c,--chunks         Create incremental chunks.
 -d,--destination    Target folder.
 -e,--errors         Produce execution error messages
 -h,--help           Display help information
 -i,--index          Path to the index folder.
 -k,--keep           Number of incremental chunks to keep.
 -l,--legacy         Build legacy .zip index file
 -n,--name           Repository name.
 -r,--repository     Path to the Maven repository.
 -s,--checksums      Create checksums for all files (sha1, md5).
 -t,--type           Indexer type (default, min, full or coma separated
                     list of custom types).
 -u,--unpack         Unpack an index file
 -v,--version        Display version information

$ cd repo-root/

$ java -jar indexer-cli-5.0.0.jar -i ./.index -r . -d ./.index -t full
Repository Folder: .../repo-root/.
Index Folder:      .../repo-root/.index
Output Folder:     .../repo-root/.index
Repository name:   repo-root
Indexers: [osgi-metadatas, maven-plugin, jarContent, maven-archetype, min]
Will not create checksum files.
Will create baseline file.
Scanning started
Artifacts added:   147
Artifacts deleted: 0
[INFO] Unable to read properties file, will force index regeneration
Total time:   0 sec
Final memory: 7M/62M

$
```
