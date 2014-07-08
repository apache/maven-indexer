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
 * Maven ontology.
 * 
 * @author cstamas
 */
public interface MAVEN
{
    /** Maven namespace */
    String MAVEN_NAMESPACE = "urn:maven#";

    Field REPOSITORY_ID = new Field( null, MAVEN_NAMESPACE, "repositoryId", "Artifact Repository ID" );

    Field GROUP_ID = new Field( null, MAVEN_NAMESPACE, "groupId", "Group ID" );

    Field ARTIFACT_ID = new Field( null, MAVEN_NAMESPACE, "artifactId", "Artifact ID" );

    Field VERSION = new Field( null, MAVEN_NAMESPACE, "version", "Version" );

    Field BASE_VERSION = new Field( null, MAVEN_NAMESPACE, "baseVersion", "Base Version" );

    Field CLASSNAMES = new Field( null, MAVEN_NAMESPACE, "classNames", "Artifact Classes" );

    // Artifact Packaging (extension for secondary artifacts!).
    Field PACKAGING = new Field( null, MAVEN_NAMESPACE, "packaging", "Packaging/Extension" );

    // Artifact extension
    Field EXTENSION = new Field( null, MAVEN_NAMESPACE, "extension", "Extension" );

    Field CLASSIFIER = new Field( null, MAVEN_NAMESPACE, "classifier", "Classifier" );

    // NAME: Artifact Name (from POM)
    Field NAME = new Field( null, MAVEN_NAMESPACE, "name", "Name" );

    // DESCRIPTION: Artifact Description (from POM)
    Field DESCRIPTION = new Field( null, MAVEN_NAMESPACE, "name", "Description" );

    // LAST_MODIFIED: Artifact Last Modified Timestamp (UTC millis)
    Field LAST_MODIFIED = new Field( null, MAVEN_NAMESPACE, "lastModified", "Last Modified Timestamp" );

    // SHA1: Artifact SHA1 checksum.
    Field SHA1 = new Field( null, MAVEN_NAMESPACE, "sha1", "SHA1 checksum" );

    // PLUGIN_PREFIX: MavenPlugin Artifact Plugin Prefix.
    Field PLUGIN_PREFIX = new Field( null, MAVEN_NAMESPACE, "pluginPrefix", "Plugin Prefix" );

    // PLUGIN_GOALS: MavenPlugin Artifact Plugin Goals (list of strings)
    Field PLUGIN_GOALS = new Field( null, MAVEN_NAMESPACE, "pluginGoals", "Plugin Goals" );
}
