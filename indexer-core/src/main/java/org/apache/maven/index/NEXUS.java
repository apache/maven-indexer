package org.apache.maven.index;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0    
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Ontology of Nexus. This is still Maven2 specific. Ideally, Nexus Ontology should contain three things only: path,
 * sha1 and last_modified. And Indexer should index _everything_, and Maven should be just "topping" extending these
 * informations. This would enable us to search and easily detect maven metadata files too, or to support non-maven
 * repository indexing, like P2 is.
 * 
 * @author cstamas
 */
public interface NEXUS
{
    String NEXUS_NAMESPACE = "urn:nexus#";

    // UINFO: Artifact Unique Info (groupId, artifactId, version, classifier, extension (or packaging))
    Field UINFO = new Field( null, NEXUS_NAMESPACE, "uinfo", "Artifact Unique Info" );

    // INFO: Artifact Info (packaging, lastModified, size, sourcesExists, javadocExists, signatureExists)
    Field INFO = new Field( null, NEXUS_NAMESPACE, "info", "Artifact Info" );

    // DELETED: Deleted field marker (will contain UINFO if document is deleted from index)
    Field DELETED = new Field( null, NEXUS_NAMESPACE, "del", "Deleted field marker" );
}
