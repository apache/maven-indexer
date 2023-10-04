<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Release cheat sheet

Check that site is build before starting (run mvn site).
Check that local repository is okay or nuke it. If local repo is OK, this command should have only one output:

```
$ find .m2/repository-oss -type f -exec stat --format='%A' '{}' \; | sort | uniq -c
```
Perform the release:

```
$ mvn release:prepare
$ mvn release:perform
```

Build and deploy site (to LATEST):

```
$ mvn -P reporting site site:stage
$ mvn scm-publish:publish-scm
```

Email the vote.

## Post-vote steps

Email the vote result. If succeeded, continue, otherwise cleanup.

Release staged artifacts, wait for sync. Once synced, continue.

JIRA: release the version.

Github make a release and point it to JIRA release notes.

Copy source release to dist area:

```
$ svn add indexer/maven-indexer-6.2.1-source-release.zip
$ svn add indexer/maven-indexer-6.2.1-source-release.zip.asc 
$ svn add indexer/maven-indexer-6.2.1-source-release.zip.sha512 
$ svn rm indexer/maven-indexer-6.2.0-source-release.zip
$ svn rm indexer/maven-indexer-6.2.0-source-release.zip.asc
$ svn rm indexer/maven-indexer-6.2.0-source-release.zip.sha512
$ svn commit
```

Copy site to final place (from LATEST):

```
$ svnmucc -m "Publish maven-indexer 6.2.1 documentation" \
  -U https://svn.apache.org/repos/asf/maven/website/components \
  cp HEAD maven-indexer-archives/maven-indexer-LATEST maven-indexer-archives/maven-indexer-6.2.1 \
  rm maven-indexer \
  cp HEAD maven-indexer-archives/maven-indexer-LATEST maven-indexer
```

Email the announce.
