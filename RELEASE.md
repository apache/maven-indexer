# Release cheat sheet

Check site build before starting.

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
