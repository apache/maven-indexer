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
package org.apache.maven.search.api;

import org.apache.maven.search.api.request.Field.BooleanField;
import org.apache.maven.search.api.request.Field.NumberField;
import org.apache.maven.search.api.request.Field.StringField;

/**
 * The ontology of Apache Maven related fields.
 */
public final class MAVEN {
    private MAVEN() {
        // no instances
    }

    /**
     * String field for artifact groupId. Searchable.
     */
    public static final StringField GROUP_ID = new StringField("groupId");

    /**
     * String field for artifact artifactId. Searchable.
     */
    public static final StringField ARTIFACT_ID = new StringField("artifactId");

    /**
     * String field for artifact version. Searchable.
     */
    public static final StringField VERSION = new StringField("version");

    /**
     * String field for artifact classifier. Searchable.
     */
    public static final StringField CLASSIFIER = new StringField("classifier");

    /**
     * String field for artifact packaging. Searchable.
     */
    public static final StringField PACKAGING = new StringField("packaging");

    /**
     * String field for artifact contained Java class name. Searchable, but not present in resulting records.
     */
    public static final StringField CLASS_NAME = new StringField("cn");

    /**
     * String field for artifact contained FQ Java class name. Searchable, but not present in resulting records.
     */
    public static final StringField FQ_CLASS_NAME = new StringField("fqcn");

    /**
     * String field for artifact SHA1 checksum. Searchable, but not present in resulting records.
     */
    public static final StringField SHA1 = new StringField("sha1");

    /**
     * String field for artifact file extension. Non-searchable. Indexer backend specific.
     */
    public static final StringField FILE_EXTENSION = new StringField("fileExtension");

    /**
     * Number field carrying {@link Integer}, representing the count of versions for given GA. Non-searchable.
     */
    public static final NumberField VERSION_COUNT = new NumberField("versionCount");

    /**
     * Boolean field representing the known presence/absence of artifact sources (is {@code -sources.jar} present).
     * Non-searchable.
     */
    public static final BooleanField HAS_SOURCE = new BooleanField("source");

    /**
     * Boolean field representing the known presence/absence of artifact Javadoc (is {@code -javadoc.jar} present).
     * Non-searchable.
     */
    public static final BooleanField HAS_JAVADOC = new BooleanField("javadoc");

    /**
     * Boolean field representing the known presence/absence of artifact GPG signature. Non-searchable. Indexer
     * backend specific.
     */
    public static final BooleanField HAS_GPG_SIGNATURE = new BooleanField("gpg");
}
