<?xml version="1.0"?>

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

<document xmlns="http://maven.apache.org/XDOC/2.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/XDOC/2.0 http://maven.apache.org/xsd/xdoc-2.0.xsd">

  <properties>
    <title>Introduction</title>
    <author email="hboutemy_AT_apache_DOT_org">Hervé Boutemy</author>
  </properties>

  <body>

    <section name="Maven :: Indexer Core">

      <p>Indexer Core contains the core support for indexing Maven repositories, searching them, publishing and consuming remotely
    published indexes.</p>

    <subsection name="Index Fields Reference">

  <ul>
    <!-- see org.apache.maven.index.ArtifactInfoRecord -->
    <li><code>u</code>: Artifact unique groupId|artifactId|version|classifier|extension (or packaging) (as keyword, stored)</li>

    <!-- see org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator -->
    <li><code>min</code> indexer type<ul>
      <li><code>i</code>: Artifact info: packaging|lastModified|size|sourcesExists|javadocExists|signatureExists (not indexed, stored)</li>
      <li><code>g</code>: Artifact GroupID (as keyword)</li>
      <li><code>groupId</code>: Artifact GroupID (tokenized)</li>
      <li><code>a</code>: Artifact ArtifactID (as keyword)</li>
      <li><code>artifactId</code>: Artifact ArtifactID (tokenized)</li>
      <li><code>v</code>: Artifact Version (as keyword)</li>
      <li><code>version</code>: Artifact Version (tokenized)</li>
      <li><code>p</code>: Artifact Packaging/Extension (as keyword)</li>
      <li><code>l</code>: Artifact classifier (as keyword)</li>
      <li><code>n</code>: Artifact name (tokenized, stored)</li>
      <li><code>d</code>: Artifact description (tokenized, stored)</li>
      <li><code>m</code>: Artifact last modified (not indexed, stored)</li>
      <li><code>1</code>: Artifact SHA1 checksum (as keyword, stored)</li>
    </ul></li>

    <!-- see org.apache.maven.index.creator.JarFileContentsIndexCreator -->
    <li><code>jarContent</code> indexer type<ul>
      <li><code>classnames</code>: Artifact Classes (tokenized)</li>
      <li><code>c</code>: Artifact Classes (tokenized on newlines only)</li>
    </ul></li>

    <!-- see org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator -->
    <li><code>maven-plugin</code> indexer type<ul>
      <li><code>px</code>: MavenPlugin prefix (as keyword, stored)</li>
      <li><code>gx</code>: MavenPlugin goals (as keyword, stored)</li>
    </ul></li>

    <!-- see org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator -->
    <li><code>maven-archetype</code> indexer type: no additional field</li>

    <!-- see org.apache.maven.index.creator.OsgiArtifactIndexCreator -->
    <li><code>osgi-metadatas</code> indexer type<ul>
      <li><code>Bundle-SymbolicName</code>: Bundle-SymbolicName (indexed, stored)</li>
      <li><code>Bundle-Version</code>: Bundle-Version (indexed, stored)</li>
      <li><code>Export-Package</code>: Export-Package (indexed, stored)</li>
      <li><code>Export-Service</code>: Export-Service (indexed, stored)</li>
      <li><code>Bundle-Description</code>: Bundle-Description (indexed, stored)</li>
      <li><code>Bundle-Name</code>: Bundle-Name (indexed, stored)</li>
      <li><code>Bundle-License</code>: Bundle-License (indexed, stored)</li>
      <li><code>Bundle-DocURL</code>: Bundle-DocURL (indexed, stored)</li>
      <li><code>Import-Package</code>: Import-Package (indexed, stored)</li>
      <li><code>Require-Bundle</code>: Require-Bundle (indexed, stored)</li>
    </ul></li>

  </ul>

    </subsection>
    </section>

  </body>

</document>
