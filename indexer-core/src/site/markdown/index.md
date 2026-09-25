---
title: Introduction
author: 
  - Hervé Boutemy
---

<!--
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-->

# Maven :: Indexer Core

Indexer Core contains the core support for indexing Maven repositories, searching them, publishing and consuming remotely published indexes.

## Index Fields Reference

<!-- see org.apache.maven.index.ArtifactInfoRecord --> 
- `u`: Artifact unique groupId|artifactId|version|classifier|extension (or packaging) (as keyword, stored)

<!-- see org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator --> 
- `min` indexer type
    - `i`: Artifact info: packaging|lastModified|size|sourcesExists|javadocExists|signatureExists (not indexed, stored)
    - `g`: Artifact GroupID (as keyword)
    - `groupId`: Artifact GroupID (tokenized)
    - `a`: Artifact ArtifactID (as keyword)
    - `artifactId`: Artifact ArtifactID (tokenized)
    - `v`: Artifact Version (as keyword)
    - `version`: Artifact Version (tokenized)
    - `p`: Artifact Packaging/Extension (as keyword)
    - `l`: Artifact classifier (as keyword)
    - `n`: Artifact name (tokenized, stored)
    - `d`: Artifact description (tokenized, stored)
    - `m`: Artifact last modified (not indexed, stored)
    - `1`: Artifact SHA1 checksum (as keyword, stored)

<!-- see org.apache.maven.index.creator.JarFileContentsIndexCreator --> 
- `jarContent` indexer type
    - `classnames`: Artifact Classes (tokenized)
    - `c`: Artifact Classes (tokenized on newlines only)

<!-- see org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator --> 
- `maven-plugin` indexer type
    - `px`: MavenPlugin prefix (as keyword, stored)
    - `gx`: MavenPlugin goals (as keyword, stored)

<!-- see org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator --> 
- `maven-archetype` indexer type: no additional field

<!-- see org.apache.maven.index.creator.OsgiArtifactIndexCreator --> 
- `osgi-metadatas` indexer type
    - `Bundle-SymbolicName`: Bundle-SymbolicName (indexed, stored)
    - `Bundle-Version`: Bundle-Version (indexed, stored)
    - `Export-Package`: Export-Package (indexed, stored)
    - `Export-Service`: Export-Service (indexed, stored)
    - `Bundle-Description`: Bundle-Description (indexed, stored)
    - `Bundle-Name`: Bundle-Name (indexed, stored)
    - `Bundle-License`: Bundle-License (indexed, stored)
    - `Bundle-DocURL`: Bundle-DocURL (indexed, stored)
    - `Import-Package`: Import-Package (indexed, stored)
    - `Require-Bundle`: Require-Bundle (indexed, stored)

